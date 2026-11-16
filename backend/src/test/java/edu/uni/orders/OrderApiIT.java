package edu.uni.orders;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.ws.test.server.RequestCreators.withPayload;
import static org.springframework.ws.test.server.ResponseMatchers.*;

import com.fasterxml.jackson.databind.*;
import edu.uni.orders.orders.application.OrderContracts.*;
import edu.uni.orders.orders.application.OrderService;
import edu.uni.orders.orders.domain.*;
import edu.uni.orders.orders.infrastructure.OrderRepository;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.ws.test.server.MockWebServiceClient;
import org.springframework.xml.transform.StringSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class OrderApiIT {
  @Container
  static PostgreSQLContainer<?> database = new PostgreSQLContainer<>("postgres:17.6-alpine");

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", database::getJdbcUrl);
    registry.add("spring.datasource.username", database::getUsername);
    registry.add("spring.datasource.password", database::getPassword);
  }

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper mapper;
  @Autowired JdbcTemplate jdbc;
  @Autowired OrderService service;
  @Autowired OrderRepository repository;
  @Autowired PlatformTransactionManager transactionManager;
  @Autowired ApplicationContext context;
  private final UUID product = UUID.fromString("11111111-1111-4111-8111-111111111111");

  @BeforeEach
  void reset() {
    jdbc.execute("TRUNCATE order_status_history,order_items,orders,products CASCADE");
    jdbc.update(
        "INSERT INTO products VALUES (?, 'UNI-1','Cuaderno','Papel',12345.67,true)", product);
  }

  private CreateOrder input() {
    return new CreateOrder(
        "Cliente prueba",
        "cliente@example.com",
        null,
        new AddressInput("Calle 1", "Bogotá", "Bogotá", null, "CO"),
        List.of(new ItemInput(product, 2)));
  }

  private String body(Object input) throws Exception {
    return mapper.writeValueAsString(input);
  }

  @Test
  void openApiDocumentsCreatedAndProblemResponses() throws Exception {
    mvc.perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.paths['/api/v1/orders'].post.responses['201']").exists())
        .andExpect(jsonPath("$.components.schemas.ApiProblem.properties.traceId").exists());
  }

  @Test
  void createsPersistsAndPreservesHistoricalPrices() throws Exception {
    var response =
        mvc.perform(
                post("/api/v1/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body(input())))
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andExpect(header().exists("X-Correlation-ID"))
            .andExpect(jsonPath("$.total").value(24691.34))
            .andReturn();
    var id = mapper.readTree(response.getResponse().getContentAsString()).get("id").asText();
    jdbc.update("UPDATE products SET price=999,name='Otro nombre' WHERE id=?", product);
    mvc.perform(get("/api/v1/orders/" + id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].name").value("Cuaderno"))
        .andExpect(jsonPath("$.items[0].unitPrice").value(12345.67))
        .andExpect(jsonPath("$.history.length()").value(1));
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE success", Integer.class))
        .isEqualTo(1);
  }

  @Test
  void rejectsInvalidBodiesDuplicateProductsAndMissingProducts() throws Exception {
    mvc.perform(post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.customerName").exists())
        .andExpect(jsonPath("$.traceId").isNotEmpty());
    var invalid = mapper.valueToTree(input());
    ((com.fasterxml.jackson.databind.node.ObjectNode) invalid).put("email", "invalid");
    mvc.perform(
            post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalid.toString()))
        .andExpect(status().isBadRequest());
    var duplicate =
        new CreateOrder(
            input().customerName(),
            input().email(),
            null,
            input().address(),
            List.of(new ItemInput(product, 1), new ItemInput(product, 1)));
    mvc.perform(
            post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON).content(body(duplicate)))
        .andExpect(status().isBadRequest());
    jdbc.update("UPDATE products SET active=false");
    mvc.perform(
            post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON).content(body(input())))
        .andExpect(status().isBadRequest());
    jdbc.update("DELETE FROM products");
    mvc.perform(
            post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON).content(body(input())))
        .andExpect(status().isNotFound());
    assertThat(repository.count()).isZero();
  }

  @Test
  void rejectsFractionalZeroAndOversizedQuantitiesAndClientPrices() throws Exception {
    for (String quantity : List.of("0", "-1", "1.5", "1001")) {
      String json = body(input()).replace("\"quantity\":2", "\"quantity\":" + quantity);
      mvc.perform(post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON).content(json))
          .andExpect(status().isBadRequest());
    }
    var tree = (com.fasterxml.jackson.databind.node.ObjectNode) mapper.valueToTree(input());
    tree.put("total", 1);
    mvc.perform(
            post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON).content(tree.toString()))
        .andExpect(status().isBadRequest());
    var empty =
        new CreateOrder(
            input().customerName(), input().email(), null, input().address(), List.of());
    mvc.perform(post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON).content(body(empty)))
        .andExpect(status().isBadRequest());
    assertThat(repository.count()).isZero();
  }

  @Test
  void jpaVersionDetectsAWriteAfterAnEntityWasRead() {
    UUID id = service.create(input()).id();
    assertThatThrownBy(
            () ->
                new TransactionTemplate(transactionManager)
                    .executeWithoutResult(
                        tx -> {
                          var stale = repository.findById(id).orElseThrow();
                          var concurrent = new TransactionTemplate(transactionManager);
                          concurrent.setPropagationBehavior(
                              org.springframework.transaction.TransactionDefinition
                                  .PROPAGATION_REQUIRES_NEW);
                          concurrent.executeWithoutResult(
                              other ->
                                  jdbc.update(
                                      "UPDATE orders SET version=version+1 WHERE id=?", id));
                          stale.advance(OrderStatus.IN_PROGRESS, 0, Instant.now());
                          repository.flush();
                        }))
        .isInstanceOf(org.springframework.dao.OptimisticLockingFailureException.class);
    assertThat(service.history(id)).hasSize(1);
    assertThat(service.get(id).status()).isEqualTo(OrderStatus.PENDING);
  }

  @Test
  void transitionsAreSequentialIdempotentAndVersioned() throws Exception {
    var order = service.create(input());
    String path = "/api/v1/orders/" + order.id() + "/status";
    mvc.perform(
            patch(path)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(new ChangeStatus(OrderStatus.PENDING, 0L))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.version").value(0))
        .andExpect(jsonPath("$.history.length()").value(1));
    mvc.perform(
            patch(path)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(new ChangeStatus(OrderStatus.SHIPPED, 0L))))
        .andExpect(status().isConflict());
    mvc.perform(
            patch(path)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(new ChangeStatus(OrderStatus.IN_PROGRESS, 0L))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.version").value(1));
    mvc.perform(
            patch(path)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(new ChangeStatus(OrderStatus.SHIPPED, 0L))))
        .andExpect(status().isConflict());
    service.change(order.id(), new ChangeStatus(OrderStatus.SHIPPED, 1L));
    service.change(order.id(), new ChangeStatus(OrderStatus.DELIVERED, 2L));
    mvc.perform(get("/api/v1/orders/" + order.id() + "/history"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(4));
  }

  @Test
  void missingResourcesAndInvalidQueryReturnCorrectErrors() throws Exception {
    mvc.perform(get("/api/v1/orders/" + UUID.randomUUID())).andExpect(status().isNotFound());
    for (String query :
        List.of(
            "size=101",
            "page=-1",
            "sort=email",
            "direction=wrong",
            "status=BAD",
            "from=bad",
            "from=2026-02-01T00:00:00Z&to=2026-01-01T00:00:00Z"))
      mvc.perform(get("/api/v1/orders?" + query)).andExpect(status().isBadRequest());
    mvc.perform(get("/api/v1/products/" + product))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sku").value("UNI-1"));
    mvc.perform(get("/api/v1/products"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1));
  }

  @Test
  void filtersAndPaginationAreStableAndStatsAreGlobal() throws Exception {
    var first = service.create(input());
    var second = service.create(input());
    var third = service.create(input());
    jdbc.update("UPDATE orders SET created_at='2026-01-01T00:00:00Z' WHERE id=?", first.id());
    jdbc.update("UPDATE orders SET created_at='2026-01-02T00:00:00Z' WHERE id=?", second.id());
    jdbc.update("UPDATE orders SET created_at='2026-01-03T00:00:00Z' WHERE id=?", third.id());
    service.change(second.id(), new ChangeStatus(OrderStatus.IN_PROGRESS, 0L));
    mvc.perform(
            get(
                "/api/v1/orders?from=2026-01-01T00:00:00Z&to=2026-01-03T00:00:00Z&size=1&sort=createdAt&direction=asc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(2))
        .andExpect(jsonPath("$.content[0].id").value(first.id().toString()));
    mvc.perform(get("/api/v1/orders?page=1&size=1&sort=createdAt&direction=asc"))
        .andExpect(jsonPath("$.content[0].id").value(second.id().toString()));
    mvc.perform(get("/api/v1/orders?status=IN_PROGRESS&search=CLIENTE@EXAMPLE.COM"))
        .andExpect(jsonPath("$.totalElements").value(1));
    mvc.perform(get("/api/v1/orders?search=" + first.number()))
        .andExpect(jsonPath("$.totalElements").value(1));
    mvc.perform(get("/api/v1/orders?search=%")).andExpect(jsonPath("$.totalElements").value(0));
    mvc.perform(get("/api/v1/orders/stats"))
        .andExpect(jsonPath("$.total").value(3))
        .andExpect(jsonPath("$.PENDING").value(2));
    var a = service.list(0, 1, null, null, null, null, "total", "asc");
    var b = service.list(1, 1, null, null, null, null, "total", "asc");
    assertThat(a.getContent().getFirst().id()).isNotEqualTo(b.getContent().getFirst().id());
  }

  @Test
  void simultaneousWritesCannotOverwriteEachOther() throws Exception {
    UUID id = service.create(input()).id();
    var barrier = new CyclicBarrier(2);
    Callable<Boolean> update =
        () -> {
          try {
            new TransactionTemplate(transactionManager)
                .executeWithoutResult(
                    tx -> {
                      var order = repository.findById(id).orElseThrow();
                      order.getHistory().size();
                      try {
                        barrier.await(10, TimeUnit.SECONDS);
                      } catch (Exception e) {
                        throw new RuntimeException(e);
                      }
                      order.advance(OrderStatus.IN_PROGRESS, 0, Instant.now());
                      repository.flush();
                    });
            return true;
          } catch (org.springframework.dao.OptimisticLockingFailureException
              | org.springframework.dao.DataIntegrityViolationException expected) {
            return false;
          }
        };
    try (var pool = Executors.newFixedThreadPool(2)) {
      var one = pool.submit(update);
      var two = pool.submit(update);
      assertThat(List.of(one.get(20, TimeUnit.SECONDS), two.get(20, TimeUnit.SECONDS)))
          .containsExactlyInAnyOrder(true, false);
    }
    assertThat(service.history(id)).hasSize(2);
    assertThat(service.get(id).version()).isEqualTo(1);
  }

  @Test
  void soapContractReturnsStatusAndClientFaults() {
    var order = service.create(input());
    var soap = MockWebServiceClient.createClient(context);
    soap.sendRequest(
            withPayload(
                new StringSource(
                    "<GetOrderStatusRequest xmlns='urn:uni:orders:v1'><orderId>"
                        + order.id()
                        + "</orderId></GetOrderStatusRequest>")))
        .andExpect(noFault())
        .andExpect(
            xpath("/o:GetOrderStatusResponse/o:status", Map.of("o", "urn:uni:orders:v1"))
                .evaluatesTo("PENDING"));
    soap.sendRequest(
            withPayload(
                new StringSource(
                    "<GetOrderStatusRequest xmlns='urn:uni:orders:v1'><orderId>invalid</orderId></GetOrderStatusRequest>")))
        .andExpect(clientOrSenderFault());
    soap.sendRequest(
            withPayload(
                new StringSource(
                    "<GetOrderStatusRequest xmlns='urn:uni:orders:v1'><orderId>"
                        + UUID.randomUUID()
                        + "</orderId></GetOrderStatusRequest>")))
        .andExpect(clientOrSenderFault());
  }

  @Test
  void blocksDoctypeBeforeSoapParsing() throws Exception {
    mvc.perform(
            post("/ws")
                .contentType("text/xml")
                .content("<!DOCTYPE a [<!ENTITY x SYSTEM 'file:///etc/passwd'>]><a>&x;</a>"))
        .andExpect(status().isBadRequest())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("XML inválido")));
  }
}
