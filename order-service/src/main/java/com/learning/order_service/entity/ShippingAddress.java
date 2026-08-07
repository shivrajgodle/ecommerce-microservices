package com.learning.order_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @Embeddable is a DIFFERENT tool from anything we've used so far — and
 * genuinely the right one here, worth contrasting carefully with
 * Identity Service's real Address entity:
 *
 * - Identity Service's Address is a full @Entity: its own table, its
 *   own id, its own lifecycle (can be created/updated/deleted
 *   independently of any single order, since a user reuses saved
 *   addresses across many orders).
 * - THIS ShippingAddress has no identity of its own, no separate
 *   table, and no lifecycle independent of the Order it belongs to —
 *   it's a pure VALUE snapshot, frozen at checkout time, describing
 *   "where this specific order shipped to." Two orders with the exact
 *   same shipping address are NOT sharing a reference to one row
 *   somewhere; they each have their own independent copy embedded
 *   directly into their own order row.
 *
 * This is precisely the same "does this need its own identity and
 * table, or is it just a value?" question that distinguishes
 * @Entity from @Embeddable in general — the answer here is
 * unambiguous: an order's shipping address, once placed, should
 * never change even if the user later edits or deletes that saved
 * address in Identity Service. Embedding a frozen copy is what
 * guarantees that.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class ShippingAddress {

    @Column(name = "shipping_street", nullable = false, length = 200)
    private String street;

    @Column(name = "shipping_city", nullable = false, length = 100)
    private String city;

    @Column(name = "shipping_state", nullable = false, length = 100)
    private String state;

    @Column(name = "shipping_zip_code", nullable = false, length = 20)
    private String zipCode;

    @Column(name = "shipping_country", nullable = false, length = 100)
    private String country;

    public ShippingAddress(String street, String city, String state, String zipCode, String country) {
        this.street = street;
        this.city = city;
        this.state = state;
        this.zipCode = zipCode;
        this.country = country;
    }
}
