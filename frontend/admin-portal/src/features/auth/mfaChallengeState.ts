import type { TokenResponse } from "../../api/services/authService";

export interface PendingMfaChallenge {
  token: string;
  email: string;
  expiresAt: number;
}

let pendingChallenge: PendingMfaChallenge | null = null;

export function beginMfaChallenge(response: TokenResponse): PendingMfaChallenge {
  const expiresIn = response.mfaChallengeExpiresIn ?? 0;
  const token = response.mfaChallengeToken?.trim() ?? "";

  if (!response.mfaRequired || !token || expiresIn <= 0) {
    throw new Error("MFA challenge response is incomplete. Please sign in again.");
  }

  pendingChallenge = {
    token,
    email: response.email,
    expiresAt: Date.now() + expiresIn * 1000,
  };

  return pendingChallenge;
}

export function getPendingMfaChallenge(): PendingMfaChallenge | null {
  if (!pendingChallenge) {
    return null;
  }

  if (pendingChallenge.expiresAt <= Date.now()) {
    pendingChallenge = null;
    return null;
  }

  return pendingChallenge;
}

export function clearPendingMfaChallenge(): void {
  pendingChallenge = null;
}
