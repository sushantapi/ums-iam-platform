// @vitest-environment jsdom

import "@testing-library/jest-dom/vitest";
import { cleanup, render, screen } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it } from "vitest";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { useAuthStore } from "../../store/authStore";
import { RequireAuth } from "./RequireAuth";
import { RequireCapability } from "./RequireCapability";

function encodeJwtPart(value: unknown): string {
  return window
    .btoa(JSON.stringify(value))
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=+$/g, "");
}

function accessToken(permissions: string[]): string {
  return `${encodeJwtPart({ alg: "none", typ: "JWT" })}.${encodeJwtPart({
    type: "ACCESS",
    roles: ["HR_MANAGER"],
    permissions,
  })}.signature`;
}

function renderAuthRoute() {
  render(
    <MemoryRouter initialEntries={["/hrms/employees"]}>
      <Routes>
        <Route path="/login" element={<div>Login screen</div>} />
        <Route
          path="/hrms/employees"
          element={
            <RequireAuth>
              <div>Employees screen</div>
            </RequireAuth>
          }
        />
      </Routes>
    </MemoryRouter>,
  );
}

function renderCapabilityRoute() {
  render(
    <MemoryRouter initialEntries={["/hrms/payroll"]}>
      <Routes>
        <Route path="/forbidden" element={<div>Forbidden screen</div>} />
        <Route
          path="/hrms/payroll"
          element={
            <RequireCapability capability="hrms.payroll.read">
              <div>Payroll screen</div>
            </RequireCapability>
          }
        />
      </Routes>
    </MemoryRouter>,
  );
}

describe("HRMS access guards", () => {
  beforeEach(() => {
    window.localStorage.clear();
    useAuthStore.getState().clearSession();
  });

  afterEach(() => {
    cleanup();
    useAuthStore.getState().clearSession();
  });

  it("redirects an unauthenticated HRMS route to login", async () => {
    renderAuthRoute();

    expect(await screen.findByText("Login screen")).toBeInTheDocument();
    expect(screen.queryByText("Employees screen")).not.toBeInTheDocument();
  });

  it("allows an authenticated HRMS route", () => {
    useAuthStore.getState().setAccessToken(accessToken(["EMPLOYEE_READ"]));

    renderAuthRoute();

    expect(screen.getByText("Employees screen")).toBeInTheDocument();
  });

  it("redirects when the required HRMS permission is missing", async () => {
    useAuthStore.getState().setAccessToken(accessToken(["EMPLOYEE_READ"]));

    renderCapabilityRoute();

    expect(await screen.findByText("Forbidden screen")).toBeInTheDocument();
    expect(screen.queryByText("Payroll screen")).not.toBeInTheDocument();
  });

  it("allows the canonical permission mapped to the HRMS capability", () => {
    useAuthStore.getState().setAccessToken(accessToken(["PAYROLL_READ"]));

    renderCapabilityRoute();

    expect(screen.getByText("Payroll screen")).toBeInTheDocument();
  });
});
