import { useEffect, useState } from "react";
import { ConfirmActionModal } from "../../components/feedback/ConfirmActionModal";
import { DataTable } from "../../components/ui/DataTable";
import { ErrorState } from "../../components/ui/ErrorState";
import { FilterBar } from "../../components/ui/FilterBar";
import { LoadingState } from "../../components/ui/LoadingState";
import { PageHeader } from "../../components/ui/PageHeader";
import { Pagination } from "../../components/ui/Pagination";
import { StatusBadge } from "../../components/ui/StatusBadge";
import { adminApi, type AdminSessionResponse, type PageResponse } from "../../lib/api";
import { hasAdminCapability } from "../../lib/auth/capabilities";
import { useAdminListState } from "../../lib/hooks/useAdminListState";
import { useMutationFeedback } from "../../lib/hooks/useMutationFeedback";

const pageSize = 20;
const defaultFilters = {
  userId: "",
  organizationId: "",
  status: "",
  from: "",
  to: "",
};

export function SessionsPage() {
  const canRevokeSessions = hasAdminCapability("sessions.revoke");
  const { page, filters, setFilter, setPage } = useAdminListState(defaultFilters, pageSize);
  const { userId, organizationId, status, from, to } = filters;
  const [sessions, setSessions] = useState<PageResponse<AdminSessionResponse>>({
    content: [],
    page: 0,
    size: pageSize,
    totalElements: 0,
    totalPages: 0,
  });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string>();
  const [pendingAction, setPendingAction] = useState<{ type: "session" | "user"; id: string }>();
  const mutation = useMutationFeedback();

  useEffect(() => {
    setLoading(true);
    setError(undefined);
    adminApi
      .sessions({ page, size: pageSize, userId, organizationId, status, from, to })
      .then(setSessions)
      .catch((err: Error) => setError(`Sessions could not be loaded: ${err.message}`))
      .finally(() => setLoading(false));
  }, [from, organizationId, page, status, to, userId]);

  async function confirmRevoke() {
    if (!pendingAction) return;
    try {
      const { type, id } = pendingAction;
      await mutation.run(
        () => type === "session" ? adminApi.revokeSession(id) : adminApi.revokeAllUserSessions(id),
        type === "session" ? "Session revoked." : "All sessions for the user were revoked.",
      );
      setSessions((current) => ({
        ...current,
        content: current.content.map((session) =>
          (type === "session" && session.id === id) || (type === "user" && session.userId === id)
            ? { ...session, status: "REVOKED" }
            : session,
        ),
      }));
      setPendingAction(undefined);
    } catch {
      // Mutation feedback renders the normalized error.
    }
  }

  return (
    <section className="page">
      <PageHeader
        eyebrow="Security Operations"
        title="Sessions"
        description="Investigate active, expired, and revoked sessions across users, tenants, devices, and clients."
        actions={<button className="button-secondary" disabled={!canRevokeSessions}>Revoke suspicious</button>}
      />
      <FilterBar>
        <label>
          User
          <input
            value={filters.userId}
            placeholder="User ID"
            onChange={(event) => setFilter("userId", event.target.value)}
          />
        </label>
        <label>
          Organization
          <input
            value={filters.organizationId}
            placeholder="Organization ID"
            onChange={(event) => setFilter("organizationId", event.target.value)}
          />
        </label>
        <label>
          Status
          <select value={filters.status} onChange={(event) => setFilter("status", event.target.value)}>
            <option value="">All</option>
            <option value="ACTIVE">Active</option>
            <option value="REVOKED">Revoked</option>
            <option value="EXPIRED">Expired</option>
          </select>
        </label>
        <label>
          From
          <input type="date" value={filters.from} onChange={(event) => setFilter("from", event.target.value)} />
        </label>
        <label>
          To
          <input type="date" value={filters.to} onChange={(event) => setFilter("to", event.target.value)} />
        </label>
      </FilterBar>
      {loading && <LoadingState label="Loading sessions" />}
      {error && <ErrorState message={error} />}
      {mutation.error && <ErrorState message={`Session action failed: ${mutation.error}`} />}
      {mutation.success && <div className="success-message" role="status">{mutation.success}</div>}
      <DataTable
        rows={sessions.content as Record<string, unknown>[]}
        fallback="No sessions returned from the admin API."
        columns={[
          { key: "userName", label: "User", render: (row) => String(row.userName ?? row.userId ?? "-") },
          { key: "organizationName", label: "Organization", render: (row) => String(row.organizationName ?? row.organizationId ?? "-") },
          { key: "device", label: "Device / client", render: (row) => String(row.device ?? row.client ?? "-") },
          { key: "issuedAt", label: "Issued" },
          { key: "lastSeenAt", label: "Last seen" },
          { key: "expiresAt", label: "Expires" },
          { key: "status", label: "Status", render: (row) => <StatusBadge status={String(row.status ?? "Unknown")} /> },
          {
            key: "actions",
            label: "Actions",
            render: (row) => (
              <select
                className="table-action"
                defaultValue=""
                disabled={!canRevokeSessions}
                onChange={(event) => {
                  const action = event.target.value;
                  event.target.value = "";
                  if (action === "revoke") setPendingAction({ type: "session", id: String(row.id) });
                  if (action === "revoke-user") setPendingAction({ type: "user", id: String(row.userId ?? "") });
                }}
              >
                <option value="">Choose action</option>
                <option value="revoke">Revoke session</option>
                <option value="revoke-user">Revoke all for user</option>
              </select>
            ),
          },
        ]}
      />
      <Pagination page={page} size={pageSize} totalElements={sessions.totalElements} onPageChange={setPage} />
      <ConfirmActionModal
        open={Boolean(pendingAction)}
        title={pendingAction?.type === "user" ? "Revoke all user sessions?" : "Revoke this session?"}
        body="This prevents refresh-token renewal. Existing access tokens remain valid until their configured expiry."
        confirmLabel="Revoke"
        pending={mutation.pending}
        onCancel={() => setPendingAction(undefined)}
        onConfirm={() => void confirmRevoke()}
      />
    </section>
  );
}
