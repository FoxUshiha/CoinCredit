package com.foxsrv.credit;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Utils {

    /**
     * Format amount according to decimals (0 => integer string; >=1 => plain string with dot)
     */
    public static String formatAmount(BigDecimal value, int decimals) {
        if (decimals <= 0) {
            return value.setScale(0, RoundingMode.DOWN).toBigInteger().toString();
        } else {
            return value.setScale(decimals, RoundingMode.HALF_UP).toPlainString();
        }
    }
}
