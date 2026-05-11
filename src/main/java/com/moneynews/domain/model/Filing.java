package com.moneynews.domain.model;

import java.time.ZonedDateTime;
import java.util.UUID;

public record Filing(
    UUID id,
    String ticker,
    String accessionNumber,
    String form,
    String filedAt,
    String reportUrl,
    String description
) {
    public Filing {
        if (id == null) id = UUID.randomUUID();
    }
}
