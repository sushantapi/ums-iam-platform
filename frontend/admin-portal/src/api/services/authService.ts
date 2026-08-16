import apiClient from "../apiClient";

export interface LoginRequest {
  email: string;
  password: string;
  deviceInfo?: string;
  client?: string;
  organizationId?: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  externalId?: string;
  provider?: string;
}

export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  userId: string;
  email: string;
}

export interface ApiResponse<T> {
  data?: T;
  message: string;
  success: boolean;
}

class AuthService {
  async login(credentials: LoginRequest): Promise<TokenResponse> {
    const response = await apiClient.post<ApiResponse<TokenResponse>>(
      "/auth/login",
      credentials,
    );

    return this.requireData(response.data, "Login response did not contain token data");
  }

  async register(data: RegisterRequest): Promise<TokenResponse> {
    const response = await apiClient.post<ApiResponse<TokenResponse>>(
      "/auth/register",
      data,
    );

    return this.requireData(
      response.data,
      "Registration response did not contain token data",
    );
  }

  async logout(): Promise<void> {
    await apiClient.post<void>("/auth/logout");
  }

  async refreshToken(refreshToken: string): Promise<TokenResponse> {
    const response = await apiClient.post<ApiResponse<TokenResponse>>(
      "/auth/refresh",
      { refreshToken },
    );

    return this.requireData(
      response.data,
      "Refresh response did not contain token data",
    );
  }

  private requireData<T>(response: ApiResponse<T>, fallbackMessage: string): T {
    if (!response.success || !response.data) {
      throw new Error(response.message || fallbackMessage);
    }

    return response.data;
  }
}

export default new AuthService();