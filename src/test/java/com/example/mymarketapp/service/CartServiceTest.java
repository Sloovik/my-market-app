package com.example.mymarketapp.service;

import com.example.mymarketapp.dto.ActionDto;
import com.example.mymarketapp.entity.*;
import com.example.mymarketapp.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CartServiceTest {

    @Mock private CartRepository cartRepository;
    @Mock private CartItemRepository cartItemRepository;
    @Mock private ItemRepository itemRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private CartService cartService;

    private User testUser;
    private Cart testCart;
    private Item testItem;
    private CartItem testCartItem;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");

        testCart = new Cart();
        testCart.setId(1L);
        testCart.setUser(testUser);

        testItem = new Item();
        testItem.setId(1L);
        testItem.setTitle("Test Item");
        testItem.setPrice(1000L);

        testCartItem = new CartItem();
        testCartItem.setId(1L);
        testCartItem.setItemId(1L);
        testCartItem.setCount(2);
        testCartItem.setPrice(1000L);
    }

    @Test
    void getCartReturnsItems() {
        when(userRepository.existsById(eq(1L))).thenReturn(true);
        when(userRepository.findById(eq(1L))).thenReturn(Optional.of(testUser));
        when(cartRepository.findByUserId(eq(1L))).thenReturn(Optional.of(testCart));
        when(cartItemRepository.findByCartId(eq(1L))).thenReturn(List.of(testCartItem));

        List<CartItem> result = cartService.getCart(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCount()).isEqualTo(2);
        verify(userRepository).existsById(eq(1L));
        verify(userRepository).findById(eq(1L));
    }

    @Test
    void getCartUserNotFoundThrowsException() {
        when(userRepository.existsById(eq(1L))).thenReturn(false);

        assertThatThrownBy(() -> cartService.getCart(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("User not found: 1");
    }

    @Test
    void getTotalReturnsCorrectSum() {
        when(userRepository.existsById(eq(1L))).thenReturn(true);
        when(userRepository.findById(eq(1L))).thenReturn(Optional.of(testUser));
        when(cartRepository.findByUserId(eq(1L))).thenReturn(Optional.of(testCart));
        when(cartItemRepository.findByCartId(eq(1L))).thenReturn(List.of(testCartItem));

        long total = cartService.getTotal(1L);

        assertThat(total).isEqualTo(2000L);
    }

    @Test
    void getCountReturnsCorrectCount() {
        when(userRepository.existsById(eq(1L))).thenReturn(true);
        when(userRepository.findById(eq(1L))).thenReturn(Optional.of(testUser));
        when(cartRepository.findByUserId(eq(1L))).thenReturn(Optional.of(testCart));
        when(cartItemRepository.findByCartId(eq(1L))).thenReturn(List.of(testCartItem));

        int count = cartService.getCount(1L, 1L);

        assertThat(count).isEqualTo(2);
    }

    @Test
    void updateCartDeleteRemovesItem() {
        when(userRepository.existsById(eq(1L))).thenReturn(true);
        when(userRepository.findById(eq(1L))).thenReturn(Optional.of(testUser));
        when(cartRepository.findByUserId(eq(1L))).thenReturn(Optional.of(testCart));
        when(cartItemRepository.findByCartIdAndItemId(eq(1L), eq(1L))).thenReturn(Optional.of(testCartItem));

        cartService.updateCart(1L, 1L, ActionDto.DELETE);

        verify(cartItemRepository).delete(testCartItem);
    }

    @Test
    void updateCartPlusIncreasesCount() {
        when(userRepository.existsById(eq(1L))).thenReturn(true);
        when(userRepository.findById(eq(1L))).thenReturn(Optional.of(testUser));
        when(cartRepository.findByUserId(eq(1L))).thenReturn(Optional.of(testCart));
        when(cartItemRepository.findByCartIdAndItemId(eq(1L), eq(1L))).thenReturn(Optional.of(testCartItem));

        cartService.updateCart(1L, 1L, ActionDto.PLUS);

        assertThat(testCartItem.getCount()).isEqualTo(3);
        verify(cartItemRepository).save(testCartItem);
    }

    @Test
    void updateCartMinusToZeroDeletesItem() {
        testCartItem.setCount(1);
        when(userRepository.existsById(eq(1L))).thenReturn(true);
        when(userRepository.findById(eq(1L))).thenReturn(Optional.of(testUser));
        when(cartRepository.findByUserId(eq(1L))).thenReturn(Optional.of(testCart));
        when(cartItemRepository.findByCartIdAndItemId(eq(1L), eq(1L))).thenReturn(Optional.of(testCartItem));

        cartService.updateCart(1L, 1L, ActionDto.MINUS);

        verify(cartItemRepository).delete(testCartItem);
    }

    @Test
    void updateCartPlusNewItemCreatesNew() {
        when(userRepository.existsById(eq(1L))).thenReturn(true);
        when(userRepository.findById(eq(1L))).thenReturn(Optional.of(testUser));
        when(cartRepository.findByUserId(eq(1L))).thenReturn(Optional.of(testCart));
        when(itemRepository.findById(eq(1L))).thenReturn(Optional.of(testItem));
        when(cartItemRepository.findByCartIdAndItemId(eq(1L), eq(1L))).thenReturn(Optional.empty());

        cartService.updateCart(1L, 1L, ActionDto.PLUS);

        verify(cartItemRepository).save(argThat(item ->
                item.getItemId().equals(1L) && item.getCount() == 1));
    }

    @Test
    void invalidUserIdThrowsException() {
        assertThatThrownBy(() -> cartService.getCart(0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Valid userId required");
    }
}