package com.moneynews.domain.port;

import com.moneynews.domain.model.Filing;
import java.util.List;

public interface FilingProvider {
    List<Filing> fetchFilings(String ticker, int limit);
}
