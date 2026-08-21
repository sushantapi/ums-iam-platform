import { FormEvent, useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import organizationAdminService from "../../api/services/organizationAdminService";
import organizationInvitationService, {
  type OrganizationInvitation,
  type OrganizationInvitationRole,
} from "../../api/services/organizationInvitationService";
import { ConfirmActionModal } from "../../components/feedback/ConfirmActionModal";
import { DataTable } from "../../components/ui/DataTable";
import { ErrorState } from "../../components/ui/ErrorState";
import { LoadingState } from "../../components/ui/LoadingState";
import { PageHeader } from "../../components/ui/PageHeader";
import { StatusBadge } from "../../components/ui/StatusBadge";
import { adminApi, type OrganizationMemberResponse } from "../../lib/api";
import { hasAdminCapability } from "../../lib/auth/capabilities";

type InvitationAction = {
  type: "resend" | "revoke";
  invitation: OrganizationInvitation;
};

export function OrganizationMembersPage() {
  const { organizationId = "" } = useParams();
  const canManageOrganizations = hasAdminCapability("organizations.manage");
  const [members, setMembers] = useState<OrganizationMemberResponse[]>([]);
  const [invitations, setInvitations] = useState<OrganizationInvitation[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string>();
  const [showAddMember, setShowAddMember] = useState(false);
  const [showInviteMember, setShowInviteMember] = useState(false);
  const [userId, setUserId] = useState("");
  const [role, setRole] = useState<"ADMIN" | "MEMBER">("MEMBER");
  const [inviteEmail, setInviteEmail] = useState("");
  const [inviteRole, setInviteRole] = useState<OrganizationInvitationRole>("MEMBER");
  const [submitting, setSubmitting] = useState(false);
  const [invitationAction, setInvitationAction] = useState<InvitationAction | null>(null);
  const [invitationActionPending, setInvitationActionPending] = useState(false);

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError(undefined);

    Promise.all([
      adminApi.organizationMembers(organizationId),
      canManageOrganizations
        ? organizationInvitationService.list(organizationId)
        : Promise.resolve([] as OrganizationInvitation[]),
    ])
      .then(([memberRows, invitationRows]) => {
        if (!active) return;
        setMembers(memberRows);
        setInvitations(invitationRows);
      })
      .catch((err: Error) => {
        if (active) {
          setError(`Organization membership could not be loaded: ${err.message}`);
        }
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
    };
  }, [canManageOrganizations, organizationId]);

  async function refreshMembers() {
    setMembers(await adminApi.organizationMembers(organizationId));
  }

  async function refreshInvitations() {
    if (!canManageOrganizations) return;
    setInvitations(await organizationInvitationService.list(organizationId));
  }

  async function handleAddMember(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const normalizedUserId = userId.trim();
    if (!normalizedUserId) {
      setError("User ID is required.");
      return;
    }

    setSubmitting(true);
    setError(undefined);
    try {
      await organizationAdminService.addMember(organizationId, {
        userId: normalizedUserId,
        role,
      });
      await refreshMembers();
      setUserId("");
      setRole("MEMBER");
      setShowAddMember(false);
    } catch (err) {
      setError(`Member could not be added: ${(err as Error).message}`);
    } finally {
      setSubmitting(false);
    }
  }

  async function handleInviteMember(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const normalizedEmail = inviteEmail.trim();
    if (!normalizedEmail) {
      setError("Invitation email is required.");
      return;
    }

    setSubmitting(true);
    setError(undefined);
    try {
      await organizationInvitationService.create(
        organizationId,
        normalizedEmail,
        inviteRole,
      );
      await refreshInvitations();
      setInviteEmail("");
      setInviteRole("MEMBER");
      setShowInviteMember(false);
    } catch (err) {
      setError(`Invitation could not be created: ${(err as Error).message}`);
    } finally {
      setSubmitting(false);
    }
  }

  async function handleRemoveMember(memberUserId: string) {
    if (!memberUserId) return;
    setError(undefined);
    try {
      await organizationAdminService.removeMember(organizationId, memberUserId);
      await refreshMembers();
    } catch (err) {
      setError(`Member could not be removed: ${(err as Error).message}`);
    }
  }

  async function confirmInvitationAction() {
    if (!invitationAction) return;
    setInvitationActionPending(true);
    setError(undefined);
    try {
      if (invitationAction.type === "resend") {
        await organizationInvitationService.resend(
          organizationId,
          invitationAction.invitation.id,
        );
      } else {
        await organizationInvitationService.revoke(
          organizationId,
          invitationAction.invitation.id,
        );
      }
      await refreshInvitations();
      setInvitationAction(null);
    } catch (err) {
      const label = invitationAction.type === "resend" ? "resent" : "revoked";
      setError(`Invitation could not be ${label}: ${(err as Error).message}`);
    } finally {
      setInvitationActionPending(false);
    }
  }

  return (
    <section className="page">
      <PageHeader
        eyebrow="Organization"
        title="Members & invitations"
        description="Manage existing memberships and invite people by email without exposing invitation credentials."
        actions={
          canManageOrganizations ? (
            <div className="action-row">
              <button
                type="button"
                className="button-secondary"
                onClick={() => {
                  setShowAddMember((current) => !current);
                  setShowInviteMember(false);
                }}
              >
                {showAddMember ? "Cancel" : "Add existing member"}
              </button>
              <button
                type="button"
                className="button-primary"
                onClick={() => {
                  setShowInviteMember((current) => !current);
                  setShowAddMember(false);
                }}
              >
                {showInviteMember ? "Cancel" : "Invite by email"}
              </button>
            </div>
          ) : undefined
        }
      />

      {showAddMember && canManageOrganizations ? (
        <section className="panel">
          <h2>Add existing member</h2>
          <form onSubmit={handleAddMember}>
            <label>
              User ID
              <input
                value={userId}
                placeholder="User UUID"
                onChange={(event) => setUserId(event.target.value)}
                disabled={submitting}
                required
              />
            </label>
            <label>
              Organization role
              <select
                value={role}
                onChange={(event) => setRole(event.target.value as "ADMIN" | "MEMBER")}
                disabled={submitting}
              >
                <option value="MEMBER">Member</option>
                <option value="ADMIN">Admin</option>
              </select>
            </label>
            <button type="submit" className="button-primary" disabled={submitting}>
              {submitting ? "Adding..." : "Add member"}
            </button>
          </form>
        </section>
      ) : null}

      {showInviteMember && canManageOrganizations ? (
        <section className="panel">
          <h2>Invite member</h2>
          <p>The invitation email contains a single-use credential. It is never shown in this portal.</p>
          <form onSubmit={handleInviteMember}>
            <label>
              Email
              <input
                type="email"
                autoComplete="email"
                value={inviteEmail}
                onChange={(event) => setInviteEmail(event.target.value)}
                disabled={submitting}
                required
              />
            </label>
            <label>
              Organization role
              <select
                value={inviteRole}
                onChange={(event) => setInviteRole(event.target.value as OrganizationInvitationRole)}
                disabled={submitting}
              >
                <option value="MEMBER">Member</option>
                <option value="ADMIN">Admin</option>
              </select>
            </label>
            <button type="submit" className="button-primary" disabled={submitting}>
              {submitting ? "Sending..." : "Send invitation"}
            </button>
          </form>
        </section>
      ) : null}

      {loading && <LoadingState label="Loading members and invitations" />}
      {error && <ErrorState message={error} />}

      <h2>Members</h2>
      <DataTable
        rows={members as Record<string, unknown>[]}
        fallback="No members returned from the admin API."
        columns={[
          { key: "id", label: "Membership ID" },
          { key: "userId", label: "User ID" },
          { key: "role", label: "Role" },
          { key: "joinedAt", label: "Joined at" },
          {
            key: "actions",
            label: "Actions",
            render: (row) => (
              <button
                type="button"
                className="button-secondary"
                disabled={!canManageOrganizations}
                onClick={() => void handleRemoveMember(String(row.userId ?? ""))}
              >
                Remove
              </button>
            ),
          },
        ]}
      />

      {canManageOrganizations ? (
        <>
          <h2>Invitations</h2>
          <DataTable
            rows={invitations as unknown as Record<string, unknown>[]}
            fallback="No organization invitations."
            columns={[
              { key: "email", label: "Email" },
              { key: "role", label: "Role" },
              {
                key: "status",
                label: "Status",
                render: (row) => <StatusBadge status={String(row.status ?? "")} />,
              },
              { key: "expiresAt", label: "Expires at" },
              { key: "lastSentAt", label: "Last sent" },
              {
                key: "actions",
                label: "Actions",
                render: (row) => {
                  const status = String(row.status ?? "");
                  const invitation = invitations.find((item) => item.id === String(row.id ?? ""));
                  if (!invitation) return null;
                  return (
                    <div className="action-row">
                      {status === "PENDING" || status === "EXPIRED" ? (
                        <button
                          type="button"
                          className="button-secondary"
                          onClick={() => setInvitationAction({ type: "resend", invitation })}
                        >
                          Resend
                        </button>
                      ) : null}
                      {status === "PENDING" ? (
                        <button
                          type="button"
                          className="button-secondary"
                          onClick={() => setInvitationAction({ type: "revoke", invitation })}
                        >
                          Revoke
                        </button>
                      ) : null}
                    </div>
                  );
                },
              },
            ]}
          />
        </>
      ) : null}

      <ConfirmActionModal
        open={Boolean(invitationAction)}
        title={invitationAction?.type === "revoke" ? "Revoke invitation?" : "Resend invitation?"}
        body={
          invitationAction?.type === "revoke"
            ? "The current invitation credential will no longer be accepted."
            : "A new single-use invitation credential will be issued and the previous one will no longer be valid."
        }
        confirmLabel={invitationAction?.type === "revoke" ? "Revoke" : "Resend"}
        pending={invitationActionPending}
        onCancel={() => setInvitationAction(null)}
        onConfirm={() => void confirmInvitationAction()}
      />
    </section>
  );
}
