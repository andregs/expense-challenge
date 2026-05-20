package com.example.expensechallenge.api.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class MoneyFormatter {

    public static final int SCALE = 2;
    public static final RoundingMode MODE = RoundingMode.HALF_UP;

    private MoneyFormatter() {}

    public static String format(BigDecimal amount) {
        return amount.setScale(SCALE, MODE).toPlainString();
    }

    public static BigDecimal round(BigDecimal amount) {
        return amount.setScale(SCALE, MODE);
    }
}
