package edu.uni.orders.orders.api;

import static edu.uni.orders.orders.application.OrderContracts.*;

import edu.uni.orders.orders.application.OrderService;
import edu.uni.orders.orders.domain.OrderStatus;
import edu.uni.orders.shared.PageView;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.Instant;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {
  private final OrderService service;

  public OrderController(OrderService service) {
    this.service = service;
  }

  @PostMapping
  @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
  @Operation(
      summary = "Crear pedido",
      description =
          "Precios del servidor. Entre 1 y 50 productos únicos; cantidades de 1 a 1000. Total en COP.")
  public ResponseEntity<OrderView> create(@Valid @RequestBody CreateOrder input) {
    var result = service.create(input);
    return ResponseEntity.created(URI.create("/api/v1/orders/" + result.id())).body(result);
  }

  @GetMapping
  @Operation(
      summary = "Consultar pedidos",
      description =
          "Fechas UTC ISO-8601: from inclusivo, to exclusivo. Orden estable por campo solicitado y UUID.")
  public PageView<OrderSummary> list(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(required = false) OrderStatus status,
      @RequestParam(required = false) Instant from,
      @RequestParam(required = false) Instant to,
      @RequestParam(required = false) String search,
      @RequestParam(defaultValue = "createdAt") String sort,
      @RequestParam(defaultValue = "desc") String direction) {
    return PageView.from(service.list(page, size, status, from, to, search, sort, direction));
  }

  @GetMapping("/stats")
  public Map<String, Long> statistics() {
    return service.statistics();
  }

  @GetMapping("/{id}")
  public OrderView get(@PathVariable UUID id) {
    return service.get(id);
  }

  @GetMapping("/{id}/history")
  public List<HistoryView> history(@PathVariable UUID id) {
    return service.history(id);
  }

  @PatchMapping("/{id}/status")
  @Operation(
      summary = "Avanzar estado",
      description =
          "Requiere versión actual. Repetir el estado actual con versión vigente no cambia el pedido; un conflicto devuelve 409.")
  public OrderView change(@PathVariable UUID id, @Valid @RequestBody ChangeStatus input) {
    return service.change(id, input);
  }
}
