package com.moneynews.application;

import com.moneynews.domain.model.NewsItem;
import com.moneynews.domain.port.NewsProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsAggregationService {

    private final List<NewsProvider> providers;
    private final List<com.moneynews.domain.port.FilingProvider> filingProviders;
    private final DeduplicationService deduplicationService;
    
    // Using a virtual thread executor for parallel I/O
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public List<NewsItem> aggregateNews(String ticker, int limitPerProvider) {
        log.info("Aggregating news for {} from {} providers", ticker, providers.size());

        List<CompletableFuture<List<NewsItem>>> futures = providers.stream()
                .map(provider -> CompletableFuture.supplyAsync(
                        () -> provider.fetchNews(ticker, limitPerProvider), executor))
                .toList();

        return futures.stream()
                .map(CompletableFuture::join)
                .flatMap(List::stream)
                .filter(item -> !deduplicationService.isDuplicate(item.ticker(), item.url(), item.title()))
                .sorted((a, b) -> b.publishedAt().compareTo(a.publishedAt()))
                .collect(Collectors.toList());
    }

    public List<com.moneynews.domain.model.Filing> getFilings(String ticker, int limit) {
        return filingProviders.stream()
                .flatMap(provider -> provider.fetchFilings(ticker, limit).stream())
                .sorted((a, b) -> b.filedAt().compareTo(a.filedAt()))
                .limit(limit)
                .collect(Collectors.toList());
    }
}
