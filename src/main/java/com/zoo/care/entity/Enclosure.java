package com.zoo.care.entity;


import jakarta.persistence.*;
import lombok.*;

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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "keeper_id", referencedColumnName = "id")
    private EnclosureKeeper enclosureKeeper;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private HabitatType habitatType;

    @Column(nullable = false)
    private int capacity;
}
