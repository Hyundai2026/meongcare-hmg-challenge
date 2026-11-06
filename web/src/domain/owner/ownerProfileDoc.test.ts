import { describe, expect, it } from 'vitest'
import example from '../../../../docs/data/pet-profile.example.json'
import ownerLink from '../../../../docs/data/owner-link.example.json'
import type { PetProfile } from '../petProfile'
import { petProfileFromJson } from '../petProfileJson'
import { profileFromServerDocs, serverDocsOf } from './ownerProfileDoc'

function exampleProfile(): PetProfile {
  const parsed = petProfileFromJson(example)
  if (!parsed.ok) throw new Error('예시가 읽히지 않는다')
  return parsed.profile
}

const PHOTO = 'data:image/jpeg;base64,/9j/4AAQ'

describe('프로필 ↔ 서버 문서 (docs/data/owner-link.md)', () => {
  it('예시 프로필을 서버 문서로 나누고 다시 합치면 같은 프로필이다', () => {
    const profile = { ...exampleProfile(), photoDataUrl: PHOTO }
    const docs = serverDocsOf(profile)
    expect(docs.profile.photoDataUrl).toBeNull()
    expect(docs.photoDataUrl).toBe(PHOTO)
    expect(profileFromServerDocs(docs.profile, { photoDataUrl: docs.photoDataUrl })).toEqual({ ok: true, profile })
  })

  it('서버 프로필 문서는 owner-link 예시와 같다', () => {
    const docs = serverDocsOf(exampleProfile())
    expect(docs.profile).toEqual(ownerLink['petProfiles/owner-uid-1'])
  })

  it('사진이 없으면 사진 문서도 없다', () => {
    const docs = serverDocsOf(exampleProfile())
    expect(docs.photoDataUrl).toBeNull()
    expect(profileFromServerDocs(docs.profile, null)).toEqual({ ok: true, profile: exampleProfile() })
  })

  it('사진 문서가 틀리면 사진 없이 읽는다', () => {
    const docs = serverDocsOf(exampleProfile())
    expect(profileFromServerDocs(docs.profile, { photoDataUrl: 'data:image/png;base64,AA' })).toEqual({ ok: true, profile: exampleProfile() })
    expect(profileFromServerDocs(docs.profile, { photoDataUrl: 7 })).toEqual({ ok: true, profile: exampleProfile() })
  })

  it('서버 문서의 모르는 필드는 무시한다', () => {
    const docs = serverDocsOf(exampleProfile())
    expect(profileFromServerDocs({ ...docs.profile, weightKg: 30 }, null)).toEqual({ ok: true, profile: exampleProfile() })
  })

  it('schemaVersion이 1이 아니면 읽지 않는다', () => {
    const docs = serverDocsOf(exampleProfile())
    expect(profileFromServerDocs({ ...docs.profile, schemaVersion: 2 }, null)).toEqual({ ok: false, reason: 'unsupported-version' })
  })

  it('객체가 아니면 손상이다', () => {
    expect(profileFromServerDocs(null, null)).toEqual({ ok: false, reason: 'corrupted' })
    expect(profileFromServerDocs([], null)).toEqual({ ok: false, reason: 'corrupted' })
  })
})
