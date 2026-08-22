// @vitest-environment jsdom

import "@testing-library/jest-dom/vitest";
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

const mocks = vi.hoisted(() => ({
  register: vi.fn(),
  get: vi.fn(),
}));

vi.mock("../../api/services/authService", () => ({
  default: {
    register: mocks.register,
  },
}));

vi.mock("../../api/apiClient", () => ({
  default: {
    get: mocks.get,
  },
}));

import { useAuthStore } from "../../store/authStore";
import { RegisterPage } from "./RegisterPage";

const session = {
  accessToken: "access-token",
  refreshToken: "refresh-token",
  tokenType: "Bearer",
  expiresIn: 900,
  userId: "user-1",
  email: "invitee@example.test",
};

describe("invitation registration handoff", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    useAuthStore.getState().clearSession();
    mocks.register.mockResolvedValue(session);
    mocks.get.mockResolvedValue({ data: { userId: session.userId } });
    window.sessionStorage.clear();
  });

  afterEach(() => {
    cleanup();
    useAuthStore.getState().clearSession();
    window.sessionStorage.clear();
  });

  it("returns a newly registered user to the clean invitation acceptance route", async () => {
    render(
      <MemoryRouter
        initialEntries={[
          { pathname: "/register", state: { from: "/accept-invitation" } },
        ]}
      >
        <Routes>
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/accept-invitation" element={<div>Invitation handoff resumed</div>} />
        </Routes>
      </MemoryRouter>,
    );

    fireEvent.change(screen.getByLabelText("First name"), { target: { value: "Invitee" } });
    fireEvent.change(screen.getByLabelText("Last name"), { target: { value: "User" } });
    fireEvent.change(screen.getByLabelText("Email"), { target: { value: "invitee@example.test" } });
    fireEvent.change(screen.getByLabelText("Password"), { target: { value: "Password@123" } });
    fireEvent.change(screen.getByLabelText("Confirm password"), { target: { value: "Password@123" } });
    fireEvent.click(screen.getByRole("button", { name: "Create account" }));

    await waitFor(() =>
      expect(mocks.register).toHaveBeenCalledWith({
        email: "invitee@example.test",
        password: "Password@123",
        firstName: "Invitee",
        lastName: "User",
      }),
    );
    expect(await screen.findByText("Invitation handoff resumed")).toBeInTheDocument();
    expect(mocks.get).toHaveBeenCalledWith("/users/me", {
      headers: { Authorization: "Bearer access-token" },
    });
  });

  it("waits for user-service profile readiness before resuming the invitation", async () => {
    window.sessionStorage.setItem("ums-organization-invitation-token", "invite-secret");
    mocks.get
      .mockRejectedValueOnce(new Error("profile not synchronized"))
      .mockResolvedValueOnce({ data: { userId: session.userId } });

    render(
      <MemoryRouter
        initialEntries={[
          { pathname: "/register", state: { from: "/accept-invitation" } },
        ]}
      >
        <Routes>
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/accept-invitation" element={<div>Invitation handoff resumed</div>} />
        </Routes>
      </MemoryRouter>,
    );

    fireEvent.change(screen.getByLabelText("First name"), { target: { value: "Invitee" } });
    fireEvent.change(screen.getByLabelText("Last name"), { target: { value: "User" } });
    fireEvent.change(screen.getByLabelText("Email"), { target: { value: "invitee@example.test" } });
    fireEvent.change(screen.getByLabelText("Password"), { target: { value: "Password@123" } });
    fireEvent.change(screen.getByLabelText("Confirm password"), { target: { value: "Password@123" } });
    fireEvent.click(screen.getByRole("button", { name: "Create account" }));

    await waitFor(() => expect(mocks.get).toHaveBeenCalledTimes(2), { timeout: 1500 });
    expect(await screen.findByText("Invitation handoff resumed", {}, { timeout: 1500 })).toBeInTheDocument();
    expect(window.sessionStorage.getItem("ums-organization-invitation-token")).toBe("invite-secret");
  });

  it("does not poll user-service for a normal registration return path", async () => {
    render(
      <MemoryRouter
        initialEntries={[
          { pathname: "/register", state: { from: "/dashboard" } },
        ]}
      >
        <Routes>
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/dashboard" element={<div>Dashboard resumed</div>} />
        </Routes>
      </MemoryRouter>,
    );

    fireEvent.change(screen.getByLabelText("First name"), { target: { value: "Normal" } });
    fireEvent.change(screen.getByLabelText("Last name"), { target: { value: "User" } });
    fireEvent.change(screen.getByLabelText("Email"), { target: { value: "normal@example.test" } });
    fireEvent.change(screen.getByLabelText("Password"), { target: { value: "Password@123" } });
    fireEvent.change(screen.getByLabelText("Confirm password"), { target: { value: "Password@123" } });
    fireEvent.click(screen.getByRole("button", { name: "Create account" }));

    expect(await screen.findByText("Dashboard resumed")).toBeInTheDocument();
    expect(mocks.get).not.toHaveBeenCalled();
  });
});
