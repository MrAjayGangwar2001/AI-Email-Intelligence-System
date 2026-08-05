# 🎯 AI Email Intelligence System

Automatically categorizes incoming Gmail emails into **Recruiter Responses** and
**Important Bank Alerts** — filtering out application confirmations, job-portal
noise, loan/credit-card offers, and spam — then sends a Telegram notification
for anything that actually needs your attention.

**Status:** Backend + automation pipeline built. React dashboard is not built yet
(planned next).

---

## How it works

```
Gmail (new email)
      │  polled every 5 min
      ▼
n8n (Gmail Trigger → clean fields → HTTP POST)
      │
      ▼
Spring Boot backend  ── POST /api/emails/analyze
      │
      ├─ 1. Check processed_message_log (duplicate? skip)
      ├─ 2. Send to Ollama (local AI) with classification prompt
      ├─ 3. IGNORED → log message ID only, discard content
      ├─ 4. RECRUITER_RESPONSE / BANK_IMPORTANT → save full record
      └─ 5. shouldNotify=true → send Telegram message
                                       │
                                       ▼
                              Your Telegram chat
```

Only two categories are ever stored with content:
- **RECRUITER_RESPONSE** — rejection, interview scheduled, next round/assessment, offer
- **BANK_IMPORTANT** — statement generated, important account alerts

Everything else (application confirmations, job alerts, loan/credit-card offers,
promotions, spam) is classified as **IGNORED** and discarded — only the Gmail
message ID is logged, so the same email is never re-processed twice.

---

## Applying this update (if you already ran `docker compose up -d` before)

The new reminder feature needs one extra database column. Since Postgres
only runs `schema.sql` automatically on a **fresh** volume, existing setups
need one manual step:

```bash
docker exec -i eis-postgres psql -U postgres -d email_intelligence \
  < database/migrations/001_add_reminder_sent.sql
```

Then rebuild and restart the backend to pick up the new code:

```bash
docker compose up -d --build backend
```

