import apiClient from "../apiClient";

export interface CreateUserRequest {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
}

export interface CreatedUserAccount {
  userId: string;
  email: string;
  status: string;
  locked: boolean;
  lockedUntil?: string | null;
  lastLoginAt?: string | null;
}

class UserAdminService {
  async create(request: CreateUserRequest): Promise<CreatedUserAccount> {
    const response = await apiClient.post<CreatedUserAccount>(
      "/admin/users",
      request,
    );

    return response.data;
  }
}

export default new UserAdminService();
