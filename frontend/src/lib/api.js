const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

async function request(path, options = {}) {
  const res = await fetch(`${BASE_URL}${path}`, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  })
  if (!res.ok) {
    const body = await res.json().catch(() => ({}))
    throw new Error(body.error || `Request failed: ${res.status}`)
  }
  return res.json()
}

export function fetchEmails({ category, priority, company, search, isRead, page = 0, size = 20 } = {}) {
  const params = new URLSearchParams()
  if (category) params.set('category', category)
  if (priority) params.set('priority', priority)
  if (company) params.set('company', company)
  if (search) params.set('search', search)
  if (isRead !== undefined && isRead !== null) params.set('isRead', isRead)
  params.set('page', page)
  params.set('size', size)
  return request(`/api/dashboard/emails?${params.toString()}`)
}

export function fetchEmail(id) {
  return request(`/api/dashboard/emails/${id}`)
}

export function markEmailAsRead(id) {
  return request(`/api/dashboard/emails/${id}/read`, { method: 'PATCH' })
}

export function markEmailAsUnread(id) {
  return request(`/api/dashboard/emails/${id}/unread`, { method: 'PATCH' })
}

export function deleteEmail(id) {
  return fetch(`${BASE_URL}/api/dashboard/emails/${id}`, { method: 'DELETE' }).then((res) => {
    if (!res.ok) throw new Error(`Delete failed: ${res.status}`)
  })
}

export function fetchUpcomingDeadlines(days = 7) {
  return request(`/api/dashboard/deadlines/upcoming?days=${days}`)
}

export function fetchPendingActions() {
  return request(`/api/dashboard/actions/pending`)
}

export function completeAction(id) {
  return request(`/api/dashboard/actions/${id}/complete`, { method: 'PATCH' })
}