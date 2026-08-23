import axios from "axios";
import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";

import type { ApiErrorResponse } from "../../api/apiClient";
import organizationSecurityPolicyService, {
  type OrganizationSecurityPolicy,
} from "../../api/services/organizationSecurityPolicyService";
import { LoadingState } from "../../components/ui/LoadingState";
import { PageHeader } from "../../components/ui/PageHeader";
import { hasAdminCapability } from "../../lib/auth/capabilities";

function policyError(error: unknown): string {
  if (axios.isAxiosError<ApiErrorResponse>(error)) {
    return error.response?.data?.message || "Organization security policy request failed.";
  }

  if (error instanceof Error) {
    return error.message;
  }

  return "Organization security policy request failed.";
}

export function OrganizationSecurityPage() {
  const { organizationId = "" } = useParams();
  const canManage = hasAdminCapability("organizations.manage");
  const [policy, setPolicy] = useState<OrganizationSecurityPolicy | null>(null);
  const [requireMfa, setRequireMfa] = useState(false);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [saved, setSaved] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError(null);

    void organizationSecurityPolicyService
      .get(organizationId)
      .then((response) => {
        if (active) {
          setPolicy(response);
          setRequireMfa(response.requireMfa);
        }
      })
      .catch((requestError) => {
        if (active) {
          setError(policyError(requestError));
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
  }, [organizationId]);

  async function savePolicy() {
    if (!canManage || !policy || policy.requireMfa === requireMfa) {
      return;
    }

    setSaving(true);
    setError(null);
    setSaved(null);

    try {
      const response = await organizationSecurityPolicyService.update(
        organizationId,
        requireMfa,
      );
      setPolicy(response);
      setRequireMfa(response.requireMfa);
      setSaved(
        response.requireMfa
          ? "Organization MFA requirement enabled."
          : "Organization MFA requirement disabled.",
      );
    } catch (requestError) {
      setError(policyError(requestError));
    } finally {
      setSaving(false);
    }
  }

  const enabling = Boolean(policy && !policy.requireMfa && requireMfa);
  const disabling = Boolean(policy && policy.requireMfa && !requireMfa);
  const changed = Boolean(policy && policy.requireMfa !== requireMfa);

  return (
    <section className="page">
      <PageHeader
        eyebrow="Organization security"
        title="Security policy"
        description="Control organization-scoped authentication requirements."
        actions={
          <Link
            className="button-secondary"
            to={`/organizations/${organizationId}`}
          >
            Back to organization
          </Link>
        }
      />

      {loading ? <LoadingState label="Loading organization security policy" /> : null}

      {error ? (
        <div className="notice notice-error" role="alert">
          {error}
        </div>
      ) : null}

      {saved ? (
        <div className="notice" role="status">
          {saved}
        </div>
      ) : null}

      {policy ? (
        <section className="panel">
          <h2>Multi-factor authentication</h2>

          <label>
            <input
              type="checkbox"
              checked={requireMfa}
              onChange={(event) => {
                setRequireMfa(event.target.checked);
                setSaved(null);
              }}
              disabled={saving || !canManage}
            />{" "}
            Require MFA for organization access
          </label>

          <p className="muted">
            Users who request an organization-scoped session must complete MFA
            before access to this organization is issued.
          </p>

          {!canManage ? (
            <div className="notice">
              You can view this policy, but changing it requires organization
              management permission.
            </div>
          ) : null}

          {enabling ? (
            <div className="notice" role="alert">
              Enabling this policy revokes existing active sessions for
              this organization. Users without MFA receive platform-only access
              and must enroll before signing in to the organization again.
            </div>
          ) : null}

          {disabling ? (
            <div className="notice">
              Disabling the policy does not restore sessions that were already
              revoked when MFA became required.
            </div>
          ) : null}

          <p className="muted">
            Last updated: {policy.updatedAt || "Not previously updated"}
          </p>

          {canManage ? (
            <div className="action-row panel-actions">
              <button
                type="button"
                className="button-primary"
                onClick={savePolicy}
                disabled={saving || !changed}
              >
                {saving
                  ? "Saving..."
                  : enabling
                    ? "Enable MFA requirement"
                    : disabling
                      ? "Disable MFA requirement"
                      : "Policy saved"}
              </button>
            </div>
          ) : null}
        </section>
      ) : null}
    </section>
  );
}
