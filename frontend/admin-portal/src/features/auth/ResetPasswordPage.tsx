import axios from "axios";
import { FormEvent, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";

import type { ApiErrorResponse } from "../../api/apiClient";
import authService from "../../api/services/authService";

const PASSWORD_PATTERN = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&]).+$/;

function getRecoveryError(error: unknown): string {
  if (axios.isAxiosError<ApiErrorResponse>(error)) {
    return (
      error.response?.data?.message ||
      "Password reset could not be completed. The link may be invalid or expired."
    );
  }

  if (error instanceof Error) {
    return error.message;
  }

  return "Password reset could not be completed. The link may be invalid or expired.";
}

export function ResetPasswordPage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get("token")?.trim() ?? "";

  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!token) {
      setError("This password reset link is invalid. Request a new reset link.");
      return;
    }

    if (newPassword.length < 8 || newPassword.length > 128 || !PASSWORD_PATTERN.test(newPassword)) {
      setError(
        "Password must be 8-128 characters and include uppercase, lowercase, a digit and a special character.",
      );
      return;
    }

    if (newPassword !== confirmPassword) {
      setError("Passwords do not match.");
      return;
    }

    setSubmitting(true);
    setError(null);
    setSuccess(null);

    try {
      const responseMessage = await authService.resetPassword({
        token,
        newPassword,
      });
      setSuccess(responseMessage || "Password reset successful. Please sign in again.");
      setNewPassword("");
      setConfirmPassword("");
    } catch (resetError) {
      setError(getRecoveryError(resetError));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="login-page">
      <section className="login-card">
        <div className="login-brand">
          <div className="brand-mark">UMS</div>
          <div>
            <span className="eyebrow">Identity & Access Management</span>
            <h1>Admin Portal</h1>
          </div>
        </div>

        <div className="login-copy">
          <h2>Choose a new password</h2>
          <p>Your reset link is single-use and expires automatically.</p>
        </div>

        {!token ? (
          <div className="notice notice-error" role="alert">
            This password reset link is invalid. Request a new reset link.
          </div>
        ) : null}

        {error ? (
          <div className="notice notice-error" role="alert">
            {error}
          </div>
        ) : null}

        {success ? (
          <div className="success-message" role="status">
            {success}
          </div>
        ) : null}

        {token && !success ? (
          <form className="login-form" onSubmit={handleSubmit}>
            <label>
              New password
              <input
                type="password"
                name="newPassword"
                autoComplete="new-password"
                value={newPassword}
                onChange={(event) => setNewPassword(event.target.value)}
                disabled={submitting}
                required
              />
            </label>

            <label>
              Confirm new password
              <input
                type="password"
                name="confirmPassword"
                autoComplete="new-password"
                value={confirmPassword}
                onChange={(event) => setConfirmPassword(event.target.value)}
                disabled={submitting}
                required
              />
            </label>

            <button
              className="button-primary login-submit"
              type="submit"
              disabled={submitting}
            >
              {submitting ? "Resetting..." : "Reset password"}
            </button>
          </form>
        ) : null}

        <div className="action-row">
          <Link className="button-secondary" to={success ? "/login" : "/forgot-password"}>
            {success ? "Sign in" : "Request a new reset link"}
          </Link>
        </div>
      </section>
    </main>
  );
}
