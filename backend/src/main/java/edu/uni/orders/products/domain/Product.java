package edu.uni.orders.products.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "products")
public class Product {
  @Id private UUID id;

  @Column(nullable = false, unique = true, length = 50)
  private String sku;

  @Column(nullable = false, length = 150)
  private String name;

  @Column(length = 500)
  private String description;

  @Column(nullable = false, precision = 14, scale = 2)
  private BigDecimal price;

  @Column(nullable = false)
  private boolean active;

  protected Product() {}

  public Product(UUID id, String sku, String name, BigDecimal price, boolean active) {
    this.id = id;
    this.sku = sku;
    this.name = name;
    this.price = price;
    this.active = active;
  }

  public UUID getId() {
    return id;
  }

  public String getSku() {
    return sku;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public BigDecimal getPrice() {
    return price;
  }

  public boolean isActive() {
    return active;
  }
}
