package com.example.FoodSave.impact.service;

import com.example.FoodSave.auth.cfg.CurrentUser;
import com.example.FoodSave.auth.cfg.UserPrincipal;
import com.example.FoodSave.impact.dto.ImpactResponse;
import com.example.FoodSave.order.entity.Order;
import com.example.FoodSave.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ImpactService {

    private final OrderRepository orderRepository;

    private static final BigDecimal CO2_PER_KG_FOOD =
            BigDecimal.valueOf(2.5);

    @Transactional(readOnly = true)
    public ImpactResponse getMyImpact() {
        UserPrincipal principal = CurrentUser.get();
        List<Order> completed = orderRepository.findCompletedOrdersByUserId(principal.getId());

        BigDecimal foodSavedKg = BigDecimal.ZERO;
        BigDecimal moneySaved = BigDecimal.ZERO;

        for (Order order : completed) {
            // Require backfill of pre-existing orders before enabling strict snapshots.
            if (order.getWeightKgPerUnitAtOrder() == null
                    || order.getOriginalUnitPriceAtOrder() == null) {
                throw new IllegalStateException(
                        "Старый заказ не содержит исторической цены/веса. Выполните миграцию orders.");
            }
            BigDecimal quantity = BigDecimal.valueOf(order.getQuantity());
            foodSavedKg = foodSavedKg.add(
                    order.getWeightKgPerUnitAtOrder().multiply(quantity));
            moneySaved = moneySaved.add(
                    order.getOriginalUnitPriceAtOrder().multiply(quantity)
                            .subtract(order.getTotalPrice()));
        }

        return new ImpactResponse(
                foodSavedKg.setScale(2, RoundingMode.HALF_UP),
                moneySaved.setScale(2, RoundingMode.HALF_UP),
                foodSavedKg.multiply(CO2_PER_KG_FOOD)
                        .setScale(2, RoundingMode.HALF_UP)
        );
    }
}