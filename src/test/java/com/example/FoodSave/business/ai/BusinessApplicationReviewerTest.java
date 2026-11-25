package com.example.FoodSave.business.ai;

import com.example.FoodSave.auth.entity.City;
import com.example.FoodSave.auth.entity.User;
import com.example.FoodSave.business.entity.BusinessProfile;
import com.example.FoodSave.business.entity.BusinessType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class BusinessApplicationReviewerTest {
    private HttpServer server;

    @AfterEach
    void close() {
        if (server != null) server.stop(0);
    }

    private AiListingService geminiReturning(int status, String body) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1beta/models/gemini-test:generateContent", exchange -> {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            try (var out = exchange.getResponseBody()) { out.write(bytes); }
        });
        server.start();
        return new AiListingService("fake-key", "gemini-test",
                URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/v1beta/models/"),
                HttpClient.newHttpClient());
    }

    private static String geminiAnswer(String json) throws IOException {
        var mapper = new ObjectMapper();
        return "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":" + mapper.writeValueAsString(json) + "}]}}]}";
    }

    private static BusinessProfile application() {
        User owner = new User();
        owner.setCity(City.ALMATY);
        BusinessProfile profile = new BusinessProfile();
        profile.setUser(owner);
        profile.setBusinessName("Пекарня Нан");
        profile.setBusinessType(BusinessType.BAKERY);
        profile.setAddress("ул. Абая 10");
        profile.setLatitude(43.24);
        profile.setLongitude(76.9);
        return profile;
    }

    @Test
    void withoutGeminiKeyApplicationGoesToManualReview() {
        var reviewer = new BusinessApplicationReviewer(
                new AiListingService("", "gemini-test", URI.create("http://localhost/"), HttpClient.newHttpClient()));
        var review = reviewer.review(application());
        assertEquals(BusinessApplicationReviewer.UNAVAILABLE, review.verdict());
        assertFalse(review.notes().isEmpty());
    }

    @Test
    void parsesRecommendationAndNotes() throws IOException {
        var reviewer = new BusinessApplicationReviewer(geminiReturning(200,
                geminiAnswer("{\"recommendation\":\"review\",\"notes\":[\"Нет описания\",\"  \"]}")));
        var review = reviewer.review(application());
        assertEquals("REVIEW", review.verdict());
        assertEquals(1, review.notes().size());
        assertEquals("Нет описания", review.notesText());
    }

    @Test
    void unknownVerdictBecomesReview() throws IOException {
        var reviewer = new BusinessApplicationReviewer(geminiReturning(200,
                geminiAnswer("{\"recommendation\":\"MAYBE\",\"notes\":[]}")));
        assertEquals("REVIEW", reviewer.review(application()).verdict());
    }

    @Test
    void geminiFailureNeverBlocksTheApplication() throws IOException {
        var reviewer = new BusinessApplicationReviewer(geminiReturning(500, "{}"));
        assertEquals(BusinessApplicationReviewer.UNAVAILABLE, reviewer.review(application()).verdict());
    }
}
