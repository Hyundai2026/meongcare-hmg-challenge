import { describe, expect, it } from 'vitest'
import example from '../../../../docs/data/pet-profile.example.json'
import type { PetProfile } from '../petProfile'
import { petProfileFromJson } from '../petProfileJson'
import { profileFromServerDocs, serverDocsOf } from './ownerProfileDoc'

function exampleProfile(): PetProfile {
  const parsed = petProfileFromJson(example)
  if (!parsed.ok) throw new Error('예시가 읽히지 않는다')
  return parsed.profile
}

const PHOTO = 'data:image/jpeg;base64,/9j/4AAQ'
const V1_KEYS = ['birthDate', 'birthDateIsEstimated', 'breed', 'medical', 'name', 'photoDataUrl', 'schemaVersion', 'sex', 'updatedAt']

/**
 * docs/data/owner-link.md "문서"와 계획 014 설계 4절(보안 규칙: 프로필 문서는 pet-profile v1 키만, photoDataUrl == null)의 경계.
 */
describe('프로필 ↔ 서버 문서 경계', () => {
  it('프로필 문서에는 pet-profile v1 키만 들어간다 (화면 상태가 섞여 있어도)', () => {
    const withExtra = { ...exampleProfile(), photoDataUrl: PHOTO, editing: true, id: 'x' } as PetProfile
    withExtra.medical = { ...withExtra.medical, note: '메모' } as PetProfile['medical']
    withExtra.medical.medications = [{ name: '심장약', dosage: '1일 1회', extra: 1 } as PetProfile['medical']['medications'][number]]
    const doc = serverDocsOf(withExtra).profile
    expect(Object.keys(doc).sort()).toEqual(V1_KEYS)
    expect(Object.keys(doc.medical).sort()).toEqual(['allergies', 'conditions', 'medications'])
    expect(Object.keys(doc.medical.medications[0]).sort()).toEqual(['dosage', 'name'])
  })

  it('사진이 있어도 프로필 문서의 사진 자리는 늘 null이다', () => {
    expect(serverDocsOf({ ...exampleProfile(), photoDataUrl: PHOTO }).profile.photoDataUrl).toBeNull()
    expect(serverDocsOf(exampleProfile()).profile.photoDataUrl).toBeNull()
  })

  it('서버 문서로 나눠도 받은 프로필은 바뀌지 않는다', () => {
    const profile = { ...exampleProfile(), photoDataUrl: PHOTO }
    const before = structuredClone(profile)
    const docs = serverDocsOf(profile)
    docs.profile.medical.allergies.push('닭고기')
    expect(profile).toEqual(before)
  })

  it('schemaVersion이 없으면 버전 문제로 읽지 않는다', () => {
    const { schemaVersion: _omit, ...rest } = serverDocsOf(exampleProfile()).profile
    expect(_omit).toBe(1)
    expect(profileFromServerDocs(rest, null)).toEqual({ ok: false, reason: 'unsupported-version' })
  })

  it('updatedAt이 서버 타임스탬프 같은 객체면 손상이다', () => {
    // owner-link.md: 프로필 문서의 updatedAt은 ISO 8601 UTC 문자열이다
    const doc = { ...serverDocsOf(exampleProfile()).profile, updatedAt: { seconds: 1762419600, nanoseconds: 0 } }
    expect(profileFromServerDocs(doc, { photoDataUrl: PHOTO })).toEqual({ ok: false, reason: 'corrupted' })
  })

  it('의료정보가 손상됐으면 사진 문서가 맞아도 손상이다', () => {
    const doc = { ...serverDocsOf(exampleProfile()).profile, medical: { conditions: [], allergies: 'x', medications: [] } }
    expect(profileFromServerDocs(doc, { photoDataUrl: PHOTO })).toEqual({ ok: false, reason: 'corrupted' })
  })

  it('사진 문서가 객체가 아니거나 비었으면 사진 없이 읽는다', () => {
    const doc = serverDocsOf(exampleProfile()).profile
    for (const photo of [undefined, 'data:image/jpeg;base64,AA', [], {}, { photoDataUrl: null }]) {
      expect(profileFromServerDocs(doc, photo), JSON.stringify(photo)).toEqual({ ok: true, profile: exampleProfile() })
    }
  })

  it('사진 문서의 사진은 다시 합친 프로필에 그대로 들어간다', () => {
    const doc = serverDocsOf(exampleProfile()).profile
    const result = profileFromServerDocs(doc, { photoDataUrl: PHOTO })
    expect(result).toEqual({ ok: true, profile: { ...exampleProfile(), photoDataUrl: PHOTO } })
  })
})
