package com.example.mymarketapp.integration;

import com.example.mymarketapp.cache.CachedItem;
import com.example.mymarketapp.cache.ItemCacheRepository;
import com.example.mymarketapp.entity.Item;
import com.example.mymarketapp.repository.ItemRepository;
import com.example.mymarketapp.service.ItemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import reactor.test.StepVerifier;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.containers.GenericContainer;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class RedisIntegrationTest {

    @Container
    static GenericContainer<?> redis =
            new GenericContainer<>("redis:7-alpine")
                    .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    private ItemCacheRepository itemCacheRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private ItemService itemService;

    @BeforeEach
    void setUp() {
        itemCacheRepository.clearCache().block();
    }

    @Test
    void shouldCacheSingleItem() {
        CachedItem item = new CachedItem(1L, "Test Item", "Description",
                "http://image.jpg", 1000L);

        StepVerifier.create(itemCacheRepository.saveItem(item))
                .expectNext(true)
                .verifyComplete();

        StepVerifier.create(itemCacheRepository.getItem(1L))
                .assertNext(cached -> {
                    assertThat(cached.getId()).isEqualTo(1L);
                    assertThat(cached.getTitle()).isEqualTo("Test Item");
                    assertThat(cached.getPrice()).isEqualTo(1000L);
                })
                .verifyComplete();
    }

    @Test
    void shouldReturnEmptyWhenItemNotInCache() {
        StepVerifier.create(itemCacheRepository.getItem(999L))
                .verifyComplete();
    }

    @Test
    void shouldCacheAllItems() {
        List<CachedItem> items = List.of(
                new CachedItem(1L, "Item 1", "Desc 1", "img1.jpg", 100L),
                new CachedItem(2L, "Item 2", "Desc 2", "img2.jpg", 200L),
                new CachedItem(3L, "Item 3", "Desc 3", "img3.jpg", 300L)
        );

        StepVerifier.create(itemCacheRepository.saveAllItems(items))
                .expectNext(true)
                .verifyComplete();

        StepVerifier.create(itemCacheRepository.getAllItems())
                .assertNext(cachedItems -> {
                    assertThat(cachedItems).hasSize(3);
                    assertThat(cachedItems.get(0).getTitle()).isEqualTo("Item 1");
                    assertThat(cachedItems.get(1).getTitle()).isEqualTo("Item 2");
                    assertThat(cachedItems.get(2).getTitle()).isEqualTo("Item 3");
                })
                .verifyComplete();
    }

    @Test
    void shouldClearCache() {
        CachedItem item = new CachedItem(1L, "Item", "Desc", "img.jpg", 100L);
        itemCacheRepository.saveItem(item).block();

        StepVerifier.create(itemCacheRepository.clearCache())
                .verifyComplete();

        StepVerifier.create(itemCacheRepository.getItem(1L))
                .verifyComplete();
    }

    @Test
    void shouldLoadItemFromDbAndCache() {
        Item item = new Item();
        item.setTitle("DB Item");
        item.setDescription("From Database");
        item.setImgPath("db.jpg");
        item.setPrice(5000L);

        Item savedItem = itemRepository.save(item).block();

        StepVerifier.create(itemService.getItemDto(savedItem.getId(), 1L))
                .assertNext(dto -> {
                    assertThat(dto.title()).isEqualTo("DB Item");
                    assertThat(dto.price()).isEqualTo(5000L);
                })
                .verifyComplete();

        StepVerifier.create(itemCacheRepository.getItem(savedItem.getId()))
                .assertNext(cached -> {
                    assertThat(cached.getTitle()).isEqualTo("DB Item");
                    assertThat(cached.getPrice()).isEqualTo(5000L);
                })
                .verifyComplete();
    }

    @Test
    void shouldUseCache_WhenItemAlreadyCached() {
        CachedItem cachedItem = new CachedItem(100L, "Cached Item",
                "Already in cache", "cached.jpg", 9999L);
        itemCacheRepository.saveItem(cachedItem).block();

        StepVerifier.create(itemService.getItemDto(100L, 1L))
                .assertNext(dto -> {
                    assertThat(dto.title()).isEqualTo("Cached Item");
                    assertThat(dto.price()).isEqualTo(9999L);
                })
                .verifyComplete();

        StepVerifier.create(itemRepository.findById(100L))
                .verifyComplete();
    }
}