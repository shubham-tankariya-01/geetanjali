package com.geetanjali.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Shop Entity: Represents the tailor shop itself (e.g. "Geetanjali Tailors").
// In Mongoose / Prisma, this is like your "Shop" model or schema.
// @Entity = Tells JPA/Hibernate that this Java class maps to a relational database table.
@Entity
// @Table = Specifies the exact name of the table in PostgreSQL ("shop").
@Table(name = "shop")
public class Shop {

    // @Id = Marks this field as the Primary Key of the table.
    // @GeneratedValue = Tells PostgreSQL to auto-generate the ID using IDENTITY (like AUTO_INCREMENT / SERIAL).
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // @Column = Maps this field to the "name" column in the table.
    @Column(name = "name", nullable = false)
    private String name;

    // Default zero-argument constructor: JPA/Hibernate requires this to instantiate objects when reading from the database.
    public Shop() {
    }

    // Convenience constructor for creating a new Shop with a name.
    // Input: shopName (String)
    // Output: A new Shop instance.
    public Shop(String name) {
        this.name = name;
    }

    // Getter for id.
    // Input: None.
    // Output: Long (the primary key).
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
    // Output: String (shop name).
    public String getName() {
        return name;
    }

    // Setter for name.
    // Input: String name.
    // Output: None (void).
    public void setName(String name) {
        this.name = name;
    }
}
