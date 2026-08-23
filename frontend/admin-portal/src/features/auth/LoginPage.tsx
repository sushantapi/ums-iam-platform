import axios from "axios";
import { FormEvent, useState } from "react";
import {
  Link,
  Navigate,
  useLocation,
  useNavigate,
} from "react-router-dom";

import authService from "../../api/services/authService";
import type { ApiErrorResponse } from "../../api/apiClient";
import { useAuthStore } from "../../store/authStore";
import { beginMfaChallenge } from "./mfaChallengeState";

function getLoginError(error: unknown): string {
  if (axios.isAxiosError<ApiErrorResponse>(error)) {
    return (
      error.response?.data?.message ||
      "Sign in failed. Check your email and password."
    );
  }

  if (error instanceof Error) {
    return error.message;
  }

  return "Sign in failed. Please try again.";
}

function safeReturnPath(state: unknown): string {
  const from =
    state && typeof state === "object" && "from" in state
      ? (state as { from?: unknown }).from
      : undefined;

  return typeof from === "string" && from.startsWith("/") && !from.startsWith("//")
    ? from
    : "/dashboard";
}

export function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const from = safeReturnPath(location.state);

  const accessToken = useAuthStore((state) => state.accessToken);
  const setSession = useAuthStore((state) => state.setSession);

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [organizationId, setOrganizationId] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (accessToken) {
    return <Navigate to={from} replace />;
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!email.trim() || !password) {
      setError("Email and password are required.");
      return;
    }

    setSubmitting(true);
    setError(null);

    try {
      const response = await authService.login({
        email: email.trim(),
        password,
        deviceInfo: navigator.userAgent,
        client: "ADMIN_PORTAL",
        ...(organizationId.trim()
          ? { organizationId: organizationId.trim() }
          : {}),
      });

      if (response.mfaRequired) {
        beginMfaChallenge(response);
        navigate("/mfa-challenge", { replace: true, state: { from } });
        return;
      }

      if (!response.accessToken || !response.refreshToken) {
        throw new Error("Login response did not contain session tokens.");
      }

      setSession(response);
      navigate(from, { replace: true });
    } catch (loginError) {
      setError(getLoginError(loginError));
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
          <h2>Sign in</h2>
          <p>Use your authorized UMS account.</p>
        </div>

        {error ? (
          <div className="notice notice-error" role="alert">
            {error}
          </div>
        ) : null}

        <form className="login-form" onSubmit={handleSubmit}>
          <label>
            Email
            <input
              type="email"
              name="email"
              autoComplete="username"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              disabled={submitting}
              required
            />
          </label>

          <label>
            Password
            <input
              type="password"
              name="password"
              autoComplete="current-password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              disabled={submitting}
              required
            />
          </label>

          <div className="action-row">
            <Link className="button-secondary" to="/forgot-password">
              Forgot password?
            </Link>
            <Link className="button-secondary" to="/register" state={{ from }}>
              Create account
            </Link>
          </div>

          <label>
            Organization ID <span className="field-hint">(optional)</span>
            <input
              type="text"
              name="organizationId"
              value={organizationId}
              onChange={(event) => setOrganizationId(event.target.value)}
              placeholder="Required for organization-scoped admin access"
              disabled={submitting}
            />
          </label>

          <button
            className="button-primary login-submit"
            type="submit"
            disabled={submitting}
          >
            {submitting ? "Signing in..." : "Sign in"}
          </button>
        </form>
      </section>
    </main>
  );
}
