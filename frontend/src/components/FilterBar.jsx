import { Search } from 'lucide-react'

export default function FilterBar({ filters, onChange, showSearch = true }) {
  const set = (key, value) => onChange({ ...filters, [key]: value, page: 0 })

  return (
    <div className="flex flex-wrap items-center gap-3 px-6 py-3 border-b border-iron bg-panel">
      {showSearch && (
        <div className="relative flex-1 min-w-[220px]">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-ash" />
          <input
            type="text"
            value={filters.search}
            onChange={(e) => set('search', e.target.value)}
            placeholder="Search subject, sender, or content..."
            className="w-full bg-panel-raised border border-iron rounded-md pl-9 pr-3 py-2 text-sm text-fog placeholder:text-ash focus:border-signal outline-none"
          />
        </div>
      )}

      <select
        value={filters.category}
        onChange={(e) => set('category', e.target.value)}
        className="bg-panel-raised border border-iron rounded-md px-3 py-2 text-sm text-fog outline-none focus:border-signal"
      >
        <option value="">All categories</option>
        <option value="RECRUITER_RESPONSE">Recruiter response</option>
        <option value="BANK_IMPORTANT">Bank important</option>
        <option value="PERSONAL_IMPORTANT">Personal</option>
        <option value="COMPANY_BUSINESS">Business</option>
        <option value="DELIVERY_UPDATE">Delivery</option>
        <option value="OTHER_IMPORTANT">Other important</option>
      </select>

      <select
        value={filters.priority}
        onChange={(e) => set('priority', e.target.value)}
        className="bg-panel-raised border border-iron rounded-md px-3 py-2 text-sm text-fog outline-none focus:border-signal"
      >
        <option value="">All priorities</option>
        <option value="CRITICAL">Critical</option>
        <option value="HIGH">High</option>
        <option value="MEDIUM">Medium</option>
      </select>

      <input
        type="text"
        value={filters.company}
        onChange={(e) => set('company', e.target.value)}
        placeholder="Company..."
        className="w-40 bg-panel-raised border border-iron rounded-md px-3 py-2 text-sm text-fog placeholder:text-ash outline-none focus:border-signal"
      />
    </div>
  )
}