package com.zoo.care.entity;


import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "species")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Species {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "species_id")
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private HabitatType habitatType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private DietType DietType;
}
