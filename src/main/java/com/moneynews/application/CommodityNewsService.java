package com.moneynews.application;

import com.moneynews.domain.model.NewsItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommodityNewsService {

    private final NewsAggregationService aggregationService;

    // Mapping commodities to retail-accessible proxies (ETFs/ETNs/Tickers)
    private static final Map<String, String> COMMODITY_PROXIES = new HashMap<>() {{
        put("GOLD", "GLD");
        put("SILVER", "SLV");
        put("LITHIUM", "LIT");
        put("WHEAT", "WEAT");
        put("COPPER", "CPER");
        put("ALUMINUM", "JJU"); // ETN for Aluminum
        put("MINING", "XME");   // Metals & Mining ETF
        put("URANIUM", "URA");
        put("AGRICULTURE", "DBA");
    }};

    public Map<String, List<NewsItem>> getCommodityNews(int limitPerCommodity) {
        log.info("Fetching news for commodities using ETF/ETN proxies");
        
        return COMMODITY_PROXIES.entrySet().parallelStream().collect(Collectors.toMap(
            Map.Entry::getKey,
            entry -> aggregationService.aggregateNews(entry.getValue(), limitPerCommodity)
        ));
    }

    public List<NewsItem> getNewsForCommodity(String commodity, int limit) {
        String ticker = COMMODITY_PROXIES.getOrDefault(commodity.toUpperCase(), commodity.toUpperCase());
        return aggregationService.aggregateNews(ticker, limit);
    }
}
