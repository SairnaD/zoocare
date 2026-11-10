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

    @Query("""
            select f.animal.id as animalId, max(f.fedAt) as lastFedAt
            from Feedings f
            where f.animal.id in :animalIds
            group by f.animal.id
            """)
    List<LastFeedingTime> findLastFeedingTimes(@Param("animalIds") Collection<Long> animalIds);

    @EntityGraph(attributePaths = {"animal.species", "animal.enclosure", "recordedBy"})
    List<Feedings> findByFedAtGreaterThanEqualOrderByFedAtDescIdDesc(Instant from);

    @EntityGraph(attributePaths = {"animal.species", "animal.enclosure", "recordedBy"})
    List<Feedings> findByAnimalEnclosureIdInAndFedAtGreaterThanEqualOrderByFedAtDescIdDesc(
            Collection<Long> enclosureIds, Instant from);
}