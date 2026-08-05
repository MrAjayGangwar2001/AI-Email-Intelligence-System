import { useEffect, useState, useCallback } from 'react'
import TopBar from './components/TopBar'
import FilterBar from './components/FilterBar'
import EmailList from './components/EmailList'
import EmailDetailPanel from './components/EmailDetailPanel'
import Sidebar from './components/Sidebar'
import {
  fetchEmails,
  fetchEmail,
  fetchUpcomingDeadlines,
  fetchPendingActions,
  completeAction,
  markEmailAsRead,
  deleteEmail,
} from './lib/api'

const DEFAULT_FILTERS = { search: '', category: '', priority: '', company: '', page: 0, size: 30 }
const POLL_INTERVAL_MS = 60_000 // refresh list every minute - new emails arrive async via n8n

export default function App() {
  // 'dashboard' = unread-only "new" emails. 'inbox' = everything, searchable.
  const [view, setView] = useState('dashboard')

  const [filters, setFilters] = useState(DEFAULT_FILTERS)
  const [emails, setEmails] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const [selectedId, setSelectedId] = useState(null)
  const [selectedEmail, setSelectedEmail] = useState(null)
  const [detailLoading, setDetailLoading] = useState(false)

  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [unreadCount, setUnreadCount] = useState(0)

  const [deadlines, setDeadlines] = useState([])
  const [actions, setActions] = useState([])

  const loadEmails = useCallback(() => {
    setLoading(true)
    setError(null)
    const isRead = view === 'dashboard' ? false : undefined
    fetchEmails({ ...filters, isRead })
      .then((page) => {
        setEmails(page.content || [])
        setTotalPages(page.totalPages || 0)
        setTotalElements(page.totalElements || 0)
      })
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false))
  }, [filters, view])

  const loadUnreadCount = useCallback(() => {
    fetchEmails({ isRead: false, page: 0, size: 1 })
      .then((page) => setUnreadCount(page.totalElements || 0))
      .catch(() => {})
  }, [])

  const loadSidebar = useCallback(() => {
    fetchUpcomingDeadlines(7).then(setDeadlines).catch(() => {})
    fetchPendingActions().then(setActions).catch(() => {})
  }, [])

  // Reset filters/page when switching views so stale search/page doesn't carry over
  const handleViewChange = (nextView) => {
    setView(nextView)
    setFilters(DEFAULT_FILTERS)
  }

  useEffect(() => {
    loadEmails()
  }, [loadEmails])

  useEffect(() => {
    loadSidebar()
    loadUnreadCount()
    const interval = setInterval(() => {
      loadEmails()
      loadSidebar()
      loadUnreadCount()
    }, POLL_INTERVAL_MS)
    return () => clearInterval(interval)
  }, [loadEmails, loadSidebar, loadUnreadCount])

  const handleSelect = (id) => {
    setSelectedId(id)
    setDetailLoading(true)
    fetchEmail(id)
      .then((email) => {
        setSelectedEmail(email)
        // Mark as read as soon as it's opened. On the Dashboard (unread-only)
        // view this means it should disappear from the list right away.
        if (!email.isRead) {
          markEmailAsRead(id)
            .then(() => {
              setUnreadCount((c) => Math.max(0, c - 1))
              if (view === 'dashboard') {
                setEmails((prev) => prev.filter((e) => e.id !== id))
                setTotalElements((t) => Math.max(0, t - 1))
              }
            })
            .catch(() => {})
        }
      })
      .catch(() => setSelectedEmail(null))
      .finally(() => setDetailLoading(false))
  }

  const handleDelete = (id) => {
    return deleteEmail(id).then(() => {
      setEmails((prev) => prev.filter((e) => e.id !== id))
      setTotalElements((t) => Math.max(0, t - 1))
      setSelectedId(null)
      setSelectedEmail(null)
    })
  }

  const handleCompleteAction = (id) => {
    setActions((prev) => prev.filter((a) => a.id !== id)) // optimistic
    completeAction(id).catch(() => loadSidebar())
  }

  const counts = emails.reduce(
    (acc, e) => {
      acc[e.priority] = (acc[e.priority] || 0) + 1
      return acc
    },
    { CRITICAL: 0, HIGH: 0, MEDIUM: 0 }
  )

  return (
    <div className="min-h-screen bg-graphite flex flex-col">
      <TopBar counts={counts} view={view} onViewChange={handleViewChange} unreadCount={unreadCount} />
      <FilterBar filters={filters} onChange={setFilters} showSearch={view === 'inbox'} />

      <div className="flex flex-1 min-h-0">
        <main className="flex-1 overflow-y-auto">
          <EmailList
            emails={emails}
            loading={loading}
            error={error}
            onSelect={handleSelect}
            selectedId={selectedId}
            emptyMessage={
              view === 'dashboard'
                ? "You're all caught up — no new emails. Check the Inbox tab for everything you've already seen."
                : undefined
            }
          />

          {!loading && !error && totalPages > 1 && (
            <div className="flex items-center justify-between px-4 py-3 border-t border-iron font-mono text-xs text-ash">
              <span>
                Page {filters.page + 1} of {totalPages} · {totalElements} total
              </span>
              <div className="flex gap-2">
                <button
                  disabled={filters.page === 0}
                  onClick={() => setFilters((f) => ({ ...f, page: f.page - 1 }))}
                  className="px-3 py-1.5 rounded border border-iron disabled:opacity-30 hover:border-signal hover:text-fog"
                >
                  Prev
                </button>
                <button
                  disabled={filters.page + 1 >= totalPages}
                  onClick={() => setFilters((f) => ({ ...f, page: f.page + 1 }))}
                  className="px-3 py-1.5 rounded border border-iron disabled:opacity-30 hover:border-signal hover:text-fog"
                >
                  Next
                </button>
              </div>
            </div>
          )}
        </main>

        <Sidebar deadlines={deadlines} actions={actions} onCompleteAction={handleCompleteAction} />
      </div>

      {selectedId && (
        <EmailDetailPanel
          email={selectedEmail}
          loading={detailLoading}
          onClose={() => setSelectedId(null)}
          onDelete={handleDelete}
        />
      )}
    </div>
  )
}