# Standup Bot 🤖

A web-based standup tool that collects daily updates from team members, enriches them with Trello activity, and uses Claude to generate a structured team report.

---

## The Idea

Team members open a simple web UI, enter their daily standup, and hit submit. At any point, anyone can generate a report — Claude reads all the entries alongside recent Trello card movements and produces a clean, structured summary of what the team accomplished, what's in progress, and who's blocked.

---

## Core Features

- **Standup form** — name, project (from config), free-text daily summary
- **Team report** — generated on demand via Claude, structured by person
- **Trello integration** — surfaces ticket changes from the last 24h per project
- **No repeats** — entries stored in Redis with 24h TTL, auto-expire after the day

---

## Tech Stack

| Layer | Technology |
|---|---|
| Frontend | HTML / JS |
| Backend | Java (Javalin) |
| Cache | Redis (Docker) |
| AI | Anthropic Claude API |
| Integrations | Trello REST API |

---

## How It Works

1. Team members submit their standup via the web UI
2. Each entry is saved to Redis with a 24h TTL
3. Anyone clicks **Generate Report**
4. Backend fetches all entries + recent Trello activity for each project
5. Claude synthesizes everything into a structured team report
6. Report is displayed in the UI

---

## Project Config

Projects are predefined in a config file — no free-text project names.

```json
{
  "projects": [
    { "name": "Auth Service", "trelloBoardId": "abc123" },
    { "name": "Payment Gateway", "trelloBoardId": "def456" }
  ]
}
```

---

## API Endpoints (to be designed)

- `POST /update` — submit a standup entry
- `GET /report` — generate and return the full team report
- `GET /projects` — return the list of configured projects

---

## Team Split (3 people)

| Person | Area |
|---|---|
| #1 | Claude integration — prompt engineering, report generation |
| #2 | Backend — Redis, HTTP routes, config loading |
| #3 | Trello integration + Frontend |

---

## What We're NOT Building Today

- Authentication
- Multi-team support
- Real-time push / WebSockets
- Persistent storage beyond Redis

---

## Getting Started

```bash
# Start Redis
docker-compose up -d

# Run the server
./mvnw spring-boot:run

# Open the UI
open http://localhost:7000
```

> You'll need an `ANTHROPIC_API_KEY` and a `TRELLO_API_KEY` + `TRELLO_TOKEN` in your `.env`.
