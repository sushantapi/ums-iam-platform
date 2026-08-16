import { FormEvent, useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import organizationAdminService from "../../api/services/organizationAdminService";
import { DataTable } from "../../components/ui/DataTable";
import { ErrorState } from "../../components/ui/ErrorState";
import { LoadingState } from "../../components/ui/LoadingState";
import { PageHeader } from "../../components/ui/PageHeader";
import { adminApi, type OrganizationMemberResponse } from "../../lib/api";
import { hasAdminCapability } from "../../lib/auth/capabilities";

export function OrganizationMembersPage() {
  const { organizationId = "" } = useParams();
  const canManageOrganizations = hasAdminCapability("organizations.manage");
  const [members, setMembers] = useState<OrganizationMemberResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string>();
  const [showAddMember, setShowAddMember] = useState(false);
  const [userId, setUserId] = useState("");
  const [role, setRole] = useState<"ADMIN" | "MEMBER">("MEMBER");
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    setLoading(true);
    setError(undefined);

    adminApi
      .organizationMembers(organizationId)
      .then(setMembers)
      .catch((err: Error) =>
        setError(`Members could not be loaded: ${err.message}`),
      )
      .finally(() => setLoading(false));
  }, [organizationId]);

  async function refreshMembers() {
    const refreshed = await adminApi.organizationMembers(organizationId);
    setMembers(refreshed);
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

  async function handleRemoveMember(memberUserId: string) {
    if (!memberUserId) {
      return;
    }

    setError(undefined);

    try {
      await organizationAdminService.removeMember(
        organizationId,
        memberUserId,
      );
      await refreshMembers();
    } catch (err) {
      setError(`Member could not be removed: ${(err as Error).message}`);
    }
  }

  return (
    <section className="page">
      <PageHeader
        eyebrow="Organization"
        title="Members"
        description="Add, review, and remove organization memberships through the UMS admin API."
        actions={
          canManageOrganizations ? (
            <button
              type="button"
              className="button-primary"
              onClick={() => setShowAddMember((current) => !current)}
            >
              {showAddMember ? "Cancel" : "Add member"}
            </button>
          ) : undefined
        }
      />

      {showAddMember && canManageOrganizations ? (
        <section className="panel">
          <h2>Add member</h2>
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
                onChange={(event) =>
                  setRole(event.target.value as "ADMIN" | "MEMBER")
                }
                disabled={submitting}
              >
                <option value="MEMBER">Member</option>
                <option value="ADMIN">Admin</option>
              </select>
            </label>

            <div>
              <button
                type="submit"
                className="button-primary"
                disabled={submitting}
              >
                {submitting ? "Adding..." : "Add member"}
              </button>
            </div>
          </form>
        </section>
      ) : null}

      {loading && <LoadingState label="Loading members" />}
      {error && <ErrorState message={error} />}

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
                onClick={() =>
                  void handleRemoveMember(String(row.userId ?? ""))
                }
              >
                Remove
              </button>
            ),
          },
        ]}
      />
    </section>
  );
}
