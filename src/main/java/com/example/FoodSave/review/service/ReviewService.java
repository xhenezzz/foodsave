package com.example.FoodSave.review.service;

import com.example.FoodSave.auth.cfg.UserPrincipal;
import com.example.FoodSave.auth.entity.User;
import com.example.FoodSave.auth.repo.UserRepository;
import com.example.FoodSave.business.entity.BusinessProfile;
import com.example.FoodSave.business.repo.BusinessProfileRepository;
import com.example.FoodSave.order.entity.Order;
import com.example.FoodSave.order.entity.OrderStatus;
import com.example.FoodSave.order.repository.OrderRepository;
import com.example.FoodSave.review.dto.CreateReviewRequest;
import com.example.FoodSave.review.dto.ReviewResponse;
import com.example.FoodSave.review.entity.Review;
import com.example.FoodSave.review.repo.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final BusinessProfileRepository businessProfileRepository;

    @Transactional
    public ReviewResponse createReview(CreateReviewRequest request) {

        UserPrincipal userPrincipal =
                (UserPrincipal) SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getPrincipal();

        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() ->
                        new RuntimeException("Заказ не найден"));

        // Проверяем, что заказ принадлежит текущему пользователю
        if (!order.getUser().getId()
                .equals(userPrincipal.getId())) {

            throw new RuntimeException(
                    "Вы не можете оставить отзыв на чужой заказ"
            );
        }

        // Отзыв можно оставить только после получения заказа
        if (order.getStatus() != OrderStatus.COMPLETED) {

            throw new RuntimeException(
                    "Отзыв можно оставить только после получения заказа"
            );
        }

        // Проверяем рейтинг
        if (request.getRating() == null
                || request.getRating() < 1
                || request.getRating() > 5) {

            throw new RuntimeException(
                    "Рейтинг должен быть от 1 до 5"
            );
        }

        // Проверяем, что отзыв на этот заказ ещё не оставляли
        if (reviewRepository.existsByOrderId(request.getOrderId())) {

            throw new RuntimeException(
                    "На этот заказ отзыв уже оставлен"
            );
        }

        User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() ->
                        new RuntimeException("Пользователь не найден")
                );

        // Теперь бизнес — это BusinessProfile
        BusinessProfile business =
                order.getListing().getBusiness();

        Review review = new Review();

        review.setOrder(order);
        review.setUser(user);
        review.setBusiness(business);
        review.setRating(request.getRating());
        review.setComment(request.getComment());
        review.setCreatedAt(LocalDateTime.now());

        Review savedReview = reviewRepository.save(review);

        return toResponse(savedReview);
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> getMyReviews() {

        UserPrincipal userPrincipal =
                (UserPrincipal) SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getPrincipal();

        BusinessProfile businessProfile =
                businessProfileRepository
                        .findByUserId(userPrincipal.getId())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Профиль бизнеса не найден"
                                )
                        );

        List<Review> reviews =
                reviewRepository.findByBusinessIdOrderByCreatedAtDesc(
                        businessProfile.getId()
                );

        return reviews.stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Double getMyRating() {

        UserPrincipal userPrincipal =
                (UserPrincipal) SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getPrincipal();

        BusinessProfile businessProfile =
                businessProfileRepository
                        .findByUserId(userPrincipal.getId())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Профиль бизнеса не найден"
                                )
                        );

        Double average =
                reviewRepository.getAverageRating(
                        businessProfile.getId()
                );

        if (average == null) {
            return 0.0;
        }

        return Math.round(average * 10.0) / 10.0;
    }

    private ReviewResponse toResponse(Review review) {

        return new ReviewResponse(
                review.getId(),
                review.getOrder().getId(),
                review.getBusiness().getId(),
                review.getUser().getUsername(),
                review.getRating(),
                review.getComment(),
                review.getCreatedAt()
        );
    }
}