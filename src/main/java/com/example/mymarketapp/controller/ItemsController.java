package com.example.mymarketapp.controller;

import com.example.mymarketapp.dto.ActionDto;
import com.example.mymarketapp.dto.ItemDto;
import com.example.mymarketapp.entity.Order;
import com.example.mymarketapp.entity.OrderItem;
import com.example.mymarketapp.model.CartItem;
import com.example.mymarketapp.model.OrderItemDto;
import com.example.mymarketapp.repository.OrderRepository;
import com.example.mymarketapp.service.CartService;
import com.example.mymarketapp.service.ItemService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/")
public class ItemsController {
    private final ItemService itemService;
    private final CartService cartService;
    private final OrderRepository orderRepository;

    public ItemsController(ItemService itemService, CartService cartService, OrderRepository orderRepository) {
        this.itemService = itemService;
        this.cartService = cartService;
        this.orderRepository = orderRepository;
    }

    @GetMapping({"/", "/items"})
    public String items(@RequestParam(required = false) String search,
                        @RequestParam(defaultValue = "NO") String sort,
                        @RequestParam(defaultValue = "1") int pageNumber,
                        @RequestParam(defaultValue = "5") int pageSize,
                        Model model, HttpSession session) {
        model.addAttribute("items", itemService.getPagedItems(search, sort, pageNumber, pageSize, cartService));
        model.addAttribute("search", search);
        model.addAttribute("sort", sort);
        model.addAttribute("paging", itemService.getPaging(search, sort, pageNumber, pageSize));
        model.addAttribute("totalCart", cartService.getTotal(session));
        return "items";
    }

    @PostMapping("/items")
    public String updateFromList(@RequestParam long id, @RequestParam String action,
                                 @RequestParam Map<String, String> allParams, HttpSession session) {
        cartService.updateCart(id, ActionDto.valueOf(action), itemService, session);
        String params = allParams.entrySet().stream()
                .filter(e -> !"id".equals(e.getKey()) && !"action".equals(e.getKey()))
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));
        return "redirect:/items?" + params;
    }

    @GetMapping("/items/{id}")
    public String item(@PathVariable long id, Model model, HttpSession session) {
        model.addAttribute("item", itemService.getItemDto(id, cartService));
        model.addAttribute("totalCart", cartService.getTotal(session));
        return "item";
    }

    @PostMapping("/items/{id}")
    public String updateFromItem(@PathVariable long id, @RequestParam String action,
                                 Model model, HttpSession session) {
        cartService.updateCart(id, ActionDto.valueOf(action), itemService, session);
        model.addAttribute("item", itemService.getItemDto(id, cartService));
        model.addAttribute("totalCart", cartService.getTotal(session));
        return "item";
    }

    @GetMapping("/cart")
    public String cart(Model model, HttpSession session) {
        List<CartItem> rawCart = cartService.getCartItems(session);
        List<ItemDto> cartItems = rawCart.stream()
                .map(i -> new ItemDto(i.getId(), i.getTitle(), i.getDescription(),
                        i.getImgPath(), i.getPrice(), i.getCount()))
                .collect(Collectors.toList());
        model.addAttribute("items", cartItems);
        model.addAttribute("total", cartService.getTotal(session));
        return "cart";
    }

    @PostMapping("/cart/items")
    public String updateFromCart(@RequestParam long id, @RequestParam String action,
                                 Model model, HttpSession session) {
        cartService.updateCart(id, ActionDto.valueOf(action), itemService, session);
        return cart(model, session);
    }

    @GetMapping("/orders")
    public String orders(Model model) {
        List<Order> orders = orderRepository.findAll();
        model.addAttribute("orders", orders.stream().map(this::mapToOrderDto).collect(Collectors.toList()));
        return "orders";
    }

    @GetMapping("/orders/{id}")
    public String order(@PathVariable long id, @RequestParam(defaultValue = "false") boolean newOrder, Model model) {
        Order orderEntity = orderRepository.findById(id).orElseThrow();
        model.addAttribute("order", mapToOrderDto(orderEntity));
        model.addAttribute("newOrder", newOrder);
        return "order";
    }

    @PostMapping("/buy")
    public String buy(HttpSession session) {
        List<CartItem> cartItems = cartService.getCartItems(session);
        if (cartItems.isEmpty()) return "redirect:/cart";

        Order order = new Order();
        order.setTotalSum(cartService.getTotal(session));

        for (CartItem cartItem : cartItems) {
            OrderItem orderItem = cartItemToOrderItem(cartItem);
            order.addItem(orderItem);
        }

        Order savedOrder = orderRepository.save(order);
        cartService.clearCart(session);
        return "redirect:/orders/" + savedOrder.getId() + "?newOrder=true";
    }

    private Object mapToOrderDto(Order order) {
        return Map.of(
                "id", order.getId(),
                "items", order.getItems().stream()
                        .map(oi -> new OrderItemDto(oi.getItemId(), oi.getTitle(), oi.getPrice(), oi.getCount()))
                        .collect(Collectors.toList()),
                "totalSum", order.getTotalSum()
        );
    }

    private OrderItem cartItemToOrderItem(CartItem cartItem) {
        OrderItem orderItem = new OrderItem();
        orderItem.setItemId(cartItem.getId());
        orderItem.setTitle(cartItem.getTitle());
        orderItem.setPrice(cartItem.getPrice());
        orderItem.setCount(cartItem.getCount());
        return orderItem;
    }
}
