package com.example.demo.controllers;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.model.persistence.Cart;
import com.example.demo.model.persistence.Item;
import com.example.demo.model.persistence.User;
import com.example.demo.model.persistence.repositories.CartRepository;
import com.example.demo.model.persistence.repositories.ItemRepository;
import com.example.demo.model.persistence.repositories.OrderRepository;
import com.example.demo.model.persistence.repositories.UserRepository;
import com.example.demo.model.requests.ModifyCartRequest;
import com.example.demo.security.UserDetailsServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@RunWith(SpringRunner.class)
@WebMvcTest(CartController.class)
public class CartControllerTest {

  private static final MediaType APPLICATION_JSON_UTF8 = new MediaType(
      MediaType.APPLICATION_JSON.getType(), MediaType.APPLICATION_JSON.getSubtype(), Charset.forName("utf8"));
  private static final String STUBBED_ENCODED_PASSWORD = "stubbedEncodedPassword";

  @Autowired
  MockMvc mockMvc;

  @MockBean
  UserRepository userRepository;

  @MockBean
  CartRepository cartRepository;

  @MockBean
  OrderRepository orderRepository;

  @MockBean
  ItemRepository itemRepository;

  @MockBean
  BCryptPasswordEncoder bCryptPasswordEncoder;

  @MockBean
  UserDetailsServiceImpl userDetailsService;

  private final ObjectMapper objectMapper;

  public CartControllerTest() {
    objectMapper = new ObjectMapper();
  }

  @Before
  public void beforeEach() {
    when(bCryptPasswordEncoder.encode(anyString())).thenReturn(STUBBED_ENCODED_PASSWORD);
  }


  @Test
  @WithMockUser
  public void addToCart() throws Exception {
    User user = new User(1L, "philip", "testpassw");
    Item item = new Item(1L, "keyboard", BigDecimal.valueOf(25.2), "gamer keyboard");
    Optional<Item> optionalItem = Optional.of(item);
    ModifyCartRequest cartRequest = new ModifyCartRequest(user.getUsername(), item.getId(), 2);
    Cart cart = new Cart(1L, new ArrayList<>(), user, BigDecimal.ZERO);
    user.setCart(cart);

    when(userRepository.findByUsername(cartRequest.getUsername())).thenReturn(user);
    when(itemRepository.findById(cartRequest.getItemId())).thenReturn(optionalItem);
    when(cartRepository.save(any())).thenReturn(cart);

    MvcResult result = mockMvc.perform(post("/api/cart/addToCart")
            .contentType(APPLICATION_JSON_UTF8)
            .content(objectMapper.writeValueAsString(cartRequest)))
        .andExpect(status().isOk())
        .andReturn();

    DocumentContext documentContext = JsonPath.parse(result.getResponse().getContentAsString());
    assertEquals(cart.getId(), documentContext.read("$.id", Long.class));
    assertEquals(cart.getTotal(), documentContext.read("$.total", BigDecimal.class));
    assertEquals(2, (int) documentContext.read("$.items.length()"));
    assertEquals(cart.getItems().get(0).getId(), documentContext.read("$.items[0].id", Long.class));
    assertEquals(cart.getItems().get(1).getId(), documentContext.read("$.items[1].id", Long.class));
    assertEquals(cart.getUser().getId(), documentContext.read("$.user.id", Long.class).longValue());

    verify(cartRepository, times(1)).save(any());
  }

  @Test
  @WithMockUser
  public void addToCart_user_not_found() throws Exception {
    User user = null;
    Item item = new Item(1L, "keyboard", BigDecimal.valueOf(25.2), "gamer keyboard");
    Optional<Item> optionalItem = Optional.of(item);
    ModifyCartRequest cartRequest = new ModifyCartRequest("pete", item.getId(), 2);
    Cart cart = new Cart(1L, new ArrayList<>(), user, BigDecimal.ZERO);

    when(userRepository.findByUsername(cartRequest.getUsername())).thenReturn(user);
    when(itemRepository.findById(cartRequest.getItemId())).thenReturn(optionalItem);
    when(cartRepository.save(any())).thenReturn(cart);

    mockMvc.perform(post("/api/cart/addToCart")
            .contentType(APPLICATION_JSON_UTF8)
            .content(objectMapper.writeValueAsString(cartRequest)))
        .andExpect(status().isNotFound());


    verify(cartRepository, times(0)).save(any());
  }

