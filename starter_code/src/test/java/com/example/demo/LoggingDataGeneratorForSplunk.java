package com.example.demo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.model.persistence.Item;
import com.example.demo.model.requests.CreateUserRequest;
import com.example.demo.model.requests.ModifyCartRequest;
import com.example.demo.util.ItemCount;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@RunWith(SpringRunner.class)
@SpringBootTest
@WebAppConfiguration
public class LoggingDataGeneratorForSplunk {

  private static final MediaType APPLICATION_JSON_UTF8 = new MediaType(
      MediaType.APPLICATION_JSON.getType(), MediaType.APPLICATION_JSON.getSubtype(), Charset.forName("utf8"));

  // same as data.sql
  private static final Item ITEM_1 = new Item(1L, "Round Widget", BigDecimal.valueOf(2.99), "A widget that is round");
  private static final Item ITEM_2 = new Item(2L, "Square Widget", BigDecimal.valueOf(1.99), "A widget that is square");
  private static final Item ITEM_3 = new Item(3L, "Cuberdon", BigDecimal.valueOf(3.2), "cone-shaped candy with a melty core and a crisp crust");
  private static final Item ITEM_4 = new Item(4L, "Vanparys", BigDecimal.valueOf(2.5), "coated with thin layers of sugar, and made in 50 colors");

  private MockMvc mockMvc;

  @Autowired
  private WebApplicationContext webApplicationContext;

  private final ObjectMapper objectMapper;

  public LoggingDataGeneratorForSplunk() {
    objectMapper = new ObjectMapper();
  }

