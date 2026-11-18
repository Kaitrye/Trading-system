package ru.trading.system.position;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import ru.trading.system.account.Account;
import ru.trading.system.instrument.Instrument;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "positions")
public class Position {

  @EmbeddedId private PositionId id;

  @MapsId("accountId")
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "account_id", nullable = false)
  private Account account;

  @MapsId("seccode")
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "seccode", nullable = false)
  private Instrument instrument;

  @Column(nullable = false)
  private long quantity;

  @Column(nullable = false)
  private long reservedQuantity;

  @Version
  @Column(nullable = false)
  private long version;

  public Position(Account account, Instrument instrument) {
    this.id = new PositionId(account.getId(), instrument.getSeccode());
    this.account = account;
    this.instrument = instrument;
    this.quantity = 0;
    this.reservedQuantity = 0;
  }

  public boolean reserve(long amount) {
    if (quantity - reservedQuantity < amount) {
      return false;
    }
    reservedQuantity += amount;
    return true;
  }

  public void release(long amount) {
    reservedQuantity -= amount;
  }

  public void executeSell(long amount) {
    quantity -= amount;
    reservedQuantity -= amount;
  }

  public void credit(long amount) {
    quantity += amount;
  }

  public void redeem() {
    if (reservedQuantity != 0) {
      throw new IllegalStateException("Cannot redeem a position with reserved securities");
    }
    quantity = 0;
  }
}
