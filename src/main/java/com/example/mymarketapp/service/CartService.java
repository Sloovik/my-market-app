package com.example.mymarketapp.service;

import com.example.mymarketapp.dto.ActionDto;
import com.example.mymarketapp.entity.Item;
import com.example.mymarketapp.model.CartItem;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class CartService {
    public static final String CART_SESSION_KEY = "cart";

    @SuppressWarnings("unchecked")
    public List<CartItem> getCart(HttpSession session) {
        if (session == null) return new ArrayList<>();

        List<CartItem> cart = (List<CartItem>) session.getAttribute(CART_SESSION_KEY);
        if (cart == null) {
            cart = new ArrayList<>();
            session.setAttribute(CART_SESSION_KEY, cart);
        }
        return cart;
    }

    public List<CartItem> getCartItems(HttpSession session) {
        return new ArrayList<>(getCart(session));
    }

    public long getTotal(HttpSession session) {
        return getCart(session).stream()
                .mapToLong(item -> item.getPrice() * item.getCount())
                .sum();
    }

    public int getCount(long id, HttpSession session) {
        return getCart(session).stream()
                .filter(item -> item.getId() == id)
                .mapToInt(CartItem::getCount)
                .sum();
    }

    public void updateCart(long id, ActionDto action, ItemService itemService, HttpSession session) {
        if (session == null) return;

        List<CartItem> cart = getCart(session);
        if (action == ActionDto.DELETE) {
            cart.removeIf(item -> item.getId() == id);
            return;
        }

        Optional<CartItem> existing = cart.stream().filter(item -> item.getId() == id).findFirst();

        if (existing.isPresent()) {
            CartItem cartItem = existing.get();
            switch (action) {
                case PLUS -> cartItem.setCount(cartItem.getCount() + 1);
                case MINUS -> {
                    if (cartItem.getCount() > 1) cartItem.setCount(cartItem.getCount() - 1);
                    else cart.remove(cartItem);
                }
            }
        } else if (action == ActionDto.PLUS) {
            Item item = itemService.getItem(id);
            CartItem newItem = new CartItem();
            newItem.setId(item.getId());
            newItem.setTitle(item.getTitle());
            newItem.setDescription(item.getDescription());
            newItem.setImgPath(item.getImgPath());
            newItem.setPrice(item.getPrice());
            newItem.setCount(1);
            cart.add(newItem);
        }

        session.setAttribute(CART_SESSION_KEY, cart);
    }

    public void clearCart(HttpSession session) {
        if (session != null) session.removeAttribute(CART_SESSION_KEY);
    }
}
