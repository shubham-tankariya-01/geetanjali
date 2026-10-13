package com.geetanjali.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

// ItemPhoto Entity: Stores cloth patterns, reference designs, or finished garment photos.
// In Cloudinary / S3 setups, this holds the URL and the cloud storage public ID.
// @Entity = Tells JPA/Hibernate that this class maps to a relational database table.
@Entity
// @Table = Specifies table name in PostgreSQL ("item_photo").
@Table(name = "item_photo")
public class ItemPhoto {

    // @Id = Marks this field as the primary key.
    // @GeneratedValue = Auto-increments using PostgreSQL IDENTITY column.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // @ManyToOne = Many photos can belong to one order item.
    // fetch = FetchType.LAZY = Don't fetch the parent order item until accessed.
    // @JoinColumn = Foreign key column "order_item_id".
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    // @Column = Accessible URL path to view the photo (e.g., "/uploads/photos/abc.jpg").
    @Column(name = "url", nullable = false)
    private String url;

    // @Column = Unique identifier for cloud providers (like Cloudinary public_id or S3 key) so it can be deleted later.
    @Column(name = "public_id")
    private String publicId;

    // Default zero-argument constructor: JPA/Hibernate requires this.
    public ItemPhoto() {
    }

    // Convenience constructor for creating a photo record.
    // Inputs: orderItem (OrderItem), url (String), publicId (String).
    // Output: New ItemPhoto instance.
    public ItemPhoto(OrderItem orderItem, String url, String publicId) {
        this.orderItem = orderItem;
        this.url = url;
        this.publicId = publicId;
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

    // Getter for orderItem.
    // Input: None.
    // Output: OrderItem.
    public OrderItem getOrderItem() {
        return orderItem;
    }

    // Setter for orderItem.
    // Input: OrderItem orderItem.
    // Output: None (void).
    public void setOrderItem(OrderItem orderItem) {
        this.orderItem = orderItem;
    }

    // Getter for url.
    // Input: None.
    // Output: String.
    public String getUrl() {
        return url;
    }

    // Setter for url.
    // Input: String url.
    // Output: None (void).
    public void setUrl(String url) {
        this.url = url;
    }

    // Getter for publicId.
    // Input: None.
    // Output: String.
    public String getPublicId() {
        return publicId;
    }

    // Setter for publicId.
    // Input: String publicId.
    // Output: None (void).
    public void setPublicId(String publicId) {
        this.publicId = publicId;
    }
}
