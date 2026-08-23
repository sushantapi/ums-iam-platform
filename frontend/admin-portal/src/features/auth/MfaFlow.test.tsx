// @vitest-environment jsdom

import "@testing-library/jest-dom/vitest";
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

const mocks = vi.hoisted(() => ({
  login: vi.fn(),
  verifyMfaChallenge: vi.fn(),
  mfaStatus: vi.fn(),
  setupTotp: vi.fn(),
  confirmTotp: vi.fn(),
}));

vi.mock("../../api/services/authService", () => ({
  default: {
    login: mocks.login,
    verifyMfaChallenge: mocks.verifyMfaChallenge,
    mfaStatus: mocks.mfaStatus,
    setupTotp: mocks.setupTotp,
    confirmTotp: mocks.confirmTotp,
  },
}));

import { useAuthStore } from "../../store/authStore";
import { LoginPage } from "./LoginPage";
import { MfaChallengePage } from "./MfaChallengePage";
import { MfaSecurityPage } from "./MfaSecurityPage";
import {
  beginMfaChallenge,
  clearPendingMfaChallenge,
} from "./mfaChallengeState";

const session = {
  accessToken: "access-token",
  refreshToken: "refresh-token",
  tokenType: "Bearer",
  expiresIn: 900,
  userId: "user-1",
  email: "user@example.test",
};

const challengeResponse = {
  accessToken: "",
  refreshToken: "",
  tokenType: "",
  expiresIn: 0,
  userId: "user-1",
  email: "user@example.test",
  mfaRequired: true,
  mfaChallengeToken: "challenge-token",
  mfaChallengeExpiresIn: 300,
};

describe("Admin Portal MFA flow", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    clearPendingMfaChallenge();
    useAuthStore.getState().clearSession();
    window.localStorage.clear();
    window.sessionStorage.clear();
    mocks.login.mockResolvedValue(challengeResponse);
    mocks.verifyMfaChallenge.mockResolvedValue(session);
  });

  afterEach(() => {
    cleanup();
    clearPendingMfaChallenge();
    useAuthStore.getState().clearSession();
    window.localStorage.clear();
    window.sessionStorage.clear();
  });

  it("does not persist a session before TOTP verification and preserves the return path", async () => {
    render(
      <MemoryRouter
        initialEntries={[
          { pathname: "/login", state: { from: "/accept-invitation" } },
        ]}
      >
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/mfa-challenge" element={<MfaChallengePage />} />
          <Route path="/accept-invitation" element={<div>Invitation resumed</div>} />
        </Routes>
      </MemoryRouter>,
    );

    fireEvent.change(screen.getByLabelText("Email"), {
      target: { value: "user@example.test" },
    });
    fireEvent.change(screen.getByLabelText("Password"), {
      target: { value: "Password@123" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Sign in" }));

    expect(await screen.findByRole("heading", { name: "Verify your identity" })).toBeInTheDocument();
    expect(useAuthStore.getState().accessToken).toBeNull();
    expect(window.localStorage.getItem("ums-admin-auth") ?? "").not.toContain("challenge-token");
    expect(window.sessionStorage.length).toBe(0);

    fireEvent.change(screen.getByLabelText("Authenticator code"), {
      target: { value: "123456" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Verify and sign in" }));

    await waitFor(() =>
      expect(mocks.verifyMfaChallenge).toHaveBeenCalledWith({
        challengeToken: "challenge-token",
        totpCode: "123456",
      }),
    );
    expect(await screen.findByText("Invitation resumed")).toBeInTheDocument();
    expect(useAuthStore.getState().accessToken).toBe("access-token");
  });

  it("supports a one-time recovery code for the MFA challenge", async () => {
    beginMfaChallenge(challengeResponse);

    render(
      <MemoryRouter initialEntries={["/mfa-challenge"]}>
        <Routes>
          <Route path="/mfa-challenge" element={<MfaChallengePage />} />
          <Route path="/dashboard" element={<div>Dashboard</div>} />
        </Routes>
      </MemoryRouter>,
    );

    fireEvent.click(screen.getByRole("button", { name: "Recovery code" }));
    fireEvent.change(screen.getByLabelText("Recovery code"), {
      target: { value: "AAAA-BBBB-CCCC-DDDD" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Verify and sign in" }));

    await waitFor(() =>
      expect(mocks.verifyMfaChallenge).toHaveBeenCalledWith({
        challengeToken: "challenge-token",
        recoveryCode: "AAAA-BBBB-CCCC-DDDD",
      }),
    );
    expect(await screen.findByText("Dashboard")).toBeInTheDocument();
  });

  it("requires a fresh sign-in when the in-memory challenge is missing", async () => {
    clearPendingMfaChallenge();

    render(
      <MemoryRouter initialEntries={["/mfa-challenge"]}>
        <Routes>
          <Route path="/mfa-challenge" element={<MfaChallengePage />} />
          <Route path="/login" element={<div>Fresh sign in required</div>} />
        </Routes>
      </MemoryRouter>,
    );

    expect(await screen.findByText("Fresh sign in required")).toBeInTheDocument();
    expect(mocks.verifyMfaChallenge).not.toHaveBeenCalled();
  });

  it("enrolls TOTP and shows recovery codes only after confirmation", async () => {
    mocks.mfaStatus.mockResolvedValue({
      enabled: false,
      setupPending: false,
      setupExpiresAt: null,
      recoveryCodesRemaining: 0,
    });
    mocks.setupTotp.mockResolvedValue({
      secret: "RAW-TOTP-SECRET",
      provisioningUri: "otpauth://totp/UMS%20IAM:user@example.test?secret=RAW-TOTP-SECRET",
      expiresAt: "2026-08-23T04:00:00Z",
    });
    mocks.confirmTotp.mockResolvedValue({
      recoveryCodes: ["AAAA-BBBB-CCCC-DDDD", "EEEE-FFFF-GGGG-HHHH"],
    });

    render(<MfaSecurityPage />);

    expect(await screen.findByText("Not enabled")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Set up authenticator app" }));

    expect(await screen.findByText("RAW-TOTP-SECRET")).toBeInTheDocument();
    expect(screen.queryByText("AAAA-BBBB-CCCC-DDDD")).not.toBeInTheDocument();

    fireEvent.change(screen.getByLabelText("Authenticator code"), {
      target: { value: "123456" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Enable MFA" }));

    await waitFor(() => expect(mocks.confirmTotp).toHaveBeenCalledWith("123456"));
    expect(await screen.findByText("AAAA-BBBB-CCCC-DDDD")).toBeInTheDocument();
    expect(screen.getByText("EEEE-FFFF-GGGG-HHHH")).toBeInTheDocument();
  });
});
