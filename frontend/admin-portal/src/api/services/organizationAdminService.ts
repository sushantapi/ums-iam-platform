import apiClient from "../apiClient";

export interface CreateOrganizationRequest {
  name: string;
  description?: string;
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
}

export default new OrganizationAdminService();
