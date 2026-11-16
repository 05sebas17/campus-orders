package edu.uni.orders.orders.domain;

import jakarta.persistence.Embeddable;

@Embeddable
public record ShippingAddress(
    String street, String city, String region, String postalCode, String country) {}
