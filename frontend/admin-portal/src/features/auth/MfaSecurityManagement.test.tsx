// @vitest-environment jsdom

import "@testing-library/jest-dom/vitest";
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

const mocks = vi.hoisted(() => ({
  mfaStatus: vi.fn(),
  setupTotp: vi.fn(),
  confirmTotp: vi.fn(),
  rotateMfaRecoveryCodes: vi.fn(),
  disableMfa: vi.fn(),
}));

vi.mock("../../api/services/authService", () => ({
  default: {
    mfaStatus: mocks.mfaStatus,
    setupTotp: mocks.setupTotp,
    confirmTotp: mocks.confirmTotp,
    rotateMfaRecoveryCodes: mocks.rotateMfaRecoveryCodes,
    disableMfa: mocks.disableMfa,
  },
}));

import { useAuthStore } from "../../store/authStore";
import { MfaSecurityPage } from "./MfaSecurityPage";

describe("MFA security management", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    useAuthStore.getState().clearSession();
    window.localStorage.clear();
    mocks.mfaStatus.mockResolvedValue({
      enabled: true,
      setupPending: false,
      setupExpiresAt: null,
      recoveryCodesRemaining: 3,
    });
  });

  afterEach(() => {
    cleanup();
    useAuthStore.getState().clearSession();
    window.localStorage.clear();
  });

  it("rotates recovery codes with password and TOTP and shows new codes once", async () => {
    mocks.rotateMfaRecoveryCodes.mockResolvedValue({
      recoveryCodes: ["NEW1-AAAA-BBBB-CCCC", "NEW2-DDDD-EEEE-FFFF"],
    });

    render(<MfaSecurityPage />);

    expect(await screen.findByText("Enabled")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Rotate recovery codes" }));
    fireEvent.change(screen.getByLabelText("Current password"), {
      target: { value: " Password@123 " },
    });
    fireEvent.change(screen.getByLabelText("Authenticator code"), {
      target: { value: "123456" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Rotate recovery codes" }));

    await waitFor(() =>
      expect(mocks.rotateMfaRecoveryCodes).toHaveBeenCalledWith({
        password: " Password@123 ",
        totpCode: "123456",
      }),
    );

    expect(await screen.findByText("NEW1-AAAA-BBBB-CCCC")).toBeInTheDocument();
    expect(screen.getByText("NEW2-DDDD-EEEE-FFFF")).toBeInTheDocument();
    expect(
      screen.getByText("Unused recovery codes remaining: 2"),
    ).toBeInTheDocument();
    expect(window.localStorage.getItem("ums-admin-auth") ?? "").not.toContain(
      "NEW1-AAAA-BBBB-CCCC",
    );
  });

  it("disables MFA with password and recovery code and clears the local session", async () => {
    mocks.disableMfa.mockResolvedValue("MFA disabled. Sign in again to continue.");
    useAuthStore.getState().setSession({
      accessToken: "access-token",
      refreshToken: "refresh-token",
      tokenType: "Bearer",
      expiresIn: 900,
      userId: "user-1",
      email: "user@example.test",
    });

    render(<MfaSecurityPage />);

    expect(await screen.findByText("Enabled")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Disable MFA" }));
    fireEvent.change(screen.getByLabelText("Current password"), {
      target: { value: "Password@123" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Recovery code" }));
    fireEvent.change(screen.getByLabelText("Recovery code"), {
      target: { value: "AAAA-BBBB-CCCC-DDDD" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Disable MFA and sign out" }));

    await waitFor(() =>
      expect(mocks.disableMfa).toHaveBeenCalledWith({
        password: "Password@123",
        recoveryCode: "AAAA-BBBB-CCCC-DDDD",
      }),
    );
    await waitFor(() => expect(useAuthStore.getState().accessToken).toBeNull());
    expect(useAuthStore.getState().refreshToken).toBeNull();
  });
});
