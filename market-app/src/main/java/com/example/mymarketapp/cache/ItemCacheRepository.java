package com.example.mymarketapp.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ItemCacheRepository {

    private final ReactiveRedisTemplate<String, Object> redisTemplate;

    private static final String ITEM_KEY_PREFIX = "item:";
    private static final String ALL_ITEMS_KEY = "items:all";
    private static final Duration CACHE_TTL = Duration.ofMinutes(3);

    public Mono<CachedItem> getItem(Long id) {
        String key = ITEM_KEY_PREFIX + id;
        return redisTemplate.opsForValue()
                .get(key)
                .cast(CachedItem.class)
                .doOnNext(item -> log.debug("Cache HIT for item: {}", id))
                .doOnError(e -> log.error("Error getting item from cache: {}", id, e))
                .onErrorResume(e -> Mono.empty());
    }

    public Mono<Boolean> saveItem(CachedItem item) {
        String key = ITEM_KEY_PREFIX + item.getId();
        return redisTemplate.opsForValue()
                .set(key, item, CACHE_TTL)
                .doOnNext(success -> log.debug("Cached item: {}, success: {}", item.getId(), success))
                .doOnError(e -> log.error("Error caching item: {}", item.getId(), e))
                .onErrorReturn(false);
    }

    public Mono<List<CachedItem>> getAllItems() {
        return redisTemplate.opsForList()
                .range(ALL_ITEMS_KEY, 0, -1)
                .cast(CachedItem.class)
                .collectList()
                .filter(list -> !list.isEmpty())
                .doOnNext(items -> log.debug("Cache HIT for all items, count: {}", items.size()))
                .doOnError(e -> log.error("Error getting all items from cache", e))
                .onErrorResume(e -> Mono.empty());
    }

    public Mono<Boolean> saveAllItems(List<CachedItem> items) {
        return redisTemplate.delete(ALL_ITEMS_KEY)
                .then(redisTemplate.opsForList()
                        .rightPushAll(ALL_ITEMS_KEY, items.toArray())
                        .then(redisTemplate.expire(ALL_ITEMS_KEY, CACHE_TTL))
                )
                .doOnNext(success -> log.debug("Cached all items, count: {}, success: {}",
                        items.size(), success))
                .doOnError(e -> log.error("Error caching all items", e))
                .onErrorReturn(false);
    }

    public Mono<Void> clearCache() {
        return redisTemplate.keys(ITEM_KEY_PREFIX + "*")
                .flatMap(redisTemplate::delete)
                .then(redisTemplate.delete(ALL_ITEMS_KEY))
                .then()
                .doOnSuccess(v -> log.info("Item cache cleared"))
                .doOnError(e -> log.error("Error clearing cache", e))
                .onErrorResume(e -> Mono.empty());
    }
}