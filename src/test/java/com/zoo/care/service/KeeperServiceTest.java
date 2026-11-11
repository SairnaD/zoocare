package com.zoo.care.service;

import com.zoo.care.dto.EnclosureAnimalCount;
import com.zoo.care.dto.KeeperOverviewResponse;
import com.zoo.care.entity.DeletedStatus;
import com.zoo.care.entity.Enclosure;
import com.zoo.care.entity.HabitatType;
import com.zoo.care.entity.KeeperAssignment;
import com.zoo.care.entity.Role;
import com.zoo.care.entity.User;
import com.zoo.care.repository.AnimalRepository;
import com.zoo.care.repository.KeeperAssignmentRepository;
import com.zoo.care.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KeeperServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-11T12:00:00Z");

    @Mock
    private UserRepository userRepository;

    @Mock
    private KeeperAssignmentRepository keeperAssignmentRepository;

    @Mock
    private AnimalRepository animalRepository;

    @Spy
    private Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    @InjectMocks
    private KeeperService keeperService;

    @Test
    void findAllWithOverview_groupsEnclosuresAndSumsAnimals() {
        User anna = keeper(7L, "Anna Berzina");
        User ben = keeper(8L, "Ben Ozols");
        when(userRepository.findAllByRoleAndDeletedStatusOrderByFullName(Role.KEEPER, DeletedStatus.PRESENT))
                .thenReturn(List.of(anna, ben));        when(keeperAssignmentRepository.findAllActive(NOW)).thenReturn(List.of(
                assignment(anna, enclosure(1L, "Savanna 1")),
                assignment(anna, enclosure(2L, "Savanna 2"))));
        when(animalRepository.countAnimalsPerEnclosure(DeletedStatus.PRESENT)).thenReturn(List.of(
                new EnclosureAnimalCount(1L, 2L),
                new EnclosureAnimalCount(2L, 3L),
                new EnclosureAnimalCount(3L, 9L)));

        List<KeeperOverviewResponse> result = keeperService.findAllWithOverview();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).enclosures()).extracting("name").containsExactly("Savanna 1", "Savanna 2");
        assertThat(result.get(0).animalCount()).isEqualTo(5);
        assertThat(result.get(1).enclosures()).isEmpty();
        assertThat(result.get(1).animalCount()).isZero();
    }

    private User keeper(Long id, String fullName) {
        return User.builder().id(id).username(fullName.split(" ")[0].toLowerCase())
                .fullName(fullName).email("keeper" + id + "@zoo.lv").role(Role.KEEPER).build();
    }

    private Enclosure enclosure(Long id, String name) {
        return Enclosure.builder().id(id).name(name).habitatType(HabitatType.SAVANNA).capacity(5).build();
    }

    private KeeperAssignment assignment(User keeper, Enclosure enclosure) {
        return KeeperAssignment.builder().keeper(keeper).enclosure(enclosure)
                .startsAt(NOW.minusSeconds(3600)).createdAt(NOW.minusSeconds(3600)).build();
    }
}