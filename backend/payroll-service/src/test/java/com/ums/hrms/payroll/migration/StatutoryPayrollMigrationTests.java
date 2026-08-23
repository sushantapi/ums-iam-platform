package com.ums.hrms.payroll.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.UUID;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;

class StatutoryPayrollMigrationTests {

    @Test
    void migratesExistingV1PayrollEntryWithBackwardCompatibleStatutoryDefaults()
            throws Exception {

        String databaseName =
                "payroll_migration_" + UUID.randomUUID().toString().replace("-", "");
        String jdbcUrl =
                "jdbc:h2:mem:" + databaseName + ";MODE=MySQL;DB_CLOSE_DELAY=-1";

        Flyway.configure()
                .dataSource(jdbcUrl, "sa", "")
                .locations("classpath:db/migration")
                .target(MigrationVersion.fromVersion("1"))
                .load()
                .migrate();

        UUID organizationId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID salaryStructureId = UUID.randomUUID();
        UUID payrollRunId = UUID.randomUUID();
        UUID payrollEntryId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();

        Timestamp createdAt = Timestamp.valueOf("2026-08-01 09:00:00");
        Timestamp processedAt = Timestamp.valueOf("2026-08-31 12:00:00");
        Timestamp finalizedAt = Timestamp.valueOf("2026-08-31 18:30:00");

        try (Connection connection =
                DriverManager.getConnection(jdbcUrl, "sa", "")) {

            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO hrms_salary_structures (
                        id,
                        organization_id,
                        employee_id,
                        currency,
                        basic_pay,
                        allowance_total,
                        deduction_total,
                        effective_from,
                        effective_to,
                        active,
                        created_by,
                        created_at,
                        updated_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """)) {

                statement.setString(1, salaryStructureId.toString());
                statement.setString(2, organizationId.toString());
                statement.setString(3, employeeId.toString());
                statement.setString(4, "INR");
                statement.setBigDecimal(5, new BigDecimal("50000.00"));
                statement.setBigDecimal(6, new BigDecimal("10000.00"));
                statement.setBigDecimal(7, new BigDecimal("5000.00"));
                statement.setDate(8, Date.valueOf("2026-08-01"));
                statement.setNull(9, java.sql.Types.DATE);
                statement.setBoolean(10, true);
                statement.setString(11, actorUserId.toString());
                statement.setTimestamp(12, createdAt);
                statement.setTimestamp(13, createdAt);

                assertEquals(1, statement.executeUpdate());
            }

            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO hrms_payroll_runs (
                        id,
                        organization_id,
                        payroll_month,
                        status,
                        created_by,
                        processed_by,
                        processed_at,
                        finalized_by,
                        finalized_at,
                        created_at,
                        updated_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """)) {

                statement.setString(1, payrollRunId.toString());
                statement.setString(2, organizationId.toString());
                statement.setString(3, "2026-08");
                statement.setString(4, "FINALIZED");
                statement.setString(5, actorUserId.toString());
                statement.setString(6, actorUserId.toString());
                statement.setTimestamp(7, processedAt);
                statement.setString(8, actorUserId.toString());
                statement.setTimestamp(9, finalizedAt);
                statement.setTimestamp(10, createdAt);
                statement.setTimestamp(11, finalizedAt);

                assertEquals(1, statement.executeUpdate());
            }

            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO hrms_payroll_entries (
                        id,
                        payroll_run_id,
                        organization_id,
                        employee_id,
                        salary_structure_id,
                        basic_pay,
                        allowance_total,
                        gross_pay,
                        deduction_total,
                        net_pay,
                        generated_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """)) {

                statement.setString(1, payrollEntryId.toString());
                statement.setString(2, payrollRunId.toString());
                statement.setString(3, organizationId.toString());
                statement.setString(4, employeeId.toString());
                statement.setString(5, salaryStructureId.toString());
                statement.setBigDecimal(6, new BigDecimal("50000.00"));
                statement.setBigDecimal(7, new BigDecimal("10000.00"));
                statement.setBigDecimal(8, new BigDecimal("60000.00"));
                statement.setBigDecimal(9, new BigDecimal("5000.00"));
                statement.setBigDecimal(10, new BigDecimal("55000.00"));
                statement.setTimestamp(11, processedAt);

                assertEquals(1, statement.executeUpdate());
            }
        }

        Flyway.configure()
                .dataSource(jdbcUrl, "sa", "")
                .locations("classpath:db/migration")
                .load()
                .migrate();

        try (Connection connection =
                        DriverManager.getConnection(jdbcUrl, "sa", "");
                PreparedStatement statement = connection.prepareStatement("""
                        SELECT
                            configured_deduction_total,
                            pf_contribution_wage,
                            employee_pf_contribution,
                            employer_pf_contribution,
                            esi_contribution_wage,
                            employee_esi_contribution,
                            employer_esi_contribution,
                            tds_amount,
                            statutory_employee_deduction_total,
                            employer_statutory_contribution_total,
                            statutory_policy_id,
                            statutory_policy_version,
                            tax_regime,
                            deduction_total,
                            net_pay
                        FROM hrms_payroll_entries
                        WHERE id = ?
                        """)) {

            statement.setString(1, payrollEntryId.toString());

            try (ResultSet result = statement.executeQuery()) {
                assertTrue(result.next());

                assertEquals(
                        new BigDecimal("5000.00"),
                        result.getBigDecimal("configured_deduction_total"));

                assertEquals(
                        new BigDecimal("0.00"),
                        result.getBigDecimal("pf_contribution_wage"));
                assertEquals(
                        new BigDecimal("0.00"),
                        result.getBigDecimal("employee_pf_contribution"));
                assertEquals(
                        new BigDecimal("0.00"),
                        result.getBigDecimal("employer_pf_contribution"));

                assertEquals(
                        new BigDecimal("0.00"),
                        result.getBigDecimal("esi_contribution_wage"));
                assertEquals(
                        new BigDecimal("0.00"),
                        result.getBigDecimal("employee_esi_contribution"));
                assertEquals(
                        new BigDecimal("0.00"),
                        result.getBigDecimal("employer_esi_contribution"));

                assertEquals(
                        new BigDecimal("0.00"),
                        result.getBigDecimal("tds_amount"));
                assertEquals(
                        new BigDecimal("0.00"),
                        result.getBigDecimal("statutory_employee_deduction_total"));
                assertEquals(
                        new BigDecimal("0.00"),
                        result.getBigDecimal("employer_statutory_contribution_total"));

                assertNull(result.getString("statutory_policy_id"));
                assertNull(result.getString("statutory_policy_version"));
                assertNull(result.getString("tax_regime"));

                assertEquals(
                        new BigDecimal("5000.00"),
                        result.getBigDecimal("deduction_total"));
                assertEquals(
                        new BigDecimal("55000.00"),
                        result.getBigDecimal("net_pay"));

                assertFalse(result.next());
            }
        }

        try (Connection connection =
                        DriverManager.getConnection(jdbcUrl, "sa", "");
                PreparedStatement statement = connection.prepareStatement("""
                        SELECT
                            pf_applicable,
                            pf_contribution_wage,
                            esi_applicable,
                            esi_contribution_wage,
                            tds_amount,
                            tax_regime
                        FROM hrms_salary_structures
                        WHERE id = ?
                        """)) {

            statement.setString(1, salaryStructureId.toString());

            try (ResultSet result = statement.executeQuery()) {
                assertTrue(result.next());
                assertFalse(result.getBoolean("pf_applicable"));
                assertNull(result.getBigDecimal("pf_contribution_wage"));
                assertFalse(result.getBoolean("esi_applicable"));
                assertNull(result.getBigDecimal("esi_contribution_wage"));
                assertEquals(
                        new BigDecimal("0.00"),
                        result.getBigDecimal("tds_amount"));
                assertNull(result.getString("tax_regime"));
            }
        }
    }
}