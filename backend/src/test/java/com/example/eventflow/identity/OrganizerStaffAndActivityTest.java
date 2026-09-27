package com.example.eventflow.identity;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
class OrganizerStaffAndActivityTest {

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
  void adminCreatesStaffAndOperatorCannotManageStaff() throws Exception {
    String admin = login("organizer", "Passw0rd!");
    mockMvc
        .perform(get("/api/auth/me").header("Authorization", "Bearer " + admin))
        .andExpect(jsonPath("$.data.permissions", hasItem("user:write")));

    mockMvc
        .perform(
            post("/api/organizer/staff")
                .header("Authorization", "Bearer " + admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"username\":\"org_op1\",\"password\":\"Passw0rd!\",\"displayName\":\"运营甲\",\"roleCode\":\"ORGANIZER_OPERATOR\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.roleCode").value("ORGANIZER_OPERATOR"));

    String operator = login("org_op1", "Passw0rd!");
    mockMvc
        .perform(get("/api/organizer/staff").header("Authorization", "Bearer " + operator))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(
            patch("/api/organizer/staff/2002")
                .header("Authorization", "Bearer " + admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"DISABLED\"}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("LAST_ADMIN_REQUIRED"));
  }

  @Test
  void organizerSubmitsActivityAndPlatformReviews() throws Exception {
    String organizer = login("organizer", "Passw0rd!");
    Instant start = Instant.now().plus(10, ChronoUnit.DAYS);
    Instant end = start.plus(2, ChronoUnit.HOURS);
    Instant saleStart = Instant.now().plus(1, ChronoUnit.HOURS);
    Instant saleEnd = start.minus(1, ChronoUnit.HOURS);

    MvcResult created =
        mockMvc
            .perform(
                post("/api/organizer/activities")
                    .header("Authorization", "Bearer " + organizer)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"title\":\"春季音乐会\",\"description\":\"室内乐\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.reviewStatus").value("DRAFT"))
            .andReturn();
    String activityId = objectMapper.readTree(created.getResponse().getContentAsString()).path("data").path("id").asText();

    mockMvc
        .perform(
            post("/api/organizer/activities/" + activityId + "/submit")
                .header("Authorization", "Bearer " + organizer))
        .andExpect(status().isBadRequest());

    String showBody =
        "{\"name\":\"晚场\",\"startAt\":\""
            + start
            + "\",\"endAt\":\""
            + end
            + "\",\"saleStartAt\":\""
            + saleStart
            + "\",\"saleEndAt\":\""
            + saleEnd
            + "\"}";
    MvcResult withShow =
        mockMvc
            .perform(
                post("/api/organizer/activities/" + activityId + "/shows")
                    .header("Authorization", "Bearer " + organizer)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(showBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.shows.length()").value(1))
            .andReturn();
    String showId =
        objectMapper
            .readTree(withShow.getResponse().getContentAsString())
            .path("data")
            .path("shows")
            .get(0)
            .path("id")
            .asText();

    mockMvc
        .perform(
            post("/api/organizer/shows/" + showId + "/tiers")
                .header("Authorization", "Bearer " + organizer)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"name\":\"VIP\",\"unitPriceFen\":12800,\"perUserLimit\":2,\"totalQty\":100}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.shows[0].tiers[0].availableQty").value(100));

    mockMvc
        .perform(
            post("/api/organizer/activities/" + activityId + "/submit")
                .header("Authorization", "Bearer " + organizer))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.reviewStatus").value("PENDING"));

    mockMvc
        .perform(
            put("/api/organizer/activities/" + activityId)
                .header("Authorization", "Bearer " + organizer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"改名\"}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("ACTIVITY_LOCKED"));

    String platform = login("platform", "Passw0rd!");
    mockMvc
        .perform(get("/api/platform/reviews").header("Authorization", "Bearer " + platform))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.items[*].id", hasItems(activityId)));

    mockMvc
        .perform(
            post("/api/platform/reviews/" + activityId + "/decide")
                .header("Authorization", "Bearer " + platform)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"APPROVED\",\"comment\":\"通过\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.reviewStatus").value("APPROVED"))
        .andExpect(jsonPath("$.data.saleStatus").value("ON_SALE"));

    mockMvc
        .perform(
            post("/api/organizer/activities/" + activityId + "/off-sale")
                .header("Authorization", "Bearer " + organizer))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.saleStatus").value("CLOSED"));
  }

  private String login(String username, String password) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
            .andExpect(status().isOk())
            .andReturn();
    JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
    return json.path("data").path("token").asText();
  }
}
