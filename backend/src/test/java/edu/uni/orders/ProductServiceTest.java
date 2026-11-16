package edu.uni.orders;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import edu.uni.orders.products.application.ProductService;
import edu.uni.orders.products.domain.Product;
import edu.uni.orders.products.infrastructure.ProductRepository;
import edu.uni.orders.shared.NotFoundException;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;

class ProductServiceTest {
  @Test
  void rejectsMissingAndInactiveProducts() {
    var repository = mock(ProductRepository.class);
    var service = new ProductService(repository);
    var id = UUID.randomUUID();
    when(repository.findAllById(Set.of(id))).thenReturn(List.of());
    assertThatThrownBy(() -> service.requireActive(Set.of(id)))
        .isInstanceOf(NotFoundException.class);
    when(repository.findAllById(Set.of(id)))
        .thenReturn(List.of(new Product(id, "A", "A", BigDecimal.TEN, false)));
    assertThatThrownBy(() -> service.requireActive(Set.of(id)))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
