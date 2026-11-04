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

