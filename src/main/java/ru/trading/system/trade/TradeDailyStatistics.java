package ru.trading.system.trade;

import java.math.BigDecimal;

public interface TradeDailyStatistics {
  BigDecimal getLowPrice();

  BigDecimal getHighPrice();

  Long getVolume();

  Long getTradeCount();
}
