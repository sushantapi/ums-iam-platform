import { create } from "zustand";
import { persist } from "zustand/middleware";

export interface User {
  id: string;
  email: string;
  firstName?: string;
  lastName?: string;
  roles?: string[];
  avatar?: string;
}

export interface AuthSession {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  userId: string;
  email: string;
}

export interface AuthState {
  user: User | null;
  accessToken: string | null;
  refreshToken: string | null;
  tokenType: string;
  expiresAt: number | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  error: string | null;

  setSession: (session: AuthSession) => void;
  setUser: (user: User) => void;
  setAccessToken: (token: string) => void;
  setRefreshToken: (token: string) => void;
  clearSession: () => void;
  logout: () => void;
  setLoading: (loading: boolean) => void;
  setError: (error: string | null) => void;
  clearError: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      user: null,
      accessToken: null,
      refreshToken: null,
      tokenType: "Bearer",
      expiresAt: null,
      isAuthenticated: false,
      isLoading: false,
      error: null,

      setSession: (session) =>
        set({
          user: {
            id: session.userId,
            email: session.email,
          },
          accessToken: session.accessToken,
          refreshToken: session.refreshToken,
          tokenType: session.tokenType || "Bearer",
          expiresAt:
            session.expiresIn > 0
              ? Date.now() + session.expiresIn * 1000
              : null,
          isAuthenticated: true,
          error: null,
        }),

      setUser: (user) =>
        set({
          user,
        }),

      setAccessToken: (token) =>
        set({
          accessToken: token,
          isAuthenticated: Boolean(token),
        }),

      setRefreshToken: (token) =>
        set({
          refreshToken: token,
        }),

      clearSession: () =>
        set({
          user: null,
          accessToken: null,
          refreshToken: null,
          tokenType: "Bearer",
          expiresAt: null,
          isAuthenticated: false,
          error: null,
        }),

      logout: () =>
        set({
          user: null,
          accessToken: null,
          refreshToken: null,
          tokenType: "Bearer",
          expiresAt: null,
          isAuthenticated: false,
          error: null,
        }),

      setLoading: (loading) => set({ isLoading: loading }),
      setError: (error) => set({ error }),
      clearError: () => set({ error: null }),
    }),
    {
      name: "ums-admin-auth",
      partialize: (state) => ({
        user: state.user,
        accessToken: state.accessToken,
        refreshToken: state.refreshToken,
        tokenType: state.tokenType,
        expiresAt: state.expiresAt,
        isAuthenticated: state.isAuthenticated,
      }),
    },
  ),
);