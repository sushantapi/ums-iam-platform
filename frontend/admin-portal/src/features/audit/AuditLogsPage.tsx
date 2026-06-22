import { useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { DataTable } from "../../components/ui/DataTable";
import { ErrorState } from "../../components/ui/ErrorState";
import { FilterBar } from "../../components/ui/FilterBar";
import { LoadingState } from "../../components/ui/LoadingState";
import { PageHeader } from "../../components/ui/PageHeader";
import { Pagination } from "../../components/ui/Pagination";
import { StatusBadge } from "../../components/ui/StatusBadge";
import { adminApi, type AuditLogResponse, type PageResponse } from "../../lib/api";

const pageSize = 50;

export function AuditLogsPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [page, setPage] = useState(0);
  const [actor, setActor] = useState("");
  const [target, setTarget] = useState(searchParams.get("target") ?? "");
  const [organizationId, setOrganizationId] = useState(searchParams.get("organizationId") ?? "");
  const [eventType, setEventType] = useState("");
  const [serviceName, setServiceName] = useState("");
  const [outcome, setOutcome] = useState("");
  const [from, setFrom] = useState("");
  const [to, setTo] = useState("");
  const [logs, setLogs] = useState<PageResponse<AuditLogResponse>>({
    content: [],
    totalElements: 0,
    totalPages: 0,
    page: 0,
    size: pageSize,
  });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string>();

  useEffect(() => {
    setLoading(true);
    adminApi
      .auditLogs({ page, size: pageSize, actor, target, organizationId, eventType, serviceName, outcome, from, to })
      .then(setLogs)
      .catch((err: Error) => setError(`Audit events could not be loaded: ${err.message}`))
      .finally(() => setLoading(false));
  }, [actor, eventType, from, organizationId, outcome, page, serviceName, target, to]);

  return (
    <section className="page">
      <PageHeader
        eyebrow="Evidence"
        title="Audit Logs"
        description="Searchable IAM evidence for actor, target, tenant, result, source service, and date range."
        actions={<button className="button-primary">Export</button>}
      />
      <FilterBar>
        <label>
          Actor
          <input value={actor} onChange={(event) => { setPage(0); setActor(event.target.value); }} />
        </label>
        <label>
          Target user
          <input value={target} onChange={(event) => { setPage(0); setTarget(event.target.value); }} />
        </label>
        <label>
          Organization
          <input value={organizationId} onChange={(event) => { setPage(0); setOrganizationId(event.target.value); }} />
        </label>
        <label>
          Event type
          <input value={eventType} onChange={(event) => { setPage(0); setEventType(event.target.value); }} />
        </label>
        <label>
          Result
          <select value={outcome} onChange={(event) => { setPage(0); setOutcome(event.target.value); }}>
            <option value="">All</option>
            <option value="SUCCESS">Success</option>
            <option value="FAILURE">Failure</option>
          </select>
        </label>
        <label>
          Service
          <input value={serviceName} onChange={(event) => { setPage(0); setServiceName(event.target.value); }} />
        </label>
        <label>
          From
          <input type="date" value={from} onChange={(event) => { setPage(0); setFrom(event.target.value); }} />
        </label>
        <label>
          To
          <input type="date" value={to} onChange={(event) => { setPage(0); setTo(event.target.value); }} />
        </label>
      </FilterBar>
      {loading && <LoadingState label="Loading audit events" />}
      {error && <ErrorState message={error} />}
      <DataTable
        rows={logs.content as Record<string, unknown>[]}
        fallback="No audit logs returned from the admin API."
        onRowClick={(row) => navigate(`/audit/${String(row.eventId ?? row.auditId ?? row.id)}`)}
        columns={[
          { key: "createdAt", label: "Timestamp", render: (row) => String(row.timestamp ?? row.createdAt ?? "-") },
          { key: "eventType", label: "Event", render: (row) => String(row.eventType ?? row.action ?? "-") },
          { key: "actor", label: "Actor", render: (row) => String(row.actor ?? row.username ?? "-") },
          { key: "target", label: "Target", render: (row) => String(row.target ?? row.targetUser ?? row.userEmail ?? "-") },
          { key: "organization", label: "Org", render: (row) => String(row.organization ?? row.module ?? "-") },
          {
            key: "status",
            label: "Result",
            render: (row) => <StatusBadge status={String(row.outcome ?? row.status ?? "Recorded")} />,
          },
          { key: "ipAddress", label: "IP / source", render: (row) => String(row.ipAddress ?? row.serviceName ?? "-") },
        ]}
      />
      <Pagination page={page} size={pageSize} totalElements={logs.totalElements} onPageChange={setPage} />
    </section>
  );
}
