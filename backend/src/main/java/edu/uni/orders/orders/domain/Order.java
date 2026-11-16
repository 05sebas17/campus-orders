package edu.uni.orders.orders.domain;

import edu.uni.orders.products.domain.Product;
import edu.uni.orders.shared.ConflictException;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "orders")
public class Order {
  @Id private UUID id;

  @Column(nullable = false, unique = true, length = 40)
  private String number;

  @Column(nullable = false, length = 120)
  private String customerName;

  @Column(nullable = false, length = 254)
  private String email;

  @Column(length = 30)
  private String phone;

  @Embedded private ShippingAddress address;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private OrderStatus status;

  @Column(nullable = false, length = 3)
  private String currency;

  @Column(nullable = false, precision = 18, scale = 2)
  private BigDecimal total;

  @Column(nullable = false)
  private Instant createdAt;

  @Column(nullable = false)
  private Instant updatedAt;

  @Version private long version;

  @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("sku ASC")
  private List<OrderItem> items = new ArrayList<>();

  @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("changedAt ASC")
  private List<OrderStatusHistory> history = new ArrayList<>();

  protected Order() {}

  public Order(
      String customerName,
      String email,
      String phone,
      ShippingAddress address,
      Map<Product, Integer> products,
      Instant now) {
    if (products.isEmpty() || products.size() > 50)
      throw new IllegalArgumentException("Incluye entre 1 y 50 productos diferentes.");
    id = UUID.randomUUID();
    number = "PED-" + id.toString().replace("-", "").toUpperCase(Locale.ROOT);
    this.customerName = customerName;
    this.email = email;
    this.phone = phone;
    this.address = address;
    status = OrderStatus.PENDING;
    currency = "COP";
    createdAt = now;
    updatedAt = now;
    products.forEach((p, q) -> items.add(new OrderItem(this, p, q)));
    total =
        items.stream().map(OrderItem::getSubtotal).reduce(new BigDecimal("0.00"), BigDecimal::add);
    history.add(new OrderStatusHistory(this, null, status, now));
  }

  public void advance(OrderStatus next, long expectedVersion, Instant now) {
    if (version != expectedVersion)
      throw new ConflictException("El pedido cambió. Actualiza el detalle y vuelve a intentarlo.");
    status.validateTransition(next);
    if (status == next) return;
    history.add(new OrderStatusHistory(this, status, next, now));
    status = next;
    updatedAt = now;
  }

  public UUID getId() {
    return id;
  }

  public String getNumber() {
    return number;
  }

  public String getCustomerName() {
    return customerName;
  }

  public String getEmail() {
    return email;
  }

  public String getPhone() {
    return phone;
  }

  public ShippingAddress getAddress() {
    return address;
  }

  public OrderStatus getStatus() {
    return status;
  }

  public String getCurrency() {
    return currency;
  }

  public BigDecimal getTotal() {
    return total;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public long getVersion() {
    return version;
  }

  public List<OrderItem> getItems() {
    return Collections.unmodifiableList(items);
  }

  public List<OrderStatusHistory> getHistory() {
    return Collections.unmodifiableList(history);
  }
}
