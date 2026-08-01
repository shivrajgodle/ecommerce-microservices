package com.learning.catalog_service.repository;

import com.learning.catalog_service.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag,Long> {
    Optional<Tag> findByName(String name);
    List<Tag> findByNameIn(List<String> names); // used for "add these tags" style bulk looku
}
