package com.example.eventflow.identity;

import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.eventflow.boot.EventflowApplication;
import com.example.eventflow.iam.SysRole;
import com.example.eventflow.iam.SysRoleMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class AuthSliceATest {

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
  @Autowired private SysUserMapper userMapper;
  @Autowired private SysRoleMapper roleMapper;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void platformLoginBindsPlatformTenantAndOpensTenantList() throws Exception {
    String token = login("platform", "Passw0rd!");
    mockMvc
        .perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.roleCode").value("PLATFORM"))
        .andExpect(jsonPath("$.data.tenantType").value("PLATFORM"))
        .andExpect(jsonPath("$.data.permissions", hasItems("tenant:read", "tenant:write")));

    mockMvc
        .perform(get("/api/platform/tenants").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.items[*].tenantCode", hasItems("platform", "buyer", "org-demo")));
  }

  @Test
  void listRejectsInvalidPage() throws Exception {
    String token = login("platform", "Passw0rd!");
    mockMvc
        .perform(get("/api/platform/tenants").param("page", "0").header("Authorization", "Bearer " + token))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    mockMvc
        .perform(get("/api/platform/tenants").param("size", "101").header("Authorization", "Bearer " + token))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
  }

  @Test
  void organizerCannotListAllTenants() throws Exception {
    String token = login("organizer", "Passw0rd!");
    mockMvc
        .perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
        .andExpect(jsonPath("$.data.roleCode").value("ORGANIZER_ADMIN"))
        .andExpect(jsonPath("$.data.tenantId").value(1003));

    mockMvc
        .perform(get("/api/platform/tenants").header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden());
  }

  @Test
  void buyerCannotSwitchTenantViaHeader() throws Exception {
    String token = login("buyer", "Passw0rd!");
    mockMvc
        .perform(
            get("/api/tenants/me")
                .header("Authorization", "Bearer " + token)
                .header("X-Tenant-Id", "1003"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("TENANT_SWITCH_FORBIDDEN"));

    mockMvc
        .perform(get("/api/tenants/me").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id").value(1002))
        .andExpect(jsonPath("$.data.type").value("BUYER"));
  }

  @Test
  void loginRejectsBadPassword() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"buyer\",\"password\":\"wrong\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("INVALID_CREDENTIAL"));
  }

  @Test
  void loginRejectsOversizedPassword() throws Exception {
    String tooLong = "x".repeat(73);
    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"buyer\",\"password\":\"" + tooLong + "\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void expiredTokenVersionIsRejected() throws Exception {
    String token = login("buyer", "Passw0rd!");
    userMapper.incrementTokenVersion(2003L);
    mockMvc
        .perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void loginLocksAfterRepeatedFailures() throws Exception {
    String ip = "203.0.113.10";
    for (int i = 0; i < 5; i++) {
      mockMvc
          .perform(
              post("/api/auth/login")
                  .with(
                      request -> {
                        request.setRemoteAddr(ip);
                        return request;
                      })
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"username\":\"buyer\",\"password\":\"wrong\"}"))
          .andExpect(status().isUnauthorized());
    }
    mockMvc
        .perform(
            post("/api/auth/login")
                .with(
                    request -> {
                      request.setRemoteAddr(ip);
                      return request;
                    })
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"buyer\",\"password\":\"Passw0rd!\"}"))
        .andExpect(status().isTooManyRequests())
        .andExpect(jsonPath("$.code").value("LOGIN_RATE_LIMITED"));
  }

  @Test
  void demoOrganizerTenantHasMultipleBuiltinRoles() {
    org.assertj.core.api.Assertions.assertThat(roleMapper.findByTenantId(1003L))
        .extracting(SysRole::getCode)
        .containsExactlyInAnyOrder(
            "ORGANIZER_ADMIN", "ORGANIZER_OPERATOR", "ORGANIZER_CHECKIN", "ORGANIZER_FINANCE");
  }

  @Test
  void platformCreatesOrganizerWithCopiedRoleTemplate() throws Exception {
    String token = login("platform", "Passw0rd!");
    MvcResult created =
        mockMvc
            .perform(
                post("/api/platform/tenants")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"tenantCode\":\"org-alpha\",\"name\":\"模板主办方\",\"adminUsername\":\"alpha_admin\",\"adminPassword\":\"Passw0rd!\",\"adminDisplayName\":\"阿尔法管理员\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.type").value("ORGANIZER"))
            .andExpect(jsonPath("$.data.tenantCode").value("org-alpha"))
            .andExpect(jsonPath("$.data.users.length()").value(1))
            .andExpect(jsonPath("$.data.users[0].username").value("alpha_admin"))
            .andExpect(jsonPath("$.data.users[0].roleCode").value("ORGANIZER_ADMIN"))
            .andReturn();
    JsonNode createdJson = objectMapper.readTree(created.getResponse().getContentAsString());
    String tenantId = createdJson.path("data").path("id").asText();
    org.assertj.core.api.Assertions.assertThat(roleMapper.findByTenantId(Long.parseLong(tenantId)))
        .extracting(SysRole::getCode)
        .containsExactlyInAnyOrder(
            "ORGANIZER_ADMIN", "ORGANIZER_OPERATOR", "ORGANIZER_CHECKIN", "ORGANIZER_FINANCE");

    mockMvc
        .perform(
            post("/api/platform/tenants")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"tenantCode\":\"org-alpha\",\"name\":\"重复\",\"adminUsername\":\"alpha_admin2\",\"adminPassword\":\"Passw0rd!\",\"adminDisplayName\":\"重复\"}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("TENANT_CODE_TAKEN"));

    String adminToken = login("alpha_admin", "Passw0rd!");
    mockMvc
        .perform(get("/api/auth/me").header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.tenantType").value("ORGANIZER"))
        .andExpect(jsonPath("$.data.roleCode").value("ORGANIZER_ADMIN"));

    mockMvc
        .perform(
            patch("/api/platform/tenants/" + tenantId + "/status")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"DISABLED\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("DISABLED"));

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"alpha_admin\",\"password\":\"Passw0rd!\"}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("TENANT_DISABLED"));
  }

  @Test
  void organizerCannotCreateTenant() throws Exception {
    String token = login("organizer", "Passw0rd!");
    mockMvc
        .perform(
            post("/api/platform/tenants")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"tenantCode\":\"org-beta\",\"name\":\"越权\",\"adminUsername\":\"beta_admin\",\"adminPassword\":\"Passw0rd!\",\"adminDisplayName\":\"越权\"}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void cannotDisableBuiltinPlatformTenant() throws Exception {
    String token = login("platform", "Passw0rd!");
    mockMvc
        .perform(
            patch("/api/platform/tenants/1001/status")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"DISABLED\"}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("BUILTIN_TENANT_LOCKED"));
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
            .andExpect(jsonPath("$.code").value("OK"))
            .andReturn();
    String body = result.getResponse().getContentAsString();
    int start = body.indexOf("\"token\":\"") + 9;
    int end = body.indexOf('"', start);
    return body.substring(start, end);
  }
}
