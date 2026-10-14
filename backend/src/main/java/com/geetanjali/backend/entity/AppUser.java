package com.geetanjali.backend.entity;

import com.geetanjali.backend.enums.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

// AppUser Entity: Represents a staff member of the tailor shop (Shop Owner or Tailor).
// In Express, this is the "User" model you query when someone logs in.
// @Entity = Tells JPA/Hibernate that this Java class maps to a relational database table.
@Entity
// @Table = Maps this class to the "app_user" table in PostgreSQL ("user" is often a reserved SQL keyword).
@Table(name = "app_user")
public class AppUser {

    // @Id = Marks this field as the Primary Key.
    // @GeneratedValue = Auto-increments using PostgreSQL IDENTITY column.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // @ManyToOne = Many users belong to one shop (Foreign Key relationship).
    // fetch = FetchType.LAZY = Don't load the Shop from DB until explicitly accessed (saves DB memory/queries).
    // @JoinColumn = Specifies the foreign key column name in the database table ("shop_id").
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;

    // @Column = Maps to column "full_name" in the table.
    @Column(name = "full_name", nullable = false)
    private String fullName;

    // @Column = Mobile number used as unique login ID (unique = true ensures no duplicate mobile numbers).
    @Column(name = "mobile", nullable = false, unique = true)
    private String mobile;

    // @Column = BCrypt hashed password (never store plain text passwords).
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    // @Enumerated(EnumType.STRING) = Stores enum as readable text ("OWNER", "TAILOR") instead of numbers (0, 1).
    // WHY: Storing text prevents database bugs if someone adds a new role in the future.
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    // Default zero-argument constructor: JPA/Hibernate requires this to instantiate objects.
    public AppUser() {
    }

    // Constructor with all fields (excluding id) to easily create a new user during signup.
    // Inputs: shop (Shop), fullName (String), mobile (String), passwordHash (String), role (Role)
    // Output: A newly created AppUser object.
    public AppUser(Shop shop, String fullName, String mobile, String passwordHash, Role role) {
        this.shop = shop;
        this.fullName = fullName;
        this.mobile = mobile;
        this.passwordHash = passwordHash;
        this.role = role;
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

    // Getter for shop.
    // Input: None.
    // Output: Shop.
    public Shop getShop() {
        return shop;
    }

    // Setter for shop.
    // Input: Shop shop.
    // Output: None (void).
    public void setShop(Shop shop) {
        this.shop = shop;
    }

    // Getter for fullName.
    // Input: None.
    // Output: String.
    public String getFullName() {
        return fullName;
    }

    // Setter for fullName.
    // Input: String fullName.
    // Output: None (void).
    public void setFullName(String fullName) {
        this.fullName = fullName;
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

    // Getter for passwordHash.
    // Input: None.
    // Output: String (BCrypt hash).
    public String getPasswordHash() {
        return passwordHash;
    }

    // Setter for passwordHash.
    // Input: String passwordHash.
    // Output: None (void).
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    // Getter for role.
    // Input: None.
    // Output: Role enum.
    public Role getRole() {
        return role;
    }

    // Setter for role.
    // Input: Role role.
    // Output: None (void).
    public void setRole(Role role) {
        this.role = role;
    }

    // Helper method to check if this staff member is a shop owner.
    // Output: boolean (true if role is OWNER).
    public boolean isOwner() {
        return this.role == Role.OWNER;
    }

    // Helper method to check if this staff member is a tailor.
    // Output: boolean (true if role is TAILOR).
    public boolean isTailor() {
        return this.role == Role.TAILOR;
    }
}
