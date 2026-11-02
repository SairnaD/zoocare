package com.zoo.care.repository;

import com.zoo.care.entity.Feeding;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpeciesRepository extends JpaRepository<Feeding, Long> {
}
