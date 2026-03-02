package com.example.mymarketapp.service;

import com.example.mymarketapp.dto.ItemDto;
import com.example.mymarketapp.entity.Item;
import com.example.mymarketapp.repository.ItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class ItemServiceTest {

    private ItemRepository itemRepository;
    private CartService cartService;
    private ItemService itemService;

    @BeforeEach
    void setUp() {
        itemRepository = Mockito.mock(ItemRepository.class);
        cartService = Mockito.mock(CartService.class);
        itemService = new ItemService(itemRepository, cartService);
    }

    @Test
    void getItemDtoReturnsDto() {
        long itemId = 1L;
        long userId = 1L;

        Item item = new Item();
        item.setId(itemId);
        item.setTitle("Test");
        item.setDescription("Desc");
        item.setImgPath("/img");
        item.setPrice(1000L);

        when(itemRepository.findById(itemId)).thenReturn(Mono.just(item));
        when(cartService.getCount(itemId, userId)).thenReturn(Mono.just(3));

        StepVerifier.create(itemService.getItemDto(itemId, userId))
                .assertNext(dto -> {
                    assertThat(dto.id()).isEqualTo(itemId);
                    assertThat(dto.title()).isEqualTo("Test");
                    assertThat(dto.count()).isEqualTo(3);
                })
                .verifyComplete();
    }

    @Test
    void getPagedItemsReturnsRowsOfThree() {
        long userId = 1L;

        Item i1 = new Item(); i1.setId(1L); i1.setTitle("A"); i1.setPrice(100L);
        Item i2 = new Item(); i2.setId(2L); i2.setTitle("B"); i2.setPrice(200L);
        Item i3 = new Item(); i3.setId(3L); i3.setTitle("C"); i3.setPrice(300L);
        Item i4 = new Item(); i4.setId(4L); i4.setTitle("D"); i4.setPrice(400L);

        when(itemRepository.findAll()).thenReturn(Flux.just(i1, i2, i3, i4));
        when(cartService.getCount(anyLong(), eq(userId))).thenReturn(Mono.just(0));

        StepVerifier.create(itemService.getPagedItems(null, "NO", 1, 5, userId))
                .assertNext(rows -> {
                    assertThat(rows).hasSize(2);
                    assertThat(rows.get(0)).hasSize(3);
                    assertThat(rows.get(1)).hasSize(3);
                    ItemDto first = rows.get(0).get(0);
                    assertThat(first.id()).isEqualTo(1L);
                })
                .verifyComplete();
    }

    @Test
    void getPagingCalculatesFlags() {
        when(itemRepository.count()).thenReturn(Mono.just(12L));

        StepVerifier.create(itemService.getPaging(null, "NO", 2, 5))
                .assertNext(paging -> {
                    assertThat(paging.pageNumber()).isEqualTo(2);
                    assertThat(paging.hasPrevious()).isTrue();
                    assertThat(paging.hasNext()).isTrue();
                })
                .verifyComplete();
    }
}