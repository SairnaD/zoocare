package com.zoo.care.repository;

import com.zoo.care.entity.Enclosure;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EnclosureRepository extends JpaRepository<Enclosure, Long> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Enclosure e where e.id = :id")
    Optional<Enclosure> findByIdForUpdate(@Param("id") Long id);
}