package com.zoo.care.repository;

import com.zoo.care.entity.Enclosure;
import com.zoo.care.entity.HabitatType;
import com.zoo.care.entity.KeeperAssignment;
import com.zoo.care.entity.Role;
import com.zoo.care.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import java.time.Instant;
import static org.assertj.core.api.Assertions.assertThat;


@DataJpaTest
public class KeeperAssignmentRepositoryTest {


    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private KeeperAssignmentRepository repository;

    @Test
    @DisplayName("Assignment for another enclosure is not considered active")
    void existsActiveAssignment_forAnotherEnclosure_returnsFalse() {
        AssignmentIds ids = persistAssignment(
                Instant.parse("2026-11-04T08:00:00Z"),
                Instant.parse("2026-11-04T16:00:00Z")
        );

        Enclosure anotherEnclosure = Enclosure.builder()
                .name("Another Savanna")
                .habitatType(HabitatType.SAVANNA)
                .capacity(5)
                .build();

        entityManager.persist(anotherEnclosure);
        entityManager.flush();

        Long anotherEnclosureId = anotherEnclosure.getId();

        entityManager.clear();

        boolean result = repository.existsActiveAssignment(
                anotherEnclosureId,
                ids.keeperId(),
                Instant.parse("2026-11-04T12:00:00Z")
        );

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Assignment for another keeper is not considered active")
    void existsActiveAssignment_forAnotherKeeper_returnsFalse() {
        AssignmentIds ids = persistAssignment(
                Instant.parse("2026-11-04T08:00:00Z"),
                Instant.parse("2026-11-04T16:00:00Z")
        );

        User anotherKeeper = User.builder()
                .username("another-keeper")
                .password("test-password-hash")
                .email("another-keeper@example.com")
                .fullName("Another Keeper")
                .role(Role.KEEPER)
                .build();

        entityManager.persist(anotherKeeper);
        entityManager.flush();

        Long anotherKeeperId = anotherKeeper.getId();

        entityManager.clear();

        boolean result = repository.existsActiveAssignment(
                ids.enclosureId(),
                anotherKeeperId,
                Instant.parse("2026-11-04T12:00:00Z")
        );

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Assignment without an end time is inactive before its start")
    void existsActiveAssignment_beforeStartWithoutEnd_returnsFalse() {
        AssignmentIds ids = persistAssignment(
                Instant.parse("2026-11-04T08:00:00Z"),
                null
        );

        boolean result = repository.existsActiveAssignment(
                ids.enclosureId(),
                ids.keeperId(),
                Instant.parse("2026-11-04T07:59:00Z")
        );

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Assignment without an end time is active after its start")
    void existsActiveAssignment_withoutEnd_returnsTrue() {
        AssignmentIds ids = persistAssignment(
                Instant.parse("2026-11-04T08:00:00Z"),
                null
        );

        boolean result = repository.existsActiveAssignment(
                ids.enclosureId(),
                ids.keeperId(),
                Instant.parse("2026-11-04T12:00:00Z")
        );

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Assignment is active at its exact start time")
    void existsActiveAssignment_atStart_returnsTrue() {
        AssignmentIds ids = persistAssignment(
                Instant.parse("2026-11-04T08:00:00Z"),
                Instant.parse("2026-11-04T16:00:00Z")
        );

        boolean result = repository.existsActiveAssignment(
                ids.enclosureId(),
                ids.keeperId(),
                Instant.parse("2026-11-04T08:00:00Z")
        );

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Assignment is inactive at its exact end time")
    void existsActiveAssignment_atEnd_returnsFalse() {
        AssignmentIds ids = persistAssignment(
                Instant.parse("2026-11-04T08:00:00Z"),
                Instant.parse("2026-11-04T16:00:00Z")
        );

        boolean result = repository.existsActiveAssignment(
                ids.enclosureId(),
                ids.keeperId(),
                Instant.parse("2026-11-04T16:00:00Z")
        );

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Assignment is active within its time period")
    void existsActiveAssignment_duringAssignment_returnsTrue() {
        AssignmentIds ids = persistAssignment(
                Instant.parse("2026-11-04T08:00:00Z"),
                Instant.parse("2026-11-04T16:00:00Z")
        );

        boolean result = repository.existsActiveAssignment(
                ids.enclosureId(),
                ids.keeperId(),
                Instant.parse("2026-11-04T12:00:00Z")
        );

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("findActive returns the assignment only while it is active")
    void findActive_onlyWhileActive() {
        AssignmentIds ids = persistAssignment(
                Instant.parse("2026-11-04T08:00:00Z"), Instant.parse("2026-11-04T10:00:00Z"));

        assertThat(repository.findActive(ids.enclosureId(), ids.keeperId(),
                Instant.parse("2026-11-04T09:00:00Z"))).isPresent();
        assertThat(repository.findActive(ids.enclosureId(), ids.keeperId(),
                Instant.parse("2026-11-04T12:00:00Z"))).isEmpty();
    }

    @Test
    @DisplayName("findActiveByEnclosure ignores finished assignments")
    void findActiveByEnclosure_ignoresFinished() {
        AssignmentIds ids = persistAssignment(
                Instant.parse("2026-11-04T08:00:00Z"), Instant.parse("2026-11-04T10:00:00Z"));

        assertThat(repository.findActiveByEnclosure(ids.enclosureId(),
                Instant.parse("2026-11-04T12:00:00Z"))).isEmpty();
        assertThat(repository.findActiveByEnclosure(ids.enclosureId(),
                Instant.parse("2026-11-04T09:00:00Z"))).hasSize(1);
    }

    @Test
    @DisplayName("deleteByEnclosureId removes the assignments of that enclosure")
    void deleteByEnclosureId_removesAssignments() {
        AssignmentIds ids = persistAssignment(Instant.parse("2026-11-04T08:00:00Z"), null);

        repository.deleteByEnclosureId(ids.enclosureId());
        entityManager.flush();

        assertThat(repository.count()).isZero();
    }

    private AssignmentIds persistAssignment(Instant startsAt, Instant endsAt) {
        User keeper = User.builder()
                .username("keeper-test")
                .password("test-password-hash")
                .email("keeper-test@example.com")
                .fullName("Test Keeper")
                .role(Role.KEEPER)
                .build();

        Enclosure enclosure = Enclosure.builder()
                .name("Savanna Test")
                .habitatType(HabitatType.SAVANNA)
                .capacity(5)
                .build();

        entityManager.persist(keeper);
        entityManager.persist(enclosure);

        KeeperAssignment assignment = KeeperAssignment.builder()
                .enclosure(enclosure)
                .keeper(keeper)
                .startsAt(startsAt)
                .endsAt(endsAt)
                .createdAt(Instant.parse("2026-11-04T07:00:00Z"))
                .build();

        entityManager.persist(assignment);
        entityManager.flush();

        AssignmentIds ids = new AssignmentIds(enclosure.getId(), keeper.getId());
        entityManager.clear();
        return ids;
    }

    private record AssignmentIds(Long enclosureId, Long keeperId) {
    }
}

