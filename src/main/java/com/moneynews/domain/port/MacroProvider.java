package com.moneynews.domain.port;

import com.moneynews.domain.model.MacroEvent;
import java.util.List;

public interface MacroProvider {
    List<MacroEvent> fetchLatestEvents();
}
