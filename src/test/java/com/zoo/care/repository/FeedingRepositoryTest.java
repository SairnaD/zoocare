package com.zoo.care.repository;

import com.zoo.care.entity.Feedings;
import com.zoo.care.support.FeedingTestData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class FeedingRepositoryTest {
    private static final Instant NOW = Instant.parse("2026-11-10T12:00:00Z");
    @Autowired private TestEntityManager em;
    @Autowired private FeedingRepository repository;

    @Test
    void animalWithNoFeedings_returnsEmptyEvenWhenOtherAnimalWasFed() {
        var first = FeedingTestData.persist(em.getEntityManager(), NOW);
        var other = FeedingTestData.persist(em.getEntityManager(), NOW);
        persistFeeding(other, NOW, "Other animal");
        em.flush();
        em.clear();
        assertThat(repository.findFirstByAnimalIdOrderByFedAtDescIdDesc(first.animal().getId())).isEmpty();
    }

    @Test
    void latestFeeding_isSelectedByTimeThenIdAndOnlyForRequestedAnimal() {
        var fixture = FeedingTestData.persist(em.getEntityManager(), NOW);
        var other = FeedingTestData.persist(em.getEntityManager(), NOW);
        persistFeeding(fixture, NOW.minusSeconds(3600), "Older");
        persistFeeding(fixture, NOW, "First at same time");
        Feedings expected = persistFeeding(fixture, NOW, "Last at same time");
        persistFeeding(fixture, NOW.minusSeconds(7200), "Inserted later but older");
        persistFeeding(other, NOW.plusSeconds(3600), "Other animal");
        em.flush();
        em.clear();

        Feedings actual = repository.findFirstByAnimalIdOrderByFedAtDescIdDesc(fixture.animal().getId())
                .orElseThrow();
        assertThat(actual.getId()).isEqualTo(expected.getId());
        assertThat(actual.getAnimal().getId()).isEqualTo(fixture.animal().getId());
        assertThat(actual.getRecordedBy().getId()).isEqualTo(fixture.keeper().getId());
        assertThat(actual.getFood()).isEqualTo("Hay");
        assertThat(actual.getAmountKg()).isEqualByComparingTo("0.750");
        assertThat(actual.getFedAt()).isEqualTo(NOW);
        assertThat(actual.getNote()).isEqualTo("Last at same time");
    }

    @Test
    void sameFoodCanBeRecordedRepeatedlyAndNoteCanBeNull() {
        var fixture = FeedingTestData.persist(em.getEntityManager(), NOW);
        persistFeeding(fixture, NOW.minusSeconds(21600), null);
        persistFeeding(fixture, NOW, null);
        em.flush();
        em.clear();
        assertThat(repository.findAll()).filteredOn(f -> f.getAnimal().getId().equals(fixture.animal().getId()))
                .hasSize(2).allSatisfy(f -> {
                    assertThat(f.getFood()).isEqualTo("Hay");
                    assertThat(f.getNote()).isNull();
                });
    }

    @Test
    void lastTimes_areGroupedByRequestedAnimalAndIncludeOtherAuthors() {
        var first = FeedingTestData.persist(em.getEntityManager(), NOW);
        var second = FeedingTestData.persist(em.getEntityManager(), NOW);
        var unrelated = FeedingTestData.persist(em.getEntityManager(), NOW);
        persistFeeding(first, NOW.minusSeconds(3600), "Older");
        Feedings latest = persistFeeding(first, NOW, "Latest by another keeper");
        latest.setRecordedBy(first.secondKeeper());
        persistFeeding(second, NOW.minusSeconds(7200), "Second animal");
        persistFeeding(unrelated, NOW.plusSeconds(3600), "Not requested");
        em.flush();
        em.clear();

        Map<Long, Instant> times = repository.findLastFeedingTimes(
                        List.of(first.animal().getId(), second.animal().getId())).stream()
                .collect(Collectors.toMap(LastFeedingTime::getAnimalId, LastFeedingTime::getLastFedAt));
        assertThat(times).containsExactlyInAnyOrderEntriesOf(Map.of(
                first.animal().getId(), NOW, second.animal().getId(), NOW.minusSeconds(7200)));
    }

    @Test
    void lastTimes_omitAnimalsWithoutHistory() {
        var neverFed = FeedingTestData.persist(em.getEntityManager(), NOW);
        var other = FeedingTestData.persist(em.getEntityManager(), NOW);
        persistFeeding(other, NOW, "Other");
        em.flush();
        em.clear();
        assertThat(repository.findLastFeedingTimes(List.of(neverFed.animal().getId()))).isEmpty();
    }

    private Feedings persistFeeding(FeedingTestData.Fixture fixture, Instant time, String note) {
        Feedings feeding = Feedings.builder().animal(fixture.animal()).recordedBy(fixture.keeper())
                .food("Hay").amountKg(new BigDecimal("0.750")).fedAt(time).note(note).build();
        em.persist(feeding);
        return feeding;
    }
}
