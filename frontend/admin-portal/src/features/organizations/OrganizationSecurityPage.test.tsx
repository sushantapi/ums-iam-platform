// @vitest-environment jsdom

import "@testing-library/jest-dom/vitest";
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

const mocks = vi.hoisted(() => ({
  get: vi.fn(),
  update: vi.fn(),
  canManage: vi.fn(() => true),
}));

vi.mock("../../api/services/organizationSecurityPolicyService", () => ({
  default: {
    get: mocks.get,
    update: mocks.update,
  },
}));

vi.mock("../../lib/auth/capabilities", () => ({
  hasAdminCapability: mocks.canManage,
}));

import { OrganizationSecurityPage } from "./OrganizationSecurityPage";

function renderPage() {
  return render(
    <MemoryRouter initialEntries={["/organizations/org-1/security"]}>
      <Routes>
        <Route
          path="/organizations/:organizationId/security"
          element={<OrganizationSecurityPage />}
        />
      </Routes>
    </MemoryRouter>,
  );
}

describe("OrganizationSecurityPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.canManage.mockReturnValue(true);
    mocks.get.mockResolvedValue({
      organizationId: "org-1",
      requireMfa: false,
      updatedAt: null,
    });
    mocks.update.mockResolvedValue({
      organizationId: "org-1",
      requireMfa: true,
      updatedAt: "2026-08-23T12:00:00",
    });
  });

  afterEach(cleanup);

  it("warns before enabling and persists the organization MFA requirement", async () => {
    renderPage();

    const checkbox = await screen.findByLabelText(
      "Require MFA for organization access",
    );
    expect(checkbox).not.toBeChecked();

    fireEvent.click(checkbox);

    expect(
      screen.getByText(/revokes existing active sessions/i),
    ).toBeInTheDocument();

    fireEvent.click(
      screen.getByRole("button", { name: "Enable MFA requirement" }),
    );

    await waitFor(() =>
      expect(mocks.update).toHaveBeenCalledWith("org-1", true),
    );
    expect(
      await screen.findByText("Organization MFA requirement enabled."),
    ).toBeInTheDocument();
  });

  it("keeps the policy read-only without organization management permission", async () => {
    mocks.canManage.mockReturnValue(false);

    renderPage();

    const checkbox = await screen.findByLabelText(
      "Require MFA for organization access",
    );
    expect(checkbox).toBeDisabled();
    expect(
      screen.getByText(/changing it requires organization management permission/i),
    ).toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: /MFA requirement/i }),
    ).not.toBeInTheDocument();
  });
});
