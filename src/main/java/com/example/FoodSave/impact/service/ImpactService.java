package com.example.FoodSave.impact.service;

import com.example.FoodSave.auth.cfg.UserPrincipal;
import com.example.FoodSave.auth.entity.User;
import com.example.FoodSave.auth.repo.UserRepository;
import com.example.FoodSave.impact.dto.ImpactResponse;
import com.example.FoodSave.order.entity.Order;
import com.example.FoodSave.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ImpactService {

    private final OrderRepository orderRepository;

    private static final BigDecimal CO2_PER_KG_FOOD =
            BigDecimal.valueOf(2.5);

    public ImpactResponse getMyImpact() {

        UserPrincipal userPrincipal =
                (UserPrincipal) SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getPrincipal();

        List<Order> completedOrders =
                orderRepository.findCompletedOrdersByUserId(
                        userPrincipal.getId()
                );

        BigDecimal foodSavedKg = BigDecimal.ZERO;
        BigDecimal moneySaved = BigDecimal.ZERO;

        for (Order order : completedOrders) {

            int quantity = order.getQuantity();

            BigDecimal weight =
                    order.getListing().getWeightKg();

            foodSavedKg = foodSavedKg.add(
                    weight.multiply(
                            BigDecimal.valueOf(quantity)
                    )
            );

            BigDecimal originalPrice =
                    order.getListing().getOriginalPrice();

            BigDecimal foodSavePrice =
                    order.getListing().getFoodSavePrice();

            BigDecimal savedPerItem =
                    originalPrice.subtract(foodSavePrice);

            moneySaved = moneySaved.add(
                    savedPerItem.multiply(
                            BigDecimal.valueOf(quantity)
                    )
            );
        }

        BigDecimal co2SavedKg =
                foodSavedKg.multiply(CO2_PER_KG_FOOD);

        return new ImpactResponse(
                foodSavedKg.setScale(2, RoundingMode.HALF_UP),
                moneySaved.setScale(2, RoundingMode.HALF_UP),
                co2SavedKg.setScale(2, RoundingMode.HALF_UP)
        );
    }
}