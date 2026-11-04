package com.geetanjali.backend.repository;

import com.geetanjali.backend.entity.Shop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// ShopRepository: Database access interface for the Shop entity.
// In Prisma / Mongoose, this is like `prisma.shop` or `ShopModel`.
// Spring Data JPA automatically generates the SQL queries behind the scenes (findById, save, delete, etc.).
// @Repository = Tells Spring to manage this interface as a data access bean and translate DB exceptions.
@Repository
public interface ShopRepository extends JpaRepository<Shop, Long> {
    // Inherits standard CRUD methods from JpaRepository:
    // - save(Shop): inserts or updates a shop
    // - findById(Long): finds a shop by ID
    // - findAll(): returns all shops
    // - count(): counts total shops
}
