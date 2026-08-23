import apiClient from "../apiClient";

export interface OrganizationSecurityPolicy {
  organizationId: string;
  requireMfa: boolean;
  updatedAt: string | null;
}

class OrganizationSecurityPolicyService {
  async get(
    organizationId: string,
  ): Promise<OrganizationSecurityPolicy> {
    const response = await apiClient.get<OrganizationSecurityPolicy>(
      `/organizations/${organizationId}/security-policy`,
    );

    return response.data;
  }

  async update(
    organizationId: string,
    requireMfa: boolean,
  ): Promise<OrganizationSecurityPolicy> {
    const response = await apiClient.put<OrganizationSecurityPolicy>(
      `/organizations/${organizationId}/security-policy`,
      { requireMfa },
    );

    return response.data;
  }
}

export default new OrganizationSecurityPolicyService();
