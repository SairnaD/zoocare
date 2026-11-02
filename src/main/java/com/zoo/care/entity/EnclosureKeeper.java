package com.zoo.care.entity;


import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "enclosure_keeper")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
//connecting table between enclosure and user to prevent many to many interaction
public class EnclosureKeeper {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "keeper_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enclosure_id", referencedColumnName = "id")
    private Enclosure enclosure;
}
