// @vitest-environment jsdom

import "@testing-library/jest-dom/vitest";
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

const mocks = vi.hoisted(() => ({
  forgotPassword: vi.fn(),
  resetPassword: vi.fn(),
}));

vi.mock("../../api/services/authService", () => ({
  default: {
    forgotPassword: mocks.forgotPassword,
    resetPassword: mocks.resetPassword,
  },
}));

import { ForgotPasswordPage } from "./ForgotPasswordPage";
import { ResetPasswordPage } from "./ResetPasswordPage";

describe("password recovery pages", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.forgotPassword.mockResolvedValue(
      "If an account exists for that email, password reset instructions have been sent.",
    );
    mocks.resetPassword.mockResolvedValue(
      "Password reset successful. Please sign in again.",
    );
  });

  afterEach(cleanup);

  it("submits a normalized email and shows the generic forgot-password response", async () => {
    render(
      <MemoryRouter>
        <ForgotPasswordPage />
      </MemoryRouter>,
    );

    fireEvent.change(screen.getByLabelText("Email"), {
      target: { value: "  User@Example.com  " },
    });
    fireEvent.click(screen.getByRole("button", { name: "Send reset link" }));

    await waitFor(() =>
      expect(mocks.forgotPassword).toHaveBeenCalledWith({
        email: "User@Example.com",
      }),
    );
    expect(
      screen.getByText(
        "If an account exists for that email, password reset instructions have been sent.",
      ),
    ).toBeInTheDocument();
  });

  it("does not render a reset form when the reset token is missing", () => {
    render(
      <MemoryRouter initialEntries={["/reset-password"]}>
        <ResetPasswordPage />
      </MemoryRouter>,
    );

    expect(
      screen.getByText("This password reset link is invalid. Request a new reset link."),
    ).toBeInTheDocument();
    expect(screen.queryByLabelText("New password")).not.toBeInTheDocument();
    expect(mocks.resetPassword).not.toHaveBeenCalled();
  });

  it("rejects mismatched passwords before calling the reset API", () => {
    render(
      <MemoryRouter initialEntries={["/reset-password?token=opaque-token"]}>
        <ResetPasswordPage />
      </MemoryRouter>,
    );

    fireEvent.change(screen.getByLabelText("New password"), {
      target: { value: "NewPassword@123" },
    });
    fireEvent.change(screen.getByLabelText("Confirm new password"), {
      target: { value: "DifferentPassword@123" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Reset password" }));

    expect(screen.getByRole("alert")).toHaveTextContent("Passwords do not match.");
    expect(mocks.resetPassword).not.toHaveBeenCalled();
  });

  it("submits the token and new password, then sends the user back to sign in", async () => {
    render(
      <MemoryRouter initialEntries={["/reset-password?token=opaque-token"]}>
        <ResetPasswordPage />
      </MemoryRouter>,
    );

    fireEvent.change(screen.getByLabelText("New password"), {
      target: { value: "NewPassword@123" },
    });
    fireEvent.change(screen.getByLabelText("Confirm new password"), {
      target: { value: "NewPassword@123" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Reset password" }));

    await waitFor(() =>
      expect(mocks.resetPassword).toHaveBeenCalledWith({
        token: "opaque-token",
        newPassword: "NewPassword@123",
      }),
    );
    expect(
      screen.getByText("Password reset successful. Please sign in again."),
    ).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Sign in" })).toHaveAttribute(
      "href",
      "/login",
    );
  });
});
