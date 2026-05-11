package com.moneynews.application;

import com.moneynews.domain.model.NewsItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketContextService {

    private final NewsAggregationService aggregationService;

    // Comprehensive mapping of all critical asset classes to their proxies
    private static final Map<String, String> MARKET_PROXIES = new LinkedHashMap<>() {{
        // Commodities
        put("GOLD", "GLD");
        put("SILVER", "SLV");
        put("LITHIUM", "LIT");
        put("WHEAT", "WEAT");
        put("COPPER", "CPER");
        put("URANIUM", "URA");
        
        // Fixed Income (Bonds)
        put("BONDS_LONG", "TLT");   // 20+ Year Treasury
        put("BONDS_TOTAL", "BND");  // Total Bond Market
        put("HIGH_YIELD", "JNK");   // Junk Bonds (Risk indicator)
        
        // Real Estate
        put("REITs", "VNQ");        // Vanguard Real Estate
        put("MORTGAGE", "REM");     // Mortgage REITs
        
        // Volatility & Fear
        put("FEAR_INDEX", "VIX");   // Volatility Index
        
        // Crypto (via Proxies)
        put("BITCOIN", "IBIT");     // BlackRock Bitcoin Trust
        put("CRYPTO_MARKET", "BITW"); // Crypto Index Fund
    }};

    public Map<String, List<NewsItem>> getMarketOverview(int limitPerCategory) {
        log.info("Fetching global market context overview");
        
        return MARKET_PROXIES.entrySet().parallelStream().collect(Collectors.toMap(
            Map.Entry::getKey,
            entry -> aggregationService.aggregateNews(entry.getValue(), limitPerCategory),
            (oldValue, newValue) -> oldValue,
            LinkedHashMap::new
        ));
    }

    public List<NewsItem> getNewsForCategory(String category, int limit) {
        String ticker = MARKET_PROXIES.getOrDefault(category.toUpperCase(), category.toUpperCase());
        return aggregationService.aggregateNews(ticker, limit);
    }
}
