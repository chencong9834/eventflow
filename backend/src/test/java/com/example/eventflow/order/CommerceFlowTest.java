package com.example.eventflow.order;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.eventflow.boot.EventflowApplication;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(classes = EventflowApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class CommerceFlowTest {

  @Container
  static final MySQLContainer<?> mysql =
      new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
          .withDatabaseName("eventflow")
          .withUsername("eventflow")
          .withPassword("eventflow");

  @Container
  static final GenericContainer<?> redis =
      new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

  @Container
  static final RabbitMQContainer rabbit =
      new RabbitMQContainer(DockerImageName.parse("rabbitmq:3.13-management"));

  @DynamicPropertySource
  static void registerProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", mysql::getJdbcUrl);
    registry.add("spring.datasource.username", mysql::getUsername);
    registry.add("spring.datasource.password", mysql::getPassword);
    registry.add("spring.data.redis.host", redis::getHost);
    registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    registry.add("spring.rabbitmq.host", rabbit::getHost);
    registry.add("spring.rabbitmq.port", rabbit::getAmqpPort);
    registry.add("spring.rabbitmq.username", rabbit::getAdminUsername);
    registry.add("spring.rabbitmq.password", rabbit::getAdminPassword);
  }

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void buyerPaysAndOrganizerRefunds() throws Exception {
    String organizer = login("organizer", "Passw0rd!");
    Instant start = Instant.now().plus(2, ChronoUnit.HOURS);
    Instant end = start.plus(2, ChronoUnit.HOURS);
    Instant saleStart = Instant.now().minus(1, ChronoUnit.HOURS);
    Instant saleEnd = start.plus(1, ChronoUnit.HOURS);
    MvcResult created =
        mockMvc
            .perform(
                post("/api/organizer/activities")
                    .header("Authorization", "Bearer " + organizer)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"title\":\"Onsale Show\"}"))
            .andExpect(status().isOk())
            .andReturn();
    String activityId = text(created, "/data/id");
    MvcResult withShow =
        mockMvc
            .perform(
                post("/api/organizer/activities/" + activityId + "/shows")
                    .header("Authorization", "Bearer " + organizer)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"name\":\"A\",\"startAt\":\""
                            + start
                            + "\",\"endAt\":\""
                            + end
                            + "\",\"saleStartAt\":\""
                            + saleStart
                            + "\",\"saleEndAt\":\""
                            + saleEnd
                            + "\"}"))
            .andExpect(status().isOk())
            .andReturn();
    String showId = objectMapper.readTree(withShow.getResponse().getContentAsString()).path("data").path("shows").get(0).path("id").asText();
    MvcResult withTier =
        mockMvc
            .perform(
                post("/api/organizer/shows/" + showId + "/tiers")
                    .header("Authorization", "Bearer " + organizer)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"A\",\"unitPriceFen\":1000,\"perUserLimit\":2,\"totalQty\":10}"))
            .andExpect(status().isOk())
            .andReturn();
    String tierId =
        objectMapper
            .readTree(withTier.getResponse().getContentAsString())
            .path("data")
            .path("shows")
            .get(0)
            .path("tiers")
            .get(0)
            .path("id")
            .asText();
    mockMvc
        .perform(post("/api/organizer/activities/" + activityId + "/submit").header("Authorization", "Bearer " + organizer))
        .andExpect(status().isOk());
    String platform = login("platform", "Passw0rd!");
    mockMvc
        .perform(
            post("/api/platform/reviews/" + activityId + "/decide")
                .header("Authorization", "Bearer " + platform)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"APPROVED\"}"))
        .andExpect(status().isOk());

    String buyer = login("buyer", "Passw0rd!");
    mockMvc
        .perform(get("/api/catalog/activities").header("Authorization", "Bearer " + buyer))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.items[0].id").value(activityId));

    MvcResult ordered =
        mockMvc
            .perform(
                post("/api/buyer/orders")
                    .header("Authorization", "Bearer " + buyer)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"showId\":" + showId + ",\"ticketTierId\":" + tierId + ",\"qty\":1}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("CREATED"))
            .andReturn();
    String orderId = text(ordered, "/data/id");

    mockMvc
        .perform(
            post("/api/buyer/orders/" + orderId + "/pay")
                .header("Authorization", "Bearer " + buyer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"success\":true}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("PAID"))
        .andExpect(jsonPath("$.data.tickets.length()").value(1));

    mockMvc
        .perform(
            post("/api/organizer/orders/" + orderId + "/refund")
                .header("Authorization", "Bearer " + organizer))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("REFUNDED"));
  }

  private String login(String username, String password) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
            .andExpect(status().isOk())
            .andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("token").asText();
  }

  private String text(MvcResult result, String pointer) throws Exception {
    JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString()).at(pointer);
    return node.asText();
  }
}
