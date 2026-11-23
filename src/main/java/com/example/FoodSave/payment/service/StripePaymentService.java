package com.example.FoodSave.payment.service;

import com.example.FoodSave.auth.cfg.UserPrincipal;
import com.example.FoodSave.auth.entity.Role;
import com.example.FoodSave.foodlisting.service.ListingRules;
import com.example.FoodSave.order.entity.Order;
import com.example.FoodSave.order.entity.OrderStatus;
import com.example.FoodSave.order.repository.OrderRepository;
import com.example.FoodSave.payment.dto.PaymentResponse;
import com.example.FoodSave.payment.dto.StripeCheckoutSessionResponse;
import com.example.FoodSave.payment.entity.Payment;
import com.example.FoodSave.payment.entity.PaymentStatus;
import com.example.FoodSave.payment.repo.PaymentRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.Refund;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.net.Webhook;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StripePaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper;

    @Value("${foodsave.payments.stripe.secret-key:}")
    private String secretKey;

    @Value("${foodsave.payments.stripe.webhook-secret:}")
    private String webhookSecret;

    @Value("${foodsave.payments.stripe.currency:kzt}")
    private String configuredCurrency;

    @Value("${foodsave.payments.stripe.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Transactional
    public StripeCheckoutSessionResponse startCheckout(UUID orderId) {
        configureStripe();
        Order order = ownedCustomerOrder(orderId);
        validatePayable(order);

        String currency = configuredCurrency.trim().toLowerCase(Locale.ROOT);
        if (!"kzt".equals(currency)) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "FoodSave хранит цены в KZT. Не меняй STRIPE_CURRENCY без отдельной конвертации цен.");
        }
        long amountMinor = toMinorUnits(order.getTotalPrice());
        if (amountMinor <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Сумма заказа должна быть больше нуля");
        }

        Payment payment = paymentRepository.findByOrderId(orderId).orElse(null);
        if (payment != null && payment.getStatus() == PaymentStatus.SUCCESS) {
            return toCheckoutResponse(payment, null);
        }

        if (payment != null && payment.getStatus() == PaymentStatus.PENDING
                && payment.getStripeCheckoutSessionId() != null) {
            try {
                Session existingSession = Session.retrieve(payment.getStripeCheckoutSessionId());
                if ("open".equals(existingSession.getStatus())
                        && existingSession.getClientSecret() != null) {
                    return toCheckoutResponse(payment, existingSession.getClientSecret());
                }
                if ("complete".equals(existingSession.getStatus())
                        && "paid".equals(existingSession.getPaymentStatus())) {
                    applySuccessfulSession(existingSession);
                    Payment refreshed = paymentRepository.findByOrderId(orderId).orElse(payment);
                    return toCheckoutResponse(refreshed, null);
                }
                payment.setStatus(PaymentStatus.FAILED);
            } catch (StripeException exception) {
                log.error("Unable to retrieve existing Stripe Checkout Session {}",
                        payment.getStripeCheckoutSessionId(), exception);
                throw stripeUnavailable(exception);
            }
        }

        if (payment == null) {
            payment = new Payment();
            payment.setOrder(order);
        }
        payment.setAmount(order.getTotalPrice());
        payment.setCurrency(currency.toUpperCase(Locale.ROOT));
        payment.setStatus(PaymentStatus.PENDING);
        payment.setCreatedAt(almatyNow());
        payment.setStripeCheckoutSessionId(null);
        payment.setStripePaymentIntentId(null);
        payment = paymentRepository.saveAndFlush(payment);

        String returnUrl = frontendUrl.replaceAll("/+$", "")
                + "/orders?payment=return&orderId=" + orderId
                + "&session_id={CHECKOUT_SESSION_ID}";

        SessionCreateParams.LineItem.PriceData.ProductData productData =
                SessionCreateParams.LineItem.PriceData.ProductData.builder()
                        .setName(order.getListing().getTitle() + " × " + order.getQuantity())
                        .build();
        SessionCreateParams.LineItem.PriceData priceData =
                SessionCreateParams.LineItem.PriceData.builder()
                        .setCurrency(currency)
                        .setUnitAmount(amountMinor)
                        .setProductData(productData)
                        .build();
        SessionCreateParams.LineItem lineItem = SessionCreateParams.LineItem.builder()
                .setQuantity(1L)
                .setPriceData(priceData)
                .build();

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setUiMode(SessionCreateParams.UiMode.EMBEDDED_PAGE)
                .setRedirectOnCompletion(SessionCreateParams.RedirectOnCompletion.ALWAYS)
                .setReturnUrl(returnUrl)
                .setLocale(SessionCreateParams.Locale.RU)
                .setCustomerEmail(order.getUser().getEmail())
                .addAllowedPaymentMethodType(SessionCreateParams.AllowedPaymentMethodType.CARD)
                .setClientReferenceId(orderId.toString())
                .putMetadata("foodsave_order_id", orderId.toString())
                .putMetadata("foodsave_payment_id", payment.getId().toString())
                .addLineItem(lineItem)
                .build();

        try {
            Session session = Session.create(params);
            payment.setStripeCheckoutSessionId(session.getId());
            paymentRepository.save(payment);
            if (session.getClientSecret() == null || session.getClientSecret().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "Stripe не вернул секрет встроенной Checkout-сессии");
            }
            return toCheckoutResponse(payment, session.getClientSecret());
        } catch (StripeException exception) {
            log.error("Stripe Checkout Session creation failed for order {}", orderId, exception);
            throw stripeUnavailable(exception);
        }
    }

    @Transactional
    public PaymentResponse getPaymentStatus(UUID orderId) {
        Order order = ownedCustomerOrder(orderId);
        Payment payment = paymentRepository.findByOrderId(order.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Платёж для этого заказа ещё не создан"));
        return toPaymentResponse(payment);
    }

    @Transactional
    public void handleWebhook(String payload, String signature) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Stripe webhook secret не настроен");
        }

        final Event event;
        try {
            event = Webhook.constructEvent(payload, signature, webhookSecret);
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Некорректная подпись Stripe webhook", exception);
        }

        final JsonNode root;
        try {
            root = objectMapper.readTree(payload);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Некорректное тело webhook", exception);
        }
        JsonNode sessionObject = root.path("data").path("object");
        String sessionId = sessionObject.path("id").asText("");
        String foodSavePaymentId = sessionObject.path("metadata").path("foodsave_payment_id").asText("");
        if (sessionId.isBlank() || foodSavePaymentId.isBlank()
                || !event.getType().startsWith("checkout.session.")) {
            return;
        }

        switch (event.getType()) {
            case "checkout.session.completed", "checkout.session.async_payment_succeeded" -> {
                configureStripe();
                try {
                    applySuccessfulSession(Session.retrieve(sessionId));
                } catch (StripeException exception) {
                    log.error("Could not retrieve Stripe Checkout Session {}", sessionId, exception);
                    throw stripeUnavailable(exception);
                }
            }
            case "checkout.session.async_payment_failed", "checkout.session.expired" -> markSessionFailed(sessionId);
            default -> {
            }
        }
    }

    @Transactional
    @Scheduled(fixedRate = 60000)
    public void expireSessionsForExpiredOrders() {
        if (secretKey == null || secretKey.isBlank()) return;
        configureStripe();
        LocalDateTime now = almatyNow();
        List<Payment> pending = paymentRepository
                .findByStatusAndStripeCheckoutSessionIdIsNotNull(PaymentStatus.PENDING);

        for (Payment candidate : pending) {
            Order order = orderRepository.findByIdForUpdate(candidate.getOrder().getId()).orElse(null);
            if (order != null && order.getStatus() == OrderStatus.RESERVED
                    && now.isBefore(order.getExpiresAt()) && now.isBefore(order.getPickupEnd())) {
                continue;
            }
            Payment payment = paymentRepository.findByStripeCheckoutSessionIdForUpdate(
                    candidate.getStripeCheckoutSessionId()).orElse(null);
            if (payment == null || payment.getStatus() != PaymentStatus.PENDING) continue;
            try {
                Session session = Session.retrieve(payment.getStripeCheckoutSessionId());
                if ("complete".equals(session.getStatus()) && "paid".equals(session.getPaymentStatus())) {
                    applySuccessfulSession(session);
                } else {
                    if ("open".equals(session.getStatus())) session.expire();
                    payment.setStatus(PaymentStatus.FAILED);
                    paymentRepository.save(payment);
                }
            } catch (StripeException exception) {
                log.warn("Could not expire stale Stripe session {}", payment.getStripeCheckoutSessionId(), exception);
            }
        }
    }

    private void applySuccessfulSession(Session session) throws StripeException {
        Payment paymentSnapshot = paymentRepository.findByStripeCheckoutSessionId(session.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "Payment row for Stripe session " + session.getId() + " is not committed yet"));
        Order order = orderRepository.findByIdForUpdate(paymentSnapshot.getOrder().getId())
                .orElseThrow(() -> new IllegalStateException("Order for Stripe payment was not found"));
        Payment payment = paymentRepository.findByStripeCheckoutSessionIdForUpdate(session.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "Payment row for Stripe session " + session.getId() + " disappeared"));
        if (payment.getStatus() == PaymentStatus.SUCCESS) return;
        if (!"paid".equals(session.getPaymentStatus())) return;
        boolean amountMatches = session.getAmountTotal() != null
                && session.getAmountTotal() == toMinorUnits(payment.getAmount());
        boolean currencyMatches = session.getCurrency() != null
                && session.getCurrency().equalsIgnoreCase(payment.getCurrency());
        boolean orderStillPayable = order.getStatus() == OrderStatus.RESERVED
                && almatyNow().isBefore(order.getExpiresAt())
                && almatyNow().isBefore(order.getPickupEnd())
                && order.getListing().getExpiresAt().isAfter(almatyNow())
                && ListingRules.safeForPickup(order.getListing().getProductExpiresAt(), almatyNow());

        if (!amountMatches || !currencyMatches || !orderStillPayable) {
            refundAndFail(session, payment, order.getId());
            return;
        }

        payment.setStripePaymentIntentId(session.getPaymentIntent());
        payment.setStatus(PaymentStatus.SUCCESS);
        paymentRepository.save(payment);
    }

    private void refundAndFail(Session session, Payment payment, UUID orderId) throws StripeException {
        String paymentIntentId = session.getPaymentIntent();
        if (paymentIntentId == null || paymentIntentId.isBlank()) {
            throw new IllegalStateException("Paid Stripe session has no PaymentIntent; manual reconciliation is required");
        }
        RefundCreateParams refundParams = RefundCreateParams.builder()
                .setPaymentIntent(paymentIntentId)
                .putMetadata("foodsave_order_id", orderId.toString())
                .putMetadata("foodsave_reason", "order_unavailable_or_amount_mismatch")
                .build();
        RequestOptions options = RequestOptions.builder()
                .setIdempotencyKey("foodsave-refund-" + session.getId())
                .build();
        Refund.create(refundParams, options);
        payment.setStripePaymentIntentId(paymentIntentId);
        payment.setStatus(PaymentStatus.FAILED);
        paymentRepository.save(payment);
        log.warn("Stripe payment for unavailable order {} was automatically refunded", orderId);
    }

    private void markSessionFailed(String sessionId) {
        paymentRepository.findByStripeCheckoutSessionIdForUpdate(sessionId).ifPresent(payment -> {
            if (payment.getStatus() != PaymentStatus.SUCCESS) {
                payment.setStatus(PaymentStatus.FAILED);
                paymentRepository.save(payment);
            }
        });
    }

    private Order ownedCustomerOrder(UUID orderId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Требуется авторизация");
        }
        if (principal.getRole() != Role.CUSTOMER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Оплата доступна только покупателю");
        }
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Заказ не найден"));
        if (!order.getUser().getId().equals(principal.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Чужой заказ оплачивать нельзя");
        }
        return order;
    }

    private void validatePayable(Order order) {
        LocalDateTime now = almatyNow();
        if (order.getStatus() != OrderStatus.RESERVED
                || !now.isBefore(order.getExpiresAt())
                || !now.isBefore(order.getPickupEnd())
                || !order.getListing().getExpiresAt().isAfter(now)
                || !ListingRules.safeForPickup(order.getListing().getProductExpiresAt(), now)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Срок оплаты или годности предложения истёк");
        }
    }

    private void configureStripe() {
        if (secretKey == null || secretKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Stripe не настроен: добавь STRIPE_SECRET_KEY в .env backend");
        }
        Stripe.apiKey = secretKey;
    }

    private long toMinorUnits(BigDecimal amount) {
        try {
            return amount.setScale(2, RoundingMode.UNNECESSARY)
                    .movePointRight(2)
                    .longValueExact();
        } catch (ArithmeticException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Сумма заказа должна содержать не более двух знаков после запятой", exception);
        }
    }

    private StripeCheckoutSessionResponse toCheckoutResponse(Payment payment, String clientSecret) {
        return new StripeCheckoutSessionResponse(payment.getId(), payment.getOrder().getId(),
                payment.getAmount(), payment.getCurrency(), payment.getStatus(), clientSecret);
    }

    private PaymentResponse toPaymentResponse(Payment payment) {
        return new PaymentResponse(payment.getId(), payment.getOrder().getId(),
                payment.getAmount(), payment.getStatus(), payment.getCreatedAt());
    }

    private ResponseStatusException stripeUnavailable(StripeException exception) {
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                "Stripe временно не смог обработать платёж. Попробуй ещё раз.", exception);
    }

    private LocalDateTime almatyNow() {
        return LocalDateTime.now(ZoneId.of("Asia/Almaty"));
    }
}
