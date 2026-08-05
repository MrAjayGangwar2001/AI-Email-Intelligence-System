import { Building2, Clock, Inbox } from 'lucide-react'
import { PriorityBadge } from './PriorityBadge'

const PRIORITY_BAR = {
  CRITICAL: 'bg-flare',
  HIGH: 'bg-amber',
  MEDIUM: 'bg-signal',
}

const CATEGORY_LABEL = {
  RECRUITER_RESPONSE: 'RECRUITER',
  BANK_IMPORTANT: 'BANK',
  PERSONAL_IMPORTANT: 'PERSONAL',
  COMPANY_BUSINESS: 'BUSINESS',
  DELIVERY_UPDATE: 'DELIVERY',
  OTHER_IMPORTANT: 'IMPORTANT',
}

function formatRelative(dateStr) {
  const date = new Date(dateStr)
  const diffMs = Date.now() - date.getTime()
  const diffHrs = diffMs / 36e5
  if (diffHrs < 1) return 'just now'
  if (diffHrs < 24) return `${Math.floor(diffHrs)}h ago`
  const diffDays = Math.floor(diffHrs / 24)
  if (diffDays < 7) return `${diffDays}d ago`
  return date.toLocaleDateString('en-IN', { day: 'numeric', month: 'short' })
}

function formatDeadline(dateStr) {
  return new Date(dateStr).toLocaleString('en-IN', {
    day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit',
  })
}

export default function EmailList({ emails, loading, error, onSelect, selectedId, emptyMessage }) {
  if (loading) {
    return (
      <div className="p-8 text-center text-ash font-mono text-sm">Loading inbox…</div>
    )
  }

  if (error) {
    return (
      <div className="p-8 text-center">
        <p className="text-flare font-mono text-sm mb-1">Could not reach the backend.</p>
        <p className="text-ash text-xs">{error}</p>
      </div>
    )
  }

  if (!emails || emails.length === 0) {
    return (
      <div className="p-16 text-center flex flex-col items-center gap-3">
        <Inbox className="w-8 h-8 text-iron" />
        <p className="text-ash text-sm">
          {emptyMessage || "Nothing here yet — important emails will appear as they're classified."}
        </p>
      </div>
    )
  }

  return (
    <div className="divide-y divide-iron">
      {emails.map((email) => (
        <button
          key={email.id}
          onClick={() => onSelect(email.id)}
          className={`w-full text-left flex gap-3 px-4 py-3 hover:bg-panel-raised transition-colors ${
            selectedId === email.id ? 'bg-panel-raised' : ''
          } ${email.isRead ? 'opacity-60' : ''}`}
        >
          <span className={`w-1 rounded-full shrink-0 ${PRIORITY_BAR[email.priority] || 'bg-ash'}`} />

          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-2 mb-1">
              {!email.isRead && (
                <span className="w-1.5 h-1.5 rounded-full bg-signal shrink-0" title="Unread" />
              )}
              <span className="font-mono text-[10px] text-ash uppercase tracking-wider border border-iron rounded px-1.5 py-0.5">
                {CATEGORY_LABEL[email.category] || email.category}
              </span>
              <PriorityBadge priority={email.priority} />
              {email.actionRequired && (
                <span className="font-mono text-[10px] text-amber uppercase tracking-wider">
                  action needed
                </span>
              )}
            </div>

            <p className={`text-sm truncate ${email.isRead ? 'text-ash font-normal' : 'text-fog font-medium'}`}>{email.subject}</p>
            <p className="text-ash text-xs truncate mt-0.5">{email.summary}</p>

            <div className="flex items-center gap-4 mt-1.5 text-[11px] text-ash font-mono">
              {email.company && (
                <span className="flex items-center gap-1">
                  <Building2 className="w-3 h-3" /> {email.company}
                </span>
              )}
              {email.deadline && (
                <span className="flex items-center gap-1 text-amber">
                  <Clock className="w-3 h-3" /> {formatDeadline(email.deadline)}
                </span>
              )}
              <span className="ml-auto">{formatRelative(email.receivedAt)}</span>
            </div>
          </div>
        </button>
      ))}
    </div>
  )
}