package com.zoo.care.repository;

import com.zoo.care.entity.Feedings;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FeedingRepository extends JpaRepository<Feedings, Long> {

    Optional<Feedings> findFirstByAnimalIdOrderByFedAtDescIdDesc(Long animalId);

    @EntityGraph(attributePaths = {"animal.species", "animal.enclosure", "recordedBy"})
    List<Feedings> findByFedAtGreaterThanEqualOrderByFedAtDescIdDesc(Instant from);

    @EntityGraph(attributePaths = {"animal.species", "animal.enclosure", "recordedBy"})
    List<Feedings> findByAnimalEnclosureIdInAndFedAtGreaterThanEqualOrderByFedAtDescIdDesc(
            Collection<Long> enclosureIds, Instant from);

    @Query("""
            select f from Feedings f
            join fetch f.recordedBy
            where f.animal.id in :animalIds
              and f.fedAt = (select max(f2.fedAt) from Feedings f2 where f2.animal = f.animal)
            """)
    List<Feedings> findLatestByAnimalIds(@Param("animalIds") Collection<Long> animalIds);
}