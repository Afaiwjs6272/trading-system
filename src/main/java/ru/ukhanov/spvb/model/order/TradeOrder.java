package ru.ukhanov.spvb.model.order;

import jakarta.persistence.*;
import lombok.*;
import ru.ukhanov.spvb.model.order.enums.OrderStatus;
import ru.ukhanov.spvb.model.order.enums.OrderType;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "orders", indexes = {
        @Index(name = "ix_orders_seccode_type_status", columnList = "seccode,type,status,price")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TradeOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "seccode", nullable = false)
    private String seccode;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private OrderType type;

    @Column(name = "price", nullable = false, precision = 19, scale = 4)
    private BigDecimal price;

    @Column(name = "quantity", nullable = false)
    private long quantity;

    @Column(name = "filled_quantity", nullable = false)
    private long filledQuantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderStatus status;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
