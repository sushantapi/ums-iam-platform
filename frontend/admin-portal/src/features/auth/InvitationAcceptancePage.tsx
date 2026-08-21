import axios from "axios";
import { useEffect, useRef, useState } from "react";
import { Link, useNavigate } from "react-router-dom";

import type { ApiErrorResponse } from "../../api/apiClient";
import organizationInvitationService, {
  type OrganizationInvitationAcceptance,
} from "../../api/services/organizationInvitationService";
import { useAuthStore } from "../../store/authStore";

export const INVITATION_TOKEN_SESSION_KEY = "ums-organization-invitation-token";
const ACCEPTANCE_PATH = "/accept-invitation";
const GENERIC_ACCEPTANCE_ERROR =
  "This invitation could not be accepted. It may be invalid, expired, revoked, already used, or intended for another account.";

function captureInvitationToken(): string {
  const queryToken = new URLSearchParams(window.location.search).get("token")?.trim() ?? "";

  if (queryToken) {
    window.sessionStorage.setItem(INVITATION_TOKEN_SESSION_KEY, queryToken);
    window.history.replaceState(
      window.history.state,
      document.title,
      `${window.location.pathname}${window.location.hash}`,
    );
    return queryToken;
  }

  return window.sessionStorage.getItem(INVITATION_TOKEN_SESSION_KEY)?.trim() ?? "";
}

export function InvitationAcceptancePage() {
  const navigate = useNavigate();
  const accessToken = useAuthStore((state) => state.accessToken);
  const tokenRef = useRef<string | null>(null);
  const attemptStarted = useRef(false);

  if (tokenRef.current === null) {
    tokenRef.current = captureInvitationToken();
  }

  const [result, setResult] = useState<OrganizationInvitationAcceptance | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [accepting, setAccepting] = useState(false);
  const [retriable, setRetriable] = useState(false);
  const [retryNonce, setRetryNonce] = useState(0);

  useEffect(() => {
    const token = tokenRef.current ?? "";

    if (!token) {
      setError(GENERIC_ACCEPTANCE_ERROR);
      return;
    }

    if (!accessToken) {
      navigate("/login", {
        replace: true,
        state: { from: ACCEPTANCE_PATH },
      });
      return;
    }

    if (attemptStarted.current) {
      return;
    }

    attemptStarted.current = true;
    setAccepting(true);
    setError(null);
    setRetriable(false);

    organizationInvitationService
      .accept(token)
      .then((acceptance) => {
        window.sessionStorage.removeItem(INVITATION_TOKEN_SESSION_KEY);
        tokenRef.current = "";
        setResult(acceptance);
      })
      .catch((acceptanceError: unknown) => {
        const status = axios.isAxiosError<ApiErrorResponse>(acceptanceError)
          ? acceptanceError.response?.status
          : undefined;
        const canRetry = status === undefined || status >= 500;

        if (!canRetry) {
          window.sessionStorage.removeItem(INVITATION_TOKEN_SESSION_KEY);
          tokenRef.current = "";
        }

        setRetriable(canRetry);
        setError(GENERIC_ACCEPTANCE_ERROR);
      })
      .finally(() => setAccepting(false));
  }, [accessToken, navigate, retryNonce]);

  function retryAcceptance() {
    attemptStarted.current = false;
    setRetryNonce((value) => value + 1);
  }

  return (
    <main className="login-page">
      <section className="login-card">
        <div className="login-brand">
          <div className="brand-mark">UMS</div>
          <div>
            <span className="eyebrow">Identity & Access Management</span>
            <h1>Organization invitation</h1>
          </div>
        </div>

        {accepting ? (
          <div className="login-copy" role="status">
            <h2>Accepting invitation</h2>
            <p>We are securely adding your signed-in account to the organization.</p>
          </div>
        ) : null}

        {error ? (
          <div className="notice notice-error" role="alert">
            {error}
          </div>
        ) : null}

        {result ? (
          <div className="success-message" role="status">
            Invitation accepted. You joined the organization as {result.role.toLowerCase()}.
          </div>
        ) : null}

        <div className="action-row">
          {retriable && !accepting ? (
            <button type="button" className="button-primary" onClick={retryAcceptance}>
              Try again
            </button>
          ) : null}
          {result ? (
            <Link className="button-primary" to={`/organizations/${result.organizationId}/members`}>
              View organization members
            </Link>
          ) : null}
          {!result && !accepting && !retriable ? (
            <Link className="button-secondary" to="/login" state={{ from: ACCEPTANCE_PATH }}>
              Sign in
            </Link>
          ) : null}
        </div>
      </section>
    </main>
  );
}
