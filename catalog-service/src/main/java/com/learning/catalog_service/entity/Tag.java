package com.learning.catalog_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Table(name = "tags")
@Entity
public class Tag extends BaseEntity{

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    public Tag(String name){
        this.name = name;
    }

}
