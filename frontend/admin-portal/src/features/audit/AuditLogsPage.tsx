import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
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
  const [searchParams] = useSearchParams();
  const [page, setPage] = useState(0);
  const [actor, setActor] = useState("");
  const [target, setTarget] = useState(searchParams.get("target") ?? "");
  const [eventType, setEventType] = useState("");
  const [serviceName, setServiceName] = useState("");
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
    setError(undefined);

    adminApi
      .auditLogs({
        page,
        size: pageSize,
        actor,
        target,
        eventType,
        serviceName,
      })
      .then(setLogs)
      .catch((err: Error) =>
        setError(`Audit logs could not be loaded: ${err.message}`),
      )
      .finally(() => setLoading(false));
  }, [actor, eventType, page, serviceName, target]);

  return (
    <section className="page">
      <PageHeader
        eyebrow="Evidence"
        title="Audit Logs"
        description="Searchable IAM evidence by actor, target, event type, and source service."
      />

      <FilterBar>
        <label>
          Actor
          <input
            value={actor}
            onChange={(event) => {
              setPage(0);
              setActor(event.target.value);
            }}
          />
        </label>

        <label>
          Target
          <input
            value={target}
            onChange={(event) => {
              setPage(0);
              setTarget(event.target.value);
            }}
          />
        </label>

        <label>
          Event type
          <input
            value={eventType}
            onChange={(event) => {
              setPage(0);
              setEventType(event.target.value);
            }}
          />
        </label>

        <label>
          Service
          <input
            value={serviceName}
            onChange={(event) => {
              setPage(0);
              setServiceName(event.target.value);
            }}
          />
        </label>
      </FilterBar>

      {loading && <LoadingState label="Loading audit logs" />}
      {error && <ErrorState message={error} />}

      <DataTable
        rows={logs.content as Record<string, unknown>[]}
        fallback="No audit logs returned from the admin API."
        columns={[
          {
            key: "timestamp",
            label: "Timestamp",
            render: (row) => String(row.timestamp ?? row.createdAt ?? "-"),
          },
          {
            key: "eventType",
            label: "Event",
            render: (row) => String(row.eventType ?? "-"),
          },
          {
            key: "action",
            label: "Action",
            render: (row) => String(row.action ?? "-"),
          },
          {
            key: "actor",
            label: "Actor",
            render: (row) => String(row.actor ?? row.username ?? "-"),
          },
          {
            key: "target",
            label: "Target",
            render: (row) => String(row.target ?? row.userEmail ?? "-"),
          },
          {
            key: "outcome",
            label: "Outcome",
            render: (row) => (
              <StatusBadge status={String(row.outcome ?? "Recorded")} />
            ),
          },
          {
            key: "serviceName",
            label: "Service",
          },
          {
            key: "ipAddress",
            label: "IP",
          },
        ]}
      />

      <Pagination
        page={page}
        size={pageSize}
        totalElements={logs.totalElements}
        onPageChange={setPage}
      />
    </section>
  );
}
