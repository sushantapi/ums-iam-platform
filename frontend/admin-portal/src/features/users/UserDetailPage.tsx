import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { ErrorState } from "../../components/ui/ErrorState";
import { LoadingState } from "../../components/ui/LoadingState";
import { PageHeader } from "../../components/ui/PageHeader";
import { StatusBadge } from "../../components/ui/StatusBadge";
import {
  adminApi,
  type AuditLogResponse,
  type UserDetailResponse,
  type UserOrganizationResponse,
  type UserRoleAssignmentResponse,
  type UserSessionResponse,
} from "../../lib/api";
import {
  getAdminRoles,
  hasAdminCapability,
} from "../../lib/auth/capabilities";

function formatDate(value?: string) {
  if (!value) return "Not reported";

  const date = new Date(value);

  return Number.isNaN(date.getTime())
    ? value
    : date.toLocaleString();
}

export function UserDetailPage() {
  const { userId = "" } = useParams();

  const isSuperAdmin = getAdminRoles().has("SUPER_ADMIN");
  const canReadAudit = hasAdminCapability("audit.read");

  const tabs = [
    "Profile",
    "Roles",
    "Organizations",
    ...(isSuperAdmin ? ["Sessions"] : []),
    ...(canReadAudit ? ["Audit"] : []),
  ];

  const [user, setUser] = useState<UserDetailResponse>();
  const [roles, setRoles] = useState<UserRoleAssignmentResponse[]>([]);
  const [organizations, setOrganizations] = useState<UserOrganizationResponse[]>([]);
  const [sessions, setSessions] = useState<UserSessionResponse[]>([]);
  const [auditEvents, setAuditEvents] = useState<AuditLogResponse[]>([]);
  const [activeTab, setActiveTab] = useState("Profile");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string>();

  useEffect(() => {
    setLoading(true);
    setError(undefined);

    const sessionsRequest = isSuperAdmin
      ? adminApi.userSessions(userId)
      : Promise.resolve<UserSessionResponse[]>([]);

    const auditRequest = canReadAudit
      ? adminApi.userAudit(userId, { page: 0, size: 25 })
      : Promise.resolve(undefined);

    Promise.allSettled([
      adminApi.userDetail(userId),
      adminApi.userRoles(userId),
      adminApi.userOrganizations(userId),
      sessionsRequest,
      auditRequest,
    ])
      .then(([userResult, rolesResult, orgsResult, sessionsResult, auditResult]) => {
        if (userResult.status === "fulfilled") {
          setUser(userResult.value);
        } else {
          setError(`User detail could not be loaded: ${String(userResult.reason)}`);
        }

        if (rolesResult.status === "fulfilled") {
          setRoles(rolesResult.value);
        }

        if (orgsResult.status === "fulfilled") {
          setOrganizations(orgsResult.value);
        }

        if (sessionsResult.status === "fulfilled") {
          setSessions(sessionsResult.value);
        }

        if (auditResult.status === "fulfilled" && auditResult.value) {
          setAuditEvents(auditResult.value.content);
        }
      })
      .finally(() => setLoading(false));
  }, [canReadAudit, isSuperAdmin, userId]);

  const displayName =
    [user?.firstName, user?.lastName].filter(Boolean).join(" ") ||
    user?.email ||
    userId;

  async function revokeSession(sessionId: string) {
    try {
      await adminApi.revokeUserSession(userId, sessionId);
      setSessions((items) =>
        items.filter((session) => session.id !== sessionId),
      );
    } catch (err) {
      setError(`Session revoke failed: ${(err as Error).message}`);
    }
  }

  async function revokeAllSessions() {
    try {
      await adminApi.revokeAllUserSessions(userId);
      setSessions([]);
    } catch (err) {
      setError(`Session revoke-all failed: ${(err as Error).message}`);
    }
  }

  return (
    <section className="page">
      <PageHeader
        eyebrow="User 360"
        title={displayName}
        description="Identity profile, role assignments, organization membership, sessions, and audit evidence."
        actions={
          <>
            <Link className="button-secondary" to="/users">
              Back to users
            </Link>

            {canReadAudit && (
              <Link
                className="button-primary"
                to={`/audit/logs?target=${userId}`}
              >
                Audit trail
              </Link>
            )}
          </>
        }
      />

      {loading && <LoadingState label="Loading user" />}
      {error && <ErrorState message={error} />}

      {user && (
        <>
          <div className="detail-summary">
            <div>
              <span>Email</span>
              <strong>{user.email ?? "-"}</strong>
            </div>

            <div>
              <span>Status</span>
              <StatusBadge status={user.status ?? "Unknown"} />
            </div>

            <div>
              <span>Active</span>
              <strong>
                {user.active === undefined
                  ? "Not reported"
                  : user.active
                    ? "Yes"
                    : "No"}
              </strong>
            </div>

            <div>
              <span>Locked</span>
              <strong>
                {user.locked === undefined
                  ? "Not reported"
                  : user.locked
                    ? "Yes"
                    : "No"}
              </strong>
            </div>
          </div>

          <div className="tabs">
            {tabs.map((tab) => (
              <button
                key={tab}
                className={activeTab === tab ? "tab tab-active" : "tab"}
                type="button"
                onClick={() => setActiveTab(tab)}
              >
                {tab}
              </button>
            ))}
          </div>

          <section className="panel">
            <h2>{activeTab}</h2>

            {activeTab === "Profile" && (
              <ul className="detail-list">
                <li>Subject ID: {user.id ?? userId}</li>
                <li>Name: {displayName}</li>
                <li>Email: {user.email ?? "Not reported"}</li>
                <li>Mobile: {user.mobile ?? "Not reported"}</li>
                <li>Status: {user.status ?? "Not reported"}</li>
                <li>
                  Active:{" "}
                  {user.active === undefined
                    ? "Not reported"
                    : user.active
                      ? "Yes"
                      : "No"}
                </li>
                <li>
                  Locked:{" "}
                  {user.locked === undefined
                    ? "Not reported"
                    : user.locked
                      ? "Yes"
                      : "No"}
                </li>
                <li>Locked until: {formatDate(user.lockedUntil)}</li>
                <li>Last login: {formatDate(user.lastLoginAt)}</li>
              </ul>
            )}

            {activeTab === "Roles" && (
              <ul className="detail-list">
                {roles.map((role) => (
                  <li key={role.assignmentId}>
                    {role.roleName} - {role.scopeType ?? "platform"}
                    {role.scopeId ? ` (${role.scopeId})` : ""} -{" "}
                    {role.active ? "active" : "inactive"}
                    {role.assignedAt
                      ? ` - assigned ${formatDate(role.assignedAt)}`
                      : ""}
                    {role.expiresAt
                      ? ` - expires ${formatDate(role.expiresAt)}`
                      : ""}
                  </li>
                ))}

                {roles.length === 0 && (
                  <li>No role assignments reported.</li>
                )}
              </ul>
            )}

            {activeTab === "Organizations" && (
              <ul className="detail-list">
                {organizations.map((org) => (
                  <li key={org.id}>
                    {org.name} - {org.slug ?? "no slug"} -{" "}
                    {org.status ?? "Not reported"}
                  </li>
                ))}

                {organizations.length === 0 && (
                  <li>No organization memberships reported.</li>
                )}
              </ul>
            )}

            {activeTab === "Sessions" && isSuperAdmin && (
              <>
                <div className="action-row panel-actions">
                  <button
                    className="button-secondary"
                    type="button"
                    onClick={() => void revokeAllSessions()}
                  >
                    Revoke all sessions
                  </button>
                </div>

                <ul className="detail-list">
                  {sessions.map((session) => (
                    <li key={session.id}>
                      {session.device ?? session.client ?? session.id} -{" "}
                      {session.ipAddress ?? "unknown IP"} -{" "}
                      {formatDate(session.lastSeenAt ?? session.issuedAt)} -{" "}
                      {session.status ?? "ACTIVE"}
                      <button
                        className="inline-action"
                        type="button"
                        onClick={() => void revokeSession(session.id)}
                      >
                        Revoke
                      </button>
                    </li>
                  ))}

                  {sessions.length === 0 && (
                    <li>No active sessions reported.</li>
                  )}
                </ul>
              </>
            )}

            {activeTab === "Audit" && canReadAudit && (
              <ul className="detail-list">
                {auditEvents.map((event) => (
                  <li key={event.auditId ?? event.id}>
                    {event.createdAt ?? event.timestamp ?? "unknown time"} -{" "}
                    {event.eventType ?? event.action ?? "event"} -{" "}
                    {event.outcome ?? "recorded"}
                  </li>
                ))}

                {auditEvents.length === 0 && (
                  <li>No user audit events returned.</li>
                )}
              </ul>
            )}
          </section>
        </>
      )}
    </section>
  );
}
