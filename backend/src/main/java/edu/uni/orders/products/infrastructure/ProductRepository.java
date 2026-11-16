package edu.uni.orders.products.infrastructure;

import edu.uni.orders.products.domain.Product;
import java.util.UUID;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, UUID> {
  Page<Product> findByActiveTrue(Pageable pageable);
}
