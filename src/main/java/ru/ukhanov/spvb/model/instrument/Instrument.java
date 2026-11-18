package ru.ukhanov.spvb.model.instrument;

import jakarta.persistence.*;
import lombok.*;
import ru.ukhanov.spvb.model.instrument.enums.InstrumentType;

import java.math.BigDecimal;

@Entity
@Table(name = "instruments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Instrument {

    @Id
    @Column(name = "seccode", nullable = false, unique = true, length = 32)
    private String seccode;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private InstrumentType type;

    @Column(name = "lot_size", nullable = false)
    private int lotSize;

    @Column(name = "min_step", nullable = false, precision = 19, scale = 4)
    private BigDecimal minStep;
}