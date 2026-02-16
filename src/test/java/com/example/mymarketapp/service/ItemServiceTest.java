package com.example.mymarketapp.service;

import com.example.mymarketapp.dto.ItemDto;
import com.example.mymarketapp.dto.PagingDto;
import com.example.mymarketapp.entity.Item;
import com.example.mymarketapp.repository.ItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @Mock private ItemRepository itemRepository;
    @Mock private CartService cartService;
    @InjectMocks private ItemService itemService;

    @Test
    void getPagedItemsReturnsCorrectFormat() {
        Item item1 = new Item(); item1.setId(1L); item1.setTitle("Test");
        when(itemRepository.findAll(PageRequest.of(0, 5)))
                .thenReturn(new PageImpl<>(List.of(item1)));
        when(cartService.getCount(anyLong(), any())).thenReturn(0);

        List<List<ItemDto>> result = itemService.getPagedItems(null, "NO", 1, 5, cartService);

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).hasSize(3);
        verify(itemRepository).findAll(PageRequest.of(0, 5));
    }

    @Test
    void getPagedItemsWithSearchUsesSearchQuery() {
        Item item1 = new Item(); item1.setId(1L); item1.setTitle("Test Match");
        Item item2 = new Item(); item2.setId(2L); item2.setTitle("Test Phone");
        PageImpl<Item> searchPage = new PageImpl<>(List.of(item1, item2), PageRequest.of(0, 5), 7);
        when(itemRepository.findBySearch("%test%", PageRequest.of(0, 5)))
                .thenReturn(searchPage);
        when(cartService.getCount(anyLong(), any())).thenReturn(0);

        List<List<ItemDto>> result = itemService.getPagedItems("test", "NO", 1, 5, cartService);

        verify(itemRepository).findBySearch(eq("%test%"), eq(PageRequest.of(0, 5)));
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).hasSize(3);
    }

    @Test
    void getPagingCalculatesCorrectly() {
        when(itemRepository.count()).thenReturn(20L);

        PagingDto paging = itemService.getPaging(null, null, 2, 5);

        assertThat(paging.hasPrevious()).isTrue();
        assertThat(paging.hasNext()).isTrue();
        assertThat(paging.pageSize()).isEqualTo(5);
    }

    @Test
    void getItemDtoReturnsCorrectDto() {
        Item item = new Item(); item.setId(1L); item.setTitle("Test");
        when(itemRepository.findById(1L)).thenReturn(java.util.Optional.of(item));
        when(cartService.getCount(1L, null)).thenReturn(2);

        ItemDto dto = itemService.getItemDto(1L, cartService);

        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.count()).isEqualTo(2);
    }

    @Test
    void getItemNotFoundThrowsException() {
        when(itemRepository.findById(999L)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> itemService.getItem(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Товар с id=999 не найден");
    }
}
