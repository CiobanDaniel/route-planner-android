# ADR 0003: Library as source of truth for stops

**Status:** Accepted  
**Date:** 2026-08-24

## Context

Stops were copy-on-add: a route stored a full snapshot, with an optional `libraryStopId` back-link. The library map showed no pins, and editing a library stop did not change routes.

## Decision

The stop library is the canonical place record (name, address, coordinates, notes). A route stop is a **reference** (`libraryStopId`) plus per-route state (order, completion, tasks).

- Creating a pinned stop (map, GPS, search, edit-route save) writes the library first, then the route row.
- Editing or deleting a stop used on more than one route asks the user: apply everywhere, only this route (fork a copy), or cancel.
- The library screen shows pins and which routes use each stop.

Unpinned draft stops may still have a null `libraryStopId` until they have coordinates.

## Consequences

Delivery progress and task completion stay per-route. A global place edit updates every referencing route stop’s place fields. Room v5 backfills library rows for existing unlinked pinned stops.
