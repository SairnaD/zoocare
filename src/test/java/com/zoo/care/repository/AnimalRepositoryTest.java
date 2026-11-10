package com.zoo.care.repository;

import com.zoo.care.entity.Animal;
import com.zoo.care.entity.DietType;
import com.zoo.care.entity.Enclosure;
import com.zoo.care.entity.HabitatType;
import com.zoo.care.entity.Species;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class AnimalRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AnimalRepository repository;

    @Test
    @DisplayName("findByEnclosureIdIn returns only animals of the given enclosures sorted by name")
    void findByEnclosureIdIn_returnsAnimalsOfGivenEnclosuresSortedByName() {
        Species lion = Species.builder()
                .name("Lion")
                .habitatType(HabitatType.SAVANNA)
                .dietType(DietType.CARNIVORE)
                .build();
        Enclosure savanna = enclosure("Savanna Test");
        Enclosure another = enclosure("Another Savanna");

        entityManager.persist(lion);
        entityManager.persist(savanna);
        entityManager.persist(another);
        entityManager.persist(animal("Nala", lion, savanna));
        entityManager.persist(animal("Leo", lion, savanna));
        entityManager.persist(animal("Simba", lion, another));
        entityManager.flush();

        Long savannaId = savanna.getId();
        entityManager.clear();

        List<Animal> result = repository.findByEnclosureIdInOrderByNameAsc(List.of(savannaId));

        assertThat(result).extracting(Animal::getName).containsExactly("Leo", "Nala");
    }

    private Enclosure enclosure(String name) {
        return Enclosure.builder()
                .name(name)
                .habitatType(HabitatType.SAVANNA)
                .capacity(5)
                .build();
    }

    private Animal animal(String name, Species species, Enclosure enclosure) {
        return Animal.builder()
                .name(name)
                .species(species)
                .enclosure(enclosure)
                .birthDate(LocalDate.of(2020, 5, 1))
                .weight(180.5f)
                .feedingIntervalHours(6)
                .build();
    }
}