import { PawIcon } from './PawIcon'

export function EmptyState({ onRegister }: { onRegister: () => void }) {
  return (
    <section className="card empty">
      <div className="empty__badge">
        <PawIcon className="empty__paw" />
      </div>
      <h1 className="empty__title">아직 등록된 반려견이 없어요</h1>
      <p className="empty__body">병원에 갈 때 차에서 바로 진료 카드를 만들 수 있게 미리 등록해 두세요.</p>
      <button type="button" className="button button--primary button--block" onClick={onRegister}>
        반려견 등록하기
      </button>
    </section>
  )
}
