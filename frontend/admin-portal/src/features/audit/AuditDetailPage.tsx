import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { DetailLayout } from "../../components/layout/DetailLayout";
import { EntitySummaryCard } from "../../components/ui/EntitySummaryCard";
import { ErrorState } from "../../components/ui/ErrorState";
import { LoadingState } from "../../components/ui/LoadingState";
import { StatusBadge } from "../../components/ui/StatusBadge";
import { adminApi, type AuditLogResponse } from "../../lib/api";

export function AuditDetailPage() {
  const { eventId = "" } = useParams();
  const [event, setEvent] = useState<AuditLogResponse>();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string>();

  useEffect(() => {
    adminApi
      .auditEvent(eventId)
      .then(setEvent)
      .catch((err: Error) => setError(`Audit event could not be loaded: ${err.message}`))
      .finally(() => setLoading(false));
  }, [eventId]);

  return (
    <DetailLayout
      eyebrow="Audit"
      title={event?.eventType ?? event?.action ?? eventId}
      description="Event metadata, actor, target, tenant, result, request context, and change summary."
      actions={<Link className="button-secondary" to="/audit">Back to audit</Link>}
    >
      {loading && <LoadingState label="Loading audit event" />}
      {error && <ErrorState message={error} />}
      {event && (
        <>
          <div className="detail-summary">
            <EntitySummaryCard label="Outcome" value={<StatusBadge status={event.outcome ?? event.status ?? "Recorded"} />} />
            <EntitySummaryCard label="Actor" value={event.actor ?? event.username ?? "-"} />
            <EntitySummaryCard label="Target" value={event.target ?? event.userEmail ?? "-"} />
            <EntitySummaryCard label="Organization" value={event.organization ?? event.module ?? "-"} />
          </div>
          <div className="blueprint-grid">
            <section className="panel">
              <h2>Request Context</h2>
              <ul className="detail-list">
                <li>IP: {event.ipAddress ?? "Not reported"}</li>
                <li>User agent: {event.userAgent ?? "Not reported"}</li>
                <li>Service: {event.serviceName ?? "Not reported"}</li>
                <li>Endpoint: {event.method ?? ""} {event.endpoint ?? "Not reported"}</li>
                <li>Correlation ID: {event.correlationId ?? "Not reported"}</li>
                <li>Trace ID: {event.traceId ?? "Not reported"}</li>
              </ul>
            </section>
            <section className="panel">
              <h2>Changed Fields</h2>
              <ul className="detail-list">
                {(event.changedFields ?? []).map((field) => (
                  <li key={field.field}>{field.field}: {field.before ?? "-"} to {field.after ?? "-"}</li>
                ))}
                {(event.changedFields ?? []).length === 0 && <li>No field-level change summary reported.</li>}
              </ul>
            </section>
          </div>
          <section className="panel stacked-panel">
            <h2>Details</h2>
            <p className="muted">{event.details ?? "No additional details reported."}</p>
          </section>
        </>
      )}
    </DetailLayout>
  );
}
