package com.example.FoodSave.order.service;

import com.example.FoodSave.auth.cfg.CurrentUser;
import com.example.FoodSave.auth.cfg.UserPrincipal;
import com.example.FoodSave.auth.entity.Role;
import com.example.FoodSave.auth.entity.User;
import com.example.FoodSave.auth.repo.UserRepository;
import com.example.FoodSave.foodlisting.entity.FoodListing;
import com.example.FoodSave.foodlisting.entity.ListingStatus;
import com.example.FoodSave.foodlisting.repo.FoodListingRepository;
import com.example.FoodSave.foodlisting.service.ListingRules;
import com.example.FoodSave.order.dto.CreateOrderRequest;
import com.example.FoodSave.order.dto.OrderQrResponse;
import com.example.FoodSave.order.dto.OrderResponse;
import com.example.FoodSave.order.entity.Order;
import com.example.FoodSave.order.entity.OrderStatus;
import com.example.FoodSave.order.repository.OrderRepository;
import com.example.FoodSave.payment.repo.PaymentRepository;
import com.example.FoodSave.payment.entity.PaymentStatus;
import com.example.FoodSave.loyalty.LoyaltyService;
import com.example.FoodSave.moderation.ModerationStatus;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final FoodListingRepository foodListingRepository;
    private final LoyaltyService loyaltyService;

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        UserPrincipal principal = CurrentUser.get();
        if (principal.getRole() != Role.CUSTOMER) {
            throw new IllegalStateException("Только покупатель может создавать заказы");
        }
        User user = userRepository.findByIdForUpdate(principal.getId())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        if (!loyaltyService.canCreateOrder(user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Бронирования временно ограничены до " + user.getBookingBlockedUntil());
        }

        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new IllegalArgumentException("Количество должно быть больше 0");
        }

        // Блокировка предотвращает двойное бронирование последних единиц товара.
        FoodListing listing = foodListingRepository.findByIdForUpdate(request.getListingId())
                .orElseThrow(() -> new RuntimeException("Предложение не найдено"));
        LocalDateTime now = almatyNow();

        if (listing.getStatus() != ListingStatus.ACTIVE
                || listing.getModerationStatus() != ModerationStatus.APPROVED
                || listing.getQuantity() < request.getQuantity()
                || !listing.getExpiresAt().isAfter(now)
                || !listing.getPickupEnd().isAfter(now)
                || !ListingRules.safeForPickup(listing.getProductExpiresAt(), now)
                || !listing.getProductExpiresAt().isAfter(listing.getPickupEnd().plusMinutes(15))) {
            throw new IllegalStateException("Предложение недоступно или недостаточно товара");
        }

        Order order = new Order();
        order.setUser(user);
        order.setListing(listing);
        order.setQuantity(request.getQuantity());
        order.setTotalPrice(listing.getFoodSavePrice()
                .multiply(BigDecimal.valueOf(request.getQuantity())));
        order.setOriginalUnitPriceAtOrder(listing.getOriginalPrice());
        order.setWeightKgPerUnitAtOrder(listing.getWeightKg());
        order.setStatus(OrderStatus.RESERVED);
        order.setCreatedAt(now);
        // Дедлайн на оплату не должен быть позже окончания выдачи.
        LocalDateTime paymentDeadline = now.plusMinutes(15);
        order.setExpiresAt(paymentDeadline.isBefore(listing.getPickupEnd())
                ? paymentDeadline : listing.getPickupEnd());
        order.setPickupStart(listing.getPickupStart());
        order.setPickupEnd(listing.getPickupEnd());
        order.setQrToken(UUID.randomUUID().toString());

        listing.setQuantity(listing.getQuantity() - request.getQuantity());
        if (listing.getQuantity() == 0) {
            listing.setStatus(ListingStatus.SOLD_OUT);
        }
        foodListingRepository.save(listing);
        loyaltyService.orderCreated(user);
        userRepository.save(user);
        return toResponse(orderRepository.save(order));
    }

    public List<OrderResponse> getMyOrders() {

        UserPrincipal userPrincipal = CurrentUser.get();

        List<Order> orders =
                orderRepository
                        .findByUserIdOrderByCreatedAtDesc(
                                userPrincipal.getId()
                        );

        return orders.stream()
                .map(this::toResponse)
                .toList();
    }

    public OrderResponse getOrderById(
            UUID id
    ) {

        UserPrincipal userPrincipal = CurrentUser.get();

        Order order =
                orderRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Заказ не найден"
                                )
                        );

        // Заказ может смотреть только его владелец
        if (!order.getUser()
                .getId()
                .equals(userPrincipal.getId())) {

            throw new RuntimeException(
                    "Вы не можете просматривать чужой заказ"
            );
        }

        return toResponse(order);
    }

    @Transactional
    public void cancelOrder(UUID id) {
        UserPrincipal principal = CurrentUser.get();
        Order order = orderRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new RuntimeException("Заказ не найден"));
        if (!order.getUser().getId().equals(principal.getId())) {
            throw new IllegalStateException("Вы не можете отменить чужой заказ");
        }
        if (order.getStatus() != OrderStatus.RESERVED) {
            throw new IllegalStateException("Этот заказ уже нельзя отменить");
        }
        // Пока нет возвратов через Stripe, оплаченный заказ не отменяем.
        if (hasSuccessfulPayment(id)) {
            throw new IllegalStateException("Оплаченный заказ нельзя отменить без возврата денег");
        }
        order.setStatus(OrderStatus.CANCELLED);
        restoreStock(order, almatyNow());
        orderRepository.save(order);
    }

    @Transactional
    @Scheduled(fixedRate = 60000)
    public void expireOrders() {
        LocalDateTime now = almatyNow();
        List<Order> candidates = orderRepository.findByStatusAndExpiresAtBefore(
                OrderStatus.RESERVED, now);
        for (Order candidate : candidates) {
            // Повторно читаем под блокировкой: статус мог измениться после поиска.
            Order order = orderRepository.findByIdForUpdate(candidate.getId())
                    .orElse(null);
            if (order == null || order.getStatus() != OrderStatus.RESERVED
                    || order.getExpiresAt().isAfter(now)
                    || hasSuccessfulPayment(order.getId())) {
                continue;
            }
            order.setStatus(OrderStatus.EXPIRED);
            restoreStock(order, now);
            orderRepository.save(order);
        }
    }

    public OrderQrResponse getOrderQr(UUID id) {
        UserPrincipal principal = CurrentUser.get();
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Заказ не найден"));
        if (!order.getUser().getId().equals(principal.getId())) {
            throw new IllegalStateException("Вы не можете получить QR чужого заказа");
        }
        if (order.getStatus() != OrderStatus.RESERVED) {
            throw new IllegalStateException("QR доступен только для активного заказа");
        }
        if (!hasSuccessfulPayment(id)) {
            throw new IllegalStateException("Сначала оплатите заказ");
        }
        LocalDateTime now = almatyNow();
        if (now.isAfter(order.getPickupEnd().plusMinutes(15))
                || !order.getListing().getExpiresAt().isAfter(now)
                || !ListingRules.safeForPickup(order.getListing().getProductExpiresAt(), now)) {
            throw new IllegalStateException("Время получения истекло или продукт больше не годен");
        }
        return new OrderQrResponse("foodsave://order/" + order.getQrToken());
    }

    @Transactional
    public OrderResponse completeOrderByQr(String qrToken) {
        UserPrincipal principal = CurrentUser.get();
        if (principal.getRole() != Role.BUSINESS) {
            throw new IllegalStateException("Только бизнес может подтвердить заказ");
        }
        // Два сканирования не могут дважды начислить FoodPoints.
        Order order = orderRepository.findByQrTokenForUpdate(qrToken)
                .orElseThrow(() -> new RuntimeException("QR-код недействителен"));
        if (order.getStatus() != OrderStatus.RESERVED) {
            throw new IllegalStateException("Этот заказ уже нельзя завершить");
        }
        if (!order.getListing().getBusiness().getUser().getId().equals(principal.getId())) {
            throw new IllegalStateException("Вы не можете подтвердить заказ другого бизнеса");
        }
        if (!hasSuccessfulPayment(order.getId())) {
            throw new IllegalStateException("Заказ не оплачен");
        }
        LocalDateTime now = almatyNow();
        if (now.isBefore(order.getPickupStart())
                || now.isAfter(order.getPickupEnd().plusMinutes(15))
                || !order.getListing().getExpiresAt().isAfter(now)
                || !ListingRules.safeForPickup(order.getListing().getProductExpiresAt(), now)) {
            throw new IllegalStateException("Выдача запрещена: время получения или срок годности истёк");
        }
        // Один пользователь может завершать разные заказы параллельно: блокируем его счётчики.
        User customer = userRepository.findByIdForUpdate(order.getUser().getId())
                .orElseThrow(() -> new IllegalStateException("Пользователь не найден"));
        order.setStatus(OrderStatus.COMPLETED);
        loyaltyService.orderCompleted(customer);
        userRepository.save(customer);
        return toResponse(orderRepository.save(order));
    }

    @Transactional
    @Scheduled(fixedRate = 60000)
    public void markNoShowOrders() {
        LocalDateTime deadline = almatyNow().minusMinutes(15);
        List<Order> candidates = orderRepository.findByStatusAndPickupEndBefore(
                OrderStatus.RESERVED, deadline);
        for (Order candidate : candidates) {
            Order order = orderRepository.findByIdForUpdate(candidate.getId())
                    .orElse(null);
            if (order == null || order.getStatus() != OrderStatus.RESERVED
                    || !order.getPickupEnd().isBefore(deadline)
                    || !hasSuccessfulPayment(order.getId())) {
                continue;
            }
            // Блокировка защищает FoodPoints и noShowCount от потерянных обновлений.
            User customer = userRepository.findByIdForUpdate(order.getUser().getId())
                    .orElseThrow(() -> new IllegalStateException("Пользователь не найден"));
            order.setStatus(OrderStatus.NO_SHOW);
            loyaltyService.orderNoShow(customer);
            userRepository.save(customer);
            orderRepository.save(order);
        }
    }

    private boolean hasSuccessfulPayment(UUID orderId) {
        return paymentRepository.existsByOrderIdAndStatus(orderId, PaymentStatus.SUCCESS);
    }

    private void restoreStock(Order order, LocalDateTime now) {
        // Блокируем объявление, чтобы конкурирующие отмены не теряли количество.
        FoodListing listing = foodListingRepository.findByIdForUpdate(order.getListing().getId())
                .orElseThrow(() -> new RuntimeException("Предложение не найдено"));
        listing.setQuantity(listing.getQuantity() + order.getQuantity());
        if (listing.getStatus() == ListingStatus.SOLD_OUT) {
            if (listing.getExpiresAt().isAfter(now) && listing.getPickupEnd().isAfter(now)
                    && ListingRules.safeForPickup(listing.getProductExpiresAt(), now)) {
                listing.setStatus(ListingStatus.ACTIVE);
            } else {
                listing.setStatus(ListingStatus.EXPIRED);
            }
        }
        foodListingRepository.save(listing);
    }

    private LocalDateTime almatyNow() {
        return LocalDateTime.now(ZoneId.of("Asia/Almaty"));
    }

    private OrderResponse toResponse(
            Order order
    ) {

        FoodListing listing =
                order.getListing();

        PaymentStatus paymentStatus = paymentRepository.findByOrderId(order.getId())
                .map(com.example.FoodSave.payment.entity.Payment::getStatus).orElse(null);

        return new OrderResponse(
                order.getId(),
                order.getUser().getFsId(),
                listing.getId(),
                listing.getTitle(),
                order.getQuantity(),
                order.getTotalPrice(),
                order.getStatus(),
                order.getCreatedAt(),
                order.getExpiresAt(),
                order.getPickupStart(),
                order.getPickupEnd(),
                paymentStatus == PaymentStatus.SUCCESS,
                paymentStatus
        );
    }
}
