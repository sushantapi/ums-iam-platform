package com.ums.hrms.payroll.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class AmountInWordsFormatterTests {

    private final AmountInWordsFormatter formatter = new AmountInWordsFormatter();

    @Test
    void formatsIndianPayrollAmountUsingLakhAndPaise() {
        assertEquals(
                "Rupees Twelve Lakh Thirty Four Thousand Five Hundred Sixty Seven and Eighty Nine Paise Only",
                formatter.format(new BigDecimal("1234567.89"), "INR"));
    }

    @Test
    void formatsZeroAndNonInrCurrency() {
        assertEquals("USD Zero Only", formatter.format(BigDecimal.ZERO, "usd"));
    }

    @Test
    void rejectsNegativeAmount() {
        assertThrows(
                IllegalArgumentException.class,
                () -> formatter.format(new BigDecimal("-1.00"), "INR"));
    }
}
