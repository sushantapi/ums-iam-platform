import apiClient from '../apiClient';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  user: {
    id: string;
    email: string;
    firstName: string;
    lastName: string;
    roles: string[];
  };
}

export interface RegisterRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
}

export interface ApiResponse<T> {
  data?: T;
  message: string;
  success: boolean;
}

class AuthService {
  async login(credentials: LoginRequest): Promise<LoginResponse> {
    try {
      const response = await apiClient.post<ApiResponse<LoginResponse>>(
        '/auth/login',
        credentials
      );
      return response.data.data as LoginResponse;
    } catch (error) {
      throw this.handleError(error);
    }
  }

  async register(data: RegisterRequest): Promise<LoginResponse> {
    try {
      const response = await apiClient.post<ApiResponse<LoginResponse>>(
        '/auth/register',
        data
      );
      return response.data.data as LoginResponse;
    } catch (error) {
      throw this.handleError(error);
    }
  }

  async logout(): Promise<void> {
    try {
      await apiClient.post('/auth/logout');
    } catch (error) {
      console.error('Logout error:', error);
    }
  }

  async refreshToken(): Promise<string> {
    try {
      const response = await apiClient.post<ApiResponse<{ accessToken: string }>>(
        '/auth/refresh'
      );
      return response.data.data?.accessToken || '';
    } catch (error) {
      throw this.handleError(error);
    }
  }

  private handleError(error: unknown): Error {
    if (error instanceof Error) {
      return error;
    }
    return new Error('An unexpected error occurred');
  }
}

export default new AuthService();
