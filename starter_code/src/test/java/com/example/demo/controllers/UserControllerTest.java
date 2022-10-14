package com.example.demo.controllers;

import com.example.demo.model.persistence.User;
import com.example.demo.model.persistence.repositories.CartRepository;
import com.example.demo.model.persistence.repositories.ItemRepository;
import com.example.demo.model.persistence.repositories.OrderRepository;
import com.example.demo.model.persistence.repositories.UserRepository;
import com.example.demo.model.requests.CreateUserRequest;
import com.example.demo.security.UserDetailsServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.Charset;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.ParameterizedTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@RunWith(SpringRunner.class)
@WebMvcTest(UserController.class)
public class UserControllerTest {

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

  public UserControllerTest() {
    objectMapper = new ObjectMapper();
  }

  @Before
  public void beforeEach() {
    when(bCryptPasswordEncoder.encode(anyString())).thenReturn(STUBBED_ENCODED_PASSWORD);
  }

  @Test
  @WithMockUser
  public void findById_found() throws Exception {
    long userId = 1L;
    User user = new User(userId, "testuser", "testpassw");
    Optional<User> optionalUser = Optional.of(user);

    when(userRepository.findById(userId)).thenReturn(optionalUser);

    MvcResult result = mockMvc.perform(get("/api/user/id/" + userId))
        .andExpect(status().isOk())
        .andReturn();


    String response = result.getResponse().getContentAsString();
    String responseUsername = JsonPath.parse(response).read("$.username");
    int responseUserId = JsonPath.parse(response).read("$.id");
    assertEquals(user.getUsername(), responseUsername);
    assertEquals(user.getId(), responseUserId);

    verify(userRepository, times(1)).findById(userId);
  }

  @Test
  @WithMockUser
  public void findById_not_found() throws Exception {
    long userId = 0L;
    Optional<User> missingUser = Optional.empty();

    when(userRepository.findById(userId)).thenReturn(missingUser);

    mockMvc.perform(get("/api/user/id/" + userId))
        .andExpect(status().isNotFound());

    verify(userRepository, times(1)).findById(userId);
  }

  @Test
  @WithMockUser
  public void findByUserName_found() throws Exception {
    String username = "anne";
    User user = new User(1L, username, "testpassw");

    when(userRepository.findByUsername(username)).thenReturn(user);

    MvcResult result = mockMvc.perform(get("/api/user/" + username))
        .andExpect(status().isOk())
        .andReturn();

    String response = result.getResponse().getContentAsString();
    String responseUsername = JsonPath.parse(response).read("$.username");
    int responseUserId = JsonPath.parse(response).read("$.id");
    assertEquals(user.getUsername(), responseUsername);
    assertEquals(user.getId(), responseUserId);

    verify(userRepository, times(1)).findByUsername(username);
  }

  @Test
  @WithMockUser
  public void findByUserName_not_found() throws Exception {
    String username = "stephen";
    User user = null;

    when(userRepository.findByUsername(username)).thenReturn(user);

    mockMvc.perform(get("/api/user/" + username))
        .andExpect(status().isNotFound());

    verify(userRepository, times(1)).findByUsername(username);
  }

  @ParameterizedTest
  @WithMockUser
  @MethodSource("provideUsers")
  public void createUser(
      long id,
      CreateUserRequest userRequest,
      HttpStatus expectedHttpStatus)
      throws Exception {

    User user = new User(id, userRequest.getUsername(), userRequest.getPassword());

    when(userRepository.save(any())).thenReturn(user);


    String userRequestStr = objectMapper.writeValueAsString(userRequest);

    MvcResult result = mockMvc.perform(post("/api/user/create")
            .contentType(APPLICATION_JSON_UTF8)
            .content(userRequestStr))
        .andExpect(status().is(expectedHttpStatus.value()))
        .andReturn();

    if (expectedHttpStatus.is2xxSuccessful()) {
      String response = result.getResponse().getContentAsString();
      String responseUsername = JsonPath.parse(response).read("$.username");
      int responseUserId = JsonPath.parse(response).read("$.id");
      assertEquals(user.getUsername(), responseUsername);
      assertEquals(user.getId(), responseUserId);

      verify(userRepository, times(1)).save(any());
    } else {
      verify(userRepository, times(0)).save(any());
    }

  }

  private static Stream<Arguments> provideUsers() {
    return Stream.of(
        Arguments.of(
            1L,
            new CreateUserRequest("jack", "testpassw", "testpassw"),
            HttpStatus.OK),
        Arguments.of(
            2L,
            new CreateUserRequest( "jenny", "test", "test"),
            HttpStatus.BAD_REQUEST),
        Arguments.of(
            3L,
            new CreateUserRequest("tom", "testpassw", "somethingelse"),
            HttpStatus.BAD_REQUEST),
        Arguments.of(
            4L,
            new CreateUserRequest("", "testpassw", "testpassw"),
            HttpStatus.BAD_REQUEST),
        Arguments.of(
            5L,
            new CreateUserRequest(null, "testpassw", "testpassw"),
            HttpStatus.BAD_REQUEST)
    );
  }
}
