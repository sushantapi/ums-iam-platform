import apiClient from "../apiClient";

export interface CreateOrganizationRequest {
  name: string;
  description?: string;
}

export interface AddOrganizationMemberRequest {
  userId: string;
  role: "ADMIN" | "MEMBER";
}

export interface CreatedOrganization {
  id: string;
  name: string;
  slug?: string;
  description?: string;
  ownerId?: string;
  status?: string;
}

class OrganizationAdminService {
  async create(
    request: CreateOrganizationRequest,
  ): Promise<CreatedOrganization> {
    const response = await apiClient.post<CreatedOrganization>(
      "/admin/organizations",
      request,
    );

    return response.data;
  }

  async addMember(
    organizationId: string,
    request: AddOrganizationMemberRequest,
  ): Promise<void> {
    await apiClient.post<void>(
      `/admin/organizations/${organizationId}/members`,
      request,
    );
  }

  async removeMember(
    organizationId: string,
    userId: string,
  ): Promise<void> {
    await apiClient.delete<void>(
      `/admin/organizations/${organizationId}/members/${userId}`,
    );
  }
}

export default new OrganizationAdminService();
