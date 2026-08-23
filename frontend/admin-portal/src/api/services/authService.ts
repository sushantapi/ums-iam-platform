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

export interface ForgotPasswordRequest {
  email: string;
}

export interface ResetPasswordRequest {
  token: string;
  newPassword: string;
}

export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  userId: string;
  email: string;
  mfaRequired?: boolean;
  mfaChallengeToken?: string;
  mfaChallengeExpiresIn?: number;
  mfaEnrollmentRequired?: boolean;
  requiredOrganizationId?: string;
}

export interface MfaChallengeVerifyRequest {
  challengeToken: string;
  totpCode?: string;
  recoveryCode?: string;
}

export interface MfaStatusResponse {
  enabled: boolean;
  setupPending: boolean;
  setupExpiresAt?: string | null;
  recoveryCodesRemaining: number;
}

export interface MfaTotpSetupResponse {
  secret: string;
  provisioningUri: string;
  expiresAt: string;
}

export interface MfaRecoveryCodesResponse {
  recoveryCodes: string[];
}

export interface MfaSensitiveActionRequest {
  password: string;
  totpCode?: string;
  recoveryCode?: string;
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

  async verifyMfaChallenge(
    data: MfaChallengeVerifyRequest,
  ): Promise<TokenResponse> {
    const response = await apiClient.post<ApiResponse<TokenResponse>>(
      "/auth/mfa/challenge/verify",
      data,
    );

    return this.requireData(
      response.data,
      "MFA verification response did not contain session data",
    );
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

  async forgotPassword(data: ForgotPasswordRequest): Promise<string> {
    const response = await apiClient.post<ApiResponse<void>>(
      "/auth/forgot-password",
      data,
    );

    return this.requireSuccess(
      response.data,
      "Password reset request could not be completed.",
    );
  }

  async resetPassword(data: ResetPasswordRequest): Promise<string> {
    const response = await apiClient.post<ApiResponse<void>>(
      "/auth/reset-password",
      data,
    );

    return this.requireSuccess(
      response.data,
      "Password reset could not be completed.",
    );
  }

  async mfaStatus(): Promise<MfaStatusResponse> {
    const response = await apiClient.get<ApiResponse<MfaStatusResponse>>(
      "/auth/mfa/status",
    );

    return this.requireData(response.data, "MFA status could not be loaded");
  }

  async setupTotp(): Promise<MfaTotpSetupResponse> {
    const response = await apiClient.post<ApiResponse<MfaTotpSetupResponse>>(
      "/auth/mfa/totp/setup",
    );

    return this.requireData(response.data, "MFA setup could not be started");
  }

  async confirmTotp(code: string): Promise<MfaRecoveryCodesResponse> {
    const response = await apiClient.post<ApiResponse<MfaRecoveryCodesResponse>>(
      "/auth/mfa/totp/confirm",
      { code },
    );

    return this.requireData(response.data, "MFA setup could not be confirmed");
  }

  async rotateMfaRecoveryCodes(
    data: MfaSensitiveActionRequest,
  ): Promise<MfaRecoveryCodesResponse> {
    const response = await apiClient.post<ApiResponse<MfaRecoveryCodesResponse>>(
      "/auth/mfa/recovery-codes/rotate",
      data,
    );

    return this.requireData(
      response.data,
      "MFA recovery codes could not be rotated",
    );
  }

  async disableMfa(data: MfaSensitiveActionRequest): Promise<string> {
    const response = await apiClient.post<ApiResponse<void>>(
      "/auth/mfa/disable",
      data,
    );

    return this.requireSuccess(
      response.data,
      "MFA could not be disabled.",
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

  private requireSuccess<T>(response: ApiResponse<T>, fallbackMessage: string): string {
    if (!response.success) {
      throw new Error(response.message || fallbackMessage);
    }

    return response.message || fallbackMessage;
  }

  private requireData<T>(response: ApiResponse<T>, fallbackMessage: string): T {
    if (!response.success || !response.data) {
      throw new Error(response.message || fallbackMessage);
    }

    return response.data;
  }
}

export default new AuthService();
