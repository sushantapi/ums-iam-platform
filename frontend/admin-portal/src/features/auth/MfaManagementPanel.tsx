import axios from "axios";
import { FormEvent, useState } from "react";

import type { ApiErrorResponse } from "../../api/apiClient";
import authService, {
  type MfaSensitiveActionRequest,
} from "../../api/services/authService";
import { useAuthStore } from "../../store/authStore";

type Action = "rotate" | "disable";
type FactorMode = "totp" | "recovery";

function getMfaError(error: unknown): string {
  if (axios.isAxiosError<ApiErrorResponse>(error)) {
    return error.response?.data?.message || "MFA request failed.";
  }

  if (error instanceof Error) {
    return error.message;
  }

  return "MFA request failed. Please try again.";
}

export function MfaManagementPanel({
  onRecoveryCodesRotated,
}: {
  onRecoveryCodesRotated: (codes: string[]) => void;
}) {
  const clearSession = useAuthStore((state) => state.clearSession);
  const [action, setAction] = useState<Action | null>(null);
  const [factorMode, setFactorMode] = useState<FactorMode>("totp");
  const [password, setPassword] = useState("");
  const [factorCode, setFactorCode] = useState("");
  const [working, setWorking] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  function openAction(nextAction: Action) {
    setAction(nextAction);
    setFactorMode("totp");
    setPassword("");
    setFactorCode("");
    setError(null);
    setSuccess(null);
  }

  function closeAction() {
    setAction(null);
    setPassword("");
    setFactorCode("");
    setError(null);
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!action) {
      return;
    }

    const trimmedFactor = factorCode.trim();
    if (!password || !trimmedFactor) {
      setError("Current password and one MFA factor are required.");
      return;
    }

    const request: MfaSensitiveActionRequest = {
      password,
      ...(factorMode === "totp"
        ? { totpCode: trimmedFactor }
        : { recoveryCode: trimmedFactor }),
    };

    setWorking(true);
    setError(null);
    setSuccess(null);

    try {
      if (action === "rotate") {
        const response = await authService.rotateMfaRecoveryCodes(request);
        onRecoveryCodesRotated(response.recoveryCodes);
        setSuccess("Recovery codes rotated. Save the new codes now.");
        setAction(null);
        setPassword("");
        setFactorCode("");
        return;
      }

      await authService.disableMfa(request);
      clearSession();
    } catch (requestError) {
      setError(getMfaError(requestError));
    } finally {
      setWorking(false);
    }
  }

  return (
    <div className="panel-actions">
      {!action ? (
        <div className="action-row">
          <button
            type="button"
            className="button-secondary"
            onClick={() => openAction("rotate")}
            disabled={working}
          >
            Rotate recovery codes
          </button>
          <button
            type="button"
            className="button-secondary"
            onClick={() => openAction("disable")}
            disabled={working}
          >
            Disable MFA
          </button>
        </div>
      ) : null}

      {success ? <div className="notice">{success}</div> : null}

      {action ? (
        <form className="login-form" onSubmit={submit}>
          <div className="notice">
            {action === "rotate"
              ? "Rotating recovery codes invalidates every previous unused recovery code."
              : "Disabling MFA revokes all active sessions. You will need to sign in again."}
          </div>

          {error ? (
            <div className="notice notice-error" role="alert">
              {error}
            </div>
          ) : null}

          <label>
            Current password
            <input
              type="password"
              autoComplete="current-password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              disabled={working}
              required
            />
          </label>

          <div className="tabs" aria-label="MFA verification method">
            <button
              type="button"
              className={`tab ${factorMode === "totp" ? "tab-active" : ""}`}
              onClick={() => {
                setFactorMode("totp");
                setFactorCode("");
                setError(null);
              }}
              disabled={working}
            >
              Authenticator code
            </button>
            <button
              type="button"
              className={`tab ${factorMode === "recovery" ? "tab-active" : ""}`}
              onClick={() => {
                setFactorMode("recovery");
                setFactorCode("");
                setError(null);
              }}
              disabled={working}
            >
              Recovery code
            </button>
          </div>

          <label>
            {factorMode === "totp" ? "Authenticator code" : "Recovery code"}
            <input
              type="text"
              inputMode={factorMode === "totp" ? "numeric" : undefined}
              autoComplete={factorMode === "totp" ? "one-time-code" : "off"}
              value={factorCode}
              onChange={(event) => setFactorCode(event.target.value)}
              disabled={working}
              required
            />
          </label>

          <div className="action-row">
            <button className="button-primary" type="submit" disabled={working}>
              {working
                ? "Verifying..."
                : action === "rotate"
                  ? "Rotate recovery codes"
                  : "Disable MFA and sign out"}
            </button>
            <button
              className="button-secondary"
              type="button"
              onClick={closeAction}
              disabled={working}
            >
              Cancel
            </button>
          </div>
        </form>
      ) : null}
    </div>
  );
}
