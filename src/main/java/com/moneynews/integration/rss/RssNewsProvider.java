package com.moneynews.integration.rss;

import com.moneynews.domain.model.NewsItem;
import com.moneynews.domain.port.NewsProvider;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class RssNewsProvider implements NewsProvider {

    private static final Map<String, String> RSS_FEEDS = Map.of(
        "CNBC", "https://search.cnbc.com/rs/search/combinedcms/view.xml?partnerId=wrss01&id=10000664",
        "Reuters", "https://www.reutersagency.com/feed/?best-topics=business&post_type=best",
        "WSJ", "https://feeds.a.dj.com/rss/WSJVideoBusiness.xml"
    );

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

    @Override
    public String getProviderName() {
        return "RSS Aggregator";
    }

    @Override
    public List<NewsItem> fetchNews(String ticker, int limit) {
        List<NewsItem> allItems = new ArrayList<>();

        for (Map.Entry<String, String> entry : RSS_FEEDS.entrySet()) {
            String sourceName = entry.getKey();
            String feedUrl = entry.getValue();

            try {
                log.info("Fetching RSS feed from {} for {}", sourceName, ticker);
                SyndFeedInput input = new SyndFeedInput();
                // Rome's XmlReader handles encoding automatically
                SyndFeed feed = input.build(new XmlReader(new URL(feedUrl).openConnection().getInputStream()));

                List<NewsItem> sourceItems = feed.getEntries().stream()
                    .filter(e -> containsTicker(e, ticker))
                    .limit(limit)
                    .map(e -> mapToNewsItem(e, ticker, sourceName))
                    .toList();

                allItems.addAll(sourceItems);
            } catch (Exception e) {
                log.error("Failed to fetch RSS from {}: {}", sourceName, e.getMessage());
            }
        }

        return allItems;
    }

    private boolean containsTicker(SyndEntry entry, String ticker) {
        String content = (entry.getTitle() + " " + (entry.getDescription() != null ? entry.getDescription().getValue() : ""))
            .toLowerCase();
        
        // Simple heuristic: ticker is present as a word
        return content.contains(ticker.toLowerCase());
    }

    private NewsItem mapToNewsItem(SyndEntry entry, String ticker, String source) {
        ZonedDateTime publishedAt = entry.getPublishedDate() != null 
            ? ZonedDateTime.ofInstant(entry.getPublishedDate().toInstant(), ZoneId.of("UTC"))
            : ZonedDateTime.now();

        return new NewsItem(
            UUID.randomUUID(),
            ticker,
            entry.getTitle(),
            entry.getDescription() != null ? entry.getDescription().getValue() : "",
            entry.getLink(),
            source,
            publishedAt,
            null
        );
    }
}
