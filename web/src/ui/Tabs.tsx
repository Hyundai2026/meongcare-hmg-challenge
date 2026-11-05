import type { ReactNode } from 'react'

export interface TabItem<K extends string> {
  key: K
  label: string
}

/** 탭 목록과 선택된 탭의 내용. 키보드 왼쪽·오른쪽 화살표로도 바꾼다. */
export function Tabs<K extends string>({
  tabs,
  selected,
  onSelect,
  children,
}: {
  tabs: TabItem<K>[]
  selected: K
  onSelect: (key: K) => void
  children: ReactNode
}) {
  const move = (delta: number) => {
    const index = tabs.findIndex((t) => t.key === selected)
    const next = tabs[(index + delta + tabs.length) % tabs.length]
    onSelect(next.key)
    document.getElementById(`tab-${next.key}`)?.focus()
  }
  return (
    <section className="card panel">
      <div className="tabs" role="tablist">
        {tabs.map((tab) => (
          <button
            key={tab.key}
            id={`tab-${tab.key}`}
            type="button"
            role="tab"
            className="tab"
            aria-selected={tab.key === selected}
            aria-controls={`tabpanel-${tab.key}`}
            tabIndex={tab.key === selected ? 0 : -1}
            onClick={() => onSelect(tab.key)}
            onKeyDown={(e) => {
              if (e.key === 'ArrowRight') move(1)
              if (e.key === 'ArrowLeft') move(-1)
            }}
          >
            {tab.label}
          </button>
        ))}
      </div>
      <div className="panel__body" role="tabpanel" id={`tabpanel-${selected}`} aria-labelledby={`tab-${selected}`}>
        {children}
      </div>
    </section>
  )
}
