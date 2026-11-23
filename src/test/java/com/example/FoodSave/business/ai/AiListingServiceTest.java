package com.example.FoodSave.business.ai;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class AiListingServiceTest {
    private HttpServer server;
    @AfterEach void close(){if(server != null)server.stop(0);}

    private AiListingService serverWith(int status, String response, AtomicReference<String> requestBody) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/v1beta/models/gemini-test:generateContent", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] bytes=response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            try (var out=exchange.getResponseBody()) {out.write(bytes);}
        });
        server.start();
        return new AiListingService("fake-key", "gemini-test",
                URI.create("http://127.0.0.1:"+server.getAddress().getPort()+"/v1beta/models/"),
                HttpClient.newHttpClient());
    }

    @Test void parsesStructuredGeminiAnswer() throws IOException {
        String response="{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"{\\\"title\\\":\\\"Круассаны\\\",\\\"description\\\":\\\"3 штуки\\\",\\\"suggestedCategory\\\":\\\"Выпечка\\\"}\"}]}}]}";
        var captured=new AtomicReference<String>();
        var service=serverWith(200,response,captured);
        var result=service.suggest(new AiListingRequest("Круассаны", "3 штуки", "ru"));
        assertEquals("Круассаны",result.title());
        assertEquals("Выпечка",result.suggestedCategory());
        assertTrue(captured.get().contains("responseMimeType"));
        assertTrue(captured.get().contains("application/json"));
    }


    @Test void analyzesPhotoAndSendsInlineImage() throws IOException {
        String response = """
                {"candidates":[{"content":{"parts":[{"text":"{\\"detectedProduct\\":\\"Круассан\\",\\"title\\":\\"Круассан с шоколадом\\",\\"description\\":\\"Круассан с шоколадной начинкой.\\",\\"suggestedCategory\\":\\"BAKERY\\",\\"ocrText\\":\\"Масса нетто 250 г\\",\\"label\\":{\\"brand\\":\\"\\",\\"netWeight\\":\\"250 г\\",\\"ingredients\\":\\"\\",\\"allergens\\":\\"\\",\\"expiryDateText\\":\\"\\",\\"storageConditions\\":\\"\\"},\\"warnings\\":[\\"Проверь состав на упаковке\\"]}"}]}}]}
                """;
        var captured = new AtomicReference<String>();
        var service = serverWith(200, response, captured);
        var result = service.analyze(new byte[]{1, 2, 3}, "image/jpeg", "ru", "3 штуки");

        assertEquals("Круассан с шоколадом", result.title());
        assertEquals(com.example.FoodSave.foodlisting.entity.FoodCategory.BAKERY, result.suggestedCategory());
        assertEquals("250 г", result.label().netWeight());
        assertEquals("Масса нетто 250 г", result.ocrText());
        assertTrue(result.warnings().contains("Проверь состав на упаковке"));
        assertTrue(captured.get().contains("inlineData"));
        assertTrue(captured.get().contains(Base64.getEncoder().encodeToString(new byte[]{1, 2, 3})));
        assertTrue(captured.get().contains("responseSchema"));
    }

    @Test void invalidImageMimeIsRejectedBeforeGeminiCall() throws IOException {
        var service = serverWith(200, "{}", new AtomicReference<>());
        var ex = assertThrows(ResponseStatusException.class,
                () -> service.analyze(new byte[]{1}, "text/plain", "ru", ""));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test void reportsQuotaError() throws IOException {
        var service=serverWith(429,"{}",new AtomicReference<>());
        var ex=assertThrows(ResponseStatusException.class,
                ()->service.suggest(new AiListingRequest("Bread", "", "en")));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS,ex.getStatusCode());
    }

    @Test void rejectsMalformedGeminiResponse() throws IOException {
        var service=serverWith(200,"{\"candidates\":[]}",new AtomicReference<>());
        var ex=assertThrows(ResponseStatusException.class,
                ()->service.suggest(new AiListingRequest("Bread", "", "en")));
        assertEquals(HttpStatus.BAD_GATEWAY,ex.getStatusCode());
    }

    @Test void reportsMissingKey() {
        var service = new AiListingService("", "gemini-test", URI.create("http://localhost/"), HttpClient.newHttpClient());
        var ex=assertThrows(ResponseStatusException.class,
                ()->service.suggest(new AiListingRequest("Bread", "", "en")));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE,ex.getStatusCode());
    }
}
