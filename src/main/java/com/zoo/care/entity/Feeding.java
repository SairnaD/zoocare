package com.zoo.care.entity;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "feeding")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Feeding {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "feeding_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "animal_id", referencedColumnName = "id")
    private Animal animal;

    @Column(nullable = false)
    private String foodType;
    /*saving as string since there's too many 'types' of food for ENUM usage
      can be changed to ENUM if we decide to use very general categories like 'meat', 'vegetable', 'seed' , etc.
      if not,then can be left to user input
    */

    @Column(nullable = false)
    private int Amount;

    @Column(nullable = false)
    private LocalDateTime feedingTime;

    @Column(length = 500)
    private String note;

}
