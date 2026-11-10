package com.zoo.care.repository;

import com.zoo.care.entity.Animal;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AnimalRepository extends JpaRepository<Animal, Long> {

    @Override
    @EntityGraph(attributePaths = {"species", "enclosure"})
    List<Animal> findAll(Sort sort);

    @Override
    @EntityGraph(attributePaths = {"species", "enclosure"})
    Optional<Animal> findById(Long id);

    boolean existsByNameIgnoreCase(String name);

    boolean existsBySpeciesId(Long speciesId);

    long countByEnclosureId(Long enclosureId);
}