(Fresh installs don't need this — `schema.sql` already includes the column.)

Also make sure your `.env` has a `CORS_ALLOWED_ORIGINS` line (see
`.env.example`) — without it, the dashboard will fail to load data with a
CORS error in the browser console.

### Configuring reminders & digest

Both are on by default. Tune them via environment variables in `.env` or
directly in `backend/src/main/resources/application.yml`:

| Setting | Default | Meaning |
|---|---|---|
| `app.reminders.enabled` | `true` | Turn deadline nudges on/off |
| `app.reminders.lead-minutes` | `30` | How long before a deadline to nudge |
| `app.reminders.check-interval-ms` | `300000` (5 min) | How often to check for upcoming deadlines |
| `app.digest.enabled` | `true` | Turn the daily summary on/off |
| `app.digest.cron` | `0 0 8 * * *` | When to send the digest (standard cron) |
| `app.digest.zone` | `Asia/Kolkata` | Timezone the cron expression is evaluated in |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000` | Comma-separated origins allowed to call the API from a browser |

---

## Prerequisites

- Docker + Docker Compose installed
- A Gmail account
- A Google Cloud project (free) for Gmail API access
- A Telegram account
- ~8GB free disk space (for the Ollama model) and a machine with at least
  8GB RAM (mistral:7b needs it to run at reasonable speed)

---

## Step 1 — Clone/copy the project and set up `.env`

```bash
cp .env.example .env
```

Leave `TELEGRAM_BOT_TOKEN` and `TELEGRAM_CHAT_ID` blank for now — you'll fill
those in Step 3.

---

## Step 2 — Google Cloud: enable Gmail API + OAuth credentials

n8n's Gmail Trigger node needs OAuth credentials to read your inbox.

1. Go to [console.cloud.google.com](https://console.cloud.google.com) → create a
   new project (any name, e.g. "email-intelligence").
2. **APIs & Services → Library** → search "Gmail API" → **Enable**.
3. **APIs & Services → OAuth consent screen**:
   - User type: **External** (unless you have a Google Workspace account)
   - Fill in app name, your email as support/developer contact
   - Scopes: you can skip adding scopes here — n8n will request them
   - Add yourself as a **test user** under "Test users" (important — otherwise
     login will be blocked while the app is unpublished)
4. **APIs & Services → Credentials → Create Credentials → OAuth client ID**:
   - Application type: **Web application**
   - Name: anything
   - Authorized redirect URIs: add
     `http://localhost:5678/rest/oauth2-credential/callback`
     (this is n8n's standard OAuth callback path — adjust the host/port if
     you're not running n8n on localhost:5678)
   - Save → copy the **Client ID** and **Client Secret**

You'll paste these into n8n itself in Step 5 (n8n stores and manages the
OAuth token refresh — nothing needs to go in `.env` for Gmail).

---

## Step 3 — Telegram bot setup

1. Open Telegram, search for **@BotFather**, start a chat.
2. Send `/newbot`, follow the prompts (choose a name and a username ending in `bot`).
3. BotFather gives you a **bot token** like `123456789:AAExxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx`
   → put this in `.env` as `TELEGRAM_BOT_TOKEN`.
4. Send your new bot **any message** first (bots can't message you until you've
   messaged them).
5. Find your **chat ID**:
   ```bash
   curl "https://api.telegram.org/bot<YOUR_BOT_TOKEN>/getUpdates"
   ```
   Look for `"chat":{"id": 123456789, ...}` in the response → that number is
   your `TELEGRAM_CHAT_ID`. Put it in `.env`.

---

## Step 4 — Start everything

```bash
docker compose up -d
```

This starts Postgres (schema auto-loaded), Ollama, a one-time model-pull job,
the Spring Boot backend, and n8n.

Check everything is healthy:
```bash
docker compose ps
docker compose logs -f ollama-pull   # wait for this to finish downloading the model
docker compose logs -f backend       # should show "Started EmailIntelligenceApplication"
```

The model pull (~4GB for mistral:7b) can take several minutes depending on
your connection — the backend will work, but AI analysis calls will fail
until the pull finishes.

---

## Step 5 — Import and configure the n8n workflow

1. Open `http://localhost:5678`, log in with the `N8N_USER` / `N8N_PASSWORD`
   from your `.env`.
2. **Workflows → Import from File** → select `workflows/gmail-analysis-workflow.json`.
3. Open the **Gmail Trigger** node → under Credentials, create new
   **Gmail OAuth2 API** credentials → paste the Client ID / Client Secret
   from Step 2 → click **Connect my account** → sign in with the Gmail
   account you want monitored → grant access.
4. Open the **Send to Backend for Analysis** node and confirm the URL resolves
   to `http://backend:8080/api/emails/analyze` (this works automatically
   inside docker-compose's network — no change needed unless you renamed
   the backend service).
5. Click **Save**, then toggle the workflow **Active**.

---

## Step 6 — Test it

Send yourself a test email that looks like a rejection or interview
invitation, then check:

```bash
# Watch backend logs for the incoming request
docker compose logs -f backend

# Check what got stored
docker exec -it eis-postgres psql -U postgres -d email_intelligence \
  -c "SELECT subject, category, priority, should_notify FROM important_emails ORDER BY created_at DESC LIMIT 5;"
```

If classified as important, you should get a Telegram message within a
minute or two of n8n's next poll.

You can also test the backend directly without waiting for a real email:

```bash
curl -X POST http://localhost:8080/api/emails/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "gmailMessageId": "test-001",
    "senderName": "Amazon Recruiting",
    "senderEmail": "recruiting@amazon.com",
    "subject": "Interview Scheduled - Software Engineer",
    "bodyText": "We are pleased to invite you to interview for the Software Engineer role. Please join the Google Meet call tomorrow at 10 AM.",
    "hasAttachments": false,
    "receivedAt": "2026-08-01T10:30:00Z"
  }'
```

---

## API reference (current)

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/emails/analyze` | Called by n8n for every new email |
| GET | `/api/dashboard/emails?category=&priority=&company=&search=&page=&size=` | Paginated/filterable email list |
| GET | `/api/dashboard/emails/{id}` | Single email detail |
| GET | `/api/dashboard/deadlines/upcoming?days=7` | Emails with a deadline in the next N days |
| GET | `/api/dashboard/actions/pending` | Pending action items |
| PATCH | `/api/dashboard/actions/{id}/complete` | Mark an action item done |

---

## Troubleshooting

**Dashboard shows "Could not reach the backend" / CORS error in browser console**
The backend only allows browser requests from origins listed in
`CORS_ALLOWED_ORIGINS` (default `http://localhost:3000`). If you're opening
the dashboard from a different host/port (e.g. a different machine's IP, or
a different port), add it to `.env`:
```
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://192.168.1.10:3000
```
then `docker compose up -d --build backend`.

**Google OAuth: "Access blocked... has not completed the Google verification process"**
Your OAuth app is in Testing mode. Go to Google Cloud Console → APIs &
Services → OAuth consent screen → Test users → add the exact Gmail address
you're logging in with, then reconnect in n8n. Test-mode refresh tokens also
expire after 7 days, so you'll need to reconnect periodically until the app
is published.

**Gmail Trigger: "Forbidden - perhaps check your credentials?"**
Almost always means the Gmail API isn't enabled yet. Go to Google Cloud
Console → APIs & Services → Library → search "Gmail API" → Enable. If it's
already enabled, delete and recreate the credential in n8n (sometimes the
first OAuth grant doesn't request every scope correctly).

**Ollama connection refused from backend**
Backend uses `http://ollama:11434` inside docker's network — this only works
when both are in the same `docker compose up`. If running the backend outside
Docker, set `OLLAMA_BASE_URL=http://localhost:11434` instead.

**Gmail Trigger not firing**
Check the OAuth credential in n8n is still valid (tokens can expire if the
Google Cloud OAuth consent screen is still in "Testing" mode — tokens for
test users expire after 7 days; you'll need to reconnect).

**Telegram messages not arriving**
Confirm you messaged your bot at least once (Telegram requires this before a
bot can message you), and double check `TELEGRAM_CHAT_ID` is a plain number,
not your username.

**Emails not being classified correctly**
Check `docker compose logs backend` for the `reason` field the AI returned —
it's stored in `ai_raw_response` in the database and explains its own
classification decision, useful for tuning the prompt in `OllamaService.java`.

**Dashboard: "operator does not exist: email_category = character varying"**
Fixed in the current version — Postgres native ENUM columns need an explicit
type hint (`@JdbcTypeCode(SqlTypes.NAMED_ENUM)`) so Hibernate compares them
correctly instead of sending plain strings. If you're on an older copy,
re-download `ImportantEmail.java`, `ProcessedMessageLog.java`, and
`EmailNotification.java`.

**Dashboard: "function lower(bytea) does not exist" or "could not determine data type of parameter $1"**
Also fixed in the current version. The original `/api/dashboard/emails`
query used a `(:param IS NULL OR column = :param)` pattern for optional
filters, which Postgres's parameter-type inference handles inconsistently.
This was rewritten using **Spring Data Specifications**
(`repository/spec/ImportantEmailSpecifications.java`) so the query is built
dynamically and only includes active filters, avoiding the issue entirely.
Make sure you have that new file plus the updated
`ImportantEmailRepository.java` and `DashboardController.java`.

**YAML parse error on backend startup (e.g. "mapping values are not allowed here")**
Usually means a line got merged with the one below it during copy-paste (a
missing newline) while editing `application.yml`. Don't hand-edit this file
— replace it wholesale with a freshly downloaded copy; some editors
auto-merge lines when pasting into an already-open file.

**Dashboard container not running / `localhost:3000` unreachable**
Run `docker compose ps` and check the `frontend` service specifically. If
it's missing or exited, check `docker compose logs frontend` and try
`docker compose up -d frontend` on its own.

---

## Project structure

```
email-intelligence-system/
├── backend/                  Spring Boot app (Java 21)
│   └── src/main/java/com/emailintelligence/
│       ├── controller/       REST endpoints (ingest + dashboard APIs)
│       ├── service/          OllamaService, EmailAnalysisService, TelegramNotificationService,
│       │                     ReminderSchedulerService, DailyDigestService
│       ├── repository/       Spring Data JPA repositories
│       │   └── spec/         Dynamic query building (ImportantEmailSpecifications)
│       ├── entity/           JPA entities matching the DB schema
│       ├── dto/               Request/response payloads
│       ├── enums/             Category/priority/status enums
│       └── config/            WebClient, CORS, and app properties (AppProperties, CorsConfig)
├── database/
│   ├── schema.sql            Full schema (auto-loaded on a fresh docker volume)
│   └── migrations/           Manual migration scripts for already-running databases
├── frontend/                 React dashboard (Vite)
├── workflows/
│   └── gmail-analysis-workflow.json   n8n workflow, ready to import
├── docker-compose.yml
├── .env.example
└── README.md
```

---

## What's built

- ✅ Gmail → n8n → Spring Boot → Ollama classification pipeline
- ✅ Telegram notifications for important emails
- ✅ React dashboard (search/filter over `/api/dashboard/*`, deadline tracker, action checklist)
- ✅ Daily digest — one Telegram summary per day (default 8:00 AM) covering
  new important emails, deadlines in the next 24h, and open action items
- ✅ Deadline reminders — a Telegram nudge sent automatically 30 minutes
  (configurable) before any stored deadline

