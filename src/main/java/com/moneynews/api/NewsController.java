package com.moneynews.api;

import com.moneynews.application.NewsAggregationService;
import com.moneynews.domain.model.NewsItem;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/news")
@RequiredArgsConstructor
public class NewsController {

    private final NewsAggregationService aggregationService;
    private final com.moneynews.application.MarketContextService marketContextService;

    @GetMapping("/{ticker}")
    public List<NewsItem> getNews(
            @PathVariable String ticker,
            @RequestParam(defaultValue = "10") int limit) {
        return aggregationService.aggregateNews(ticker, limit);
    }

    @GetMapping("/market-context")
    public Map<String, List<NewsItem>> getMarketOverview(
            @RequestParam(defaultValue = "3") int limit) {
        return marketContextService.getMarketOverview(limit);
    }

    @GetMapping("/market-context/{category}")
    public List<NewsItem> getNewsForCategory(
            @PathVariable String category,
            @RequestParam(defaultValue = "10") int limit) {
        return marketContextService.getNewsForCategory(category, limit);
    }
    @GetMapping("/{ticker}/filings")
    public List<com.moneynews.domain.model.Filing> getFilings(
            @PathVariable String ticker,
            @RequestParam(defaultValue = "5") int limit) {
        return aggregationService.getFilings(ticker, limit);
    }

    @GetMapping("/macro")
    public List<com.moneynews.domain.model.MacroEvent> getMacro() {
        return aggregationService.getMacroEvents();
    }
}
