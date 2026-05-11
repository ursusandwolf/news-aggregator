package com.moneynews.domain.model;

import java.time.ZonedDateTime;
import java.util.UUID;

public record NewsItem(
    UUID id,
    String ticker,
    String title,
    String summary,
    String url,
    String source,
    ZonedDateTime publishedAt,
    Double sentimentScore
) {
    public NewsItem {
        if (id == null) id = UUID.randomUUID();
    }
}
