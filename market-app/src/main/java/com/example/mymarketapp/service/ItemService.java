package com.example.mymarketapp.service;

import com.example.mymarketapp.cache.CachedItem;
import com.example.mymarketapp.cache.ItemCacheRepository;
import com.example.mymarketapp.dto.ItemDto;
import com.example.mymarketapp.dto.PagingDto;
import com.example.mymarketapp.entity.Item;
import com.example.mymarketapp.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;
    private final CartService cartService;
    private final ItemCacheRepository itemCacheRepository;

    public Mono<Void> initData() {
        return itemRepository.count()
                .filter(count -> count == 0)
                .flatMapMany(ignore -> itemRepository.saveAll(initialItems()))
                .then()
                .doOnSuccess(v -> log.info("Initial data loaded"));
    }

    private List<Item> initialItems() {
        List<Item> items = new ArrayList<>();
        items.add(createItem("Футбольный мяч", "Кожаный футбольный мяч Nike",
                "https://via.placeholder.com/200x150/FF6B35/FFFFFF?text=Мяч", 1500L));
        items.add(createItem("Книга Java", "Spring Boot в действии",
                "https://via.placeholder.com/200x150/4ECDC4/FFFFFF?text=Книга", 2500L));
        items.add(createItem("Ноутбук Dell", "Dell XPS 13 i7",
                "https://via.placeholder.com/200x150/45B7D1/FFFFFF?text=Лаптоп", 120000L));
        items.add(createItem("iPhone 15", "Apple iPhone 15 Pro",
                "https://via.placeholder.com/200x150/F9CA24/000000?text=Телефон", 80000L));
        items.add(createItem("Клавиатура", "Механическая клавиатура Keychron",
                "https://via.placeholder.com/200x150/96CEB4/FFFFFF?text=Клавиатура", 5000L));

        IntStream.range(6, 26).forEach(i ->
                items.add(createItem("Товар №" + i, "Описание товара №" + i,
                        "https://via.placeholder.com/200x150/AAAAAA/FFFFFF?text=Т" + i, 1000L + i * 100)));
        return items;
    }

    private Item createItem(String title, String description, String imgPath, long price) {
        Item item = new Item();
        item.setTitle(title);
        item.setDescription(description);
        item.setImgPath(imgPath);
        item.setPrice(price);
        return item;
    }

    public Mono<List<List<ItemDto>>> getPagedItems(String search,
                                                   String sort,
                                                   int pageNumber,
                                                   int pageSize,
                                                   Long userId) {
        Sort sortObj = getSort(sort);
        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize, sortObj);

        if (search != null && !search.trim().isEmpty()) {
            return getItemsFromDb(search.trim(), pageable, userId);
        }

        return itemCacheRepository.getAllItems()
                .flatMap(cachedItems -> {
                    log.info("Using cached items, count: {}", cachedItems.size());
                    return processItemsWithPagination(
                            Flux.fromIterable(cachedItems),
                            sortObj,
                            pageNumber,
                            pageSize,
                            userId
                    );
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.info("Cache MISS, loading from DB");
                    return loadAndCacheAllItems()
                            .flatMap(items -> processItemsWithPagination(
                                    Flux.fromIterable(items),
                                    sortObj,
                                    pageNumber,
                                    pageSize,
                                    userId
                            ));
                }));
    }

    private Mono<List<CachedItem>> loadAndCacheAllItems() {
        return itemRepository.findAll()
                .map(this::itemToCachedItem)
                .collectList()
                .flatMap(items ->
                        itemCacheRepository.saveAllItems(items)
                                .thenReturn(items)
                );
    }

    private Mono<List<List<ItemDto>>> getItemsFromDb(String search,
                                                     Pageable pageable,
                                                     Long userId) {
        return itemRepository.findBySearch(search, pageable)
                .flatMap(item -> cartService.getCount(item.getId(), userId)
                        .map(count -> itemToDto(item, count)))
                .collectList()
                .map(this::groupIntoRowsOfThree);
    }

    private Mono<List<List<ItemDto>>> processItemsWithPagination(
            Flux<CachedItem> itemsFlux,
            Sort sort,
            int pageNumber,
            int pageSize,
            Long userId) {

        return itemsFlux
                .sort((a, b) -> sortComparatorCached(a, b, sort))
                .skip((long) (pageNumber - 1) * pageSize)
                .take(pageSize)
                .flatMap(cached -> cartService.getCount(cached.getId(), userId)
                        .map(count -> cachedItemToDto(cached, count)))
                .collectList()
                .map(this::groupIntoRowsOfThree);
    }

    public Mono<PagingDto> getPaging(String search, String sort, int pageNumber, int pageSize) {
        Mono<Long> totalItemsMono;

        if (search == null || search.trim().isEmpty()) {
            totalItemsMono = itemCacheRepository.getAllItems()
                    .map(items -> (long) items.size())
                    .switchIfEmpty(itemRepository.count());
        } else {
            totalItemsMono = itemRepository.countBySearch(search.trim());
        }

        return totalItemsMono.map(totalItems -> {
            int totalPages = (int) Math.ceil((double) totalItems / pageSize);
            return new PagingDto(pageSize, pageNumber, pageNumber > 1, pageNumber < totalPages);
        });
    }

    public Mono<ItemDto> getItemDto(Long id, Long userId) {
        return itemCacheRepository.getItem(id)
                .flatMap(cached -> {
                    log.info("Item {} loaded from cache", id);
                    return cartService.getCount(id, userId)
                            .map(count -> cachedItemToDto(cached, count));
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.info("Item {} not in cache, loading from DB", id);
                    return getItem(id)
                            .flatMap(item -> {
                                CachedItem cached = itemToCachedItem(item);
                                return itemCacheRepository.saveItem(cached)
                                        .then(cartService.getCount(id, userId))
                                        .map(count -> itemToDto(item, count));
                            });
                }));
    }

    public Flux<ItemDto> getCartItems(Long userId) {
        validateUserId(userId);
        return cartService.getCart(userId)
                .map(ci -> new ItemDto(
                        ci.getItemId(),
                        ci.getTitle(),
                        ci.getDescription(),
                        ci.getImgPath(),
                        ci.getPrice(),
                        ci.getCount()
                ));
    }

    public Mono<Item> getItem(Long id) {
        return itemRepository.findById(id)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Item not found: " + id)));
    }

    private void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("Valid userId required");
        }
    }

    private CachedItem itemToCachedItem(Item item) {
        return new CachedItem(
                item.getId(),
                item.getTitle(),
                item.getDescription(),
                item.getImgPath(),
                item.getPrice()
        );
    }

    private ItemDto itemToDto(Item item, int count) {
        return new ItemDto(
                item.getId(),
                item.getTitle(),
                item.getDescription(),
                item.getImgPath(),
                item.getPrice(),
                count
        );
    }

    private ItemDto cachedItemToDto(CachedItem cached, int count) {
        return new ItemDto(
                cached.getId(),
                cached.getTitle(),
                cached.getDescription(),
                cached.getImgPath(),
                cached.getPrice(),
                count
        );
    }

    private List<List<ItemDto>> groupIntoRowsOfThree(List<ItemDto> items) {
        List<List<ItemDto>> rows = new ArrayList<>();
        int size = items.size();
        int rowCount = (int) Math.ceil((double) size / 3);
        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            int start = rowIndex * 3;
            int end = Math.min(start + 3, size);
            List<ItemDto> row = new ArrayList<>(items.subList(start, end));
            while (row.size() < 3) {
                row.add(new ItemDto(-1L, null, null, null, 0L, 0));
            }
            rows.add(row);
        }
        return rows;
    }

    private Sort getSort(String sortParam) {
        return switch (sortParam != null ? sortParam.toUpperCase() : "NO") {
            case "ALPHA" -> Sort.by(Sort.Direction.ASC, "title");
            case "PRICE" -> Sort.by(Sort.Direction.ASC, "price");
            default -> Sort.unsorted();
        };
    }

    private int sortComparator(Item a, Item b, Sort sort) {
        if (sort.isUnsorted()) return 0;
        String property = sort.stream().findFirst().map(Sort.Order::getProperty).orElse("id");
        return switch (property) {
            case "title" -> a.getTitle().compareToIgnoreCase(b.getTitle());
            case "price" -> Long.compare(a.getPrice(), b.getPrice());
            default -> 0;
        };
    }

    private int sortComparatorCached(CachedItem a, CachedItem b, Sort sort) {
        if (sort.isUnsorted()) return 0;
        String property = sort.stream().findFirst().map(Sort.Order::getProperty).orElse("id");
        return switch (property) {
            case "title" -> a.getTitle().compareToIgnoreCase(b.getTitle());
            case "price" -> Long.compare(a.getPrice(), b.getPrice());
            default -> 0;
        };
    }
}