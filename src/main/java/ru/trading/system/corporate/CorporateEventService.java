package ru.trading.system.corporate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

@Service
@RequiredArgsConstructor
public class CorporateEventService {
  private static final BigDecimal MONTHS_IN_YEAR = BigDecimal.valueOf(12);
  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

  private final InstrumentRepository instrumentRepository;
  private final PositionRepository positionRepository;
  private final AccountOperationRepository accountOperationRepository;
  private final OrderService orderService;

  @Transactional
  public int processEvents(LocalDate processingDate) {
    List<Instrument> bonds =
        instrumentRepository.lockProcessableBonds(InstrumentType.BOND, processingDate);
    for (Instrument bond : bonds) {
      processBond(bond, processingDate);
    }
    return bonds.size();
  }

  private void processBond(Instrument bond, LocalDate processingDate) {
    while (!bond.getNextCouponDate().isAfter(processingDate)
        && !bond.getNextCouponDate().isAfter(bond.getMaturityDate())) {
      payCoupon(bond);
      if (bond.getNextCouponDate().equals(bond.getMaturityDate())) {
        break;
      }
      bond.advanceCouponDate();
    }

    if (!bond.getMaturityDate().isAfter(processingDate)) {
      orderService.cancelActiveOrdersForInstrument(bond.getSeccode());
      redeem(bond);
      bond.deactivate();
    }
  }

  private void payCoupon(Instrument bond) {
    BigDecimal couponPerSecurity =
        bond.getNominal()
            .multiply(bond.getCouponRate())
            .multiply(BigDecimal.valueOf(bond.getCouponFrequencyMonths()))
            .divide(HUNDRED.multiply(MONTHS_IN_YEAR), 4, RoundingMode.HALF_UP);

    List<AccountOperation> operations =
        positionRepository.lockPositivePositionsByInstrument(bond.getSeccode()).stream()
            .map(position -> creditCoupon(position, couponPerSecurity))
            .toList();
    accountOperationRepository.saveAll(operations);
  }

  private AccountOperation creditCoupon(Position position, BigDecimal couponPerSecurity) {
    BigDecimal amount = couponPerSecurity.multiply(BigDecimal.valueOf(position.getQuantity()));
    Account account = position.getAccount();
    account.credit(amount);
    return new AccountOperation(account, null, OperationType.COUPON_PAYMENT, amount);
  }

  private void redeem(Instrument bond) {
    List<AccountOperation> operations =
        positionRepository.lockPositivePositionsByInstrument(bond.getSeccode()).stream()
            .map(position -> redeemPosition(position, bond.getNominal()))
            .toList();
    accountOperationRepository.saveAll(operations);
  }

  private AccountOperation redeemPosition(Position position, BigDecimal nominal) {
    BigDecimal amount =
        nominal
            .multiply(BigDecimal.valueOf(position.getQuantity()))
            .setScale(4, RoundingMode.HALF_UP);
    Account account = position.getAccount();
    position.redeem();
    account.credit(amount);
    return new AccountOperation(account, null, OperationType.BOND_REDEMPTION, amount);
  }
}
