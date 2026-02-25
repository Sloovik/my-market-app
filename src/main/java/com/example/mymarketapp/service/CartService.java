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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class CartService {
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;

    public List<CartItem> getCart(Long userId) {
        validateUser(userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found: " + userId));
        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> createCartForUser(user));
        return cartItemRepository.findByCartId(cart.getId());
    }

    public long getTotal(Long userId) {
        validateUserId(userId);
        return getCart(userId).stream()
                .mapToLong(item -> item.getPrice() * item.getCount()).sum();
    }

    public int getCount(Long itemId, Long userId) {
        validateUserId(userId);
        return getCart(userId).stream()
                .filter(item -> item.getItemId().equals(itemId))
                .mapToInt(CartItem::getCount).sum();
    }

    public void updateCart(Long userId, Long itemId, ActionDto action) {
        validateUserId(userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found: " + userId));
        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> createCartForUser(user));

        if (action == ActionDto.DELETE) {
            cartItemRepository.findByCartIdAndItemId(cart.getId(), itemId)
                    .ifPresent(cartItemRepository::delete);
            return;
        }

        Optional<CartItem> existing = cartItemRepository.findByCartIdAndItemId(cart.getId(), itemId);

        if (existing.isPresent()) {
            CartItem cartItem = existing.get();
            switch (action) {
                case PLUS -> cartItem.setCount(cartItem.getCount() + 1);
                case MINUS -> {
                    if (cartItem.getCount() > 1) {
                        cartItem.setCount(cartItem.getCount() - 1);
                    } else {
                        cartItemRepository.delete(cartItem);
                    }
                }
            }
            cartItemRepository.save(cartItem);
        } else if (action == ActionDto.PLUS) {
            Item item = itemRepository.findById(itemId)
                    .orElseThrow(() -> new IllegalArgumentException("Item not found: " + itemId));
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setItemId(item.getId());
            newItem.setTitle(item.getTitle());
            newItem.setDescription(item.getDescription());
            newItem.setImgPath(item.getImgPath());
            newItem.setPrice(item.getPrice());
            newItem.setCount(1);
            cartItemRepository.save(newItem);
        }
    }

    public void clearCart(Long userId) {
        validateUserId(userId);
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Cart not found for user: " + userId));
        cartItemRepository.deleteAll(cart.getItems());
        cart.getItems().clear();
        cartRepository.save(cart);
    }

    private void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("Valid userId required");
        }
    }

    private void validateUser(Long userId) {
        validateUserId(userId);
        if (!userRepository.existsById(userId)) {
            throw new IllegalStateException("User not found: " + userId);
        }
    }

    private Cart createCartForUser(User user) {
        Cart cart = new Cart();
        cart.setUser(user);
        user.setCart(cart);
        return cartRepository.save(cart);
    }
}