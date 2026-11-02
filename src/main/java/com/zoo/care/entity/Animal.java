package com.zoo.care.entity;


import jakarta.persistence.*;
import lombok.*;

import java.time.Duration;
import java.time.LocalDate;

@Entity
@Table(name = "animal")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Animal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "animal_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "species_id", referencedColumnName = "id")
    private Species species;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enclosure_id", referencedColumnName = "id")
    private Enclosure enclosure;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(nullable = false)
    private LocalDate birthdate;

    @Column(nullable = false)
    private float weight; //assuming KGs with one decimal point for accuracy

    @Column(nullable = false)
    private int size; // assuming size measured in CM or MM without decimal places

    @Column(nullable = false)
    private Duration feedingInterval; // example - Duration.ofHours(24); for full day

    @Column(nullable = false, length = 500)
    private String description;

    @Column(nullable = false)
    private String photo; //file path (ex. "photos/snake.jpg")

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private HealthStatus healthStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DeletedStatus deletedStatus;

}
