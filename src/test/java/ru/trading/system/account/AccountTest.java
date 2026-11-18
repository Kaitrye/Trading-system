package ru.trading.system.account;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class AccountTest {

  @Test
  void reservesOnlyAvailableBalance() {
    Account account = new Account(null, "RUB");
    account.credit(new BigDecimal("100"));

    assertTrue(account.reserve(new BigDecimal("70")));
    assertFalse(account.reserve(new BigDecimal("31")));
    assertEquals(0, new BigDecimal("70").compareTo(account.getReservedBalance()));
  }

  @Test
  void releasesReservedBalance() {
    Account account = new Account(null, "RUB");
    account.credit(new BigDecimal("100"));
    account.reserve(new BigDecimal("70"));

    account.release(new BigDecimal("20"));

    assertEquals(0, new BigDecimal("50").compareTo(account.getReservedBalance()));
    assertTrue(account.reserve(new BigDecimal("50")));
  }
}
