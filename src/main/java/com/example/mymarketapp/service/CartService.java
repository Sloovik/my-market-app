package com.example.mymarketapp.service;

import com.example.mymarketapp.dto.ActionDto;
import com.example.mymarketapp.entity.Cart;
import com.example.mymarketapp.entity.CartItem;
import com.example.mymarketapp.entity.User;
import com.example.mymarketapp.repository.CartItemRepository;
import com.example.mymarketapp.repository.CartRepository;
import com.example.mymarketapp.repository.ItemRepository;
import com.example.mymarketapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;

    public Flux<CartItem> getCart(Long userId) {
        validateUserId(userId);
        return validateUser(userId)
                .flatMap(this::getOrCreateCartForUser)
                .flatMapMany(cart -> cartItemRepository.findByCartId(cart.getId()));
    }

    public Mono<Long> getTotal(Long userId) {
        validateUserId(userId);
        return getCart(userId)
                .map(ci -> ci.getPrice() * (long) ci.getCount())
                .reduce(0L, Long::sum);
    }

    public Mono<Integer> getCount(Long itemId, Long userId) {
        validateUserId(userId);
        return getCart(userId)
                .filter(ci -> ci.getItemId().equals(itemId))
                .map(CartItem::getCount)
                .reduce(0, Integer::sum);
    }

    public Mono<Void> updateCart(Long userId, Long itemId, ActionDto action) {
        validateUserId(userId);

        return validateUser(userId)
                .flatMap(this::getOrCreateCartForUser)
                .flatMap(cart -> {
                    if (action == ActionDto.DELETE) {
                        return cartItemRepository.findByCartIdAndItemId(cart.getId(), itemId)
                                .flatMap(cartItemRepository::delete)
                                .then();
                    }

                    return cartItemRepository.findByCartIdAndItemId(cart.getId(), itemId)
                            .flatMap(existing -> applyActionToExistingItem(existing, action))
                            .switchIfEmpty(Mono.defer(() ->
                                    action == ActionDto.PLUS
                                            ? createNewCartItem(cart, itemId)
                                            : Mono.empty()
                            ));
                })
                .then();
    }

    public Mono<Void> clearCart(Long userId) {
        validateUserId(userId);
        return cartRepository.findByUserId(userId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Cart not found for user: " + userId)))
                .flatMap(cart -> cartItemRepository.deleteByCartId(cart.getId()));
    }

    private Mono<User> validateUser(Long userId) {
        return userRepository.existsById(userId)
                .flatMap(exists -> {
                    if (!exists) {
                        return Mono.error(new IllegalStateException("User not found: " + userId));
                    }
                    return userRepository.findById(userId)
                            .switchIfEmpty(Mono.error(new IllegalStateException("User not found: " + userId)));
                });
    }

    private void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("Valid userId required");
        }
    }

    private Mono<Cart> getOrCreateCartForUser(User user) {
        return cartRepository.findByUserId(user.getId())
                .switchIfEmpty(Mono.defer(() -> {
                    Cart cart = new Cart();
                    cart.setUserId(user.getId());
                    return cartRepository.save(cart);
                }));
    }

    private Mono<Void> applyActionToExistingItem(CartItem cartItem, ActionDto action) {
        return switch (action) {
            case PLUS -> {
                cartItem.setCount(cartItem.getCount() + 1);
                yield cartItemRepository.save(cartItem).then();
            }
            case MINUS -> {
                if (cartItem.getCount() > 1) {
                    cartItem.setCount(cartItem.getCount() - 1);
                    yield cartItemRepository.save(cartItem).then();
                } else {
                    yield cartItemRepository.delete(cartItem);
                }
            }
            case DELETE -> cartItemRepository.delete(cartItem);
        };
    }

    private Mono<Void> createNewCartItem(Cart cart, Long itemId) {
        return itemRepository.findById(itemId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Item not found: " + itemId)))
                .flatMap(item -> {
                    CartItem newItem = new CartItem();
                    newItem.setCartId(cart.getId());
                    newItem.setItemId(item.getId());
                    newItem.setTitle(item.getTitle());
                    newItem.setDescription(item.getDescription());
                    newItem.setImgPath(item.getImgPath());
                    newItem.setPrice(item.getPrice());
                    newItem.setCount(1);
                    return cartItemRepository.save(newItem).then();
                });
    }
}