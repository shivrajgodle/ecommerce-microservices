package com.learning.identity_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@Table(name = "roles")
@NoArgsConstructor // JPA requires a no-arg constructor to instantiate entities via reflection
public class Role extends BaseEntity{

    /**
     * Storing as an enum rather than a free-text String prevents typos
     * like "ADMN" from ever reaching the database, and gives us
     * compile-time safety anywhere in code that assigns a role.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "name", nullable = false, unique = true, length = 20)
    private RoleName name;


    /**
     * This is the INVERSE (non-owning) side of the relationship.
     * mappedBy = "roles" points at the field NAME on the User entity
     * that owns this relationship. This side does NOT get a @JoinTable —
     * it's purely a convenience for navigating Role -> Users in code.
     * Hibernate never writes to the join table based on changes made here.
     */
    @ManyToMany(mappedBy = "roles")
    private Set<User> users = new HashSet<>();

    private Role(RoleName name){
        this.name = name;
    }


}
