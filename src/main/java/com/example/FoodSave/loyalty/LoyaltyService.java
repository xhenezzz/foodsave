package com.example.FoodSave.loyalty.service;

import com.example.FoodSave.auth.entity.User;
import org.springframework.stereotype.Service;

@Service
public class LoyaltyService {

    public void addPoints(User user, int points) {

        int currentPoints =
                user.getFoodPoints() == null
                        ? 0
                        : user.getFoodPoints();

        user.setFoodPoints(currentPoints + points);
    }

    public void subtractPoints(User user, int points) {

        int currentPoints =
                user.getFoodPoints() == null
                        ? 0
                        : user.getFoodPoints();

        user.setFoodPoints(
                Math.max(0, currentPoints - points)
        );
    }

    public void orderCompleted(User user) {

        addPoints(user, 10);

        int completed =
                user.getCompletedOrders() == null
                        ? 0
                        : user.getCompletedOrders();

        user.setCompletedOrders(completed + 1);
    }

    public void orderNoShow(User user) {

        subtractPoints(user, 5);

        int noShows =
                user.getNoShowCount() == null
                        ? 0
                        : user.getNoShowCount();

        user.setNoShowCount(noShows + 1);
    }
}