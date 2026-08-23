import axios from "axios";
import { FormEvent, useState } from "react";
import { Navigate, useLocation, useNavigate } from "react-router-dom";

import type { ApiErrorResponse } from "../../api/apiClient";
import authService from "../../api/services/authService";
import { useAuthStore } from "../../store/authStore";
import {
  clearPendingMfaChallenge,
  getPendingMfaChallenge,
} from "./mfaChallengeState";

function getMfaError(error: unknown): string {
  if (axios.isAxiosError<ApiErrorResponse>(error)) {
    return error.response?.data?.message || "MFA verification failed.";
  }

  if (error instanceof Error) {
    return error.message;
  }

  return "MFA verification failed. Please try again.";
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

type FactorMode = "totp" | "recovery";

export function MfaChallengePage() {
  const navigate = useNavigate();
  const location = useLocation();
  const from = safeReturnPath(location.state);
  const accessToken = useAuthStore((state) => state.accessToken);
  const setSession = useAuthStore((state) => state.setSession);
  const [challenge] = useState(() => getPendingMfaChallenge());
  const [mode, setMode] = useState<FactorMode>("totp");
  const [code, setCode] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (accessToken) {
    return <Navigate to={from} replace />;
  }

  if (!challenge) {
    return <Navigate to="/login" replace state={{ from }} />;
  }

  const activeChallenge = challenge;
  const remainingMinutes = Math.max(
    1,
    Math.ceil((activeChallenge.expiresAt - Date.now()) / 60_000),
  );

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const value = code.trim();

    if (!value) {
      setError(mode === "totp" ? "Authenticator code is required." : "Recovery code is required.");
      return;
    }

    setSubmitting(true);
    setError(null);

    try {
      const session = await authService.verifyMfaChallenge({
        challengeToken: activeChallenge.token,
        ...(mode === "totp" ? { totpCode: value } : { recoveryCode: value }),
      });

      if (!session.accessToken || !session.refreshToken || session.mfaRequired) {
        throw new Error("MFA verification did not return a valid session.");
      }

      clearPendingMfaChallenge();
      setSession(session);
      navigate(from, { replace: true });
    } catch (verificationError) {
      setError(getMfaError(verificationError));
    } finally {
      setSubmitting(false);
    }
  }

  function switchMode(nextMode: FactorMode) {
    setMode(nextMode);
    setCode("");
    setError(null);
  }

  function restartSignIn() {
    clearPendingMfaChallenge();
    navigate("/login", { replace: true, state: { from } });
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
          <h2>Verify your identity</h2>
          <p>
            MFA is enabled for {activeChallenge.email}. This challenge expires in about {remainingMinutes} minute
            {remainingMinutes === 1 ? "" : "s"}.
          </p>
        </div>

        <div className="tabs" aria-label="MFA verification method">
          <button
            type="button"
            className={`tab ${mode === "totp" ? "tab-active" : ""}`}
            onClick={() => switchMode("totp")}
            disabled={submitting}
          >
            Authenticator code
          </button>
          <button
            type="button"
            className={`tab ${mode === "recovery" ? "tab-active" : ""}`}
            onClick={() => switchMode("recovery")}
            disabled={submitting}
          >
            Recovery code
          </button>
        </div>

        {error ? (
          <div className="notice notice-error" role="alert">
            {error}
          </div>
        ) : null}

        <form className="login-form" onSubmit={handleSubmit}>
          {mode === "totp" ? (
            <label>
              Authenticator code
              <input
                type="text"
                name="totpCode"
                inputMode="numeric"
                autoComplete="one-time-code"
                value={code}
                onChange={(event) => setCode(event.target.value)}
                disabled={submitting}
                required
              />
            </label>
          ) : (
            <label>
              Recovery code
              <input
                type="text"
                name="recoveryCode"
                autoComplete="off"
                value={code}
                onChange={(event) => setCode(event.target.value)}
                disabled={submitting}
                required
              />
            </label>
          )}

          <button
            className="button-primary login-submit"
            type="submit"
            disabled={submitting}
          >
            {submitting ? "Verifying..." : "Verify and sign in"}
          </button>

          <button
            className="button-secondary login-submit"
            type="button"
            onClick={restartSignIn}
            disabled={submitting}
          >
            Start sign in again
          </button>
        </form>
      </section>
    </main>
  );
}
