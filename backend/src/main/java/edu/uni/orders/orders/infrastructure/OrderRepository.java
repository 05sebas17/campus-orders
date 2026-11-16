package edu.uni.orders.orders.infrastructure;

import edu.uni.orders.orders.domain.*;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;

public interface OrderRepository
    extends JpaRepository<Order, UUID>, JpaSpecificationExecutor<Order> {
  long countByStatus(OrderStatus status);
}
