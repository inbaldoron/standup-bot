# Java × Claude × Trello Integration Plan

## Overview

A Java server (Javalin) that acts as middleware between a client, the Claude API, and the Trello REST API.
Claude uses **tool use** to decide when and what to fetch from Trello, with the Java server executing
those calls in a loop until Claude has enough information to produce a final answer.

---

## Architecture

```
Client
  │
  │  POST /chat  { "prompt": "..." }
  ▼
┌─────────────────────────────────────────┐
│           Javalin HTTP Server           │
│                                         │
│  ┌──────────────────────────────────┐   │
│  │         ClaudeService            │   │
│  │   (agentic loop)                 │   │
│  │                                  │   │
│  │  while (stop_reason == tool_use) │   │
│  │    → call TrelloClient           │   │
│  │    → feed result back to Claude  │   │
│  └──────────────────────────────────┘   │
│                                         │
│  ┌──────────────────────────────────┐   │
│  │         TrelloClient             │   │
│  │   (executes Trello REST calls)   │   │
│  └──────────────────────────────────┘   │
└─────────────────────────────────────────┘
  │                          │
  │  Anthropic API           │  Trello REST API
  ▼                          ▼
Claude (claude-sonnet-4-6)   api.trello.com
```

---

## The Agentic Loop (Key Concept)

A single `/chat` request may require **multiple round trips** between Java and the two APIs:

```
1.  Java  →  Claude   "Which card is most overdue?" + tool definitions
2.  Claude →  Java    tool_use: get_lists(boardId)
3.  Java  →  Trello   GET /boards/{id}/lists
4.  Trello →  Java    [ { "id": "abc", "name": "In Progress" }, ... ]
5.  Java  →  Claude   tool_result: [ ... list data ... ]
6.  Claude →  Java    tool_use: get_cards(listId: "abc")
7.  Java  →  Trello   GET /lists/{id}/cards
8.  Trello →  Java    [ { "name": "Fix login", "due": "2026-04-01" }, ... ]
9.  Java  →  Claude   tool_result: [ ... card data ... ]
10. Claude →  Java    end_turn: "The most overdue card is 'Fix login', due 2026-04-01."
11. Java  →  Client   { "answer": "The most overdue card is 'Fix login'..." }
```

Steps 2–9 repeat until `stop_reason == "end_turn"`.

---

## Project Structure

```
inbal-hackathon/
├── pom.xml                          # Maven dependencies
├── .env                             # API keys (never committed)
├── .gitignore
└── src/main/java/com/hackathon/
    ├── Main.java                    # Entry point, starts Javalin
    ├── ChatServer.java              # Javalin routes
    ├── ClaudeService.java           # Agentic loop logic
    ├── TrelloClient.java            # Trello REST API calls
    └── model/
        ├── ChatRequest.java         # { "prompt": "..." }
        └── ChatResponse.java        # { "answer": "..." }
```

---

## Dependencies (Maven)

| Library | Purpose |
|---|---|
| `io.javalin:javalin` | HTTP server |
| `com.anthropic:anthropic-java` | Claude API SDK |
| `com.squareup.okhttp3:okhttp` | Trello HTTP calls |
| `com.fasterxml.jackson.core:jackson-databind` | JSON serialization |
| `io.github.cdimascio:dotenv-java` | Load `.env` file |

---

## Trello Tools (exposed to Claude)

These are defined as tool schemas in the Claude API request. Java executes them when Claude asks.

| Tool Name | Description | Trello Endpoint |
|---|---|---|
| `get_board` | Get board name and metadata | `GET /boards/{boardId}` |
| `get_lists` | Get all lists on the board | `GET /boards/{boardId}/lists` |
| `get_cards_in_list` | Get all cards in a list | `GET /lists/{listId}/cards` |
| `get_card_details` | Get full details of a card | `GET /cards/{cardId}` |
| `get_members` | Get board members | `GET /boards/{boardId}/members` |

---

## Implementation Steps

### Phase 1 — Project Scaffold
- [ ] Create `pom.xml` with all dependencies
- [ ] Create `.env` with keys (Claude API key, Trello API key, Trello token, board ID)
- [ ] Create `.gitignore` (exclude `.env`, `target/`)
- [ ] Create `Main.java` — starts Javalin on port 7070

### Phase 2 — Trello Client
- [ ] Implement `TrelloClient.java` with one method per tool
- [ ] Each method makes an authenticated GET request to Trello REST API
- [ ] Returns raw JSON string (Claude will interpret it)

### Phase 3 — Claude Service (Agentic Loop)
- [ ] Implement `ClaudeService.java`
- [ ] Build tool definitions (JSON schema for each Trello tool)
- [ ] Send initial prompt + tools to Claude
- [ ] Loop: if `stop_reason == "tool_use"`, dispatch to `TrelloClient`, send `tool_result` back
- [ ] Exit loop when `stop_reason == "end_turn"`
- [ ] Return final text response

### Phase 4 — HTTP Server
- [ ] Implement `ChatServer.java` with `POST /chat`
- [ ] Parse `ChatRequest`, call `ClaudeService`, return `ChatResponse`
- [ ] Add basic error handling (Trello errors, Claude errors, timeouts)

### Phase 5 — Test
- [ ] Test with `curl` or Postman
- [ ] Try prompts that require 1, 2, and 3+ Trello calls
- [ ] Verify correct answers against the actual board

---

## Example Prompts to Test

```
"What lists are on the board?"
"How many cards are in each list?"
"Which card is most overdue?"
"Who is assigned to the most cards?"
"Summarize everything that's In Progress"
```

---

## Configuration (`.env`)

```
ANTHROPIC_API_KEY=sk-ant-api03-...
TRELLO_API_KEY=0172f3ea84ff70394bd9dd98241e308a
TRELLO_TOKEN=ATTA6490c906...
TRELLO_BOARD_ID=RB4cVxn7
PORT=7070
```

---

## Key Design Decisions

- **No MCP server needed** — tool use in the Claude API is sufficient for server-to-server integration
- **Stateless requests** — each `/chat` call starts a fresh conversation (no session state)
- **Raw JSON to Claude** — Trello responses are passed as-is; Claude handles interpretation
- **Loop cap** — limit iterations to ~10 to prevent runaway tool calls
