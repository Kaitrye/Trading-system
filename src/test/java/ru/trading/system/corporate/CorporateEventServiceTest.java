package ru.trading.system.corporate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.trading.system.account.Account;
import ru.trading.system.instrument.Instrument;
import ru.trading.system.instrument.InstrumentRepository;
import ru.trading.system.instrument.InstrumentType;
import ru.trading.system.operation.AccountOperation;
import ru.trading.system.operation.AccountOperationRepository;
import ru.trading.system.operation.OperationType;
import ru.trading.system.order.OrderService;
import ru.trading.system.position.Position;
import ru.trading.system.position.PositionRepository;

@ExtendWith(MockitoExtension.class)
class CorporateEventServiceTest {

  @Mock private InstrumentRepository instrumentRepository;
  @Mock private PositionRepository positionRepository;
  @Mock private AccountOperationRepository accountOperationRepository;
  @Mock private OrderService orderService;

  @InjectMocks private CorporateEventService corporateEventService;

  @Test
  void paysCouponAndAdvancesNextCouponDate() {
    Instrument bond = bond(LocalDate.of(2030, 5, 15), LocalDate.of(2026, 5, 15));
    Position position = positionWithQuantity(bond, 2);
    LocalDate processingDate = LocalDate.of(2026, 5, 15);
    when(instrumentRepository.lockProcessableBonds(InstrumentType.BOND, processingDate))
        .thenReturn(List.of(bond));
    when(positionRepository.lockPositivePositionsByInstrument("OFZ26238"))
        .thenReturn(List.of(position));

    int processed = corporateEventService.processEvents(processingDate);

    ArgumentCaptor<Iterable<AccountOperation>> operations = ArgumentCaptor.forClass(Iterable.class);
    verify(accountOperationRepository).saveAll(operations.capture());
    AccountOperation operation = operations.getValue().iterator().next();
    assertEquals(1, processed);
    assertEquals(new BigDecimal("120.0000"), operation.getAmount());
    assertEquals(OperationType.COUPON_PAYMENT, operation.getOperationType());
    assertEquals(new BigDecimal("120.0000"), position.getAccount().getBalance());
    assertEquals(LocalDate.of(2026, 11, 15), bond.getNextCouponDate());
  }

  @Test
  void paysFinalCouponCancelsOrdersAndRedeemsBondOnMaturity() {
    Instrument bond = bond(LocalDate.of(2026, 5, 15), LocalDate.of(2026, 5, 15));
    Position position = positionWithQuantity(bond, 2);
    LocalDate processingDate = LocalDate.of(2026, 5, 15);
    when(instrumentRepository.lockProcessableBonds(InstrumentType.BOND, processingDate))
        .thenReturn(List.of(bond));
    when(positionRepository.lockPositivePositionsByInstrument("OFZ26238"))
        .thenReturn(List.of(position));

    corporateEventService.processEvents(processingDate);

    ArgumentCaptor<Iterable<AccountOperation>> operations = ArgumentCaptor.forClass(Iterable.class);
    verify(accountOperationRepository, org.mockito.Mockito.times(2)).saveAll(operations.capture());
    List<AccountOperation> firstBatch = (List<AccountOperation>) operations.getAllValues().get(0);
    List<AccountOperation> secondBatch = (List<AccountOperation>) operations.getAllValues().get(1);
    assertEquals(OperationType.COUPON_PAYMENT, firstBatch.getFirst().getOperationType());
    assertEquals(new BigDecimal("120.0000"), firstBatch.getFirst().getAmount());
    assertEquals(OperationType.BOND_REDEMPTION, secondBatch.getFirst().getOperationType());
    assertEquals(new BigDecimal("2000.0000"), secondBatch.getFirst().getAmount());
    assertEquals(new BigDecimal("2120.0000"), position.getAccount().getBalance());
    assertEquals(0, position.getQuantity());
    assertEquals(false, bond.isActive());
    verify(orderService).cancelActiveOrdersForInstrument("OFZ26238");
  }

  private Instrument bond(LocalDate maturityDate, LocalDate nextCouponDate) {
    return new Instrument(
        "OFZ26238",
        "ОФЗ 26238",
        1,
        new BigDecimal("0.01"),
        "RUB",
        InstrumentType.BOND,
        new BigDecimal("1000"),
        maturityDate,
        new BigDecimal("12"),
        6,
        nextCouponDate);
  }

  private Position positionWithQuantity(Instrument instrument, long quantity) {
    Account account = new Account(null, "RUB");
    Position position = new Position(account, instrument);
    position.credit(quantity);
    return position;
  }
}
