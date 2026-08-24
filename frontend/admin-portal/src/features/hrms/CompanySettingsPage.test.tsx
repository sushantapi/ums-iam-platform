// @vitest-environment jsdom

import "@testing-library/jest-dom/vitest";
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

const mocks = vi.hoisted(() => ({
  get: vi.fn(),
  update: vi.fn(),
  uploadLogo: vi.fn(),
  getLogo: vi.fn(),
}));

vi.mock("./hrmsOrganizationScopeStorage", () => ({
  getStoredHrmsOrganizationId: () => "org-1",
  setStoredHrmsOrganizationId: vi.fn(),
}));

vi.mock("./companyProfileApi", () => ({
  companyProfileApi: {
    get: mocks.get,
    update: mocks.update,
    uploadLogo: mocks.uploadLogo,
    getLogo: mocks.getLogo,
  },
}));

import { CompanySettingsPage } from "./CompanySettingsPage";

const profile = {
  organizationId: "org-1",
  legalName: "Acme Technologies Private Limited",
  displayName: "Acme Technologies",
  registeredAddress: "42 Platform Road, Bengaluru",
  businessEmail: "payroll@acme.test",
  businessPhone: "+91 9999999999",
  website: "https://acme.test",
  defaultCurrency: "INR",
  payrollCountry: "IN",
  payslipFooterText: "This is a system generated payslip.",
  authorizedSignatoryLabel: "Authorized Signatory",
  logoAssetId: null,
  logoAssetVersion: null,
  createdAt: null,
  updatedAt: null,
};

describe("CompanySettingsPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.get.mockResolvedValue(profile);
    mocks.update.mockResolvedValue(profile);
  });

  afterEach(cleanup);

  it("loads reusable tenant company profile fields", async () => {
    render(<CompanySettingsPage />);

    expect(await screen.findByDisplayValue("Acme Technologies Private Limited"))
      .toBeInTheDocument();
    expect(screen.getByDisplayValue("Acme Technologies")).toBeInTheDocument();
    expect(screen.getByDisplayValue("INR")).toBeInTheDocument();
    expect(mocks.get).toHaveBeenCalledWith("org-1");
  });

  it("saves normalized payslip branding values for the selected organization", async () => {
    render(<CompanySettingsPage />);
    await screen.findByDisplayValue("Acme Technologies");

    fireEvent.change(screen.getByLabelText("Display name"), {
      target: { value: " Acme Payroll " },
    });
    fireEvent.change(screen.getByLabelText("Payslip footer text"), {
      target: { value: " Salary processed by Acme HRMS. " },
    });
    fireEvent.click(screen.getByRole("button", { name: "Save company profile" }));

    await waitFor(() =>
      expect(mocks.update).toHaveBeenCalledWith(
        "org-1",
        expect.objectContaining({
          displayName: "Acme Payroll",
          defaultCurrency: "INR",
          payrollCountry: "IN",
          payslipFooterText: "Salary processed by Acme HRMS.",
        }),
      ),
    );
  });
});