  @BeforeEach
  public void setup() throws Exception {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.webApplicationContext)
        .apply(springSecurity())
        .build();
  }

  @Test
  public void contextLoads() {}

  @ParameterizedTest
  @MethodSource("provideData")
  public void bulkOrder(
      CreateUserRequest userRequest,
      HttpStatus httpStatusSignup,
      HttpStatus httpStatusLogin,
      List<List<ItemCount>> orders) throws Exception {
    // create credentials for signup
    String userRequestStr = objectMapper.writeValueAsString(userRequest);

    // singup user
    MvcResult result;
    result = mockMvc.perform(post("/api/user/create")
            .contentType(APPLICATION_JSON_UTF8)
            .content(userRequestStr))
        .andReturn();

    assertEquals(httpStatusSignup.value(), result.getResponse().getStatus());

    // create credentials for login
    String loginStr = new JSONObject()
        .put("username", userRequest.getUsername())
        .put("password", userRequest.getPassword())
        .toString();

    // login user
    result = mockMvc.perform(post("/login")
            .contentType(APPLICATION_JSON_UTF8)
            .content(loginStr))
        .andReturn();

    assertEquals(httpStatusLogin.value(), result.getResponse().getStatus());


    if (httpStatusLogin.is2xxSuccessful()) {
      assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());
      // get the JWT token
      String bearer = result.getResponse().getHeader(HttpHeaders.AUTHORIZATION);
      assertNotNull(bearer);
      assertTrue(bearer.startsWith("Bearer "));


      for (List<ItemCount> order : orders) {
        // variable to track the total value
        BigDecimal totalPrice = BigDecimal.ZERO;

        // add items to cart
        for (ItemCount itemCount : order) {
          Item item = itemCount.getItem();
          Integer count = itemCount.getCount();
          // update the the total value
          totalPrice = totalPrice.add(item.getPrice().multiply(BigDecimal.valueOf(count)));
          ModifyCartRequest cartRequest = new ModifyCartRequest();
          cartRequest.setUsername(userRequest.getUsername());
          cartRequest.setItemId(item.getId());
          cartRequest.setQuantity(count);

          result = mockMvc.perform(post("/api/cart/addToCart")
                  .header(HttpHeaders.AUTHORIZATION, bearer)
                  .contentType(APPLICATION_JSON_UTF8)
                  .content(objectMapper.writeValueAsString(cartRequest)))
              .andExpect(status().isOk())
              .andReturn();

          assertNotNull(result);

        }
        // submit order
        result = mockMvc.perform(post("/api/order/submit/" + userRequest.getUsername())
                .header(HttpHeaders.AUTHORIZATION, bearer))
            .andExpect(status().isOk())
            .andReturn();

        DocumentContext documentContext = JsonPath.parse(result.getResponse().getContentAsString());
        assertEquals(totalPrice, documentContext.read("$.total", BigDecimal.class));
      }
    }
  }

  private static Stream<Arguments> provideData() {
    return Stream.of(
        Arguments.of(
            new CreateUserRequest( "jackie", "passw1234", "passw1234"),
            HttpStatus.OK, HttpStatus.OK,
            Arrays.asList(
                Arrays.asList(
                    new ItemCount(ITEM_1, 2),
                    new ItemCount(ITEM_2, 1),
                    new ItemCount(ITEM_1, 3)),
                Arrays.asList(
                    new ItemCount(ITEM_2, 1),
                    new ItemCount(ITEM_1,5)
                ),
                Arrays.asList(
                    new ItemCount(ITEM_1, 2)
                )
            )
        ),
        Arguments.of(
            new CreateUserRequest("thomas", "passw1234", "passw1234"),
            HttpStatus.OK, HttpStatus.OK,
            Arrays.asList(
                Arrays.asList(
                    new ItemCount(ITEM_3, 1),
                    new ItemCount(ITEM_2, 2),
                    new ItemCount(ITEM_3, 2),
                    new ItemCount(ITEM_2, 1)
                ),
                Arrays.asList(
                    new ItemCount(ITEM_1, 1),
                    new ItemCount(ITEM_3, 4),
                    new ItemCount(ITEM_4, 1)
                ),
                Arrays.asList(
                    new ItemCount(ITEM_4, 4),
                    new ItemCount(ITEM_3, 5)
                ),
                Arrays.asList(
                    new ItemCount(ITEM_1, 1),
                    new ItemCount(ITEM_2, 1),
                    new ItemCount(ITEM_3, 1),
                    new ItemCount(ITEM_4, 1)
                )
            )
        ),
        Arguments.of(
            new CreateUserRequest("anne", "password11", "11password"),
            HttpStatus.BAD_REQUEST, HttpStatus.UNAUTHORIZED, null
        ),
        Arguments.of(
            new CreateUserRequest("john", "pass", "pass"),
            HttpStatus.BAD_REQUEST, HttpStatus.UNAUTHORIZED, null
        ),
        Arguments.of(
            new CreateUserRequest("alain", "passw1234", "passw1234"),
            HttpStatus.OK, HttpStatus.OK,
            Arrays.asList(
                Arrays.asList(
                    new ItemCount(ITEM_3, 3),
                    new ItemCount(ITEM_2, 2)
                )
            )
        ),
        Arguments.of(
            new CreateUserRequest("", "password11", "password11"),
            HttpStatus.BAD_REQUEST, HttpStatus.UNAUTHORIZED, null
        ),
        Arguments.of(
            new CreateUserRequest("anne1", "password11", "11password"),
            HttpStatus.BAD_REQUEST, HttpStatus.UNAUTHORIZED, null
        ),
        Arguments.of(
            new CreateUserRequest("john1", "pass", "pass"),
            HttpStatus.BAD_REQUEST, HttpStatus.UNAUTHORIZED, null
        ),
        Arguments.of(
            new CreateUserRequest("daniel", "passw1234", "passw1234"),
            HttpStatus.OK, HttpStatus.OK,
            Arrays.asList(
                Arrays.asList(
                    new ItemCount(ITEM_1, 1),
                    new ItemCount(ITEM_1, 1),
                    new ItemCount(ITEM_4, 6)
                ),
                Arrays.asList(
                    new ItemCount(ITEM_3, 2),
                    new ItemCount(ITEM_4, 1)
                )
            )
        ),
        Arguments.of(
            new CreateUserRequest("  ", "password1234", "password1234"),
            HttpStatus.BAD_REQUEST, HttpStatus.UNAUTHORIZED, null
        ),
        Arguments.of(
            new CreateUserRequest("anne", "password11", "11password"),
            HttpStatus.BAD_REQUEST, HttpStatus.UNAUTHORIZED, null
        ),
        Arguments.of(
            new CreateUserRequest("anne", "pass", "pass"),
            HttpStatus.BAD_REQUEST, HttpStatus.UNAUTHORIZED, null
        ),
        Arguments.of(
            new CreateUserRequest("zack", "passw1234", "passw1234"),
            HttpStatus.OK, HttpStatus.OK,
            Arrays.asList(
                Arrays.asList(
                    new ItemCount(ITEM_2, 1),
                    new ItemCount(ITEM_1, 1),
                    new ItemCount(ITEM_4, 1)
                ),
                Arrays.asList(
                    new ItemCount(ITEM_2, 2),
                    new ItemCount(ITEM_4, 5)
                ),
                Arrays.asList(
                    new ItemCount(ITEM_1, 1),
                    new ItemCount(ITEM_4, 4)
                )
            )
        )
    );
  }

}
