package com.moneynews.integration.cryptomacro;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moneynews.domain.model.MacroEvent;
import com.moneynews.domain.port.MacroProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class CoinGeckoCryptoProvider implements MacroProvider {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private static final String GLOBAL_URL = "https://api.coingecko.com/api/v3/global";
    private static final String MARKETS_URL = "https://api.coingecko.com/api/v3/coins/markets?vs_currency=usd&ids=bitcoin,tether,usd-coin,spdr-s-p-500-etf-trust"; 
    // Note: CoinGecko uses IDs for everything. S&P 500 proxy is often used in crypto circles as a reference.

    public CoinGeckoCryptoProvider(ObjectMapper objectMapper) {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();
        this.objectMapper = objectMapper;
    }

    @Override
    public List<MacroEvent> fetchLatestEvents() {
        List<MacroEvent> events = new ArrayList<>();
        
        // 1. Fetch BTC Dominance
        try {
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(GLOBAL_URL)).GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode data = objectMapper.readTree(response.body()).path("data");
                double btcDominance = data.path("market_cap_percentage").path("btc").asDouble();
                
                events.add(new MacroEvent(
                    UUID.randomUUID(),
                    "BTC_DOMINANCE",
                    "Bitcoin Market Dominance",
                    String.format("%.2f%%", btcDominance),
                    "LATEST",
                    "%",
                    "REALTIME"
                ));
            }
        } catch (Exception e) {
            log.error("Failed to fetch BTC Dominance: {}", e.getMessage());
        }

        // 2. Fetch Stablecoin Supply & BTC vs S&P 500 Price Data
        try {
            // Fetching Bitcoin, Tether, and USD Coin
            String marketsRequestUrl = "https://api.coingecko.com/api/v3/coins/markets?vs_currency=usd&ids=bitcoin,tether,usd-coin";
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(marketsRequestUrl)).GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                JsonNode coins = objectMapper.readTree(response.body());
                long stablecoinSupply = 0;
                double btcChange = 0;

                for (JsonNode coin : coins) {
                    String id = coin.path("id").asText();
                    if (id.equals("tether") || id.equals("usd-coin")) {
                        stablecoinSupply += coin.path("market_cap").asLong();
                    }
                    if (id.equals("bitcoin")) {
                        btcChange = coin.path("price_change_percentage_24h").asDouble();
                    }
                }

                events.add(new MacroEvent(
                    UUID.randomUUID(),
                    "STABLECOIN_SUPPLY",
                    "Major Stablecoin Supply (USDT+USDC)",
                    String.format("$%.2fB", stablecoinSupply / 1_000_000_000.0),
                    "LATEST",
                    "USD",
                    "REALTIME"
                ));

                events.add(new MacroEvent(
                    UUID.randomUUID(),
                    "BTC_PERFORMANCE_24H",
                    "Bitcoin 24h Performance",
                    String.format("%.2f%%", btcChange),
                    "LATEST",
                    "%",
                    "REALTIME"
                ));
            }
        } catch (Exception e) {
            log.error("Failed to fetch Crypto Market data: {}", e.getMessage());
        }

        return events;
    }
}
