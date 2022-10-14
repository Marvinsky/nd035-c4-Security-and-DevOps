package com.example.demo.controllers;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.model.persistence.Cart;
import com.example.demo.model.persistence.Item;
import com.example.demo.model.persistence.User;
import com.example.demo.model.persistence.UserOrder;
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
@WebMvcTest(OrderController.class)
public class OrderControllerTest {

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

  public OrderControllerTest() {
    objectMapper = new ObjectMapper();
  }

  @Before
  public void beforeEach() {
    when(bCryptPasswordEncoder.encode(anyString())).thenReturn(STUBBED_ENCODED_PASSWORD);
  }


  @Test
  @WithMockUser
  public void submit() throws Exception {
    User user = new User(1L, "julia", "testpassw");
    Item item = new Item(1L, "keyboard", BigDecimal.valueOf(25.2), "gamer keyboard");
    Optional<Item> optionalItem = Optional.of(item);
    ModifyCartRequest cartRequest = new ModifyCartRequest(user.getUsername(), item.getId(), 2);
    Cart cart = new Cart(
        1L,
        new ArrayList<>(Arrays.asList(item, item)),
        user,
        item.getPrice().multiply(BigDecimal.valueOf(2)));
    user.setCart(cart);
    UserOrder order = UserOrder.createFromCart(user.getCart());
    order.setId(1L);

    when(userRepository.findByUsername(user.getUsername())).thenReturn(user);
    when(orderRepository.save(any())).thenReturn(order);

    MvcResult result = mockMvc.perform(post("/api/order/submit/" + user.getUsername()))
        .andExpect(status().isOk())
        .andReturn();

    DocumentContext documentContext = JsonPath.parse(result.getResponse().getContentAsString());
    assertEquals(order.getId(), documentContext.read("$.id", Long.class));
    assertEquals(order.getTotal(), documentContext.read("$.total", BigDecimal.class));
    assertEquals(2, (int) documentContext.read("$.items.length()"));
    assertEquals(order.getItems().get(0).getId(), documentContext.read("$.items[0].id", Long.class));
    assertEquals(order.getItems().get(1).getId(), documentContext.read("$.items[1].id", Long.class));
    assertEquals(order.getUser().getId(), documentContext.read("$.user.id", Long.class).longValue());

    verify(orderRepository, times(1)).save(any());
  }

  @Test
  @WithMockUser
  public void submit_user_not_found() throws Exception {
    String username = "jock";
    User user = null;

    when(userRepository.findByUsername(username)).thenReturn(user);

    mockMvc.perform(post("/api/order/submit/" + username))
        .andExpect(status().isNotFound());

    verify(orderRepository, times(0)).save(any());
  }

  @Test
  @WithMockUser
  public void getOrdersForUser() throws Exception {
    User user = new User(1L, "julia", "testpassw");
    Item item = new Item(1L, "keyboard", BigDecimal.valueOf(25.2), "gamer keyboard");
    Optional<Item> optionalItem = Optional.of(item);
    ModifyCartRequest cartRequest = new ModifyCartRequest(user.getUsername(), item.getId(), 2);
    Cart cart = new Cart(
        1L,
        new ArrayList<>(Arrays.asList(item, item)),
        user,
        item.getPrice().multiply(BigDecimal.valueOf(2)));
    user.setCart(cart);
    UserOrder order = UserOrder.createFromCart(user.getCart());
    order.setId(1L);

    when(userRepository.findByUsername(user.getUsername())).thenReturn(user);
    when(orderRepository.findByUser(any())).thenReturn(Arrays.asList(order));

    MvcResult result = mockMvc.perform(get("/api/order/history/" + user.getUsername()))
        .andExpect(status().isOk())
        .andReturn();

    DocumentContext documentContext = JsonPath.parse(result.getResponse().getContentAsString());
    assertEquals(1, (int) documentContext.read("$.length()"));
    assertEquals(order.getId(), documentContext.read("$[0].id", Long.class));
    assertEquals(order.getTotal(), documentContext.read("$[0].total", BigDecimal.class));
    assertEquals(2, (int) documentContext.read("$[0].items.length()"));
    assertEquals(order.getItems().get(0).getId(), documentContext.read("$[0].items[0].id", Long.class));
    assertEquals(order.getItems().get(1).getId(), documentContext.read("$[0].items[1].id", Long.class));
    assertEquals(order.getUser().getId(), documentContext.read("$[0].user.id", Long.class).longValue());

    verify(orderRepository, times(1)).findByUser(any());
  }

  @Test
  @WithMockUser
  public void getOrdersForUser_user_not_found() throws Exception {
    String username = "jock";
    User user = null;

    when(userRepository.findByUsername(username)).thenReturn(user);

    mockMvc.perform(get("/api/order/history/" + username))
        .andExpect(status().isNotFound());

    verify(orderRepository, times(0)).findByUser(any());
  }

}
