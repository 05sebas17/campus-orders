package edu.uni.orders.orders.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "order_status_history")
public class OrderStatusHistory {
  @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "order_id")
  private Order order;

  @Enumerated(EnumType.STRING)
  @Column(length = 20)
  private OrderStatus previousStatus;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private OrderStatus newStatus;

  @Column(nullable = false)
  private Instant changedAt;

  protected OrderStatusHistory() {}

  public OrderStatusHistory(Order order, OrderStatus previous, OrderStatus next, Instant now) {
    this.id = UUID.randomUUID();
    this.order = order;
    previousStatus = previous;
    newStatus = next;
    changedAt = now;
  }

  public OrderStatus getPreviousStatus() {
    return previousStatus;
  }

  public OrderStatus getNewStatus() {
    return newStatus;
  }

  public Instant getChangedAt() {
    return changedAt;
  }
}
