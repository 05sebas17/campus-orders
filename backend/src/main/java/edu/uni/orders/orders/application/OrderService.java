package edu.uni.orders.orders.application;

import static edu.uni.orders.orders.application.OrderContracts.*;

import edu.uni.orders.orders.domain.*;
import edu.uni.orders.orders.infrastructure.OrderRepository;
import edu.uni.orders.products.application.ProductService;
import edu.uni.orders.products.domain.Product;
import edu.uni.orders.shared.NotFoundException;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
@Transactional(readOnly = true)
public class OrderService {
  private final OrderRepository repository;
  private final ProductService products;

  public OrderService(OrderRepository repository, ProductService products) {
    this.repository = repository;
    this.products = products;
  }

  @Transactional
  public OrderView create(CreateOrder input) {
    var ids = new HashSet<UUID>();
    for (var item : input.items())
      if (!ids.add(item.productId()))
        throw new IllegalArgumentException(
            "No se permiten productos repetidos; cambia la cantidad de la línea existente.");
    var catalog = products.requireActive(ids);
    var selected = new LinkedHashMap<Product, Integer>();
    input.items().forEach(i -> selected.put(catalog.get(i.productId()), i.quantity()));
    var order =
        new Order(
            input.customerName().trim(),
            input.email().trim(),
            input.phone(),
            input.address().toDomain(),
            selected,
            Instant.now());
    return OrderView.from(repository.saveAndFlush(order));
  }

  private Order require(UUID id) {
    return repository
        .findById(id)
        .orElseThrow(() -> new NotFoundException("Pedido no encontrado."));
  }

  public OrderView get(UUID id) {
    return OrderView.from(require(id));
  }

  public List<HistoryView> history(UUID id) {
    return require(id).getHistory().stream().map(HistoryView::from).toList();
  }

  @Transactional
  public OrderView change(UUID id, ChangeStatus input) {
    var order = require(id);
    order.advance(input.status(), input.version(), Instant.now());
    repository.flush();
    return OrderView.from(order);
  }

  @Transactional(isolation = Isolation.REPEATABLE_READ, readOnly = true)
  public Map<String, Long> statistics() {
    var result = new LinkedHashMap<String, Long>();
    long total = 0;
    for (var status : OrderStatus.values()) {
      long count = repository.countByStatus(status);
      result.put(status.name(), count);
      total += count;
    }
    result.put("total", total);
    return result;
  }

  public Page<OrderSummary> list(
      int page,
      int size,
      OrderStatus status,
      Instant from,
      Instant to,
      String search,
      String sort,
      String direction) {
    if (page < 0 || size < 1 || size > 100)
      throw new IllegalArgumentException("Página inválida; tamaño entre 1 y 100.");
    if (!Set.of("createdAt", "total", "status").contains(sort))
      throw new IllegalArgumentException("Ordena por createdAt, total o status.");
    if (!Set.of("asc", "desc").contains(direction))
      throw new IllegalArgumentException("Dirección de orden inválida.");
    if (from != null && to != null && !from.isBefore(to))
      throw new IllegalArgumentException("La fecha inicial debe ser anterior a la final.");
    if (search != null && search.length() > 120)
      throw new IllegalArgumentException("La búsqueda admite máximo 120 caracteres.");
    Specification<Order> spec =
        (root, query, cb) -> {
          var predicates = new ArrayList<Predicate>();
          if (status != null) predicates.add(cb.equal(root.get("status"), status));
          if (from != null) predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
          if (to != null) predicates.add(cb.lessThan(root.get("createdAt"), to));
          if (search != null && !search.isBlank()) {
            String pattern =
                "%"
                    + search
                        .trim()
                        .toLowerCase(Locale.ROOT)
                        .replace("!", "!!")
                        .replace("%", "!%")
                        .replace("_", "!_")
                    + "%";
            predicates.add(
                cb.or(
                    cb.like(cb.lower(root.get("number")), pattern, '!'),
                    cb.like(cb.lower(root.get("customerName")), pattern, '!'),
                    cb.like(cb.lower(root.get("email")), pattern, '!')));
          }
          return cb.and(predicates.toArray(Predicate[]::new));
        };
    var ordering = Sort.by(Sort.Direction.fromString(direction), sort).and(Sort.by("id"));
    return repository.findAll(spec, PageRequest.of(page, size, ordering)).map(OrderSummary::from);
  }
}
