package ru.ukhanov.spvb.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.ukhanov.spvb.model.order.TradeOrder;
import ru.ukhanov.spvb.model.order.enums.OrderStatus;
import ru.ukhanov.spvb.model.order.enums.OrderType;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

public interface OrderRepository extends JpaRepository<TradeOrder, Long> {

    @Query("SELECT o FROM TradeOrder o " +
            "WHERE o.seccode = :seccode AND o.type = :type " +
            "  AND o.status IN :statuses AND o.price <= :maxPrice AND o.id <> :excludeId " +
            "ORDER BY o.price ASC, o.createdAt ASC")
    List<TradeOrder> findMatchableSells(@Param("seccode") String seccode,
                                        @Param("type") OrderType type,
                                        @Param("statuses") Collection<OrderStatus> statuses,
                                        @Param("maxPrice") BigDecimal maxPrice,
                                        @Param("excludeId") Long excludeId);

    @Query("SELECT o FROM TradeOrder o " +
            "WHERE o.seccode = :seccode AND o.type = :type " +
            "  AND o.status IN :statuses AND o.price >= :minPrice AND o.id <> :excludeId " +
            "ORDER BY o.price DESC, o.createdAt ASC")
    List<TradeOrder> findMatchableBuys(@Param("seccode") String seccode,
                                       @Param("type") OrderType type,
                                       @Param("statuses") Collection<OrderStatus> statuses,
                                       @Param("minPrice") BigDecimal minPrice,
                                       @Param("excludeId") Long excludeId);

    List<TradeOrder> findByAccountId(Long accountId);

    List<TradeOrder> findBySeccodeOrderByCreatedAtDesc(String seccode);

    @Query("SELECT o FROM TradeOrder o WHERE o.seccode = :seccode " +
            "AND o.type = :type AND o.status IN :statuses ORDER BY o.price ASC, o.createdAt ASC")
    List<TradeOrder> findActiveOrders(@Param("seccode") String seccode,
                                      @Param("type") OrderType type,
                                      @Param("statuses") Collection<OrderStatus> statuses);
}
