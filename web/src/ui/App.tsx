import { useEffect, useRef, useState } from 'react'
import { APP_NAME } from '../config'
import { localDateOf } from '../domain/localDate'
import { linkCodeFromQuery } from '../domain/owner/linkCode'
import { migrateBrowserProfile, type BrowserProfileStore } from '../domain/owner/migration'
import type { LinkedCar, OwnerServer } from '../domain/owner/ownerAccount'
import { storageNoticeOf, type StorageMode } from '../domain/owner/ownerNotice'
import { BasicInfoTab } from './BasicInfoTab'
import { ConsentScreen } from './ConsentScreen'
import { EmptyState } from './EmptyState'
import { Header } from './Header'
import { CarLinkCard, LinkCarScreen } from './LinkCarScreen'
import { LoadErrorState } from './LoadErrorState'
import { MedicalForm } from './MedicalForm'
import { MedicalInfoTab } from './MedicalInfoTab'
import { carLinkStatusOf, ownerBannerOf } from './ownerBanner'
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

/** 서버 모드 (계획 014): 서버 없음 / 동의 여부 확인 중 / 동의 전 / 동의 뒤 */
type Mode = 'browser-only' | 'checking' | 'before-consent' | 'consented'
type Screen = 'main' | 'consent' | 'link'

interface Props {
  /** 브라우저 저장소 (서버 설정이 없거나 동의하기 전) */
  browser: BrowserProfileStore
  /** 서버 (Firestore). 설정 없이 빌드했으면 null */
  server: OwnerServer | null
  /** 지금 시각. 테스트·화면 확인에서 고정할 수 있게 받는다 */
  now: () => Date
  /** 주소의 쿼리 (`?code=`로 연결 코드를 채운다) */
  search?: string
}

export function App({ browser, server, now, search = '' }: Props) {
  const [mode, setMode] = useState<Mode>(server === null ? 'browser-only' : 'checking')
  const [screen, setScreen] = useState<Screen>('main')
  const [banner, setBanner] = useState<string | null>(null)
  const [consenting, setConsenting] = useState(false)
  const [consentError, setConsentError] = useState<'network' | 'unknown' | null>(null)
  const [prefill] = useState(() => linkCodeFromQuery(search))
  const consentingRef = useRef(false)
  /** 연결된 차 (서버 모드에서 실시간). undefined = 아직 읽는 중 */
  const [cars, setCars] = useState<LinkedCar[] | null | undefined>(undefined)

  const repository = mode === 'consented' && server !== null ? server.repository : browser
  const { state, edit, cancel, save } = usePetProfile(repository)
  const [tab, setTab] = useState<TabKey>('medical')
  const editing = state.kind === 'ready' ? state.editing : null

  /**
   * 동의한 계정이면 브라우저에 남은 정보를 옮기고(있으면) 서버 모드로 바꾼다.
   * @return 서버에 반려견이 있는지 (있으면 동의 직후 바로 코드 입력으로 간다)
   */
  const enterServerMode = async (target: OwnerServer, afterConsent: boolean): Promise<boolean> => {
    const outcome = await migrateBrowserProfile(browser, target.repository)
    // 서버에 반려견이 있는지는 옮긴 결과로 안다 (옮겼거나 서버에 이미 있음). 없으면 등록부터 권한다
    const serverNow = outcome.kind === 'nothing' && afterConsent ? await target.repository.load() : null
    const hasProfile = outcome.kind === 'moved' || outcome.kind === 'server-kept' || (serverNow?.ok === true && serverNow.profile !== null)
    setBanner(ownerBannerOf(outcome, afterConsent, hasProfile))
    setMode('consented')
    if (prefill !== null) setScreen('link')
    return hasProfile
  }

  // 처음 열 때: 이 브라우저가 기억한 계정이 동의했는지 본다 (동의 전에는 계정을 만들지 않는다)
  useEffect(() => {
    if (server === null) return
    let active = true
    void server.account.status().then((status) => {
      if (!active) return
      // 서버에 닿지 못했으면 기억한 계정이 있다는 뜻이라 서버 모드로 둔다 (읽기 실패 화면이 알린다)
      if (status === 'network') setMode('consented')
      else if (status === 'consented') void enterServerMode(server, false)
      else setMode('before-consent')
    })
    return () => {
      active = false
    }
    // eslint 없음: enterServerMode는 처음 한 번만 쓴다
  }, [server])

  // 서버 모드에서는 연결된 차를 실시간으로 본다 (연결하면 카드가 "차와 연결됨"으로, 차가 끊으면 다시 연결하기로)
  useEffect(() => {
    if (mode !== 'consented' || server === null) return
    return server.account.watchLinkedCars(setCars)
  }, [mode, server])

  // 화면이 바뀌면 맨 위부터 보이게 한다.
  useEffect(() => {
    window.scrollTo(0, 0)
  }, [editing, screen])

  const agree = async () => {
    if (server === null || consentingRef.current) return
    consentingRef.current = true
    setConsenting(true)
    setConsentError(null)
    const result = await server.account.consent()
    if (result.ok) {
      // 동의하면 바로 코드 입력으로 간다 (처음 화면으로 돌아가 버튼을 다시 찾지 않게)
      const hasProfile = await enterServerMode(server, true)
      setScreen(hasProfile || prefill !== null ? 'link' : 'main')
    } else {
      setConsentError(result.reason)
    }
    consentingRef.current = false
    setConsenting(false)
  }

  if (state.kind === 'loading' || mode === 'checking') {
    return (
      <div className="page">
        <Header />
      </div>
    )
  }

  const { profile, loadError, saving, saveError } = state
  const today = localDateOf(now())
  const storageMode: StorageMode =
    mode === 'consented' ? { kind: 'server', emulator: server?.emulator ?? false } : mode === 'before-consent' ? { kind: 'before-consent' } : { kind: 'browser-only' }

  let content
  if (screen === 'consent' && mode === 'before-consent') {
    content = (
      <ConsentScreen
        browserPetName={profile?.name ?? null}
        busy={consenting}
        error={consentError}
        onAgree={() => void agree()}
        onLater={() => setScreen('main')}
      />
    )
  } else if (screen === 'link' && mode === 'consented' && server !== null) {
    content = <LinkCarScreen account={server.account} petName={profile?.name ?? null} prefill={prefill} onBack={() => setScreen('main')} />
  } else if (screen === 'link' && mode === 'before-consent') {
    // 동의 전에 [차와 연결하기]·?code=로 왔으면 동의부터
    content = (
      <ConsentScreen
        browserPetName={profile?.name ?? null}
        busy={consenting}
        error={consentError}
        onAgree={() => void agree()}
        onLater={() => setScreen('main')}
      />
    )
  } else if (editing === 'profile') {
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
        {server !== null && (
          <CarLinkCard
            status={mode === 'consented' ? carLinkStatusOf(cars) : { kind: 'not-linked' }}
            onOpen={() => setScreen(mode === 'consented' ? 'link' : 'consent')}
          />
        )}
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

  const onMain = screen === 'main' && editing === null
  return (
    <div className="page">
      <Header />
      <main className="content">
        {onMain && banner !== null && (
          <p className="banner banner--info" role="status">
            {banner}
          </p>
        )}
        {content}
        {onMain && <StorageNotice text={storageNoticeOf(storageMode, APP_NAME)} />}
      </main>
    </div>
  )
}
