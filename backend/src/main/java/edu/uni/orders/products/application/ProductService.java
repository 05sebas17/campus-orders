package edu.uni.orders.products.application;

import edu.uni.orders.products.domain.Product;
import edu.uni.orders.products.infrastructure.ProductRepository;
import edu.uni.orders.shared.NotFoundException;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProductService {
  private final ProductRepository repository;

  public ProductService(ProductRepository repository) {
    this.repository = repository;
  }

  public record ProductView(
      UUID id, String sku, String name, String description, BigDecimal price, boolean active) {
    static ProductView from(Product p) {
      return new ProductView(
          p.getId(), p.getSku(), p.getName(), p.getDescription(), p.getPrice(), p.isActive());
    }
  }

  public Page<ProductView> list(int page, int size) {
    if (page < 0 || size < 1 || size > 100)
      throw new IllegalArgumentException("Página inválida; tamaño entre 1 y 100.");
    return repository
        .findByActiveTrue(PageRequest.of(page, size, Sort.by("name", "id")))
        .map(ProductView::from);
  }

  public ProductView get(UUID id) {
    return ProductView.from(
        repository
            .findById(id)
            .orElseThrow(() -> new NotFoundException("Producto no encontrado.")));
  }

  public Map<UUID, Product> requireActive(Set<UUID> ids) {
    var result = new HashMap<UUID, Product>();
    for (var p : repository.findAllById(ids)) result.put(p.getId(), p);
    for (var id : ids) {
      if (!result.containsKey(id)) throw new NotFoundException("Producto no encontrado: " + id);
      if (!result.get(id).isActive())
        throw new IllegalArgumentException("El producto no está activo: " + id);
    }
    return result;
  }
}
