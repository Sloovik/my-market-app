package com.example.mymarketapp.service;

import com.example.mymarketapp.dto.ActionDto;
import com.example.mymarketapp.entity.Cart;
import com.example.mymarketapp.entity.CartItem;
import com.example.mymarketapp.entity.Item;
import com.example.mymarketapp.entity.User;
import com.example.mymarketapp.repository.CartItemRepository;
import com.example.mymarketapp.repository.CartRepository;
import com.example.mymarketapp.repository.ItemRepository;
import com.example.mymarketapp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CartServiceTest {

    private CartRepository cartRepository;
    private CartItemRepository cartItemRepository;
    private ItemRepository itemRepository;
    private UserRepository userRepository;

    private CartService cartService;

    @BeforeEach
    void setUp() {
        cartRepository = Mockito.mock(CartRepository.class);
        cartItemRepository = Mockito.mock(CartItemRepository.class);
        itemRepository = Mockito.mock(ItemRepository.class);
        userRepository = Mockito.mock(UserRepository.class);

        cartService = new CartService(cartRepository, cartItemRepository, itemRepository, userRepository);
    }

    @Test
    void getTotalReturnsCorrectSum() {
        long userId = 1L;

        User user = new User();
        user.setId(userId);
        user.setUsername("demo");

        Cart cart = new Cart();
        cart.setId(10L);
        cart.setUserId(userId);

        CartItem ci1 = new CartItem();
        ci1.setCartId(cart.getId());
        ci1.setItemId(100L);
        ci1.setPrice(1000L);
        ci1.setCount(2);

        CartItem ci2 = new CartItem();
        ci2.setCartId(cart.getId());
        ci2.setItemId(200L);
        ci2.setPrice(500L);
        ci2.setCount(1);

        when(userRepository.existsById(userId)).thenReturn(Mono.just(true));
        when(userRepository.findById(userId)).thenReturn(Mono.just(user));
        when(cartRepository.findByUserId(userId)).thenReturn(Mono.just(cart));
        when(cartItemRepository.findByCartId(cart.getId())).thenReturn(Flux.just(ci1, ci2));

        StepVerifier.create(cartService.getTotal(userId))
                .expectNext(1000L * 2 + 500L)
                .verifyComplete();
    }

    @Test
    void updateCartPlusCreatesNewItem() {
        long userId = 1L;
        long itemId = 100L;

        User user = new User();
        user.setId(userId);
        user.setUsername("demo");

        Cart cart = new Cart();
        cart.setId(10L);
        cart.setUserId(userId);

        Item item = new Item();
        item.setId(itemId);
        item.setTitle("Test");
        item.setDescription("Desc");
        item.setImgPath("/img");
        item.setPrice(1000L);

        when(userRepository.existsById(userId)).thenReturn(Mono.just(true));
        when(userRepository.findById(userId)).thenReturn(Mono.just(user));
        when(cartRepository.findByUserId(userId)).thenReturn(Mono.just(cart));
        when(cartItemRepository.findByCartIdAndItemId(cart.getId(), itemId)).thenReturn(Mono.empty());
        when(itemRepository.findById(itemId)).thenReturn(Mono.just(item));
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(invocation ->
                Mono.just(invocation.getArgument(0, CartItem.class)));

        StepVerifier.create(cartService.updateCart(userId, itemId, ActionDto.PLUS))
                .verifyComplete();

        verify(cartItemRepository, times(1)).save(any(CartItem.class));
    }
}