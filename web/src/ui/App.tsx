import { useEffect } from 'react'
import { localDateOf } from '../domain/localDate'
import type { PetProfileRepository } from '../domain/petProfileRepository'
import { EmptyState } from './EmptyState'
import { Header } from './Header'
import { LoadErrorState } from './LoadErrorState'
import { ProfileForm } from './ProfileForm'
import { StorageNotice } from './StorageNotice'
import { usePetProfile } from './usePetProfile'

interface Props {
  repository: PetProfileRepository
  /** 지금 시각. 테스트·화면 확인에서 고정할 수 있게 받는다 */
  now: () => Date
}

export function App({ repository, now }: Props) {
  const { state, edit, cancel, save } = usePetProfile(repository)
  const editing = state.kind === 'ready' ? state.editing : null

  // 화면이 바뀌면 맨 위부터 보이게 한다.
  useEffect(() => {
    window.scrollTo(0, 0)
  }, [editing])

  if (state.kind === 'loading') {
    return (
      <div className="page">
        <Header />
      </div>
    )
  }

  const { profile, loadError, saving, saveError } = state
  const today = localDateOf(now())

  let content
  if (editing === 'profile') {
    content = <ProfileForm initial={profile} today={today} now={now} saving={saving} saveError={saveError} onSave={save} onCancel={cancel} />
  } else if (profile === null) {
    content =
      loadError !== null ? (
        <LoadErrorState reason={loadError} onStartNew={() => edit('profile')} />
      ) : (
        <EmptyState onRegister={() => edit('profile')} />
      )
  } else {
    // 프로필 화면은 다음 커밋에서 만든다.
    content = (
      <section className="card">
        <h1 className="empty__title">{profile.name}</h1>
        <button type="button" className="button button--pill" onClick={() => edit('profile')}>
          프로필 수정
        </button>
      </section>
    )
  }

  return (
    <div className="page">
      <Header />
      <main className="content">
        {content}
        {editing === null && <StorageNotice />}
      </main>
    </div>
  )
}
