# UMS IAM Admin Portal

Implementation-ready React blueprint for the UMS IAM control plane.

## Structure

- `src/app` - route registration and app composition.
- `src/components/layout` - admin shell, topbar, and sidebar.
- `src/components/ui` - small reusable UI primitives.
- `src/features/iam/screenBlueprints.ts` - single source of truth for all IAM screens, sidebar sections, screen status, actions, data sources, and widgets.
- `src/features/dashboard` - starter dashboard wired to `GET /api/v1/admin/dashboard`.
- `src/features/users` - starter user list wired to `GET /api/v1/admin/users`.
- `src/features/roles` - starter role assignment form wired to `POST /api/v1/admin/roles/assign`.
- `src/features/audit` - starter audit log table wired to `GET /api/v1/admin/audit/logs`.
- `src/lib/api.ts` - typed admin API facade with bearer token support from `localStorage.ums_admin_access_token`.
- `src/lib/mockApi.ts` - fixture adapter used by the same API facade when a feature's mock mode is enabled.
- `docs/admin-integration-checklist.md` - source of truth for backend/frontend integration status and order.
- `src/lib/auth` - route capability guards. Override capabilities with a JSON array in `localStorage.ums_admin_capabilities`.
- `src/lib/hooks` - shared URL-backed list state and mutation feedback.

## Route Map

The sidebar and placeholder routes are generated from `screenBlueprints`. Existing backend endpoints have custom starter pages; roadmap screens render a blueprint page that documents expected actions, data contracts, and widgets.

## Run

```bash
npm install
npm run build
npm run dev
```

Copy `.env.example` to `.env.local`. Set `VITE_USE_MOCKS=true` and use the `VITE_MOCK_*` flags to integrate one feature at a time. Set `VITE_USE_MOCKS=false` to disable all mocks.

The Vite dev server runs on port `5174` and proxies `/api` to `http://localhost:8080`.
