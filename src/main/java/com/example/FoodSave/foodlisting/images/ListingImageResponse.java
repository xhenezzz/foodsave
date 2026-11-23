package com.example.FoodSave.foodlisting.images;

import java.util.UUID;

public record ListingImageResponse(UUID id, UUID listingId, String contentUrl, int sortOrder) {
    public static ListingImageResponse from(ListingImage image) {
        UUID listingId = image.getListing().getId();
        return new ListingImageResponse(image.getId(), listingId,
                "/api/listings/" + listingId + "/images/" + image.getId() + "/content",
                image.getSortOrder());
    }
}
