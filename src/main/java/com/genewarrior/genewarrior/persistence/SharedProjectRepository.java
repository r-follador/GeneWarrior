package com.genewarrior.genewarrior.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SharedProjectRepository extends JpaRepository<SharedProjectEntity, Long> {
    boolean existsByShareKey(String shareKey);

    Optional<SharedProjectEntity> findByShareKey(String shareKey);
}
