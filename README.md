# Standup Bot 🤖

A standup tool that collects daily updates from team members, enriches them with Trello activity, and uses Claude to generate a structured team report.

---

## The Idea

Team members submit their daily standup, and at any point anyone can generate a report — Claude reads all the entries alongside recent Trello card movements and produces a clean, structured summary of what the team accomplished, what's in progress, and who's blocked.

---

## Core Features

- **Standup submission** — name + free-text daily summary
- **Team report** — generated on demand via Claude, structured by person
- **Trello integration** — surfaces ticket changes from the last 24h
- **In-memory storage** — entries kept in a simple in-memory list for now

---

## Tech Stack

| Layer | Technology |
|---|---|
| Client | **TBD** — web UI (HTML/JS) or Slack integration |
| Backend | Java (Javalin) |
| Storage | In-memory list |
| AI | Anthropic Claude API |
| Integrations | Trello REST API |

---

## How It Works

1. Team members submit their standup (via web UI or Slack — TBD)
2. Each entry is saved to an in-memory list
3. Anyone triggers **Generate Report**
4. Backend fetches all entries + recent Trello activity
5. Claude synthesizes everything into a structured team report
6. Report is returned to the client

---

## API Endpoints (to be designed)

- `POST /update` — submit a standup entry
- `GET /report` — generate and return the full team report

---

## Team Split (3 people)

| Person | Area |
|---|---|
| #1 | Claude integration — prompt engineering, report generation |
| #2 | Backend — in-memory storage, HTTP routes |
| #3 | Trello integration + Client (web UI or Slack) |

---

## What We're NOT Building Today

- Authentication
- Project management / multi-project config
- Persistent storage (no Redis, no DB)
- Multi-team support
- Real-time push / WebSockets

---

## Open Questions

- **Client**: web UI vs. Slack integration — to be decided

---

## Getting Started

```bash
# Run the server
./mvnw compile exec:java
```

> You'll need an `ANTHROPIC_API_KEY` and a `TRELLO_API_KEY` + `TRELLO_TOKEN` in your `.env`.
