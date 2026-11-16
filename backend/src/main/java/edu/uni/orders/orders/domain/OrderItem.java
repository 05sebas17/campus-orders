package edu.uni.orders.orders.domain;

import edu.uni.orders.products.domain.Product;
import jakarta.persistence.*;
import java.math.*;
import java.util.UUID;

@Entity
@Table(name = "order_items")
public class OrderItem {
  @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "order_id")
  private Order order;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "product_id")
  private Product product;

  @Column(nullable = false, length = 50)
  private String sku;

  @Column(nullable = false, length = 150)
  private String name;

  @Column(nullable = false, precision = 14, scale = 2)
  private BigDecimal unitPrice;

  private int quantity;

  @Column(nullable = false, precision = 18, scale = 2)
  private BigDecimal subtotal;

  protected OrderItem() {}

  public OrderItem(Order order, Product product, int quantity) {
    if (quantity < 1 || quantity > 1000)
      throw new IllegalArgumentException("La cantidad debe estar entre 1 y 1000.");
    this.id = UUID.randomUUID();
    this.order = order;
    this.product = product;
    this.quantity = quantity;
    this.sku = product.getSku();
    this.name = product.getName();
    this.unitPrice = product.getPrice().setScale(2, RoundingMode.HALF_UP);
    this.subtotal =
        unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
  }

  public UUID getProductId() {
    return product.getId();
  }

  public String getSku() {
    return sku;
  }

  public String getName() {
    return name;
  }

  public BigDecimal getUnitPrice() {
    return unitPrice;
  }

  public int getQuantity() {
    return quantity;
  }

  public BigDecimal getSubtotal() {
    return subtotal;
  }
}
