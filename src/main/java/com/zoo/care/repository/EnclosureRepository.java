package com.zoo.care.repository;

import com.zoo.care.entity.Enclosure;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnclosureRepository extends JpaRepository<Enclosure, Long> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}