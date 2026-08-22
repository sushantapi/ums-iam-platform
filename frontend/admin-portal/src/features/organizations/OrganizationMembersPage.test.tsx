// @vitest-environment jsdom

import "@testing-library/jest-dom/vitest";
import { cleanup, fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

const mocks = vi.hoisted(() => ({
  members: vi.fn(),
  addMember: vi.fn(),
  removeMember: vi.fn(),
  list: vi.fn(),
  create: vi.fn(),
  resend: vi.fn(),
  revoke: vi.fn(),
}));

vi.mock("../../lib/api", () => ({
  adminApi: {
    organizationMembers: mocks.members,
  },
}));

vi.mock("../../lib/auth/capabilities", () => ({
  hasAdminCapability: () => true,
}));

vi.mock("../../api/services/organizationAdminService", () => ({
  default: {
    addMember: mocks.addMember,
    removeMember: mocks.removeMember,
  },
}));

vi.mock("../../api/services/organizationInvitationService", () => ({
  default: {
    list: mocks.list,
    create: mocks.create,
    resend: mocks.resend,
    revoke: mocks.revoke,
  },
}));

import { OrganizationMembersPage } from "./OrganizationMembersPage";

const invitation = {
  id: "invite-1",
  organizationId: "org-1",
  email: "invitee@example.test",
  role: "MEMBER" as const,
  status: "PENDING" as const,
  inviterId: "user-1",
  expiresAt: "2026-08-24T12:00:00",
  lastSentAt: null,
  createdAt: "2026-08-21T12:00:00",
};

function renderPage() {
  return render(
    <MemoryRouter initialEntries={["/organizations/org-1/members"]}>
      <Routes>
        <Route path="/organizations/:organizationId/members" element={<OrganizationMembersPage />} />
      </Routes>
    </MemoryRouter>,
  );
}

describe("OrganizationMembersPage invitations", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.members.mockResolvedValue([]);
    mocks.list.mockResolvedValue([]);
    mocks.create.mockResolvedValue(invitation);
    mocks.resend.mockResolvedValue(invitation);
    mocks.revoke.mockResolvedValue({ ...invitation, status: "REVOKED" });
  });

  afterEach(cleanup);

  it("invites a member by normalized email and safe organization role", async () => {
    mocks.list.mockResolvedValueOnce([]).mockResolvedValueOnce([invitation]);
    renderPage();

    await waitFor(() => expect(mocks.list).toHaveBeenCalledWith("org-1"));
    fireEvent.click(screen.getByRole("button", { name: "Invite by email" }));
    fireEvent.change(screen.getByLabelText("Email"), {
      target: { value: "  invitee@example.test  " },
    });
    fireEvent.click(screen.getByRole("button", { name: "Send invitation" }));

    await waitFor(() =>
      expect(mocks.create).toHaveBeenCalledWith(
        "org-1",
        "invitee@example.test",
        "MEMBER",
      ),
    );
    expect(await screen.findByText("invitee@example.test")).toBeInTheDocument();
  });

  it("requires confirmation before revoking a pending invitation", async () => {
    mocks.list.mockResolvedValue([invitation]);
    renderPage();

    await screen.findByText("invitee@example.test");
    fireEvent.click(screen.getByRole("button", { name: "Revoke" }));

    const dialog = screen.getByRole("dialog");
    expect(within(dialog).getByText("Revoke invitation?")).toBeInTheDocument();
    expect(mocks.revoke).not.toHaveBeenCalled();

    fireEvent.click(within(dialog).getByRole("button", { name: "Revoke" }));
    await waitFor(() => expect(mocks.revoke).toHaveBeenCalledWith("org-1", "invite-1"));
  });
});
