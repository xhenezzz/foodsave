package com.example.FoodSave.order.service;

import com.example.FoodSave.auth.cfg.UserPrincipal;
import com.example.FoodSave.auth.entity.Role;
import com.example.FoodSave.auth.entity.User;
import com.example.FoodSave.auth.repo.UserRepository;
import com.example.FoodSave.foodlisting.entity.FoodListing;
import com.example.FoodSave.foodlisting.entity.ListingStatus;
import com.example.FoodSave.foodlisting.repo.FoodListingRepository;
import com.example.FoodSave.order.dto.CreateOrderRequest;
import com.example.FoodSave.order.dto.OrderQrResponse;
import com.example.FoodSave.order.dto.OrderResponse;
import com.example.FoodSave.order.entity.Order;
import com.example.FoodSave.order.entity.OrderStatus;
import com.example.FoodSave.order.repository.OrderRepository;
import com.example.FoodSave.payment.repo.PaymentRepository;
import com.example.FoodSave.loyalty.service.LoyaltyService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    public OrderResponse createOrder(
            CreateOrderRequest request
    ) {

        UserPrincipal userPrincipal =
                (UserPrincipal) SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getPrincipal();

        User user =
                userRepository.findById(userPrincipal.getId())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Пользователь не найден"
                                )
                        );

        FoodListing listing =
                foodListingRepository.findById(
                        request.getListingId()
                ).orElseThrow(() ->
                        new RuntimeException(
                                "Предложение не найдено"
                        )
                );

        // Проверяем статус объявления
        if (listing.getStatus() != ListingStatus.ACTIVE) {
            throw new RuntimeException(
                    "Это предложение больше недоступно"
            );
        }

        // Проверяем количество
        if (request.getQuantity() == null
                || request.getQuantity() <= 0) {

            throw new RuntimeException(
                    "Количество должно быть больше 0"
            );
        }

        // Проверяем наличие товара
        if (listing.getQuantity()
                < request.getQuantity()) {

            throw new RuntimeException(
                    "Недостаточно товара"
            );
        }

        // Рассчитываем стоимость
        BigDecimal totalPrice =
                listing.getFoodSavePrice()
                        .multiply(
                                BigDecimal.valueOf(
                                        request.getQuantity()
                                )
                        );

        LocalDateTime now =
                LocalDateTime.now();

        Order order = new Order();

        order.setUser(user);
        order.setListing(listing);
        order.setQuantity(request.getQuantity());
        order.setTotalPrice(totalPrice);

        order.setStatus(
                OrderStatus.RESERVED
        );

        order.setCreatedAt(now);

        // Бронь действует 15 минут
        order.setExpiresAt(
                now.plusMinutes(15)
        );

        order.setPickupStart(
                listing.getPickupStart()
        );

        order.setPickupEnd(
                listing.getPickupEnd()
        );

        // Уникальный QR token
        order.setQrToken(
                UUID.randomUUID().toString()
        );

        // Уменьшаем количество товара
        listing.setQuantity(
                listing.getQuantity()
                        - request.getQuantity()
        );

        // Если товар закончился
        if (listing.getQuantity() == 0) {

            listing.setStatus(
                    ListingStatus.SOLD_OUT
            );
        }

        foodListingRepository.save(listing);

        Order savedOrder =
                orderRepository.save(order);

        return toResponse(savedOrder);
    }

    public List<OrderResponse> getMyOrders() {

        UserPrincipal userPrincipal =
                (UserPrincipal) SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getPrincipal();

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

        UserPrincipal userPrincipal =
                (UserPrincipal) SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getPrincipal();

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
    public void cancelOrder(
            UUID id
    ) {

        UserPrincipal userPrincipal =
                (UserPrincipal) SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getPrincipal();

        Order order =
                orderRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Заказ не найден"
                                )
                        );

        // Проверяем владельца
        if (!order.getUser()
                .getId()
                .equals(userPrincipal.getId())) {

            throw new RuntimeException(
                    "Вы не можете отменить чужой заказ"
            );
        }

        // Отменять можно только RESERVED
        if (order.getStatus()
                != OrderStatus.RESERVED) {

            throw new RuntimeException(
                    "Этот заказ уже нельзя отменить"
            );
        }

        order.setStatus(
                OrderStatus.CANCELLED
        );

        // Возвращаем товар
        FoodListing listing =
                order.getListing();

        listing.setQuantity(
                listing.getQuantity()
                        + order.getQuantity()
        );

        // Если товар был SOLD_OUT,
        // снова делаем ACTIVE
        if (listing.getStatus()
                == ListingStatus.SOLD_OUT) {

            listing.setStatus(
                    ListingStatus.ACTIVE
            );
        }

        foodListingRepository.save(listing);
        orderRepository.save(order);
    }

    @Transactional
    @Scheduled(fixedRate = 60000)
    public void expireOrders() {

        List<Order> orders =
                orderRepository
                        .findByStatusAndExpiresAtBefore(
                                OrderStatus.RESERVED,
                                LocalDateTime.now()
                        );

        for (Order order : orders) {

            order.setStatus(
                    OrderStatus.EXPIRED
            );

            FoodListing listing =
                    order.getListing();

            // Возвращаем товар
            listing.setQuantity(
                    listing.getQuantity()
                            + order.getQuantity()
            );

            // Если товар закончился из-за брони,
            // возвращаем объявление в ACTIVE
            if (listing.getStatus()
                    == ListingStatus.SOLD_OUT) {

                listing.setStatus(
                        ListingStatus.ACTIVE
                );
            }

            foodListingRepository.save(listing);
        }

        orderRepository.saveAll(orders);
    }

    public OrderQrResponse getOrderQr(
            UUID id
    ) {

        UserPrincipal userPrincipal =
                (UserPrincipal) SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getPrincipal();

        Order order =
                orderRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Заказ не найден"
                                )
                        );

        // QR может получить только владелец заказа
        if (!order.getUser()
                .getId()
                .equals(userPrincipal.getId())) {

            throw new RuntimeException(
                    "Вы не можете получить QR чужого заказа"
            );
        }

        // QR только для активного заказа
        if (order.getStatus()
                != OrderStatus.RESERVED) {

            throw new RuntimeException(
                    "QR доступен только для активного заказа"
            );
        }

        // Проверяем оплату
        if (!paymentRepository
                .existsByOrderId(id)) {

            throw new RuntimeException(
                    "Сначала оплатите заказ"
            );
        }

        String qrData =
                "foodsave://order/"
                        + order.getQrToken();

        return new OrderQrResponse(
                qrData
        );
    }

    @Transactional
    public OrderResponse completeOrderByQr(
            String qrToken
    ) {

        UserPrincipal userPrincipal =
                (UserPrincipal) SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getPrincipal();

        Order order =
                orderRepository
                        .findByQrToken(qrToken)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "QR-код недействителен"
                                )
                        );

        // Заказ должен быть RESERVED
        if (order.getStatus()
                != OrderStatus.RESERVED) {

            throw new RuntimeException(
                    "Этот заказ уже нельзя завершить"
            );
        }

        // Только BUSINESS
        if (userPrincipal.getRole()
                != Role.BUSINESS) {

            throw new RuntimeException(
                    "Только бизнес может подтвердить заказ"
            );
        }

        FoodListing listing =
                order.getListing();

        if (!listing.getBusiness()
                .getUser()
                .getId()
                .equals(userPrincipal.getId())) {

            throw new RuntimeException(
                    "Вы не можете подтвердить заказ другого бизнеса"
            );
        }

        // Проверяем время получения
        LocalDateTime now =
                LocalDateTime.now();

        if (now.isBefore(
                order.getPickupStart()
        )) {

            throw new RuntimeException(
                    "Время получения заказа ещё не наступило"
            );
        }

        // Даём 15 минут после pickupEnd
        if (now.isAfter(
                order.getPickupEnd()
                        .plusMinutes(15)
        )) {

            throw new RuntimeException(
                    "Время получения заказа истекло"
            );
        }

        // Завершаем заказ
        order.setStatus(
                OrderStatus.COMPLETED
        );

        // Начисляем FoodPoints
        loyaltyService.orderCompleted(
                order.getUser()
        );

        userRepository.save(
                order.getUser()
        );

        Order savedOrder =
                orderRepository.save(order);

        return toResponse(savedOrder);
    }

    @Transactional
    @Scheduled(fixedRate = 60000)
    public void markNoShowOrders() {

        LocalDateTime deadline =
                LocalDateTime.now()
                        .minusMinutes(15);

        List<Order> orders =
                orderRepository
                        .findByStatusAndPickupEndBefore(
                                OrderStatus.RESERVED,
                                deadline
                        );

        for (Order order : orders) {

            // Если заказ не оплачен,
            // не считаем его NO_SHOW
            if (!paymentRepository
                    .existsByOrderId(
                            order.getId()
                    )) {

                continue;
            }

            order.setStatus(
                    OrderStatus.NO_SHOW
            );

            loyaltyService.orderNoShow(
                    order.getUser()
            );

            userRepository.save(
                    order.getUser()
            );
        }

        orderRepository.saveAll(orders);
    }

    private OrderResponse toResponse(
            Order order
    ) {

        FoodListing listing =
                order.getListing();

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
                order.getPickupEnd()
        );
    }
}