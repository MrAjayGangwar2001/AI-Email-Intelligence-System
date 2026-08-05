import { Check, Clock } from 'lucide-react'

export default function Sidebar({ deadlines, actions, onCompleteAction }) {
  return (
    <aside className="w-72 shrink-0 border-l border-iron bg-panel h-full overflow-y-auto hidden lg:block">
      <Section title="Upcoming deadlines">
        {(!deadlines || deadlines.length === 0) ? (
          <EmptyNote text="No deadlines in the next 7 days." />
        ) : (
          <ul className="flex flex-col gap-2">
            {deadlines.map((d) => (
              <li key={d.id} className="text-sm">
                <p className="text-fog font-medium truncate">{d.company || d.subject}</p>
                <p className="text-ash text-xs flex items-center gap-1 mt-0.5">
                  <Clock className="w-3 h-3" />
                  {new Date(d.deadline).toLocaleString('en-IN', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' })}
                </p>
              </li>
            ))}
          </ul>
        )}
      </Section>

      <Section title="Pending actions">
        {(!actions || actions.length === 0) ? (
          <EmptyNote text="Nothing pending — you're caught up." />
        ) : (
          <ul className="flex flex-col gap-2">
            {actions.map((a) => (
              <li key={a.id} className="flex items-start gap-2 text-sm group">
                <button
                  onClick={() => onCompleteAction(a.id)}
                  className="mt-0.5 w-4 h-4 rounded border border-iron flex items-center justify-center shrink-0 hover:border-signal"
                  aria-label="Mark complete"
                >
                  <Check className="w-3 h-3 text-transparent group-hover:text-signal" />
                </button>
                <span className="text-fog leading-snug">{a.actionDescription}</span>
              </li>
            ))}
          </ul>
        )}
      </Section>
    </aside>
  )
}

function Section({ title, children }) {
  return (
    <div className="px-4 py-4 border-b border-iron">
      <p className="font-mono text-[11px] text-ash uppercase tracking-wider mb-3">{title}</p>
      {children}
    </div>
  )
}

function EmptyNote({ text }) {
  return <p className="text-ash text-xs">{text}</p>
}
