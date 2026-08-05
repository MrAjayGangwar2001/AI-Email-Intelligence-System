import { PriorityDot } from './PriorityBadge'

export default function TopBar({ counts, view, onViewChange, unreadCount }) {
  return (
    <header className="border-b border-iron bg-graphite px-6 py-4 flex items-center justify-between flex-wrap gap-4">
      <div className="flex items-baseline gap-3">
        <h1 className="font-mono text-sm tracking-[0.2em] text-fog uppercase">
          Email Intelligence
        </h1>
        <span className="text-ash text-xs font-mono hidden sm:inline">// inbox triage</span>
      </div>

      <div className="flex items-center gap-1 bg-panel border border-iron rounded-md p-1 font-mono text-xs">
        <button
          onClick={() => onViewChange('dashboard')}
          className={`px-3 py-1.5 rounded transition-colors ${
            view === 'dashboard' ? 'bg-signal text-graphite font-semibold' : 'text-ash hover:text-fog'
          }`}
        >
          New{unreadCount > 0 ? ` (${unreadCount})` : ''}
        </button>
        <button
          onClick={() => onViewChange('inbox')}
          className={`px-3 py-1.5 rounded transition-colors ${
            view === 'inbox' ? 'bg-signal text-graphite font-semibold' : 'text-ash hover:text-fog'
          }`}
        >
          Inbox
        </button>
      </div>

      <div className="flex items-center gap-5 font-mono text-xs">
        <CountStat priority="CRITICAL" count={counts.CRITICAL} />
        <CountStat priority="HIGH" count={counts.HIGH} />
        <CountStat priority="MEDIUM" count={counts.MEDIUM} />
      </div>
    </header>
  )
}

function CountStat({ priority, count }) {
  return (
    <div className="flex items-center gap-2">
      <PriorityDot priority={priority} size="md" />
      <span className="text-fog font-medium">{count ?? 0}</span>
      <span className="text-ash uppercase tracking-wide">{priority}</span>
    </div>
  )
}