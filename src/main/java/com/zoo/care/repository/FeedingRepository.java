package com.zoo.care.repository;

import com.zoo.care.entity.Feedings;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.Optional;

public interface FeedingRepository extends JpaRepository<Feedings, Long> {

    Optional<Feedings> findFirstByAnimalIdOrderByFedAtDescIdDesc(Long animalId);
}
