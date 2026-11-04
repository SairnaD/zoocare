package com.zoo.care.repository;

import com.zoo.care.entity.Animal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnimalRepository extends JpaRepository<Animal, Long> {
    boolean existsByName(String name);
    List<Animal> findByEnclosureId(Long id);
}