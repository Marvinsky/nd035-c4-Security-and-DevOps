package com.example.demo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.model.requests.CreateUserRequest;
import com.example.demo.model.requests.ModifyCartRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import javax.servlet.ServletContext;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@RunWith(SpringRunner.class)
@SpringBootTest
@WebAppConfiguration
public class SareetaApplicationTests {

	private static final MediaType APPLICATION_JSON_UTF8 = new MediaType(
			MediaType.APPLICATION_JSON.getType(), MediaType.APPLICATION_JSON.getSubtype(), Charset.forName("utf8"));


	private MockMvc mockMvc;

	@Autowired
	private WebApplicationContext webApplicationContext;

	private final ObjectMapper objectMapper;

	public SareetaApplicationTests() {
		objectMapper = new ObjectMapper();
	}

	@Before
	public void setup() throws Exception {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(this.webApplicationContext)
				.apply(springSecurity())
				.build();
	}

	@Test
	public void contextLoads() {}

	@Test
	public void givenWac_whenServletContext_thenItProvidesGreetController() {
		ServletContext servletContext = webApplicationContext.getServletContext();

		Assert.assertNotNull(servletContext);
		Assert.assertTrue(servletContext instanceof MockServletContext);
		Assert.assertNotNull(webApplicationContext.getBean("userController"));
	}

	@Test
	public void request_without_token_failed() throws Exception {
		mockMvc.perform(get("/api/item/100"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	public void request_with_valid_token_success() throws Exception {
		// JWT token
		String bearer = "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJtYXJ2aW5za3kiLCJleHAiOjE2NjY2MjIxMjV9.RjVgqHVVG-l_9920NPvVsWcJsBYtXV9UApCMvG1SIkshTQOkucmWKZZN-uum_jGS_MxgokY_CgBVYhk3B--B7A";

		mockMvc.perform(get("/api/item/1")
						.header(HttpHeaders.AUTHORIZATION, bearer))
				.andExpect(status().isOk());
	}

	@Test
	public void request_with_invalid_token_failed() throws Exception {
		// JWT token
		String bearer = "eyJ0eXAiOiJHOLA_PERU_PERUKV1QiLCJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJtYXJ2aW5za3kiLCJleHAiOjE2NjY2MjIxMjV9.RjVgqHVVG-l_9920NPvVsWcJsBYtXV9UApCMvG1SIkshTQOkucmWKZZN-uum_jGS_MxgokY_CgBVYhk3B--B7A";

		mockMvc.perform(get("/api/item/1")
						.header(HttpHeaders.AUTHORIZATION, bearer))
				.andExpect(status().isUnauthorized());
	}

	@Test
	public void login_unknown_user_failed() throws Exception {
		// create credentials for login
		String loginStr = new JSONObject()
				.put("username", "jock")
				.put("password", "passsssswwwwwwww")
				.toString();

		// login user
		mockMvc.perform(post("/login")
						.contentType(APPLICATION_JSON_UTF8)
						.content(loginStr))
				.andExpect(status().isUnauthorized());
	}

	@Test
	public void login_wrong_password_failed() throws Exception {
		// create credentials for login
		String loginStr = new JSONObject()
				.put("username", "jenny")
				.put("password", "passsssswwwwwwww")
				.toString();

		// login user
		mockMvc.perform(post("/login")
						.contentType(APPLICATION_JSON_UTF8)
						.content(loginStr))
				.andExpect(status().isUnauthorized());
	}

	@Test
	public void login_success() throws Exception {
		// create credentials for login
		String loginStr = new JSONObject()
				.put("username", "jenny")
				.put("password", "pass1234")
				.toString();

		// login user
		mockMvc.perform(post("/login")
						.contentType(APPLICATION_JSON_UTF8)
						.content(loginStr))
				.andExpect(status().isOk());
	}

	@Test
	public void order_success() throws Exception {
		// create credentials for signup
		CreateUserRequest userRequest = new CreateUserRequest("jack", "testpassw", "testpassw");
		String userRequestStr = objectMapper.writeValueAsString(userRequest);

		// singup user
		mockMvc.perform(post("/api/user/create")
						.contentType(APPLICATION_JSON_UTF8)
						.content(userRequestStr))
				.andExpect(status().isOk());


		// create credentials for login
		String loginStr = new JSONObject()
				.put("username", userRequest.getUsername())
				.put("password", userRequest.getPassword())
				.toString();

		// login user
		MvcResult result = mockMvc.perform(post("/login")
						.contentType(APPLICATION_JSON_UTF8)
						.content(loginStr))
				.andExpect(status().isOk())
				.andReturn();

		// get the JWT token
		String bearer = result.getResponse().getHeader(HttpHeaders.AUTHORIZATION);
		assertNotNull(bearer);
		assertTrue(bearer.startsWith("Bearer "));

		// get items
		result = mockMvc.perform(get("/api/item")
						.header(HttpHeaders.AUTHORIZATION, bearer))
				.andExpect(status().isOk())
				.andReturn();

		// check items
		String response = result.getResponse().getContentAsString();
		DocumentContext documentContext = JsonPath.parse(response);
		assertTrue((int) documentContext.read("$.length()") > 0);
		Long itemId = documentContext.read("$[0].id", Long.class);
		BigDecimal itemPrice = documentContext.read("$[0].price", BigDecimal.class);
		assertEquals(1L, itemId.longValue());
		assertEquals(BigDecimal.valueOf(2.99), itemPrice);

		// add item to cart
		int itemCount = 2;
		BigDecimal totalPrice = itemPrice.multiply(BigDecimal.valueOf(itemCount));
		ModifyCartRequest cartRequest = new ModifyCartRequest();
		cartRequest.setUsername(userRequest.getUsername());
		cartRequest.setItemId(itemId);
		cartRequest.setQuantity(itemCount);

		result = mockMvc.perform(post("/api/cart/addToCart")
						.header(HttpHeaders.AUTHORIZATION, bearer)
						.contentType(APPLICATION_JSON_UTF8)
						.content(objectMapper.writeValueAsString(cartRequest)))
				.andExpect(status().isOk())
				.andReturn();

		// check cart
		documentContext = JsonPath.parse(result.getResponse().getContentAsString());
		assertEquals(itemCount, (int) documentContext.read("$.items.length()"));
		assertEquals(totalPrice, documentContext.read("$.total", BigDecimal.class));

		// submit order
		result = mockMvc.perform(post("/api/order/submit/" + userRequest.getUsername())
						.header(HttpHeaders.AUTHORIZATION, bearer))
				.andExpect(status().isOk())
				.andReturn();

		// check order
		documentContext = JsonPath.parse(result.getResponse().getContentAsString());
		assertEquals(itemCount, (int) documentContext.read("$.items.length()"));
		assertEquals(totalPrice, documentContext.read("$.total", BigDecimal.class));
		assertEquals(userRequest.getUsername(), documentContext.read("$.user.username"));

	}

}
