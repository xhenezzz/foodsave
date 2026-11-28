package com.example.FoodSave.business.service;

import com.example.FoodSave.auth.entity.City;
import com.example.FoodSave.business.dto.NominatimResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class GeocodingService {

    private final WebClient.Builder webClientBuilder;

    public Coordinates geocode(
            City city,
            String address
    ) {

        String query =
                address + ", "
                        + city.getDisplayName()
                        + ", Kazakhstan";

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
                                .queryParam("q", query)
                                .queryParam("format", "json")
                                .queryParam("limit", 1)
                                .queryParam("countrycodes", "kz")
                                .build()
                        )
                        .retrieve()
                        .bodyToMono(NominatimResponse[].class)
                        .block();

        if (response == null || response.length == 0) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Не удалось найти адрес: " + query
            );
        }

        return coordinates(response[0]);
    }

    public String reverseGeocode(double latitude, double longitude) {
        NominatimResponse response = webClientBuilder
                .baseUrl("https://nominatim.openstreetmap.org")
                .defaultHeader(HttpHeaders.USER_AGENT, "FoodSave/1.0")
                .build()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/reverse")
                        .queryParam("lat", latitude)
                        .queryParam("lon", longitude)
                        .queryParam("format", "json")
                        .build())
                .retrieve()
                .bodyToMono(NominatimResponse.class)
                .block();

        if (response == null || response.getDisplayName() == null
                || response.getDisplayName().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Не удалось определить адрес для этой точки"
            );
        }
        return response.getDisplayName();
    }

    private static Coordinates coordinates(NominatimResponse response) {
        try {
            double latitude = Double.parseDouble(response.getLatitude());
            double longitude = Double.parseDouble(response.getLongitude());
            if (!Double.isFinite(latitude) || !Double.isFinite(longitude)
                    || latitude < -90 || latitude > 90
                    || longitude < -180 || longitude > 180) {
                throw new NumberFormatException("Coordinates outside valid range");
            }
            return new Coordinates(latitude, longitude);
        } catch (NumberFormatException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Сервис геокодирования вернул некорректные координаты",
                    exception
            );
        }
    }

    public record Coordinates(
            double latitude,
            double longitude
    ) {
    }
}