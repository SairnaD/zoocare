package com.zoo.care.entity;

import jakarta.persistence.*;
import lombok.*;

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
    @JoinColumn(name = "species_id", nullable = false)
    private Species species;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enclosure_id", nullable = false)
    private Enclosure enclosure;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Column(nullable = false)
    private float weight; // kg

    @Column(nullable = false)
    private int size; // cm

    @Column(name = "feeding_interval_hours", nullable = false)
    private int feedingIntervalHours;

    @Column(length = 500)
    private String description;

    @Column
    private String photo; // stored file name, served from /photos/{name}

    @Enumerated(EnumType.STRING)
    @Column(name = "health_status", nullable = false, length = 30)
    private HealthStatus healthStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "deleted_status", nullable = false, length = 30)
    @Builder.Default
    private DeletedStatus deletedStatus = DeletedStatus.PRESENT;
}