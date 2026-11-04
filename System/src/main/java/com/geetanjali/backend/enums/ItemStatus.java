package com.geetanjali.backend.enums;

// ItemStatus: Represents the lifecycle milestones for an individual custom garment item.
// In the tailor shop workflow, each garment progresses through:
// 1. PENDING: Initial booking state while garment is waiting to be cut or stitched.
// 2. DONE: Stitching is completed by tailor; ready for customer trial or pickup.
// 3. DELIVERED: Handed over to the customer (settled or override approved).
public enum ItemStatus {
    PENDING,
    DONE,
    DELIVERED
}
