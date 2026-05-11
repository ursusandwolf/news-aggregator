package com.moneynews.domain.model;

import java.util.UUID;

public record MacroEvent(
    UUID id,
    String seriesId,
    String title,
    String value,
    String date,
    String units,
    String frequency
) {
    public MacroEvent {
        if (id == null) id = UUID.randomUUID();
    }
}
