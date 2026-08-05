const CONFIG = {
  CRITICAL: { color: 'bg-flare', text: 'text-flare', label: 'Critical', pulse: true },
  HIGH: { color: 'bg-amber', text: 'text-amber', label: 'High', pulse: false },
  MEDIUM: { color: 'bg-signal', text: 'text-signal', label: 'Medium', pulse: false },
}

export function PriorityDot({ priority, size = 'sm' }) {
  const cfg = CONFIG[priority] || CONFIG.MEDIUM
  const dim = size === 'sm' ? 'w-2 h-2' : 'w-2.5 h-2.5'
  return (
    <span
      className={`inline-block rounded-full ${dim} ${cfg.color} ${cfg.pulse ? 'signal-critical' : ''}`}
      aria-hidden="true"
    />
  )
}

export function PriorityBadge({ priority }) {
  const cfg = CONFIG[priority] || CONFIG.MEDIUM
  return (
    <span className={`inline-flex items-center gap-1.5 font-mono text-[11px] uppercase tracking-wider ${cfg.text}`}>
      <PriorityDot priority={priority} />
      {cfg.label}
    </span>
  )
}

export const PRIORITY_ORDER = ['CRITICAL', 'HIGH', 'MEDIUM']
