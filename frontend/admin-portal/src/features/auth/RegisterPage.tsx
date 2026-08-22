import axios from "axios";
import { FormEvent, useState } from "react";
import { Link, Navigate, useLocation, useNavigate } from "react-router-dom";

import apiClient, { type ApiErrorResponse } from "../../api/apiClient";
import authService from "../../api/services/authService";
import { useAuthStore } from "../../store/authStore";

const INVITATION_ACCEPTANCE_PATH = "/accept-invitation";
const INVITATION_PROFILE_MAX_ATTEMPTS = 20;
const INVITATION_PROFILE_RETRY_DELAY_MS = 250;
const INVITATION_PROFILE_SETUP_MESSAGE =
  "Account created, but profile setup is still completing. Please use the sign-in link below in a moment to continue your invitation.";

function delay(milliseconds: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, milliseconds));
}

async function waitForInvitationProfileReadiness(accessToken: string): Promise<boolean> {
  for (let attempt = 1; attempt <= INVITATION_PROFILE_MAX_ATTEMPTS; attempt += 1) {
    try {
      await apiClient.get("/users/me", {
        headers: { Authorization: `Bearer ${accessToken}` },
      });
      return true;
    } catch (readinessError) {
      const status = axios.isAxiosError<ApiErrorResponse>(readinessError)
        ? readinessError.response?.status
        : undefined;

      if (status === 401 || status === 403 || attempt === INVITATION_PROFILE_MAX_ATTEMPTS) {
        return false;
      }

      await delay(INVITATION_PROFILE_RETRY_DELAY_MS);
    }
  }

  return false;
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

function getRegistrationError(error: unknown): string {
  if (axios.isAxiosError<ApiErrorResponse>(error)) {
    return error.response?.data?.message || "Account registration failed. Please review your details.";
  }
  return error instanceof Error
    ? error.message
    : "Account registration failed. Please try again.";
}

export function RegisterPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const from = safeReturnPath(location.state);
  const accessToken = useAuthStore((state) => state.accessToken);
  const setSession = useAuthStore((state) => state.setSession);

  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (accessToken) {
    return <Navigate to={from} replace />;
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!firstName.trim() || !lastName.trim() || !email.trim() || !password) {
      setError("First name, last name, email, and password are required.");
      return;
    }
    if (password.length < 8 || password.length > 128) {
      setError("Password must be between 8 and 128 characters.");
      return;
    }
    if (password !== confirmPassword) {
      setError("Passwords do not match.");
      return;
    }

    setSubmitting(true);
    setError(null);
    try {
      const session = await authService.register({
        email: email.trim(),
        password,
        firstName: firstName.trim(),
        lastName: lastName.trim(),
      });
      if (from === INVITATION_ACCEPTANCE_PATH) {
        const profileReady = await waitForInvitationProfileReadiness(session.accessToken);
        if (!profileReady) {
          setError(INVITATION_PROFILE_SETUP_MESSAGE);
          return;
        }
      }

      setSession(session);
      navigate(from, { replace: true });
    } catch (registrationError) {
      setError(getRegistrationError(registrationError));
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
            <h1>Create account</h1>
          </div>
        </div>

        <div className="login-copy">
          <h2>Register</h2>
          <p>Create your UMS identity, then continue to the invitation you opened.</p>
        </div>

        {error ? (
          <div className="notice notice-error" role="alert">
            {error}
          </div>
        ) : null}

        <form className="login-form" onSubmit={handleSubmit}>
          <label>
            First name
            <input value={firstName} onChange={(event) => setFirstName(event.target.value)} disabled={submitting} required />
          </label>
          <label>
            Last name
            <input value={lastName} onChange={(event) => setLastName(event.target.value)} disabled={submitting} required />
          </label>
          <label>
            Email
            <input type="email" autoComplete="username" value={email} onChange={(event) => setEmail(event.target.value)} disabled={submitting} required />
          </label>
          <label>
            Password
            <input type="password" autoComplete="new-password" value={password} onChange={(event) => setPassword(event.target.value)} disabled={submitting} required />
          </label>
          <label>
            Confirm password
            <input type="password" autoComplete="new-password" value={confirmPassword} onChange={(event) => setConfirmPassword(event.target.value)} disabled={submitting} required />
          </label>

          <button className="button-primary login-submit" type="submit" disabled={submitting}>
            {submitting ? "Creating account..." : "Create account"}
          </button>
        </form>

        <div className="action-row">
          <Link className="button-secondary" to="/login" state={{ from }}>
            Already have an account? Sign in
          </Link>
        </div>
      </section>
    </main>
  );
}
