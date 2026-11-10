package com.zoo.care.entity;


import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "feedings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Feedings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "feeding_id")
    private Long id;

    @ManyToOne (fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "animal_id", nullable = false)
    private Animal animal;

    @ManyToOne (fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recorded_by_id", nullable = false)
    private User recordedBy;

    @Column(nullable = false, length = 100)
    private String food;

    @Column(name = "amount_kg", nullable = false, precision = 10, scale = 3)
    private BigDecimal amountKg;

    @Column(name = "fed_at", nullable = false)
    private Instant fedAt;

    @Column(length = 500)
    private String note;

}
