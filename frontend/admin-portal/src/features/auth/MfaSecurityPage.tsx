import axios from "axios";
import { FormEvent, useEffect, useState } from "react";
import { ShieldCheck } from "lucide-react";

import type { ApiErrorResponse } from "../../api/apiClient";
import authService, {
  type MfaStatusResponse,
  type MfaTotpSetupResponse,
} from "../../api/services/authService";
import { MfaManagementPanel } from "./MfaManagementPanel";

function getMfaError(error: unknown): string {
  if (axios.isAxiosError<ApiErrorResponse>(error)) {
    return error.response?.data?.message || "MFA request failed.";
  }

  if (error instanceof Error) {
    return error.message;
  }

  return "MFA request failed. Please try again.";
}

export function MfaSecurityPage() {
  const [status, setStatus] = useState<MfaStatusResponse | null>(null);
  const [setup, setSetup] = useState<MfaTotpSetupResponse | null>(null);
  const [confirmationCode, setConfirmationCode] = useState("");
  const [recoveryCodes, setRecoveryCodes] = useState<string[]>([]);
  const [loading, setLoading] = useState(true);
  const [working, setWorking] = useState(false);
  const [copied, setCopied] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;

    void authService
      .mfaStatus()
      .then((result) => {
        if (active) {
          setStatus(result);
        }
      })
      .catch((requestError) => {
        if (active) {
          setError(getMfaError(requestError));
        }
      })
      .finally(() => {
        if (active) {
          setLoading(false);
        }
      });

    return () => {
      active = false;
    };
  }, []);

  async function startSetup() {
    setWorking(true);
    setError(null);
    setRecoveryCodes([]);
    setCopied(false);

    try {
      const response = await authService.setupTotp();
      setSetup(response);
      setConfirmationCode("");
      setStatus((current) => ({
        enabled: false,
        setupPending: true,
        setupExpiresAt: response.expiresAt,
        recoveryCodesRemaining: current?.recoveryCodesRemaining ?? 0,
      }));
    } catch (requestError) {
      setError(getMfaError(requestError));
    } finally {
      setWorking(false);
    }
  }

  async function confirmSetup(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const code = confirmationCode.trim();

    if (!code) {
      setError("Authenticator code is required.");
      return;
    }

    setWorking(true);
    setError(null);

    try {
      const response = await authService.confirmTotp(code);
      setRecoveryCodes(response.recoveryCodes);
      setSetup(null);
      setConfirmationCode("");
      setStatus({
        enabled: true,
        setupPending: false,
        setupExpiresAt: null,
        recoveryCodesRemaining: response.recoveryCodes.length,
      });
    } catch (requestError) {
      setError(getMfaError(requestError));
    } finally {
      setWorking(false);
    }
  }

  async function copyRecoveryCodes() {
    if (!navigator.clipboard) {
      setError("Clipboard access is unavailable. Save the recovery codes manually.");
      return;
    }

    try {
      await navigator.clipboard.writeText(recoveryCodes.join("\n"));
      setCopied(true);
    } catch {
      setError("Recovery codes could not be copied. Save them manually.");
    }
  }

  return (
    <section className="page">
      <header className="page-header">
        <div>
          <span className="eyebrow">Account security</span>
          <h1>
            <ShieldCheck size={30} />
            Multi-factor authentication
          </h1>
          <p>
            Protect this account with a time-based authenticator app. MFA secrets and recovery codes are never stored in this browser.
          </p>
        </div>
      </header>

      {loading ? <div className="loading-state">Loading MFA status...</div> : null}

      {error ? (
        <div className="notice notice-error" role="alert">
          {error}
        </div>
      ) : null}

      {status ? (
        <div className="blueprint-grid">
          <section className="panel">
            <h2>Status</h2>
            <p>
              <strong>{status.enabled ? "Enabled" : "Not enabled"}</strong>
            </p>
            {status.enabled ? (
              <>
                <p className="muted">
                  Unused recovery codes remaining: {status.recoveryCodesRemaining}
                </p>
                <MfaManagementPanel
                  onRecoveryCodesRotated={(codes) => {
                    setRecoveryCodes(codes);
                    setCopied(false);
                    setStatus((current) =>
                      current
                        ? { ...current, recoveryCodesRemaining: codes.length }
                        : current,
                    );
                  }}
                />
              </>
            ) : status.setupPending && !setup ? (
              <div className="notice">
                A setup is pending, but its secret is intentionally not retrievable. Restart setup to generate a new one-time secret.
              </div>
            ) : (
              <p className="muted">No active second factor is configured.</p>
            )}

            {!status.enabled ? (
              <button
                type="button"
                className="button-primary"
                onClick={startSetup}
                disabled={working}
              >
                {working ? "Starting setup..." : status.setupPending ? "Restart TOTP setup" : "Set up authenticator app"}
              </button>
            ) : null}
          </section>

          {setup ? (
            <section className="form-panel">
              <h2>Confirm authenticator</h2>
              <div className="notice">
                This secret is shown only for this setup response. Add it to your authenticator app now before confirming.
              </div>

              <label>
                Secret
                <code>{setup.secret}</code>
              </label>

              <label>
                Provisioning URI
                <code>{setup.provisioningUri}</code>
              </label>

              <form className="login-form" onSubmit={confirmSetup}>
                <label>
                  Authenticator code
                  <input
                    type="text"
                    inputMode="numeric"
                    autoComplete="one-time-code"
                    value={confirmationCode}
                    onChange={(event) => setConfirmationCode(event.target.value)}
                    disabled={working}
                    required
                  />
                </label>

                <button className="button-primary" type="submit" disabled={working}>
                  {working ? "Confirming..." : "Enable MFA"}
                </button>
              </form>
            </section>
          ) : null}

          {recoveryCodes.length > 0 ? (
            <section className="panel panel-wide">
              <h2>Save your recovery codes now</h2>
              <div className="notice">
                Each recovery code works once. Store them somewhere secure. They will not be shown again after you leave this page.
              </div>

              <ol className="detail-list">
                {recoveryCodes.map((code) => (
                  <li key={code}>
                    <code>{code}</code>
                  </li>
                ))}
              </ol>

              <div className="action-row panel-actions">
                <button
                  type="button"
                  className="button-secondary"
                  onClick={copyRecoveryCodes}
                >
                  {copied ? "Copied" : "Copy recovery codes"}
                </button>
              </div>
            </section>
          ) : null}
        </div>
      ) : null}
    </section>
  );
}
