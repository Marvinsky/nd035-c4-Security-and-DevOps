package com.example.demo.controllers;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.model.persistence.Item;
import com.example.demo.model.persistence.repositories.CartRepository;
import com.example.demo.model.persistence.repositories.ItemRepository;
import com.example.demo.model.persistence.repositories.OrderRepository;
import com.example.demo.model.persistence.repositories.UserRepository;
import com.example.demo.security.UserDetailsServiceImpl;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@RunWith(SpringRunner.class)
@WebMvcTest(ItemController.class)
public class ItemControllerTest {

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

  @Before
  public void beforeEach() {
    when(bCryptPasswordEncoder.encode(anyString())).thenReturn(STUBBED_ENCODED_PASSWORD);
  }

  @Test
  @WithMockUser
  public void getItemById_found() throws Exception {
    long itemId = 1L;
    Item item = new Item(itemId, "item", BigDecimal.valueOf(5.2), "desc");
    Optional<Item> optionalItem = Optional.of(item);

    when(itemRepository.findById(itemId)).thenReturn(optionalItem);

    MvcResult result = mockMvc.perform(get("/api/item/" + itemId))
        .andExpect(status().isOk())
        .andReturn();

    String response = result.getResponse().getContentAsString();
    DocumentContext documentContext = JsonPath.parse(response);
    assertEquals(item.getId().intValue(), (int) documentContext.read("$.id"));
    assertEquals(item.getPrice().doubleValue(), (double) documentContext.read("$.price"), 0.01);
    assertEquals(item.getName(), documentContext.read("$.name"));
    assertEquals(item.getDescription(), documentContext.read("$.description"));

    verify(itemRepository, times(1)).findById(itemId);
  }

  @Test
  @WithMockUser
  public void getItemById_not_found() throws Exception {
    long itemId = 0L;
    Optional<Item> optionalItem = Optional.empty();

    when(itemRepository.findById(itemId)).thenReturn(optionalItem);

    mockMvc.perform(get("/api/item/" + itemId))
        .andExpect(status().isNotFound());

    verify(itemRepository, times(1)).findById(itemId);
  }

  @Test
  @WithMockUser
  public void getItemsByName_found() throws Exception {
    String itemName = "charger";
    Item item = new Item(1L, itemName, BigDecimal.valueOf(5.2), "desc");

    when(itemRepository.findByName(itemName)).thenReturn(Arrays.asList(item));

    MvcResult result = mockMvc.perform(get("/api/item/name/" + itemName))
        .andExpect(status().isOk())
        .andReturn();

    String response = result.getResponse().getContentAsString();
    DocumentContext documentContext = JsonPath.parse(response);
    assertEquals(item.getId().intValue(), (int) documentContext.read("$[0].id"));
    assertEquals(item.getPrice().doubleValue(), (double) documentContext.read("$[0].price"), 0.01);
    assertEquals(item.getName(), documentContext.read("$[0].name"));
    assertEquals(item.getDescription(), documentContext.read("$[0].description"));

    verify(itemRepository, times(1)).findByName(itemName);
  }

  @Test
  @WithMockUser
  public void getItemsByName_not_found() throws Exception {
    String itemName = "rocket";

    when(itemRepository.findByName(itemName)).thenReturn(Collections.emptyList());

    mockMvc.perform(get("/api/item/name/" + itemName))
        .andExpect(status().isNotFound());

    verify(itemRepository, times(1)).findByName(itemName);
  }

  @Test
  @WithMockUser
  public void getItems() throws Exception {
    String itemName = "charger";
    Item item = new Item(1L, itemName, BigDecimal.valueOf(5.2), "desc");

    when(itemRepository.findAll()).thenReturn(Arrays.asList(item));

    MvcResult result = mockMvc.perform(get("/api/item"))
        .andExpect(status().isOk())
        .andReturn();

    String response = result.getResponse().getContentAsString();
    DocumentContext documentContext = JsonPath.parse(response);
    assertEquals(item.getId().intValue(), (int) documentContext.read("$[0].id"));
    assertEquals(item.getPrice().doubleValue(), (double) documentContext.read("$[0].price"), 0.01);
    assertEquals(item.getName(), documentContext.read("$[0].name"));
    assertEquals(item.getDescription(), documentContext.read("$[0].description"));

    verify(itemRepository, times(1)).findAll();
  }
}
