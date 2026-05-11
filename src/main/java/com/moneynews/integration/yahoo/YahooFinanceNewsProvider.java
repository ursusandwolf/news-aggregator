package com.moneynews.integration.yahoo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moneynews.domain.model.NewsItem;
import com.moneynews.domain.port.NewsProvider;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class YahooFinanceNewsProvider implements NewsProvider {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

    public YahooFinanceNewsProvider(ObjectMapper objectMapper) {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();
        this.objectMapper = objectMapper;
    }

    @Override
    public String getProviderName() {
        return "Yahoo Finance";
    }

    @Override
    public List<NewsItem> fetchNews(String ticker, int limit) {
        String url = String.format("https://query2.finance.yahoo.com/v1/finance/search?q=%s&newsCount=%d", ticker, limit);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", USER_AGENT)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.error("Failed to fetch news from Yahoo for {}: HTTP {}", ticker, response.statusCode());
                return Collections.emptyList();
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode newsNode = root.path("news");

            List<NewsItem> items = new ArrayList<>();
            for (JsonNode node : newsNode) {
                long publishTime = node.path("providerPublishTime").asLong();
                ZonedDateTime publishedAt = ZonedDateTime.ofInstant(Instant.ofEpochSecond(publishTime), ZoneId.of("UTC"));
                
                String uuidStr = node.path("uuid").asText();
                UUID uuid;
                try {
                    uuid = UUID.fromString(uuidStr);
                } catch (Exception e) {
                    uuid = UUID.randomUUID();
                }

                items.add(new NewsItem(
                        uuid,
                        ticker,
                        node.path("title").asText(),
                        "", // Summary not reliably available in search API
                        node.path("link").asText(),
                        node.path("publisher").asText(),
                        publishedAt,
                        null // Sentiment score will be calculated later
                ));
            }

            log.info("Successfully fetched {} news items from Yahoo for {}", items.size(), ticker);
            return items;
        } catch (Exception e) {
            log.error("Error fetching news from Yahoo for {}: {}", ticker, e.getMessage());
            return Collections.emptyList();
        }
    }
}
