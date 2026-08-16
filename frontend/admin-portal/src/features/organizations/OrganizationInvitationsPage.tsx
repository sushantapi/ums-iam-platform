import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { DataTable } from "../../components/ui/DataTable";
import { ErrorState } from "../../components/ui/ErrorState";
import { LoadingState } from "../../components/ui/LoadingState";
import { PageHeader } from "../../components/ui/PageHeader";
import { Pagination } from "../../components/ui/Pagination";
import { StatusBadge } from "../../components/ui/StatusBadge";
import { adminApi, type OrganizationInvitationResponse, type PageResponse } from "../../lib/api";

const pageSize = 20;

export function OrganizationInvitationsPage() {
  const { organizationId = "" } = useParams();
  const [page, setPage] = useState(0);
  const [invitations, setInvitations] = useState<PageResponse<OrganizationInvitationResponse>>({
    content: [],
    page: 0,
    size: pageSize,
    totalElements: 0,
    totalPages: 0,
  });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string>();

  useEffect(() => {
    setLoading(true);
    adminApi
      .organizationInvitations(organizationId, { page, size: pageSize })
      .then(setInvitations)
      .catch((err: Error) => setError(`Invitations could not be loaded: ${err.message}`))
      .finally(() => setLoading(false));
  }, [organizationId, page]);

  async function runInvitationAction(invitationId: string, action: string) {
    try {
      if (action === "resend") await adminApi.resendInvitation(organizationId, invitationId);
      if (action === "revoke") await adminApi.revokeInvitation(organizationId, invitationId);
    } catch (err) {
      setError(`Invitation action failed: ${(err as Error).message}`);
    }
  }

  return (
    <section className="page">
      <PageHeader
        eyebrow="Organization"
        title="Invitations"
        description="Review pending invites, resend invitations, and revoke stale access."
        actions={<button className="button-primary">Invite member</button>}
      />
      {loading && <LoadingState label="Loading invitations" />}
      {error && <ErrorState message={error} />}
      <DataTable
        rows={invitations.content as Record<string, unknown>[]}
        fallback="No invitations returned from the admin API."
        columns={[
          { key: "email", label: "Email" },
          { key: "orgRole", label: "Org role" },
          { key: "status", label: "Status", render: (row) => <StatusBadge status={String(row.status ?? "Pending")} /> },
          { key: "expiresAt", label: "Expires" },
          {
            key: "actions",
            label: "Actions",
            render: (row) => {
              const invitationId = String(row.id ?? "");
              return (
                <select
                  className="table-action"
                  defaultValue=""
                  onChange={(event) => {
                    const action = event.target.value;
                    event.target.value = "";
                    if (action) void runInvitationAction(invitationId, action);
                  }}
                >
                  <option value="">Choose action</option>
                  <option value="resend">Resend</option>
                  <option value="revoke">Revoke</option>
                </select>
              );
            },
          },
        ]}
      />
      <Pagination page={page} size={pageSize} totalElements={invitations.totalElements} onPageChange={setPage} />
    </section>
  );
}
