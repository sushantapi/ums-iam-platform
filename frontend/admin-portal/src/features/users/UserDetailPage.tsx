import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { ErrorState } from "../../components/ui/ErrorState";
import { LoadingState } from "../../components/ui/LoadingState";
import { PageHeader } from "../../components/ui/PageHeader";
import { StatusBadge } from "../../components/ui/StatusBadge";
import {
  adminApi,
  type AuditLogResponse,
  type RoleResponse,
  type UserDetailResponse,
  type UserOrganizationResponse,
  type UserSessionResponse,
} from "../../lib/api";

const tabs = ["Profile", "Roles & Permissions", "Organizations", "Sessions", "Audit"];

export function UserDetailPage() {
  const { userId = "" } = useParams();
  const [user, setUser] = useState<UserDetailResponse>();
  const [roles, setRoles] = useState<RoleResponse[]>([]);
  const [organizations, setOrganizations] = useState<UserOrganizationResponse[]>([]);
  const [sessions, setSessions] = useState<UserSessionResponse[]>([]);
  const [auditEvents, setAuditEvents] = useState<AuditLogResponse[]>([]);
  const [activeTab, setActiveTab] = useState(tabs[0]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string>();

  useEffect(() => {
    Promise.allSettled([
      adminApi.userDetail(userId),
      adminApi.userRoles(userId),
      adminApi.userOrganizations(userId),
      adminApi.userSessions(userId),
      adminApi.userAudit(userId, { page: 0, size: 25 }),
    ])
      .then(([userResult, rolesResult, orgsResult, sessionsResult, auditResult]) => {
        if (userResult.status === "fulfilled") setUser(userResult.value);
        if (rolesResult.status === "fulfilled") setRoles(rolesResult.value);
        if (orgsResult.status === "fulfilled") setOrganizations(orgsResult.value);
        if (sessionsResult.status === "fulfilled") setSessions(sessionsResult.value);
        if (auditResult.status === "fulfilled") setAuditEvents(auditResult.value.content);
        if (userResult.status === "rejected") {
          setError(`User detail could not be loaded: ${String(userResult.reason)}`);
        }
      })
      .finally(() => setLoading(false));
  }, [userId]);

  const displayName =
    user?.fullName || [user?.firstName, user?.lastName].filter(Boolean).join(" ") || user?.email || userId;

  async function revokeSession(sessionId: string) {
    try {
      await adminApi.revokeUserSession(userId, sessionId);
      setSessions((items) => items.filter((session) => session.id !== sessionId));
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
        description="Identity profile, tenant membership, entitlements, sessions, and user-filtered audit evidence."
        actions={
          <>
            <Link className="button-secondary" to="/users">
              Back to users
            </Link>
            <Link className="button-primary" to={`/audit?target=${userId}`}>
              Audit trail
            </Link>
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
              <span>Organizations</span>
              <strong>{organizations.length}</strong>
            </div>
            <div>
              <span>MFA</span>
              <strong>{user.mfaEnabled ? "Enabled" : "Not reported"}</strong>
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
                <li>Subject ID: {user.id ?? user.userId ?? userId}</li>
                <li>Name: {displayName}</li>
                <li>Email: {user.email ?? "Not reported"}</li>
                <li>Status: {user.status ?? "Not reported"}</li>
                <li>Created: {user.createdAt ?? "Not reported"}</li>
                <li>Last login: {user.lastLoginAt ?? "Not reported"}</li>
                <li>Verified: {user.verified ? "Yes" : "Not reported"}</li>
              </ul>
            )}
            {activeTab === "Roles & Permissions" && (
              <ul className="detail-list">
                {roles.map((role) => (
                  <li key={role.id ?? role.roleId ?? role.name ?? role.roleName}>
                    {role.name ?? role.roleName} - {role.scopeType ?? "platform"} -{" "}
                    {role.permissionCount ?? 0} permissions
                  </li>
                ))}
                {(user.permissions ?? []).map((permission) => (
                  <li key={permission}>{permission}</li>
                ))}
                {!(roles.length || user.permissions?.length) && <li>No entitlements reported.</li>}
              </ul>
            )}
            {activeTab === "Organizations" && (
              <ul className="detail-list">
                {organizations.map((org) => (
                  <li key={org.id}>
                    {org.name} - {org.role ?? "member"} - {org.joinedAt ?? org.status ?? "active"}
                  </li>
                ))}
                {organizations.length === 0 && <li>No organization memberships reported.</li>}
              </ul>
            )}
            {activeTab === "Sessions" && (
              <>
                <div className="action-row panel-actions">
                  <button className="button-secondary" type="button" onClick={() => void revokeAllSessions()}>
                    Revoke all sessions
                  </button>
                </div>
                <ul className="detail-list">
                  {sessions.map((session) => (
                    <li key={session.id}>
                      {session.device ?? session.id} - {session.ipAddress ?? "unknown IP"} -{" "}
                      {session.lastSeenAt ?? session.issuedAt ?? "unknown time"} -{" "}
                      {session.status ?? "ACTIVE"}
                      <button className="inline-action" type="button" onClick={() => void revokeSession(session.id)}>
                        Revoke
                      </button>
                    </li>
                  ))}
                  {sessions.length === 0 && <li>No active sessions reported.</li>}
                </ul>
              </>
            )}
            {activeTab === "Audit" && (
              <ul className="detail-list">
                {auditEvents.map((event) => (
                  <li key={event.auditId ?? event.id}>
                    {event.createdAt ?? event.timestamp ?? "unknown time"} -{" "}
                    {event.eventType ?? event.action ?? "event"} - {event.outcome ?? event.status ?? "recorded"}
                  </li>
                ))}
                {auditEvents.length === 0 && <li>No user audit events returned.</li>}
              </ul>
            )}
          </section>
        </>
      )}
    </section>
  );
}
