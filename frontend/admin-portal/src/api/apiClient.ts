import axios, {
  AxiosError,
  AxiosInstance,
  type AxiosRequestConfig,
  type InternalAxiosRequestConfig,
} from "axios";

import {
  redirectToForbidden,
  redirectToLogin,
  refreshAccessToken,
} from "../lib/auth/sessionManager";
import { useAuthStore } from "../store/authStore";

const configuredBaseUrl = (
  import.meta.env.VITE_API_BASE_URL || "http://localhost:8080"
).replace(/\/+$/, "");

const API_BASE_URL = configuredBaseUrl.endsWith("/api/v1")
  ? configuredBaseUrl
  : `${configuredBaseUrl}/api/v1`;

export interface ApiErrorResponse {
  errorCode: string;
  message: string;
  status: number;
  fieldErrors?: Array<{
    field: string;
    message: string;
    rejectedValue?: unknown;
  }>;
}

type RetryableRequestConfig = InternalAxiosRequestConfig & {
  _retry?: boolean;
};

class ApiClient {
  private client: AxiosInstance;

  constructor() {
    this.client = axios.create({
      baseURL: API_BASE_URL,
      headers: {
        "Content-Type": "application/json",
      },
    });

    this.setupInterceptors();
  }

  private setupInterceptors() {
    this.client.interceptors.request.use((config) => {
      const accessToken = useAuthStore.getState().accessToken;

      if (accessToken) {
        config.headers.Authorization = `Bearer ${accessToken}`;
      }

      return config;
    });

    this.client.interceptors.response.use(
      (response) => response,
      async (error: AxiosError<ApiErrorResponse>) => {
        const request = error.config as RetryableRequestConfig | undefined;
        const url = request?.url ?? "";

        const isAuthEndpoint = url.includes("/auth/");

        if (
          error.response?.status === 401 &&
          request &&
          !request._retry &&
          !isAuthEndpoint
        ) {
          request._retry = true;

          const accessToken = await refreshAccessToken();

          if (accessToken) {
            request.headers.Authorization = `Bearer ${accessToken}`;
            return this.client.request(request);
          }

          redirectToLogin();
        }

        if (
          error.response?.status === 401 &&
          !isAuthEndpoint &&
          request?._retry
        ) {
          redirectToLogin();
        }

        if (
          error.response?.status === 403 &&
          !isAuthEndpoint
        ) {
          redirectToForbidden();
        }

        return Promise.reject(error);
      },
    );
  }

  get<T>(url: string, config?: AxiosRequestConfig) {
    return this.client.get<T>(url, config);
  }

  post<T>(url: string, data?: unknown, config?: AxiosRequestConfig) {
    return this.client.post<T>(url, data, config);
  }

  put<T>(url: string, data?: unknown, config?: AxiosRequestConfig) {
    return this.client.put<T>(url, data, config);
  }

  patch<T>(url: string, data?: unknown, config?: AxiosRequestConfig) {
    return this.client.patch<T>(url, data, config);
  }

  delete<T>(url: string, config?: AxiosRequestConfig) {
    return this.client.delete<T>(url, config);
  }
}

export default new ApiClient();