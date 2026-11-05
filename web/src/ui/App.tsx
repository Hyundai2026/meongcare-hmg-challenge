import { useEffect, useState } from 'react'
import { localDateOf } from '../domain/localDate'
import type { PetProfileRepository } from '../domain/petProfileRepository'
import { BasicInfoTab } from './BasicInfoTab'
import { EmptyState } from './EmptyState'
import { Header } from './Header'
import { LoadErrorState } from './LoadErrorState'
import { MedicalForm } from './MedicalForm'
import { MedicalInfoTab } from './MedicalInfoTab'
import { ProfileCard } from './ProfileCard'
import { ProfileForm } from './ProfileForm'
import { StorageNotice } from './StorageNotice'
import { Tabs, type TabItem } from './Tabs'
import { usePetProfile } from './usePetProfile'

type TabKey = 'medical' | 'basic'

/** 이 웹의 목적이 의료정보 등록이라 의료정보 탭이 먼저다. */
const TABS: TabItem<TabKey>[] = [
  { key: 'medical', label: '의료정보' },
  { key: 'basic', label: '기본정보' },
]

interface Props {
  repository: PetProfileRepository
  /** 지금 시각. 테스트·화면 확인에서 고정할 수 있게 받는다 */
  now: () => Date
}

export function App({ repository, now }: Props) {
  const { state, edit, cancel, save } = usePetProfile(repository)
  const [tab, setTab] = useState<TabKey>('medical')
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
  } else if (editing === 'medical' && profile !== null) {
    content = <MedicalForm profile={profile} now={now} saving={saving} saveError={saveError} onSave={save} onCancel={cancel} />
  } else if (profile === null) {
    content =
      loadError !== null ? (
        <LoadErrorState reason={loadError} onStartNew={() => edit('profile')} />
      ) : (
        <EmptyState onRegister={() => edit('profile')} />
      )
  } else {
    content = (
      <>
        <ProfileCard profile={profile} today={today} onEdit={() => edit('profile')} />
        <Tabs tabs={TABS} selected={tab} onSelect={setTab}>
          {tab === 'medical' ? (
            <MedicalInfoTab medical={profile.medical} onEdit={() => edit('medical')} />
          ) : (
            <BasicInfoTab profile={profile} today={today} />
          )}
        </Tabs>
      </>
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
