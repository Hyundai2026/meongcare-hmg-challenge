/**
 * 동의한 뒤 브라우저 정보를 서버로 한 번 옮기기 (계획 014 U6, docs/architecture.md "Firebase로 옮길 때 할 일").
 *
 * | 브라우저 | 서버 | 할 일 |
 * |---|---|---|
 * | 프로필 있음 | 없음 | 올린다 → 성공하면 브라우저에서 지운다. 실패하면 브라우저 그대로 |
 * | 있음 | 있음 | 서버를 덮어쓰지 않는다. 브라우저 정보도 그대로 둔다 (화면에 알린다) |
 * | 없음·읽기 실패 | 상관없음 | 할 일 없음 |
 * | 상관없음 | 읽기 실패 | 아무것도 하지 않는다 (서버의 기존 정보를 덮어쓰지 않게) |
 */
import type { PetProfile } from '../petProfile'
import type { LoadResult, PetProfileRepository } from '../petProfileRepository'

export type MigrationOutcome =
  /** 옮겼다 (브라우저에서 지움) */
  | { kind: 'moved'; profile: PetProfile }
  /** 서버에 이미 있어 옮기지 않았다. 브라우저 정보는 남아 있다 */
  | { kind: 'server-kept' }
  /** 옮길 것이 없다 */
  | { kind: 'nothing' }
  /** 서버를 읽거나 쓰지 못했다. 브라우저 정보는 그대로다 */
  | { kind: 'failed' }

export type MigrationStep = { kind: 'upload'; profile: PetProfile } | { kind: 'keep-server' } | { kind: 'nothing' } | { kind: 'wait' }

/** 읽은 결과 두 개 → 할 일 (순수) */
export function migrationStepOf(browser: LoadResult, server: LoadResult): MigrationStep {
  if (!server.ok) return { kind: 'wait' }
  if (!browser.ok || browser.profile === null) return { kind: 'nothing' }
  if (server.profile !== null) return { kind: 'keep-server' }
  return { kind: 'upload', profile: browser.profile }
}

/** 브라우저 저장소에 지우기를 더한 것 */
export interface BrowserProfileStore extends PetProfileRepository {
  /** 브라우저의 프로필을 지운다. 지웠으면(또는 없었으면) true */
  clear(): Promise<boolean>
}

export async function migrateBrowserProfile(browser: BrowserProfileStore, server: PetProfileRepository): Promise<MigrationOutcome> {
  const step = migrationStepOf(await browser.load(), await server.load())
  switch (step.kind) {
    case 'wait':
      return { kind: 'failed' }
    case 'nothing':
      return { kind: 'nothing' }
    case 'keep-server':
      return { kind: 'server-kept' }
    case 'upload': {
      const saved = await server.save(step.profile)
      if (!saved.ok) return { kind: 'failed' }
      await browser.clear()
      return { kind: 'moved', profile: step.profile }
    }
  }
}
