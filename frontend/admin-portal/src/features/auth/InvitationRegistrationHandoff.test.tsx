// @vitest-environment jsdom

import "@testing-library/jest-dom/vitest";
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

const mocks = vi.hoisted(() => ({
  register: vi.fn(),
}));

vi.mock("../../api/services/authService", () => ({
  default: {
    register: mocks.register,
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
  });

  afterEach(() => {
    cleanup();
    useAuthStore.getState().clearSession();
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
  });
});
