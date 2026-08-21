export const runtimeConfig = {
  apiBaseUrl: import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080",
  useMocks: import.meta.env.VITE_USE_MOCKS === "true",
  mockFeatures: {
    dashboard: import.meta.env.VITE_MOCK_DASHBOARD !== "false",
    users: import.meta.env.VITE_MOCK_USERS !== "false",
    sessions: import.meta.env.VITE_MOCK_SESSIONS !== "false",
    audit: import.meta.env.VITE_MOCK_AUDIT !== "false",
    roles: import.meta.env.VITE_MOCK_ROLES !== "false",
    permissions: import.meta.env.VITE_MOCK_PERMISSIONS !== "false",
    grants: import.meta.env.VITE_MOCK_GRANTS !== "false",
    organizations: import.meta.env.VITE_MOCK_ORGANIZATIONS !== "false",
    hrms: import.meta.env.VITE_MOCK_HRMS === "true",
  },
};

export type MockFeature = keyof typeof runtimeConfig.mockFeatures;

export function shouldUseMock(feature: MockFeature): boolean {
  return runtimeConfig.useMocks && runtimeConfig.mockFeatures[feature];
}
