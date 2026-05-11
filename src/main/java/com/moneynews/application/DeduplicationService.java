package com.moneynews.application;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Slf4j
@Service
public class DeduplicationService {

    private final StringRedisTemplate redisTemplate;
    private static final String REDIS_KEY_PREFIX = "news:dedup:";
    private static final Pattern URL_CLEANUP_PATTERN = Pattern.compile("\\?.*$");

    public DeduplicationService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Checks if the news item is a duplicate based on URL or Title.
     * Returns true if it IS a duplicate.
     */
    public boolean isDuplicate(String ticker, String url, String title) {
        String canonicalUrl = URL_CLEANUP_PATTERN.matcher(url).replaceAll("");
        String urlKey = REDIS_KEY_PREFIX + "url:" + canonicalUrl.hashCode();
        
        // Level 1: URL Check
        if (Boolean.TRUE.equals(redisTemplate.hasKey(urlKey))) {
            return true;
        }

        // Level 2: Title Check (Simple Normalized Hash for now)
        String normalizedTitle = title.toLowerCase().replaceAll("[^a-z0-9]", "");
        String titleKey = REDIS_KEY_PREFIX + "title:" + ticker + ":" + normalizedTitle.hashCode();
        
        if (Boolean.TRUE.equals(redisTemplate.hasKey(titleKey))) {
            return true;
        }

        // Mark as seen
        redisTemplate.opsForValue().set(urlKey, "1", 48, TimeUnit.HOURS);
        redisTemplate.opsForValue().set(titleKey, "1", 48, TimeUnit.HOURS);
        
        return false;
    }
}
