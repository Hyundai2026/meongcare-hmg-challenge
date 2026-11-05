export interface InfoRow {
  label: string
  /** 비어 있으면 emptyText를 보인다 */
  values: string[]
}

export function InfoTable({ rows, emptyText }: { rows: InfoRow[]; emptyText: string }) {
  return (
    <dl className="info-table">
      {rows.map((row) => (
        <div className="info-row" key={row.label}>
          <dt className="info-row__label">{row.label}</dt>
          <dd className="info-row__value">
            {row.values.length === 0 ? (
              <span className="info-row__empty">{emptyText}</span>
            ) : (
              row.values.map((value, i) => <div key={i}>{value}</div>)
            )}
          </dd>
        </div>
      ))}
    </dl>
  )
}
