package com.example.mymarketapp.service;

import com.example.mymarketapp.dto.ActionDto;
import com.example.mymarketapp.entity.Item;
import com.example.mymarketapp.model.CartItem;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock private ItemService itemService;
    @Mock private HttpSession session;
    @InjectMocks private CartService cartService;

    private Item testItem;

    @BeforeEach
    void setUp() {
        testItem = new Item();
        testItem.setId(1L);
        testItem.setTitle("Test Item");
        testItem.setPrice(1000L);
    }

    @Test
    void getCartNullSessionReturnsEmpty() {
        List<CartItem> result = cartService.getCart(null);
        assertThat(result).isEmpty();
    }

    @Test
    void getCartNoAttributeReturnsNewCart() {
        when(session.getAttribute(CartService.CART_SESSION_KEY)).thenReturn(null);

        List<CartItem> result = cartService.getCart(session);

        assertThat(result).isEmpty();
        verify(session).setAttribute(CartService.CART_SESSION_KEY, result);
    }

    @Test
    void getCartExistingReturnsCopy() {
        CartItem existingItem = new CartItem();
        existingItem.setId(1L);
        List<CartItem> cart = List.of(existingItem);
        when(session.getAttribute(CartService.CART_SESSION_KEY)).thenReturn(cart);

        List<CartItem> result = cartService.getCart(session);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        verify(session, never()).setAttribute(anyString(), any());
    }

    @Test
    void getTotalEmptyCartReturnsZero() {
        when(session.getAttribute(CartService.CART_SESSION_KEY)).thenReturn(new ArrayList<>());

        long total = cartService.getTotal(session);

        assertThat(total).isZero();
    }

    @Test
    void getTotalWithItemsReturnsSum() {
        CartItem item1 = new CartItem(); item1.setPrice(1000L); item1.setCount(2);
        CartItem item2 = new CartItem(); item2.setPrice(500L); item2.setCount(3);
        when(session.getAttribute(CartService.CART_SESSION_KEY)).thenReturn(List.of(item1, item2));

        long total = cartService.getTotal(session);

        assertThat(total).isEqualTo(3500L);
    }

    @Test
    void getCountNotFoundReturnsZero() {
        when(session.getAttribute(CartService.CART_SESSION_KEY)).thenReturn(new ArrayList<>());

        int count = cartService.getCount(999L, session);

        assertThat(count).isZero();
    }

    @Test
    void getCountFoundReturnsCount() {
        CartItem item = new CartItem(); item.setId(1L); item.setCount(3);
        when(session.getAttribute(CartService.CART_SESSION_KEY)).thenReturn(List.of(item));

        int count = cartService.getCount(1L, session);

        assertThat(count).isEqualTo(3);
    }

    @Test
    void updateCartDeleteRemovesItem() {
        CartItem item = new CartItem(); item.setId(1L);
        List<CartItem> cart = new ArrayList<>(List.of(item));
        when(session.getAttribute(CartService.CART_SESSION_KEY)).thenReturn(cart);

        cartService.updateCart(1L, ActionDto.DELETE, itemService, session);

        assertThat(cartService.getCart(session)).isEmpty();
    }


    @Test
    void updateCartPlusIncreasesCount() {
        CartItem item = new CartItem(); item.setId(1L); item.setCount(1);
        List<CartItem> cart = new ArrayList<>(List.of(item));
        when(session.getAttribute(CartService.CART_SESSION_KEY)).thenReturn(cart);

        cartService.updateCart(1L, ActionDto.PLUS, itemService, session);

        assertThat(item.getCount()).isEqualTo(2);
        verify(session).setAttribute(CartService.CART_SESSION_KEY, cart);
    }

    @Test
    void updateCartMinusToZeroRemovesItem() {
        CartItem item = new CartItem(); item.setId(1L); item.setCount(1);
        List<CartItem> cart = new ArrayList<>(List.of(item));
        when(session.getAttribute(CartService.CART_SESSION_KEY)).thenReturn(cart);

        cartService.updateCart(1L, ActionDto.MINUS, itemService, session);

        assertThat(cart).isEmpty();
        verify(session).setAttribute(CartService.CART_SESSION_KEY, cart);
    }

    @Test
    void updateCartPlusNewItemCreatesFromItemService() {
        List<CartItem> cart = new ArrayList<>();
        when(session.getAttribute(CartService.CART_SESSION_KEY)).thenReturn(cart);
        when(itemService.getItem(1L)).thenReturn(testItem);

        cartService.updateCart(1L, ActionDto.PLUS, itemService, session);

        assertThat(cart).hasSize(1);
        CartItem newItem = cart.get(0);
        assertThat(newItem.getId()).isEqualTo(1L);
        assertThat(newItem.getTitle()).isEqualTo("Test Item");
        assertThat(newItem.getCount()).isOne();
        verify(session).setAttribute(CartService.CART_SESSION_KEY, cart);
    }

    @Test
    void updateCartMinusExistingDecreases() {
        CartItem item = new CartItem(); item.setId(1L); item.setCount(3);
        List<CartItem> cart = new ArrayList<>(List.of(item));
        when(session.getAttribute(CartService.CART_SESSION_KEY)).thenReturn(cart);

        cartService.updateCart(1L, ActionDto.MINUS, itemService, session);

        assertThat(item.getCount()).isEqualTo(2);
        verify(session).setAttribute(CartService.CART_SESSION_KEY, cart);
    }

    @Test
    void clearCartRemovesAttribute() {
        cartService.clearCart(session);
        verify(session).removeAttribute(CartService.CART_SESSION_KEY);
    }
}
