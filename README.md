# 🧠 Mail Mind — AI-Powered Email Intelligence System

A self-hosted, AI-powered inbox triage system built to survive the chaos of
active job-hunting: dozens of emails a day, and only a handful that actually
matter. Mail Mind reads every new email, classifies it with an LLM, stores
only what's genuinely important, and pings you on Telegram the moment
something needs your attention — while everything else is silently discarded.

Built as a full end-to-end personal project: automation pipeline, AI
classification, a Spring Boot backend, a Postgres-backed data model, and an
installable PWA dashboard with a dark "control-room" aesthetic.

---

## ✨ What it does

- **Watches your Gmail inbox** via n8n, polling for new mail automatically.
- **Classifies every email with an LLM** (Groq's Llama 3.1, cloud-hosted —
  fast, free-tier, and reliable even on modest hardware) into 6 categories:
  - 🎯 **Recruiter response** — interview invites, rejections, next rounds, offers
  - 🏦 **Bank important** — statements, security alerts
  - 👤 **Personal** — genuine messages from people you know
  - 🏢 **Business** — client/vendor/work-official communication
  - 📦 **Delivery** — time-sensitive package/order updates
  - ⚠️ **Other important** — a safety net for anything else that matters
  - *(everything else — job-portal noise, promotions, loan offers, spam — is
    silently discarded, keeping only a dedup log)*
- **Notifies you instantly on Telegram** when something important lands,
  plus a daily digest and deadline reminders.
- **Dashboard with a "New" / "Inbox" split**: unread emails surface on the
  main view; once opened they're marked read and move to a searchable Inbox
  — so your triage view never gets cluttered, but nothing is ever lost.
- **Delete anything you don't need** — right from the email detail view.
- **Installable as a PWA** — add it to your home screen / desktop and open
  it like a native app, no browser chrome, works offline for the last-seen
  data.

---

## 🏗️ Architecture

```
Gmail (new email)
      │  polled by n8n
      ▼
n8n  ──HTTP POST──▶  Spring Boot backend (Java 21)
                            │
                            ├─ Groq (LLM) → classify
                            ├─ IGNORED?  → log message ID only, discard
                            ├─ Important? → save full record (Postgres)
                            └─ shouldNotify → Telegram bot
                                                    │
                                                    ▼
                                            Your Telegram chat

React (Vite) PWA dashboard ──▶ REST API ──▶ Postgres
```

---

## 🛠️ Tech stack

| Layer | Tech |
|---|---|
| Automation | [n8n](https://n8n.io) (Gmail Trigger → HTTP → backend) |
| Backend | Java 21, Spring Boot 3, Spring Data JPA (Specifications) |
| AI classification | Groq API (Llama 3.1 8B, OpenAI-compatible) |
| Database | PostgreSQL 16 (native enums, JSONB for raw AI responses) |
| Notifications | Telegram Bot API |
| Frontend | React + Vite, Tailwind CSS, installable PWA (Workbox) |
| Infra | Docker Compose (Postgres, backend, n8n, frontend) |

---

## 🚀 Getting started

```bash
cp .env.example .env
# fill in GROQ_API_KEY, TELEGRAM_BOT_TOKEN, TELEGRAM_CHAT_ID
docker compose up -d
```

Import the n8n workflow (`workflows/gmail-analysis-workflow.json`), connect
your Gmail account via OAuth, publish the workflow, and you're live.

Full setup walkthrough (Google Cloud OAuth setup, Telegram bot creation,
environment variables, troubleshooting) is in [`SETUP.md`](./SETUP.md).

---

## 📌 Project status

Actively used daily for my own job search. Built solo, end-to-end — from
infra debugging (RAM-constrained local LLM → cloud LLM migration) to backend
design (Postgres enum + FK gotchas, Spring Data Specifications) to a
from-scratch PWA dashboard.

## 📄 License

MIT
