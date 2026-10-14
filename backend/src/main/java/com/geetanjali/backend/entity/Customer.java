package com.geetanjali.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Customer Entity: Represents a customer who visits the tailor shop.
// In Express, this is the Customer model where orders and measurements are linked.
// @Entity = Tells JPA/Hibernate that this class maps to a relational database table.
@Entity
// @Table = Specifies the table name in PostgreSQL ("customer").
@Table(name = "customer")
public class Customer {

    // @Id = Marks this field as the primary key.
    // @GeneratedValue = Auto-increments using PostgreSQL IDENTITY column.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // @Column = Maps to the "name" column in the table.
    @Column(name = "name", nullable = false)
    private String name;

    // @Column = Customer's mobile number, must be unique across customers.
    @Column(name = "mobile", nullable = false, unique = true)
    private String mobile;

    // Default zero-argument constructor: JPA/Hibernate requires this.
    public Customer() {
    }

    // Convenience constructor for creating a customer with name and mobile.
    // Inputs: name (String), mobile (String).
    // Output: New Customer instance.
    public Customer(String name, String mobile) {
        this.name = name;
        this.mobile = mobile;
    }

    // Getter for id.
    // Input: None.
    // Output: Long (primary key).
    public Long getId() {
        return id;
    }

    // Setter for id.
    // Input: Long id.
    // Output: None (void).
    public void setId(Long id) {
        this.id = id;
    }

    // Getter for name.
    // Input: None.
    // Output: String.
    public String getName() {
        return name;
    }

    // Setter for name.
    // Input: String name.
    // Output: None (void).
    public void setName(String name) {
        this.name = name;
    }

    // Getter for mobile.
    // Input: None.
    // Output: String.
    public String getMobile() {
        return mobile;
    }

    // Setter for mobile.
    // Input: String mobile.
    // Output: None (void).
    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    // Convenience alias for getName() to support standard full name referencing.
    public String getFullName() {
        return name;
    }

    // Convenience alias for getMobile() to support standard phone referencing.
    public String getPhone() {
        return mobile;
    }
}
