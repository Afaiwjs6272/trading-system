package ru.ukhanov.spvb.model.holding;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "holdings",
        uniqueConstraints = @UniqueConstraint(columnNames = {"account_id", "seccode"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Holding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "seccode", nullable = false)
    private String seccode;

    @Column(name = "quantity", nullable = false)
    private long quantity;

    @Column(name = "reserved_quantity", nullable = false)
    private long reservedQuantity;
}