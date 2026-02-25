package com.example.mymarketapp.service;

import com.example.mymarketapp.dto.ItemDto;
import com.example.mymarketapp.dto.PagingDto;
import com.example.mymarketapp.entity.Item;
import com.example.mymarketapp.repository.ItemRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class ItemService {
    private final ItemRepository itemRepository;
    private final CartService cartService;

    @PostConstruct
    private void initData() {
        if (itemRepository.count() == 0) {
            itemRepository.saveAll(List.of(
                    createItem("Футбольный мяч", "Кожаный футбольный мяч Nike",
                            "https://via.placeholder.com/200x150/FF6B35/FFFFFF?text=Мяч", 1500L),
                    createItem("Книга Java", "Spring Boot в действии",
                            "https://via.placeholder.com/200x150/4ECDC4/FFFFFF?text=Книга", 2500L),
                    createItem("Ноутбук Dell", "Dell XPS 13 i7",
                            "https://via.placeholder.com/200x150/45B7D1/FFFFFF?text=Лаптоп", 120000L),
                    createItem("iPhone 15", "Apple iPhone 15 Pro",
                            "https://via.placeholder.com/200x150/F9CA24/000000?text=Телефон", 80000L),
                    createItem("Клавиатура", "Механическая клавиатура Keychron",
                            "https://via.placeholder.com/200x150/96CEB4/FFFFFF?text=Клавиатура", 5000L)
            ));

            IntStream.range(6, 26).forEach(i ->
                    itemRepository.save(createItem("Товар №" + i, "Описание товара №" + i,
                            "https://via.placeholder.com/200x150/AAAAAA/FFFFFF?text=Т" + i, 1000L + i * 100)));
        }
    }

    private Item createItem(String title, String description, String imgPath, long price) {
        Item item = new Item();
        item.setTitle(title);
        item.setDescription(description);
        item.setImgPath(imgPath);
        item.setPrice(price);
        return item;
    }

    public List<List<ItemDto>> getPagedItems(String search, String sort, int pageNumber, int pageSize, Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("Valid userId required");
        }

        Sort sortObj = getSort(sort);
        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize, sortObj);
        Page<Item> page;

        if (search == null || search.trim().isEmpty()) {
            page = itemRepository.findAll(pageable);
        } else {
            page = itemRepository.findBySearch(search.trim(), pageable);
        }

        List<Item> content = page.getContent();
        List<Item> modifiableContent = new ArrayList<>(content);

        return IntStream.range(0, (int) Math.ceil((double) modifiableContent.size() / 3))
                .mapToObj(rowIndex -> {
                    int start = rowIndex * 3;
                    int end = Math.min(start + 3, modifiableContent.size());
                    List<Item> row = new ArrayList<>(modifiableContent.subList(start, end));
                    while (row.size() < 3) {
                        row.add(createStubItem());
                    }
                    return row.stream()
                            .map(item -> new ItemDto(
                                    item.getId(),
                                    item.getTitle(),
                                    item.getDescription(),
                                    item.getImgPath(),
                                    item.getPrice(),
                                    cartService.getCount(item.getId(), userId)
                            ))
                            .collect(Collectors.toList());
                })
                .collect(Collectors.toList());
    }

    public PagingDto getPaging(String search, String sort, int pageNumber, int pageSize) {
        long totalItems;
        if (search == null || search.trim().isEmpty()) {
            totalItems = itemRepository.count();
        } else {
            totalItems = itemRepository.findBySearch(search.trim(), PageRequest.of(0, 1)).getTotalElements();
        }

        int totalPages = (int) Math.ceil((double) totalItems / pageSize);
        return new PagingDto(pageSize, pageNumber, pageNumber > 1, pageNumber < totalPages);
    }

    public ItemDto getItemDto(Long id, Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("Valid userId required");
        }
        Item item = getItem(id);
        return new ItemDto(
                item.getId(),
                item.getTitle(),
                item.getDescription(),
                item.getImgPath(),
                item.getPrice(),
                cartService.getCount(id, userId)
        );
    }

    public List<ItemDto> getCartItems(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("Valid userId required");
        }
        return cartService.getCart(userId).stream()
                .map(ci -> new ItemDto(
                        ci.getItemId(),
                        ci.getTitle(),
                        ci.getDescription(),
                        ci.getImgPath(),
                        ci.getPrice(),
                        ci.getCount()
                ))
                .collect(Collectors.toList());
    }

    public Item getItem(Long id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Item not found: " + id));
    }

    private Item createStubItem() {
        Item stub = new Item();
        stub.setId(-1L);
        return stub;
    }

    private Sort getSort(String sortParam) {
        return switch (sortParam != null ? sortParam.toUpperCase() : "NO") {
            case "ALPHA" -> Sort.by(Sort.Direction.ASC, "title");
            case "PRICE" -> Sort.by(Sort.Direction.ASC, "price");
            default -> Sort.unsorted();
        };
    }
}