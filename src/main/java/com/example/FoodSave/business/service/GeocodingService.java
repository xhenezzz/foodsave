package com.example.FoodSave.business.service;

import com.example.FoodSave.business.dto.NominatimResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
public class GeocodingService {

    private final WebClient.Builder webClientBuilder;

    public Coordinates geocode(String address) {

        NominatimResponse[] response =
                webClientBuilder
                        .baseUrl("https://nominatim.openstreetmap.org")
                        .defaultHeader(
                                HttpHeaders.USER_AGENT,
                                "FoodSave/1.0"
                        )
                        .build()
                        .get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/search")
                                .queryParam("q", address)
                                .queryParam("format", "json")
                                .queryParam("limit", 1)
                                .build()
                        )
                        .retrieve()
                        .bodyToMono(NominatimResponse[].class)
                        .block();

        if (response == null || response.length == 0) {
            throw new RuntimeException(
                    "Не удалось найти адрес: " + address
            );
        }

        double latitude =
                Double.parseDouble(response[0].getLatitude());

        double longitude =
                Double.parseDouble(response[0].getLongitude());

        return new Coordinates(latitude, longitude);
    }

    public record Coordinates(
            double latitude,
            double longitude
    ) {
    }
}