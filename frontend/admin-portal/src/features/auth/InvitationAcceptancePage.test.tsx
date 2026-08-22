// @vitest-environment jsdom

import "@testing-library/jest-dom/vitest";
import { cleanup, render, screen, waitFor } from "@testing-library/react";
import { StrictMode } from "react";
import { BrowserRouter } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

const mocks = vi.hoisted(() => ({
  accept: vi.fn(),
}));

vi.mock("../../api/services/organizationInvitationService", () => ({
  default: {
    accept: mocks.accept,
  },
}));

import { useAuthStore } from "../../store/authStore";
import {
  InvitationAcceptancePage,
  INVITATION_TOKEN_SESSION_KEY,
} from "./InvitationAcceptancePage";

const acceptance = {
  invitationId: "invite-1",
  organizationId: "org-1",
  membershipId: "member-1",
  role: "MEMBER" as const,
  status: "ACCEPTED" as const,
  acceptedAt: "2026-08-21T18:00:00",
};

function accessTokenWithPermissions(...permissions: string[]): string {
  const payload = btoa(
    JSON.stringify({
      type: "ACCESS",
      roles: [],
      permissions,
    }),
  )
    .replace(/=/g, "")
    .replace(/\+/g, "-")
    .replace(/\//g, "_");

  return `header.${payload}.signature`;
}

describe("InvitationAcceptancePage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    window.sessionStorage.clear();
    window.history.replaceState({}, "", "/");
    useAuthStore.setState({
      accessToken: null,
      isAuthenticated: false,
    });
    mocks.accept.mockResolvedValue(acceptance);
  });

  afterEach(() => {
    cleanup();
    window.sessionStorage.clear();
  });

  it("removes the fragment bearer token from the URL immediately and accepts it only once in StrictMode", async () => {
    useAuthStore.setState({ accessToken: "header.payload.signature", isAuthenticated: true });
    window.history.pushState({}, "", "/accept-invitation#token=opaque-secret-token");

    render(
      <StrictMode>
        <BrowserRouter>
          <InvitationAcceptancePage />
        </BrowserRouter>
      </StrictMode>,
    );

    expect(window.location.pathname).toBe("/accept-invitation");
    expect(window.location.search).toBe("");
    expect(window.location.hash).toBe("");
    expect(document.body).not.toHaveTextContent("opaque-secret-token");

    await waitFor(() => expect(mocks.accept).toHaveBeenCalledTimes(1));
    expect(mocks.accept).toHaveBeenCalledWith("opaque-secret-token");
    await screen.findByText(/Invitation accepted/i);
    expect(window.sessionStorage.getItem(INVITATION_TOKEN_SESSION_KEY)).toBeNull();
  });

  it("keeps the token only in same-tab session storage while handing an unauthenticated user to sign in", async () => {
    window.history.pushState({}, "", "/accept-invitation#token=handoff-secret-token");

    render(
      <BrowserRouter>
        <InvitationAcceptancePage />
      </BrowserRouter>,
    );

    expect(window.location.search).toBe("");
    expect(window.location.hash).toBe("");
    expect(document.body).not.toHaveTextContent("handoff-secret-token");
    await waitFor(() => expect(window.location.pathname).toBe("/login"));
    expect(window.sessionStorage.getItem(INVITATION_TOKEN_SESSION_KEY)).toBe("handoff-secret-token");
    expect(mocks.accept).not.toHaveBeenCalled();
  });

  it("scrubs legacy query bearer tokens without accepting them", () => {
    window.history.pushState({}, "", "/accept-invitation?token=query-secret-token");
    useAuthStore.setState({ accessToken: "header.payload.signature", isAuthenticated: true });

    render(
      <BrowserRouter>
        <InvitationAcceptancePage />
      </BrowserRouter>,
    );

    expect(window.location.pathname).toBe("/accept-invitation");
    expect(window.location.search).toBe("");
    expect(window.location.hash).toBe("");
    expect(document.body).not.toHaveTextContent("query-secret-token");
    expect(screen.getByRole("alert")).toHaveTextContent("This invitation could not be accepted");
    expect(window.sessionStorage.getItem(INVITATION_TOKEN_SESSION_KEY)).toBeNull();
    expect(mocks.accept).not.toHaveBeenCalled();
  });

  it("shows a generic error without calling the API when no invitation token exists", () => {
    window.history.pushState({}, "", "/accept-invitation");
    useAuthStore.setState({ accessToken: "header.payload.signature", isAuthenticated: true });

    render(
      <BrowserRouter>
        <InvitationAcceptancePage />
      </BrowserRouter>,
    );

    expect(screen.getByRole("alert")).toHaveTextContent("This invitation could not be accepted");
    expect(mocks.accept).not.toHaveBeenCalled();
  });

  it("never renders a failed bearer token in the error state", async () => {
    mocks.accept.mockRejectedValue(new Error("transport failed"));
    useAuthStore.setState({ accessToken: "header.payload.signature", isAuthenticated: true });
    window.history.pushState({}, "", "/accept-invitation#token=failed-secret-token");

    render(
      <BrowserRouter>
        <InvitationAcceptancePage />
      </BrowserRouter>,
    );

    await screen.findByRole("alert");
    expect(document.body).not.toHaveTextContent("failed-secret-token");
    expect(screen.getByRole("button", { name: "Try again" })).toBeInTheDocument();
  });

  it("does not send an organization member into platform-admin member management after acceptance", async () => {
    useAuthStore.setState({ accessToken: "header.payload.signature", isAuthenticated: true });
    window.history.pushState({}, "", "/accept-invitation#token=member-secret-token");

    render(
      <BrowserRouter>
        <InvitationAcceptancePage />
      </BrowserRouter>,
    );

    expect(await screen.findByText(/organization-scoped member membership is active/i)).toBeInTheDocument();
    expect(screen.getByText(/Admin Portal permissions are separate/i)).toBeInTheDocument();
    expect(screen.queryByRole("link", { name: "View organization members" })).not.toBeInTheDocument();
    expect(screen.queryByRole("link", { name: "Open Admin Portal" })).not.toBeInTheDocument();
  });

  it("offers the Admin Portal only when the signed-in user already has platform dashboard permission", async () => {
    useAuthStore.setState({
      accessToken: accessTokenWithPermissions("DASHBOARD_READ"),
      isAuthenticated: true,
    });
    window.history.pushState({}, "", "/accept-invitation#token=platform-admin-secret-token");

    render(
      <BrowserRouter>
        <InvitationAcceptancePage />
      </BrowserRouter>,
    );

    await screen.findByText(/Invitation accepted/i);
    const adminLink = screen.getByRole("link", { name: "Open Admin Portal" });
    expect(adminLink).toHaveAttribute("href", "/dashboard");
    expect(screen.queryByRole("link", { name: "View organization members" })).not.toBeInTheDocument();
  });
});
