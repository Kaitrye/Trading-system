package ru.trading.system.order;

import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, UUID> {

  Optional<Order> findByIdAndAccountUserId(UUID id, UUID userId);

  List<Order> findByAccountUserIdOrderByCreatedAtDesc(UUID userId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
            select o from Order o
            where o.instrument.seccode = :ticker and o.status in :statuses
            """)
  List<Order> lockActiveOrdersByInstrument(
      @Param("ticker") String ticker, @Param("statuses") Collection<OrderStatus> statuses);

  @Query(
      """
            select o.price as price, sum(o.remainingQuantity) as quantity from Order o
            where o.instrument.seccode = :ticker and o.side = :side and o.status in :statuses
            group by o.price
            order by o.price desc
            """)
  List<OrderBookLevel> findBuyLevels(
      @Param("ticker") String ticker,
      @Param("side") OrderSide side,
      @Param("statuses") Collection<OrderStatus> statuses);

  @Query(
      """
            select o.price as price, sum(o.remainingQuantity) as quantity from Order o
            where o.instrument.seccode = :ticker and o.side = :side and o.status in :statuses
            group by o.price
            order by o.price asc
            """)
  List<OrderBookLevel> findSellLevels(
      @Param("ticker") String ticker,
      @Param("side") OrderSide side,
      @Param("statuses") Collection<OrderStatus> statuses);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
            select o from Order o
            where o.instrument.seccode = :ticker
              and o.account.id <> :accountId
              and o.side = :side
              and o.status in :statuses
              and o.price <= :price
            order by o.price asc, o.createdAt asc, o.priority asc
            """)
  List<Order> findMatchingSellOrders(
      @Param("ticker") String ticker,
      @Param("accountId") UUID accountId,
      @Param("side") OrderSide side,
      @Param("statuses") Collection<OrderStatus> statuses,
      @Param("price") BigDecimal price);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
            select o from Order o
            where o.instrument.seccode = :ticker
              and o.account.id <> :accountId
              and o.side = :side
              and o.status in :statuses
              and o.price >= :price
            order by o.price desc, o.createdAt asc, o.priority asc
            """)
  List<Order> findMatchingBuyOrders(
      @Param("ticker") String ticker,
      @Param("accountId") UUID accountId,
      @Param("side") OrderSide side,
      @Param("statuses") Collection<OrderStatus> statuses,
      @Param("price") BigDecimal price);
}
