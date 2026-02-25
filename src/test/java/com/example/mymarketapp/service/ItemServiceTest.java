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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ItemServiceTest {

    @Mock private ItemRepository itemRepository;
    @Mock private CartService cartService;
    @InjectMocks private ItemService itemService;

    @Test
    void getPagedItemsReturnsCorrectFormat() {
        Item item1 = new Item();
        item1.setId(1L);
        item1.setTitle("Test");

        when(itemRepository.findAll(PageRequest.of(0, 5)))
                .thenReturn(new PageImpl<>(List.of(item1)));
        when(cartService.getCount(eq(1L), eq(1L))).thenReturn(0);

        List<List<ItemDto>> result = itemService.getPagedItems(null, "NO", 1, 5, 1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).hasSize(3);
        verify(itemRepository).findAll(PageRequest.of(0, 5));
        verify(cartService).getCount(eq(1L), eq(1L));
    }

    @Test
    void getPagedItemsWithSearchUsesSearchQuery() {
        Item item1 = new Item();
        item1.setId(1L);
        item1.setTitle("Test Match");

        PageImpl<Item> searchPage = new PageImpl<>(List.of(item1), PageRequest.of(0, 5), 7);

        when(itemRepository.findBySearch(eq("test"), eq(PageRequest.of(0, 5))))
                .thenReturn(searchPage);
        when(cartService.getCount(anyLong(), anyLong())).thenReturn(0);

        List<List<ItemDto>> result = itemService.getPagedItems("test", "NO", 1, 5, 1L);

        verify(itemRepository).findBySearch(eq("test"), eq(PageRequest.of(0, 5)));
        assertThat(result).hasSize(1);
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
        Item item = new Item();
        item.setId(1L);
        item.setTitle("Test");
        when(itemRepository.findById(eq(1L))).thenReturn(Optional.of(item));
        when(cartService.getCount(eq(1L), eq(1L))).thenReturn(2);

        ItemDto dto = itemService.getItemDto(1L, 1L);

        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.count()).isEqualTo(2);
    }

    @Test
    void getItemDtoInvalidUserIdThrowsException() {
        assertThatThrownBy(() -> itemService.getItemDto(1L, 0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Valid userId required");
    }

    @Test
    void getPagedItemsInvalidUserIdThrowsException() {
        assertThatThrownBy(() -> itemService.getPagedItems(null, "NO", 1, 5, 0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Valid userId required");
    }

    @Test
    void getItemNotFoundThrowsException() {
        when(itemRepository.findById(eq(999L))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> itemService.getItem(999L))
                .isInstanceOf(IllegalArgumentException.class);
    }
}