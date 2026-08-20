package com.ums.hrms.payroll.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.YearMonth;

import org.junit.jupiter.api.Test;

class PayrollFoundationModelTests {

    @Test
    void yearMonthConverterPersistsCanonicalYearMonthText() {
        YearMonthAttributeConverter converter = new YearMonthAttributeConverter();
        YearMonth month = YearMonth.of(2026, 8);

        assertEquals("2026-08", converter.convertToDatabaseColumn(month));
        assertEquals(month, converter.convertToEntityAttribute("2026-08"));
    }

    @Test
    void payrollMoneyUsesBigDecimalWithoutBinaryFloatingPoint() {
        BigDecimal basicPay = new BigDecimal("10000.10");
        BigDecimal allowanceTotal = new BigDecimal("2000.20");
        BigDecimal deductionTotal = new BigDecimal("500.05");

        BigDecimal grossPay = basicPay.add(allowanceTotal);
        BigDecimal netPay = grossPay.subtract(deductionTotal);

        assertEquals(new BigDecimal("12000.30"), grossPay);
        assertEquals(new BigDecimal("11500.25"), netPay);
    }
}
