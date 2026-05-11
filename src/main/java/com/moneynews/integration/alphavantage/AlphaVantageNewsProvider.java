package com.moneynews.integration.alphavantage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moneynews.domain.model.NewsItem;
import com.moneynews.domain.port.NewsProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class AlphaVantageNewsProvider implements NewsProvider {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    
    private static final DateTimeFormatter AV_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");

    public AlphaVantageNewsProvider(
            ObjectMapper objectMapper, 
            @Value("${moneynews.api.alpha-vantage.key}") String apiKey) {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
    }

    @Override
    public String getProviderName() {
        return "Alpha Vantage";
    }

    @Override
    public List<NewsItem> fetchNews(String ticker, int limit) {
        if ("demo".equals(apiKey)) {
            log.warn("Alpha Vantage API key is not configured, using demo mode.");
        }

        String url = String.format(
            "https://www.alphavantage.co/query?function=NEWS_SENTIMENT&tickers=%s&limit=%d&apikey=%s",
            ticker, limit, apiKey
        );

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.error("Alpha Vantage error for {}: HTTP {}", ticker, response.statusCode());
                return Collections.emptyList();
            }

            JsonNode root = objectMapper.readTree(response.body());
            
            // Check for API limits or errors in response body
            if (root.has("Note") || root.has("Information")) {
                log.warn("Alpha Vantage API message: {}", root.path("Note").asText(root.path("Information").asText()));
                return Collections.emptyList();
            }

            JsonNode feed = root.path("feed");
            List<NewsItem> items = new ArrayList<>();

            for (JsonNode node : feed) {
                String timeStr = node.path("time_published").asText();
                ZonedDateTime publishedAt = ZonedDateTime.now(); // Fallback
                try {
                    LocalDateTime ldt = LocalDateTime.parse(timeStr, AV_DATE_FORMAT);
                    publishedAt = ldt.atZone(ZoneId.of("UTC"));
                } catch (Exception e) {
                    log.warn("Failed to parse Alpha Vantage date: {}", timeStr);
                }

                // Extract specific ticker sentiment if available, else overall
                double sentiment = node.path("overall_sentiment_score").asDouble();
                JsonNode tickerSentiments = node.path("ticker_sentiment");
                for (JsonNode ts : tickerSentiments) {
                    if (ts.path("ticker").asText().equalsIgnoreCase(ticker)) {
                        sentiment = ts.path("ticker_sentiment_score").asDouble();
                        break;
                    }
                }

                items.add(new NewsItem(
                        UUID.randomUUID(),
                        ticker,
                        node.path("title").asText(),
                        node.path("summary").asText(),
                        node.path("url").asText(),
                        node.path("source").asText(),
                        publishedAt,
                        sentiment
                ));
            }

            log.info("Fetched {} sentiment-enabled news from Alpha Vantage for {}", items.size(), ticker);
            return items;

        } catch (Exception e) {
            log.error("Error calling Alpha Vantage for {}: {}", ticker, e.getMessage());
            return Collections.emptyList();
        }
    }
}
