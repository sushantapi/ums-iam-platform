import axios from "axios";
import { FormEvent, useState } from "react";
import { Link } from "react-router-dom";

import type { ApiErrorResponse } from "../../api/apiClient";
import authService from "../../api/services/authService";

const GENERIC_SUCCESS_MESSAGE =
  "If an account exists for that email, password reset instructions have been sent.";

function getRecoveryError(error: unknown): string {
  if (axios.isAxiosError<ApiErrorResponse>(error)) {
    return (
      error.response?.data?.message ||
      "Password reset request could not be completed. Please try again."
    );
  }

  if (error instanceof Error) {
    return error.message;
  }

  return "Password reset request could not be completed. Please try again.";
}

export function ForgotPasswordPage() {
  const [email, setEmail] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    const normalizedEmail = email.trim();
    if (!normalizedEmail) {
      setError("Email is required.");
      return;
    }

    setSubmitting(true);
    setError(null);
    setMessage(null);

    try {
      const responseMessage = await authService.forgotPassword({
        email: normalizedEmail,
      });
      setMessage(responseMessage || GENERIC_SUCCESS_MESSAGE);
    } catch (requestError) {
      setError(getRecoveryError(requestError));
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
          <h2>Reset your password</h2>
          <p>Enter your account email to request a password reset link.</p>
        </div>

        {error ? (
          <div className="notice notice-error" role="alert">
            {error}
          </div>
        ) : null}

        {message ? (
          <div className="success-message" role="status">
            {message}
          </div>
        ) : null}

        <form className="login-form" onSubmit={handleSubmit}>
          <label>
            Email
            <input
              type="email"
              name="email"
              autoComplete="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              disabled={submitting}
              required
            />
          </label>

          <button
            className="button-primary login-submit"
            type="submit"
            disabled={submitting}
          >
            {submitting ? "Sending..." : "Send reset link"}
          </button>
        </form>

        <div className="action-row">
          <Link className="button-secondary" to="/login">
            Back to sign in
          </Link>
        </div>
      </section>
    </main>
  );
}
