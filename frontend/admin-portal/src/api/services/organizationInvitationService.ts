import apiClient from "../apiClient";

export type OrganizationInvitationRole = "ADMIN" | "MEMBER";
export type OrganizationInvitationStatus =
  | "PENDING"
  | "ACCEPTED"
  | "REVOKED"
  | "EXPIRED";

export interface OrganizationInvitation {
  id: string;
  organizationId: string;
  email: string;
  role: OrganizationInvitationRole;
  status: OrganizationInvitationStatus;
  inviterId: string;
  expiresAt: string;
  lastSentAt?: string | null;
  createdAt: string;
}

export interface OrganizationInvitationAcceptance {
  invitationId: string;
  organizationId: string;
  membershipId: string;
  role: OrganizationInvitationRole;
  status: "ACCEPTED";
  acceptedAt: string;
}

class OrganizationInvitationService {
  async list(organizationId: string): Promise<OrganizationInvitation[]> {
    const response = await apiClient.get<OrganizationInvitation[]>(
      `/organizations/${organizationId}/invitations`,
    );
    return response.data;
  }

  async create(
    organizationId: string,
    email: string,
    role: OrganizationInvitationRole,
  ): Promise<OrganizationInvitation> {
    const response = await apiClient.post<OrganizationInvitation>(
      `/organizations/${organizationId}/invitations`,
      { email, role },
    );
    return response.data;
  }

  async resend(
    organizationId: string,
    invitationId: string,
  ): Promise<OrganizationInvitation> {
    const response = await apiClient.post<OrganizationInvitation>(
      `/organizations/${organizationId}/invitations/${invitationId}/resend`,
    );
    return response.data;
  }

  async revoke(
    organizationId: string,
    invitationId: string,
  ): Promise<OrganizationInvitation> {
    const response = await apiClient.post<OrganizationInvitation>(
      `/organizations/${organizationId}/invitations/${invitationId}/revoke`,
    );
    return response.data;
  }

  async accept(token: string): Promise<OrganizationInvitationAcceptance> {
    const response = await apiClient.post<OrganizationInvitationAcceptance>(
      "/organizations/invitations/accept",
      { token },
    );
    return response.data;
  }
}

export default new OrganizationInvitationService();
