import type { DashboardResponse } from "../lib/api";

export const mockDashboard: DashboardResponse = {
  users: { total: 1200, active: 1100, locked: 8, suspended: 12 },
  organizations: { total: 45, active: 41, pendingInvitations: 6 },
  roles: { total: 28 },
  audit: { eventsLast24Hours: 560, failedLogins: 14 },
};
