package com.zoo.care.repository;

import com.zoo.care.entity.Animal;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AnimalRepository extends JpaRepository<Animal, Long> {

    @Override
    @EntityGraph(attributePaths = {"species", "enclosure"})
    List<Animal> findAll(Sort sort);

    @Override
    @EntityGraph(attributePaths = {"species", "enclosure"})
    Optional<Animal> findById(Long id);

    @EntityGraph(attributePaths = {"species", "enclosure"})
    List<Animal> findByEnclosureIdInOrderByNameAsc(Collection<Long> enclosureIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Animal a where a.id = :id")
    Optional<Animal> findByIdForUpdate(@Param("id") Long id);

    boolean existsByNameIgnoreCase(String name);

    long countByEnclosureId(Long enclosureId);

    boolean existsBySpeciesId(Long speciesId);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}