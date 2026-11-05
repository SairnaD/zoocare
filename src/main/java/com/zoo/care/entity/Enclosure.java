package com.zoo.care.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "enclosure")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Enclosure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "enclosure_id")
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "habitat_type", nullable = false, length = 30)
    private HabitatType habitatType;

    @Column(nullable = false)
    private int capacity;
}