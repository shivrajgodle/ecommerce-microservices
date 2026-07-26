package com.learning.identity_service.repository;

import com.learning.identity_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User,Long> {

    // Derived query method — Spring Data parses this method NAME at
    // startup and generates the implementation (SELECT * FROM users
    // WHERE email = ?) without you writing any SQL/JPQL. We'll cover
    // the full derived-query vocabulary properly in the repository
    // deep-dive (coming right after this auth flow).
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
