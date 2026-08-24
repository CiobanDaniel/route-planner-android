# ADR 0002: Local-first sync and account groundwork

## Status

Accepted (2026-08-22)

## Context

The app is **local-first**: routes, stops, stop library, and per-stop tasks live in Room on
device. Menu placeholders and product docs reference future accounts and cloud sync, but no
auth provider is chosen yet (Entra, Firebase, custom).

We need a path to sync without rewriting Compose screens or breaking offline delivery use.

## Decision

1. **Stable `remoteId` (UUID)** on every syncable entity (`routes`, `stops`, `stop_library`,
   `stop_tasks`). Local `Long` primary keys stay for Room relations and delivery session.
2. **Optional `deletedAtEpochMs`** tombstone column on those tables. UI still hard-deletes
   today; sync workers will soft-delete later.
3. **`AccountSessionStore` (DataStore)** holds display metadata only (name, email, provider
   slug). Tokens belong in Credential Manager / encrypted storage when auth lands — not in
   DataStore.
4. **JSON backup/export** (`RouteBackupManager`) uses the same payload shape future sync will
   merge. Import merges by `remoteId` with **last-write-wins** on `updatedAtEpochMs` for
   routes and library entries.
5. **Sync API sits beside `RouteRepository`**, not inside composables. UI keeps calling
   `RouteRepository`; a future `RemoteRouteDataSource` pushes/pulls JSON or API DTOs.

## Consequences

### Positive

- Device-only users unaffected; no network permission beyond existing OSRM/Nominatim.
- Backup/restore works before any server exists.
- Import dedupe rehearsal reduces risk when cloud sync ships.

### Negative

- Schema migration (v3→v4) on every install with existing DB.
- Last-write-wins is naive for multi-user edit conflicts — revisit when sharing with permissions.
- Preview sign-in (debug) must not be mistaken for production auth.

## Not in scope (P4 follow-up)

- Auth SDK, refresh tokens, WorkManager sync loop, server API, share-with-edit ACLs.
- Task-template library sync (tasks remain per-stop copies for now).

## When to revisit

When picking an auth provider, or when field testing shows LWW loses courier edits on shared routes.