  @Test
  @WithMockUser
  public void addToCart_item_not_found() throws Exception {
    User user = new User(1L, "philip", "testpassw");
    Optional<Item> optionalItem = Optional.empty();
    ModifyCartRequest cartRequest = new ModifyCartRequest(user.getUsername(), 0L, 2);
    Cart cart = new Cart(1L, new ArrayList<>(), user, BigDecimal.ZERO);
    user.setCart(cart);

    when(userRepository.findByUsername(cartRequest.getUsername())).thenReturn(user);
    when(itemRepository.findById(cartRequest.getItemId())).thenReturn(optionalItem);
    when(cartRepository.save(any())).thenReturn(cart);

    mockMvc.perform(post("/api/cart/addToCart")
            .contentType(APPLICATION_JSON_UTF8)
            .content(objectMapper.writeValueAsString(cartRequest)))
        .andExpect(status().isNotFound());

    verify(cartRepository, times(0)).save(any());
  }

  @Test
  @WithMockUser
  public void removeFromCart() throws Exception {
    User user = new User(1L, "philip", "testpassw");
    Item item = new Item(1L, "keyboard", BigDecimal.valueOf(25.2), "gamer keyboard");
    Optional<Item> optionalItem = Optional.of(item);
    ModifyCartRequest cartRequest = new ModifyCartRequest(user.getUsername(), item.getId(), 2);
    List<Item> items = new ArrayList<>(Arrays.asList(item, item));
    Cart cart = new Cart(1L, items, user, item.getPrice().multiply(BigDecimal.valueOf(2)));
    user.setCart(cart);

    when(userRepository.findByUsername(cartRequest.getUsername())).thenReturn(user);
    when(itemRepository.findById(cartRequest.getItemId())).thenReturn(optionalItem);
    when(cartRepository.save(any())).thenReturn(cart);

    MvcResult result = mockMvc.perform(post("/api/cart/removeFromCart")
            .contentType(APPLICATION_JSON_UTF8)
            .content(objectMapper.writeValueAsString(cartRequest)))
        .andExpect(status().isOk())
        .andReturn();

    DocumentContext documentContext = JsonPath.parse(result.getResponse().getContentAsString());
    assertEquals(cart.getId(), documentContext.read("$.id", Long.class));
    assertEquals(cart.getTotal(), documentContext.read("$.total", BigDecimal.class));
    assertEquals(0, (int) documentContext.read("$.items.length()"));
    assertEquals(cart.getUser().getId(), documentContext.read("$.user.id", Long.class).longValue());

    verify(cartRepository, times(1)).save(any());
  }

  @Test
  @WithMockUser
  public void removeFromCart_user_not_found() throws Exception {
    User user = null;
    Item item = new Item(1L, "keyboard", BigDecimal.valueOf(25.2), "gamer keyboard");
    Optional<Item> optionalItem = Optional.of(item);
    ModifyCartRequest cartRequest = new ModifyCartRequest("el", item.getId(), 2);
    List<Item> items = new ArrayList<>(Arrays.asList(item, item));
    Cart cart = new Cart(1L, items, user, item.getPrice().multiply(BigDecimal.valueOf(2)));

    when(userRepository.findByUsername(cartRequest.getUsername())).thenReturn(user);
    when(itemRepository.findById(cartRequest.getItemId())).thenReturn(optionalItem);
    when(cartRepository.save(any())).thenReturn(cart);

    mockMvc.perform(post("/api/cart/removeFromCart")
            .contentType(APPLICATION_JSON_UTF8)
            .content(objectMapper.writeValueAsString(cartRequest)))
        .andExpect(status().isNotFound());

    verify(cartRepository, times(0)).save(any());
  }

  @Test
  @WithMockUser
  public void removeFromCart_item_not_found() throws Exception {
    User user = new User(1L, "philip", "testpassw");
    Optional<Item> optionalItem = Optional.empty();
    ModifyCartRequest cartRequest = new ModifyCartRequest(user.getUsername(), 0L, 2);
    List<Item> items = new ArrayList<>();
    Cart cart = new Cart(1L, items, user, BigDecimal.ZERO);
    user.setCart(cart);

    when(userRepository.findByUsername(cartRequest.getUsername())).thenReturn(user);
    when(itemRepository.findById(cartRequest.getItemId())).thenReturn(optionalItem);
    when(cartRepository.save(any())).thenReturn(cart);

    mockMvc.perform(post("/api/cart/removeFromCart")
            .contentType(APPLICATION_JSON_UTF8)
            .content(objectMapper.writeValueAsString(cartRequest)))
        .andExpect(status().isNotFound());

    verify(cartRepository, times(0)).save(any());
  }

}
