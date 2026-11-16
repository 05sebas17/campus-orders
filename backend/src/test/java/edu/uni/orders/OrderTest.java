package edu.uni.orders;

import static org.assertj.core.api.Assertions.*;

import edu.uni.orders.orders.domain.*;
import edu.uni.orders.products.domain.Product;
import edu.uni.orders.shared.ConflictException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;

class OrderTest {
  private final Instant now = Instant.parse("2026-01-01T00:00:00Z");

  private Order order() {
    return new Order(
        "Cliente",
        "test@example.com",
        null,
        new ShippingAddress("Calle 1", "Bogotá", "Bogotá", null, "CO"),
        Map.of(
            new Product(UUID.randomUUID(), "A", "Producto", new BigDecimal("1234.565"), true), 3),
        now);
  }

  @Test
  void calculatesMoneyWithExplicitRoundingAndInitialHistory() {
    var order = order();
    assertThat(order.getTotal()).isEqualByComparingTo("3703.71");
    assertThat(order.getItems().getFirst().getUnitPrice()).isEqualByComparingTo("1234.57");
    assertThat(order.getHistory()).hasSize(1);
    assertThat(order.getHistory().getFirst().getPreviousStatus()).isNull();
  }

  @Test
  void allowsOnlySequentialTransitions() {
    var order = order();
    assertThatThrownBy(() -> order.advance(OrderStatus.SHIPPED, 0, now))
        .isInstanceOf(ConflictException.class);
    order.advance(OrderStatus.IN_PROGRESS, 0, now);
    order.advance(OrderStatus.SHIPPED, 0, now);
    order.advance(OrderStatus.DELIVERED, 0, now);
    assertThat(order.getHistory()).hasSize(4);
    assertThatThrownBy(() -> order.advance(OrderStatus.PENDING, 0, now))
        .isInstanceOf(ConflictException.class);
  }

  @Test
  void repetitionIsNoOp() {
    var order = order();
    order.advance(OrderStatus.PENDING, 0, now.plusSeconds(10));
    assertThat(order.getHistory()).hasSize(1);
    assertThat(order.getUpdatedAt()).isEqualTo(now);
  }

  @Test
  void rejectsStaleVersionEvenOnRepeatedStatus() {
    assertThatThrownBy(() -> order().advance(OrderStatus.PENDING, 9, now))
        .isInstanceOf(ConflictException.class);
  }

  @Test
  void rejectsEmptyOrdersAndOutOfRangeQuantities() {
    assertThatThrownBy(() -> new Order("A", "a@b.co", null, null, Map.of(), now))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                new OrderItem(
                    order(), new Product(UUID.randomUUID(), "X", "X", BigDecimal.ONE, true), 0))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
