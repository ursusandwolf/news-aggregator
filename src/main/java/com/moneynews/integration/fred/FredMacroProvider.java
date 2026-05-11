package com.moneynews.integration.fred;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moneynews.domain.model.MacroEvent;
import com.moneynews.domain.port.MacroProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class FredMacroProvider implements MacroProvider {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;

    // Key macroeconomic series to track
    private static final Map<String, String> SERIES_MAP = Map.of(
        "FEDFUNDS", "Federal Funds Effective Rate",
        "CPIAUCSL", "Consumer Price Index (Inflation)",
        "UNRATE", "Unemployment Rate",
        "GDP", "Gross Domestic Product",
        "T10Y2Y", "10-Year Treasury Constant Maturity Minus 2-Year Treasury"
    );

    public FredMacroProvider(
            ObjectMapper objectMapper,
            @Value("${moneynews.api.fred.key}") String apiKey) {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
    }

    @Override
    public List<MacroEvent> fetchLatestEvents() {
        List<MacroEvent> events = new ArrayList<>();

        for (Map.Entry<String, String> entry : SERIES_MAP.entrySet()) {
            String seriesId = entry.getKey();
            String title = entry.getValue();

            // FRED API: Get latest observation for a series
            String url = String.format(
                "https://api.stlouisfed.org/fred/series/observations?series_id=%s&api_key=%s&file_type=json&sort_order=desc&limit=1",
                seriesId, apiKey
            );

            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    JsonNode root = objectMapper.readTree(response.body());
                    JsonNode observation = root.path("observations").path(0);

                    if (!observation.isMissingNode()) {
                        events.add(new MacroEvent(
                            UUID.randomUUID(),
                            seriesId,
                            title,
                            observation.path("value").asText(),
                            observation.path("date").asText(),
                            "", // Units could be fetched from series metadata if needed
                            ""  // Frequency could be fetched from series metadata
                        ));
                    }
                } else {
                    log.error("FRED error for {}: HTTP {}", seriesId, response.statusCode());
                }
            } catch (Exception e) {
                log.error("Error calling FRED for {}: {}", seriesId, e.getMessage());
            }
        }

        return events;
    }
}
