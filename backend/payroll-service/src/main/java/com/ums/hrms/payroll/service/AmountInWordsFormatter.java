package com.ums.hrms.payroll.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

import org.springframework.stereotype.Component;

@Component
public class AmountInWordsFormatter {

    private static final String[] BELOW_TWENTY = {
            "Zero", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
            "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen"
    };

    private static final String[] TENS = {
            "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
    };

    public String format(BigDecimal amount, String currencyCode) {
        BigDecimal safeAmount = amount == null ? BigDecimal.ZERO : amount;
        BigDecimal normalized = safeAmount.setScale(2, RoundingMode.HALF_UP);
        if (normalized.signum() < 0) {
            throw new IllegalArgumentException("Amount in words does not support negative values");
        }

        long whole = normalized.setScale(0, RoundingMode.DOWN).longValueExact();
        int fraction = normalized
                .subtract(BigDecimal.valueOf(whole))
                .movePointRight(2)
                .intValueExact();

        String currency = currencyCode == null || currencyCode.isBlank()
                ? "INR"
                : currencyCode.trim().toUpperCase(Locale.ROOT);
        String unit = "INR".equals(currency) ? "Rupees" : currency;

        StringBuilder words = new StringBuilder(unit)
                .append(' ')
                .append(toIndianWords(whole));

        if (fraction > 0) {
            words.append(" and ")
                    .append(toIndianWords(fraction))
                    .append(" Paise");
        }

        return words.append(" Only").toString();
    }

    private String toIndianWords(long value) {
        if (value == 0) {
            return BELOW_TWENTY[0];
        }

        StringBuilder result = new StringBuilder();
        appendGroup(result, value / 10_000_000L, "Crore");
        value %= 10_000_000L;
        appendGroup(result, value / 100_000L, "Lakh");
        value %= 100_000L;
        appendGroup(result, value / 1_000L, "Thousand");
        value %= 1_000L;
        appendGroup(result, value / 100L, "Hundred");
        value %= 100L;

        if (value > 0) {
            appendToken(result, underHundred((int) value));
        }

        return result.toString();
    }

    private void appendGroup(StringBuilder result, long value, String scale) {
        if (value <= 0) {
            return;
        }
        appendToken(result, toIndianWords(value));
        appendToken(result, scale);
    }

    private String underHundred(int value) {
        if (value < 20) {
            return BELOW_TWENTY[value];
        }
        int tens = value / 10;
        int ones = value % 10;
        return ones == 0 ? TENS[tens] : TENS[tens] + " " + BELOW_TWENTY[ones];
    }

    private void appendToken(StringBuilder result, String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        if (!result.isEmpty()) {
            result.append(' ');
        }
        result.append(token);
    }
}
