import { useAuthStore, type AuthSession } from "../../store/authStore";

const configuredBaseUrl = (
  import.meta.env.VITE_API_BASE_URL || "http://localhost:8080"
).replace(/\/+$/, "");

const API_BASE_URL = configuredBaseUrl.endsWith("/api/v1")
  ? configuredBaseUrl
  : `${configuredBaseUrl}/api/v1`;

type ApiResponse<T> = {
  data?: T;
  message: string;
  success: boolean;
};

let refreshInFlight: Promise<string | null> | null = null;

async function performRefresh(): Promise<string | null> {
  const refreshToken = useAuthStore.getState().refreshToken;

  if (!refreshToken) {
    return null;
  }

  try {
    const response = await fetch(`${API_BASE_URL}/auth/refresh`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ refreshToken }),
    });

    if (!response.ok) {
      return null;
    }

    const payload = (await response.json()) as ApiResponse<AuthSession>;

    if (
      !payload.success ||
      !payload.data?.accessToken ||
      !payload.data.refreshToken
    ) {
      return null;
    }

    useAuthStore.getState().setSession(payload.data);
    return payload.data.accessToken;
  } catch {
    return null;
  }
}

export async function refreshAccessToken(): Promise<string | null> {
  if (!refreshInFlight) {
    refreshInFlight = performRefresh().finally(() => {
      refreshInFlight = null;
    });
  }

  return refreshInFlight;
}

export function redirectToLogin() {
  useAuthStore.getState().clearSession();

  if (window.location.pathname !== "/login") {
    window.location.assign("/login");
  }
}