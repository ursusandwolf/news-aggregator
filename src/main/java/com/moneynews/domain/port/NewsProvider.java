package com.moneynews.domain.port;

import com.moneynews.domain.model.NewsItem;
import java.util.List;

public interface NewsProvider {
    List<NewsItem> fetchNews(String ticker, int limit);
    String getProviderName();
}
