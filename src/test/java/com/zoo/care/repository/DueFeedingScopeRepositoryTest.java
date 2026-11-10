package com.zoo.care.repository;

import com.zoo.care.entity.KeeperAssignment;
import com.zoo.care.support.FeedingTestData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class DueFeedingScopeRepositoryTest {
    private static final Instant NOW = Instant.parse("2026-11-10T12:00:00Z");
    @Autowired private TestEntityManager em;
    @Autowired private KeeperAssignmentRepository assignments;
    @Autowired private AnimalRepository animals;

    @Test
    void activeAssignmentsAndAnimals_areScopedToKeeperAndCurrentEnclosure() {
        var own = FeedingTestData.persist(em.getEntityManager(), NOW);
        var other = FeedingTestData.persist(em.getEntityManager(), NOW);
        em.flush();
        em.clear();
        var active = assignments.findActiveByKeeper(own.keeper().getId(), NOW);
        assertThat(active).extracting(a -> a.getEnclosure().getId())
                .containsExactly(own.animal().getEnclosure().getId());
        var ids = active.stream().map(a -> a.getEnclosure().getId()).toList();
        assertThat(animals.findByEnclosureIdInOrderByNameAsc(ids))
                .extracting(a -> a.getId()).containsExactly(own.animal().getId())
                .doesNotContain(other.animal().getId());
    }

    @ParameterizedTest
    @ValueSource(strings = {"startsNow", "future", "endedNow"})
    void activeAssignment_usesInclusiveStartAndExclusiveEnd(String scenario) {
        var fixture = FeedingTestData.persist(em.getEntityManager(), NOW);
        if (scenario.equals("startsNow")) fixture.assignment().setStartsAt(NOW);
        if (scenario.equals("future")) fixture.assignment().setStartsAt(NOW.plusSeconds(1));
        if (scenario.equals("endedNow")) fixture.assignment().setEndsAt(NOW);
        em.flush();
        em.clear();
        List<KeeperAssignment> result = assignments.findActiveByKeeper(fixture.keeper().getId(), NOW);
        if (scenario.equals("startsNow")) {
            assertThat(result).extracting(KeeperAssignment::getId).containsExactly(fixture.assignment().getId());
        } else {
            assertThat(result).isEmpty();
        }
    }
}
