import {
  hrmsGatewayDownload,
  hrmsGatewayRequest,
  withHrmsQuery,
} from "./hrmsGatewayClient";

export type PayrollRunStatus = "DRAFT" | "PROCESSED" | "FINALIZED";
export type TaxRegime = "OLD" | "NEW";

export type SalaryStructureResponse = {
  id: string;
  organizationId: string;
  employeeId: string;
  versionNumber: number;
  supersedesStructureId: string | null;
  currency: string;
  basicPay: number;
  allowanceTotal: number;
  deductionTotal: number;
  pfApplicable: boolean;
  pfContributionWage: number | null;
  esiApplicable: boolean;
  esiContributionWage: number | null;
  tdsAmount: number;
  taxRegime: TaxRegime | null;
  effectiveFrom: string;
  effectiveTo: string | null;
  active: boolean;
  supersededAt: string | null;
  supersededBy: string | null;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
};

export type CreateSalaryStructureRequest = {
  organizationId: string;
  employeeId: string;
  currency: string;
  basicPay: number;
  allowanceTotal: number;
  deductionTotal: number;
  pfApplicable?: boolean;
  pfContributionWage?: number | null;
  esiApplicable?: boolean;
  esiContributionWage?: number | null;
  tdsAmount?: number;
  taxRegime?: TaxRegime | null;
  effectiveFrom: string;
  effectiveTo?: string | null;
  active?: boolean;
};

export type SupersedeSalaryStructureRequest = {
  organizationId: string;
  currency: string;
  basicPay: number;
  allowanceTotal: number;
  deductionTotal: number;
  pfApplicable?: boolean;
  pfContributionWage?: number | null;
  esiApplicable?: boolean;
  esiContributionWage?: number | null;
  tdsAmount?: number;
  taxRegime?: TaxRegime | null;
  effectiveFrom: string;
};

export type PayrollRunResponse = {
  id: string;
  organizationId: string;
  payrollMonth: string;
  status: PayrollRunStatus;
  createdBy: string;
  processedBy: string | null;
  processedAt: string | null;
  finalizedBy: string | null;
  finalizedAt: string | null;
  createdAt: string;
  updatedAt: string;
};

export type PayrollEntryResponse = {
  id: string;
  payrollRunId: string;
  organizationId: string;
  employeeId: string;
  salaryStructureId: string;
  basicPay: number;
  allowanceTotal: number;
  grossPay: number;
  configuredDeductionTotal: number;
  pfContributionWage: number;
  employeePfContribution: number;
  employerPfContribution: number;
  esiContributionWage: number;
  employeeEsiContribution: number;
  employerEsiContribution: number;
  tdsAmount: number;
  statutoryEmployeeDeductionTotal: number;
  employerStatutoryContributionTotal: number;
  statutoryPolicyId: string | null;
  statutoryPolicyVersion: string | null;
  taxRegime: TaxRegime | null;
  deductionTotal: number;
  netPay: number;
  generatedAt: string;
};

export const payrollApi = {
  salaryStructures: (organizationId: string, employeeId: string) =>
    hrmsGatewayRequest<SalaryStructureResponse[]>(
      withHrmsQuery("/api/v1/hrms/payroll/salary-structures", {
        organizationId,
        employeeId,
      }),
    ),
  createSalaryStructure: (body: CreateSalaryStructureRequest) =>
    hrmsGatewayRequest<SalaryStructureResponse>(
      "/api/v1/hrms/payroll/salary-structures",
      {
        method: "POST",
        body: JSON.stringify(body),
      },
    ),
  supersedeSalaryStructure: (
    salaryStructureId: string,
    body: SupersedeSalaryStructureRequest,
  ) =>
    hrmsGatewayRequest<SalaryStructureResponse>(
      `/api/v1/hrms/payroll/salary-structures/${salaryStructureId}/supersede`,
      {
        method: "POST",
        body: JSON.stringify(body),
      },
    ),
  runs: (organizationId: string) =>
    hrmsGatewayRequest<PayrollRunResponse[]>(
      withHrmsQuery("/api/v1/hrms/payroll/runs", { organizationId }),
    ),
  createRun: (organizationId: string, payrollMonth: string) =>
    hrmsGatewayRequest<PayrollRunResponse>("/api/v1/hrms/payroll/runs", {
      method: "POST",
      body: JSON.stringify({ organizationId, payrollMonth }),
    }),
  processRun: (runId: string, organizationId: string) =>
    hrmsGatewayRequest<PayrollRunResponse>(
      `/api/v1/hrms/payroll/runs/${runId}/process`,
      {
        method: "POST",
        body: JSON.stringify({ organizationId }),
      },
    ),
  finalizeRun: (runId: string, organizationId: string) =>
    hrmsGatewayRequest<PayrollRunResponse>(
      `/api/v1/hrms/payroll/runs/${runId}/finalize`,
      {
        method: "POST",
        body: JSON.stringify({ organizationId }),
      },
    ),
  entries: (runId: string, organizationId: string) =>
    hrmsGatewayRequest<PayrollEntryResponse[]>(
      withHrmsQuery(`/api/v1/hrms/payroll/runs/${runId}/entries`, {
        organizationId,
      }),
    ),
  payslip: (entryId: string, organizationId: string) =>
    hrmsGatewayRequest<PayrollEntryResponse>(
      withHrmsQuery(`/api/v1/hrms/payroll/payslips/${entryId}`, {
        organizationId,
      }),
    ),
  downloadPayslipPdf: (entryId: string, organizationId: string) =>
    hrmsGatewayDownload(
      withHrmsQuery(`/api/v1/hrms/payroll/payslips/${entryId}/pdf`, {
        organizationId,
      }),
    ),
};
