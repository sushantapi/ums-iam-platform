import {
  redirectToForbidden,
  redirectToLogin,
  refreshAccessToken,
} from "../../lib/auth/sessionManager";
import { runtimeConfig } from "../../lib/runtimeConfig";
import { useAuthStore } from "../../store/authStore";

export type HrmsQueryValue = string | number | boolean | undefined;

export type HrmsDownloadResponse = {
  blob: Blob;
  filename?: string;
};

export function withHrmsQuery(
  path: string,
  query: Record<string, HrmsQueryValue>,
): string {
  const params = new URLSearchParams();

  Object.entries(query).forEach(([key, value]) => {
    if (value !== undefined && value !== "") {
      params.set(key, String(value));
    }
  });

  const queryString = params.toString();
  return queryString ? `${path}?${queryString}` : path;
}

async function readResponseBody(response: Response): Promise<unknown> {
  if (response.status === 204) {
    return undefined;
  }

  const text = await response.text();
  if (!text) {
    return undefined;
  }

  const contentType = response.headers.get("content-type") ?? "";
  if (
    contentType.includes("application/json") ||
    contentType.includes("+json")
  ) {
    try {
      return JSON.parse(text) as unknown;
    } catch {
      return text;
    }
  }

  return text;
}

function errorMessage(response: Response, body: unknown): string {
  if (body && typeof body === "object" && "message" in body) {
    const message = (body as { message?: unknown }).message;
    if (typeof message === "string" && message.trim()) {
      return message;
    }
  }

  if (typeof body === "string" && body.trim()) {
    return body;
  }

  return `Request failed: ${response.status} ${response.statusText}`;
}

function filenameFromDisposition(value: string | null): string | undefined {
  if (!value) {
    return undefined;
  }

  const encodedMatch = value.match(/filename\*=UTF-8''([^;]+)/i);
  if (encodedMatch?.[1]) {
    try {
      return decodeURIComponent(encodedMatch[1].trim());
    } catch {
      return undefined;
    }
  }

  const plainMatch = value.match(/filename="?([^";]+)"?/i);
  return plainMatch?.[1]?.trim();
}

async function executeAuthenticated(
  path: string,
  init: RequestInit | undefined,
  accept?: string,
): Promise<Response> {
  async function execute(accessToken: string | null) {
    return fetch(`${runtimeConfig.apiBaseUrl}${path}`, {
      ...init,
      headers: {
        ...(accept ? { Accept: accept } : {}),
        ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
        ...init?.headers,
      },
    });
  }

  let response = await execute(useAuthStore.getState().accessToken);

  if (response.status === 401) {
    const refreshedAccessToken = await refreshAccessToken();
    if (refreshedAccessToken) {
      response = await execute(refreshedAccessToken);
    }
  }

  if (response.status === 401) {
    redirectToLogin();
  }

  if (response.status === 403) {
    redirectToForbidden();
  }

  return response;
}

export async function hrmsGatewayRequest<T>(
  path: string,
  init?: RequestInit,
): Promise<T> {
  const response = await executeAuthenticated(path, {
    ...init,
    headers: {
      "Content-Type": "application/json",
      ...init?.headers,
    },
  });
  const body = await readResponseBody(response);

  if (!response.ok) {
    throw new Error(errorMessage(response, body));
  }

  return body as T;
}

export async function hrmsGatewayDownload(
  path: string,
): Promise<HrmsDownloadResponse> {
  const response = await executeAuthenticated(path, undefined, "application/pdf");

  if (!response.ok) {
    const body = await readResponseBody(response);
    throw new Error(errorMessage(response, body));
  }

  return {
    blob: await response.blob(),
    filename: filenameFromDisposition(response.headers.get("content-disposition")),
  };
}
