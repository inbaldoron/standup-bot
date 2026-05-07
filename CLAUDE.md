# Standup Bot — Project Context

A standup tool that collects daily updates from team members, enriches them with Trello activity, and uses Claude to generate a structured team report.

## Architecture

- **Backend**: Java + Javalin
- **Storage**: in-memory list (no Redis, no DB — entries live for the process lifetime)
- **AI**: Anthropic Claude API for report synthesis
- **Integrations**: Trello REST API for recent card activity
- **Client**: **TBD** — either a web UI (HTML/JS) or a Slack integration. Don't assume one; keep the backend client-agnostic until decided.

## Endpoints (to be designed)

- `POST /update` — submit a standup entry (name + free-text summary)
- `GET /report` — generate and return the full team report

## Flow

1. Team member submits standup → backend appends to in-memory list
2. Someone triggers report → backend gathers entries + recent Trello activity
3. Claude synthesizes a structured report grouped by person
4. Report returned to client

## Explicit Non-Goals

Do **not** build any of these unless asked:

- Authentication / users / sessions
- Project management or multi-project configuration (a single shared scope for now)
- Persistent storage (Redis, DB, files)
- Multi-team support
- Real-time push / WebSockets

## Open Questions

- Client surface (web UI vs. Slack) — undecided. Flag any work that forces a choice.

## Team Split

Three contributors are working in parallel:
1. Claude integration — prompt engineering, report generation
2. Backend — in-memory storage, HTTP routes
3. Trello integration + client

Keep module boundaries clean so the three areas don't step on each other.

## Environment

Requires `ANTHROPIC_API_KEY`, `TRELLO_API_KEY`, `TRELLO_TOKEN` in `.env`.

Run: `./mvnw compile exec:java`
