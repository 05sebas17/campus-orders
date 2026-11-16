package edu.uni.orders.orders.application;

import edu.uni.orders.orders.domain.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

public final class OrderContracts {
  private OrderContracts() {}

  public record AddressInput(
      @NotBlank @Size(max = 200) String street,
      @NotBlank @Size(max = 100) String city,
      @NotBlank @Size(max = 100) String region,
      @Size(max = 20) String postalCode,
      @NotBlank @Size(max = 2, min = 2) String country) {
    public ShippingAddress toDomain() {
      return new ShippingAddress(
          street.trim(), city.trim(), region.trim(), postalCode, country.toUpperCase(Locale.ROOT));
    }
  }

  public record ItemInput(@NotNull UUID productId, @NotNull @Min(1) @Max(1000) Integer quantity) {}

  public record CreateOrder(
      @NotBlank @Size(max = 120) String customerName,
      @NotBlank @Email @Size(max = 254) String email,
      @Size(max = 30) String phone,
      @NotNull @Valid AddressInput address,
      @NotEmpty @Size(max = 50) List<@NotNull @Valid ItemInput> items) {}

  public record ChangeStatus(@NotNull OrderStatus status, @NotNull @PositiveOrZero Long version) {}

  public record ItemView(
      UUID productId,
      String sku,
      String name,
      BigDecimal unitPrice,
      int quantity,
      BigDecimal subtotal) {
    static ItemView from(OrderItem i) {
      return new ItemView(
          i.getProductId(),
          i.getSku(),
          i.getName(),
          i.getUnitPrice(),
          i.getQuantity(),
          i.getSubtotal());
    }
  }

  public record HistoryView(OrderStatus previousStatus, OrderStatus newStatus, Instant changedAt) {
    public static HistoryView from(OrderStatusHistory h) {
      return new HistoryView(h.getPreviousStatus(), h.getNewStatus(), h.getChangedAt());
    }
  }

  public record OrderSummary(
      UUID id,
      String number,
      String customerName,
      OrderStatus status,
      BigDecimal total,
      Instant createdAt) {
    public static OrderSummary from(Order o) {
      return new OrderSummary(
          o.getId(),
          o.getNumber(),
          o.getCustomerName(),
          o.getStatus(),
          o.getTotal(),
          o.getCreatedAt());
    }
  }

  public record OrderView(
      UUID id,
      String number,
      String customerName,
      String email,
      String phone,
      ShippingAddress address,
      OrderStatus status,
      String currency,
      BigDecimal total,
      Instant createdAt,
      Instant updatedAt,
      long version,
      List<ItemView> items,
      List<HistoryView> history) {
    public static OrderView from(Order o) {
      return new OrderView(
          o.getId(),
          o.getNumber(),
          o.getCustomerName(),
          o.getEmail(),
          o.getPhone(),
          o.getAddress(),
          o.getStatus(),
          o.getCurrency(),
          o.getTotal(),
          o.getCreatedAt(),
          o.getUpdatedAt(),
          o.getVersion(),
          o.getItems().stream().map(ItemView::from).toList(),
          o.getHistory().stream().map(HistoryView::from).toList());
    }
  }
}
