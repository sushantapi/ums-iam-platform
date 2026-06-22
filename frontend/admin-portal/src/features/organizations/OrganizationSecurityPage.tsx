import { FormEvent, useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { FieldRow } from "../../components/forms/FieldRow";
import { FormSection } from "../../components/forms/FormSection";
import { PageActionBar } from "../../components/forms/PageActionBar";
import { ErrorState } from "../../components/ui/ErrorState";
import { LoadingState } from "../../components/ui/LoadingState";
import { PageHeader } from "../../components/ui/PageHeader";
import { adminApi, type OrganizationSecurityPolicyResponse } from "../../lib/api";

export function OrganizationSecurityPage() {
  const { organizationId = "" } = useParams();
  const [policy, setPolicy] = useState<OrganizationSecurityPolicyResponse>({
    organizationId,
    mfaRequired: false,
    sessionTimeoutMinutes: 60,
    inviteExpiryHours: 72,
    selfServiceJoinEnabled: false,
    inviteResendLimit: 3,
  });
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState<string>();
  const [error, setError] = useState<string>();

  useEffect(() => {
    adminApi
      .organizationSecurityPolicy(organizationId)
      .then(setPolicy)
      .catch((err: Error) => setError(`Security policy could not be loaded: ${err.message}`))
      .finally(() => setLoading(false));
  }, [organizationId]);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(undefined);
    setMessage(undefined);
    try {
      setPolicy(await adminApi.updateOrganizationSecurityPolicy(organizationId, policy));
      setMessage("Security policy saved.");
    } catch (err) {
      setError(`Security policy could not be saved: ${(err as Error).message}`);
    }
  }

  return (
    <section className="page">
      <PageHeader
        eyebrow="Organization Security"
        title="Security Policy"
        description="Tenant authentication, membership, invitation, notification, and audit policy controls."
      />
      {loading && <LoadingState label="Loading security policy" />}
      {error && <ErrorState message={error} />}
      {message && <div className="notice">{message}</div>}
      <form onSubmit={submit}>
        <FormSection title="Authentication Policy" description="Controls sign-in and session posture for this tenant.">
          <FieldRow label="MFA required">
            <input
              type="checkbox"
              checked={Boolean(policy.mfaRequired)}
              onChange={(event) => setPolicy({ ...policy, mfaRequired: event.target.checked })}
            />
          </FieldRow>
          <FieldRow label="Session timeout minutes">
            <input
              type="number"
              value={policy.sessionTimeoutMinutes ?? 60}
              onChange={(event) => setPolicy({ ...policy, sessionTimeoutMinutes: Number(event.target.value) })}
            />
          </FieldRow>
          <FieldRow label="Password policy reference">
            <input
              value={policy.passwordPolicyRef ?? ""}
              onChange={(event) => setPolicy({ ...policy, passwordPolicyRef: event.target.value })}
            />
          </FieldRow>
          <FieldRow label="Invite expiry hours">
            <input
              type="number"
              value={policy.inviteExpiryHours ?? 72}
              onChange={(event) => setPolicy({ ...policy, inviteExpiryHours: Number(event.target.value) })}
            />
          </FieldRow>
        </FormSection>

        <FormSection title="Membership Policy" description="Controls who can invite and administer membership.">
          <FieldRow label="Self-service join enabled">
            <input
              type="checkbox"
              checked={Boolean(policy.selfServiceJoinEnabled)}
              onChange={(event) => setPolicy({ ...policy, selfServiceJoinEnabled: event.target.checked })}
            />
          </FieldRow>
          <FieldRow label="Invite roles">
            <input
              value={(policy.invitedByRoles ?? []).join(", ")}
              onChange={(event) =>
                setPolicy({ ...policy, invitedByRoles: event.target.value.split(",").map((item) => item.trim()).filter(Boolean) })
              }
            />
          </FieldRow>
          <FieldRow label="Role assignment roles">
            <input
              value={(policy.roleAssignmentRoles ?? []).join(", ")}
              onChange={(event) =>
                setPolicy({ ...policy, roleAssignmentRoles: event.target.value.split(",").map((item) => item.trim()).filter(Boolean) })
              }
            />
          </FieldRow>
        </FormSection>

        <FormSection title="Notification Policy" description="Controls invitation retry behavior and templates.">
          <FieldRow label="Invite resend limit">
            <input
              type="number"
              value={policy.inviteResendLimit ?? 3}
              onChange={(event) => setPolicy({ ...policy, inviteResendLimit: Number(event.target.value) })}
            />
          </FieldRow>
          <FieldRow label="Default invite template">
            <input
              value={policy.defaultInviteTemplate ?? ""}
              onChange={(event) => setPolicy({ ...policy, defaultInviteTemplate: event.target.value })}
            />
          </FieldRow>
          <FieldRow label="Audit severity">
            <select
              value={policy.auditSeverity ?? ""}
              onChange={(event) => setPolicy({ ...policy, auditSeverity: event.target.value })}
            >
              <option value="">Default</option>
              <option value="LOW">Low</option>
              <option value="MEDIUM">Medium</option>
              <option value="HIGH">High</option>
            </select>
          </FieldRow>
        </FormSection>

        <PageActionBar>
          <button className="button-primary" type="submit">
            Save policy
          </button>
        </PageActionBar>
      </form>
    </section>
  );
}
