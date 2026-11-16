package edu.uni.orders.products.api;

import edu.uni.orders.products.application.ProductService;
import edu.uni.orders.products.application.ProductService.ProductView;
import edu.uni.orders.shared.PageView;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {
  private final ProductService service;

  public ProductController(ProductService service) {
    this.service = service;
  }

  @GetMapping
  public PageView<ProductView> list(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) {
    return PageView.from(service.list(page, size));
  }

  @GetMapping("/{id}")
  public ProductView get(@PathVariable UUID id) {
    return service.get(id);
  }
}
