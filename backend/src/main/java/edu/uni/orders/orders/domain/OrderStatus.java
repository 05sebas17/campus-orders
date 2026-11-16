package edu.uni.orders.orders.domain;

import edu.uni.orders.shared.ConflictException;

public enum OrderStatus {
  PENDING,
  IN_PROGRESS,
  SHIPPED,
  DELIVERED;

  public void validateTransition(OrderStatus next) {
    if (next != this && next.ordinal() != ordinal() + 1)
      throw new ConflictException("Solo se permite avanzar al siguiente estado.");
  }
}
