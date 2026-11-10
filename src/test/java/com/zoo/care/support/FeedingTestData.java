package com.zoo.care.support;

import com.zoo.care.entity.*;
import jakarta.persistence.EntityManager;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Valid, independent fixtures shared only by the feeding persistence tests. */
public final class FeedingTestData {
    private FeedingTestData() { }

    public record Fixture(Animal animal, User keeper, User secondKeeper, KeeperAssignment assignment) { }

    public static Fixture persist(EntityManager em, Instant now) {
        String suffix = UUID.randomUUID().toString().substring(0, 12);
        Species species = Species.builder().name("Species-" + suffix)
                .habitatType(HabitatType.SAVANNA).dietType(DietType.HERBIVORE).build();
        Enclosure enclosure = Enclosure.builder().name("Enclosure-" + suffix)
                .habitatType(HabitatType.SAVANNA).capacity(10).build();
        User keeper = user("keeper-" + suffix);
        User secondKeeper = user("other-" + suffix);
        em.persist(species);
        em.persist(enclosure);
        em.persist(keeper);
        em.persist(secondKeeper);
        Animal animal = Animal.builder().name("Animal-" + suffix).species(species).enclosure(enclosure)
                .birthDate(LocalDate.of(2020, 1, 1)).weight(100)
                .feedingIntervalHours(6)
                .deletedStatus(DeletedStatus.PRESENT).build();
        em.persist(animal);
        KeeperAssignment assignment = assignment(enclosure, keeper, now);
        em.persist(assignment);
        em.persist(assignment(enclosure, secondKeeper, now));
        em.flush();
        return new Fixture(animal, keeper, secondKeeper, assignment);
    }

    private static User user(String name) {
        return User.builder().username(name).email(name + "@example.com").password("test-hash")
                .fullName(name).role(Role.KEEPER).build();
    }

    private static KeeperAssignment assignment(Enclosure enclosure, User keeper, Instant now) {
        return KeeperAssignment.builder().enclosure(enclosure).keeper(keeper)
                .startsAt(now.minusSeconds(3600)).createdAt(now.minusSeconds(3600)).build();
    }
}
