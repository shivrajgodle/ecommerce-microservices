package com.learning.identity_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "addresses")
@Getter
@Setter
@NoArgsConstructor
public class Address extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String street;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 100)
    private String state;

    @Column(name = "zip_code", nullable = false, length = 20)
    private String zipCode;

    @Column(nullable = false, length = 100)
    private String country;

    @Enumerated(EnumType.STRING)
    @Column(name = "address_type", nullable = false, length = 20)
    private AddressType addressType;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault = false;

    /**
     * THIS is the owning side of the relationship — it holds the actual
     * foreign key column (user_id). @ManyToOne always owns the relationship
     * by definition; there's no mappedBy option here because the FK
     * physically lives on this table's row.
     *
     * fetch = LAZY (explicit, even though it's already @ManyToOne's default)
     * — we're being explicit on purpose. @ManyToOne/@OneToOne default to
     * EAGER in the JPA spec, but Spring Boot + Hibernate commonly still
     * treat @ManyToOne as LAZY-friendly when explicitly annotated. Writing
     * it out removes any ambiguity for whoever reads this later — a good
     * habit, since "what's the actual fetch type here" is a very common
     * source of confusion (and a very common interview question).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public Address(String street, String city, String state, String zipCode,
                   String country, AddressType addressType) {
        this.street = street;
        this.city = city;
        this.state = state;
        this.zipCode = zipCode;
        this.country = country;
        this.addressType = addressType;
    }
}