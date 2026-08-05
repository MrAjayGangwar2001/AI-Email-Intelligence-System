import { X, Building2, Briefcase, Clock, ArrowRight, Mail, Trash2 } from 'lucide-react'
import { useState } from 'react'
import { PriorityBadge } from './PriorityBadge'

export default function EmailDetailPanel({ email, loading, onClose, onDelete }) {
  const [deleting, setDeleting] = useState(false)
  const [confirmingDelete, setConfirmingDelete] = useState(false)

  if (!email && !loading) return null

  const handleDeleteClick = () => {
    if (!confirmingDelete) {
      setConfirmingDelete(true)
      return
    }
    setDeleting(true)
    onDelete(email.id).finally(() => setDeleting(false))
  }

  return (
    <div className="fixed inset-0 z-40 flex justify-end">
      <div className="absolute inset-0 bg-black/50" onClick={onClose} />

      <div className="relative w-full max-w-md h-full bg-panel border-l border-iron overflow-y-auto flex flex-col">
        <div className="sticky top-0 bg-panel border-b border-iron px-5 py-4 flex items-center justify-between z-10">
          <span className="font-mono text-xs text-ash uppercase tracking-wider">Email detail</span>
          <button onClick={onClose} className="text-ash hover:text-fog" aria-label="Close">
            <X className="w-4 h-4" />
          </button>
        </div>

        {loading ? (
          <div className="p-8 text-center text-ash font-mono text-sm">Loading…</div>
        ) : (
          <>
            <div className="p-5 flex flex-col gap-4 flex-1">
              <div>
                <PriorityBadge priority={email.priority} />
                <h2 className="text-fog text-lg font-semibold mt-2 leading-snug">{email.subject}</h2>
              </div>

              <div className="flex items-center gap-2 text-sm text-ash">
                <Mail className="w-4 h-4 shrink-0" />
                <span>{email.senderName ? `${email.senderName} · ` : ''}{email.senderEmail}</span>
              </div>

              {email.company && (
                <div className="flex items-center gap-2 text-sm text-fog">
                  <Building2 className="w-4 h-4 shrink-0 text-ash" />
                  <span>{email.company}</span>
                </div>
              )}

              {email.jobRole && (
                <div className="flex items-center gap-2 text-sm text-fog">
                  <Briefcase className="w-4 h-4 shrink-0 text-ash" />
                  <span>{email.jobRole}</span>
                </div>
              )}

              {email.deadline && (
                <div className="flex items-center gap-2 text-sm text-amber">
                  <Clock className="w-4 h-4 shrink-0" />
                  <span>{new Date(email.deadline).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' })}</span>
                </div>
              )}

              <div className="border-t border-iron pt-4">
                <p className="font-mono text-[11px] text-ash uppercase tracking-wider mb-2">Summary</p>
                <p className="text-fog text-sm leading-relaxed">{email.summary}</p>
              </div>

              {email.nextStep && (
                <div className="bg-panel-raised border border-iron rounded-md p-3">
                  <p className="font-mono text-[11px] text-amber uppercase tracking-wider mb-1.5 flex items-center gap-1.5">
                    <ArrowRight className="w-3 h-3" /> Next step
                  </p>
                  <p className="text-fog text-sm">{email.nextStep}</p>
                </div>
              )}

              <div className="border-t border-iron pt-4">
                <p className="font-mono text-[11px] text-ash uppercase tracking-wider mb-2">Full message</p>
                <p className="text-ash text-sm leading-relaxed whitespace-pre-line">
                  {email.bodyText || email.bodySnippet}
                </p>
              </div>
            </div>

            <div className="sticky bottom-0 bg-panel border-t border-iron p-4">
              <button
                onClick={handleDeleteClick}
                disabled={deleting}
                className={`w-full flex items-center justify-center gap-2 px-4 py-2.5 rounded-md font-mono text-xs uppercase tracking-wider transition-colors disabled:opacity-50 ${
                  confirmingDelete
                    ? 'bg-flare text-graphite hover:opacity-90'
                    : 'border border-iron text-ash hover:border-flare hover:text-flare'
                }`}
              >
                <Trash2 className="w-3.5 h-3.5" />
                {deleting ? 'Deleting…' : confirmingDelete ? 'Confirm delete' : 'Delete email'}
              </button>
              {confirmingDelete && !deleting && (
                <button
                  onClick={() => setConfirmingDelete(false)}
                  className="w-full text-center text-ash text-xs font-mono mt-2 hover:text-fog"
                >
                  Cancel
                </button>
              )}
            </div>
          </>
        )}
      </div>
    </div>
  )
}