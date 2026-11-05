import { describe, expect, it } from 'vitest'
import example from '../../../docs/data/pet-profile.example.json'
import type { PetProfile } from './petProfile'
import { parsePetProfile, petProfileFromJson, serializePetProfile } from './petProfileJson'

const bori: PetProfile = {
  schemaVersion: 1,
  name: '보리',
  breed: '골든리트리버',
  sex: 'male',
  birthDate: '2014-08-01',
  birthDateIsEstimated: false,
  photoDataUrl: null,
  medical: {
    conditions: ['심장질환'],
    allergies: ['특정 단백질 알레르기'],
    medications: [{ name: '심장약', dosage: '1일 1회' }],
  },
  updatedAt: '2026-11-05T00:00:00.000Z',
}

/** 보리 JSON을 고쳐서 문자열로 */
function variant(change: (json: Record<string, unknown>) => void): string {
  const json = JSON.parse(serializePetProfile(bori)) as Record<string, unknown>
  change(json)
  return JSON.stringify(json)
}

describe('프로필 JSON', () => {
  it('예시 JSON 파일을 읽을 수 있다', () => {
    expect(petProfileFromJson(example)).toEqual({ ok: true, profile: bori })
  })

  it('저장한 프로필을 다시 읽으면 같은 값이다', () => {
    const withPhoto: PetProfile = { ...bori, photoDataUrl: 'data:image/jpeg;base64,AAAA', sex: null, breed: null }
    expect(parsePetProfile(serializePetProfile(bori))).toEqual({ ok: true, profile: bori })
    expect(parsePetProfile(serializePetProfile(withPhoto))).toEqual({ ok: true, profile: withPhoto })
  })

  it('schemaVersion이 없거나 1이 아니면 버전 문제로 읽지 않는다', () => {
    for (const version of [undefined, 0, 2, '1', null]) {
      const raw = variant((json) => {
        if (version === undefined) delete json.schemaVersion
        else json.schemaVersion = version
      })
      expect(parsePetProfile(raw), String(version)).toEqual({ ok: false, reason: 'unsupported-version' })
    }
  })

  it('JSON이 깨졌으면 손상으로 읽지 않는다', () => {
    for (const raw of ['{broken', '', 'null', '[]', '"보리"', '42']) {
      expect(parsePetProfile(raw), raw).toEqual({ ok: false, reason: 'corrupted' })
    }
  })

  it('필수 필드가 빠졌거나 타입이 틀리면 손상으로 읽지 않는다', () => {
    const breaks: Array<[string, (json: Record<string, unknown>) => void]> = [
      ['이름 없음', (j) => delete j.name],
      ['빈 이름', (j) => (j.name = '')],
      ['이름이 숫자', (j) => (j.name = 7)],
      ['품종이 숫자', (j) => (j.breed = 3)],
      ['품종 없음', (j) => delete j.breed],
      ['모르는 성별', (j) => (j.sex = 'unknown')],
      ['추정 여부가 문자열', (j) => (j.birthDateIsEstimated = 'false')],
      ['사진이 JPEG data URL이 아님', (j) => (j.photoDataUrl = 'https://example.com/a.jpg')],
      ['갱신 시각을 읽을 수 없음', (j) => (j.updatedAt = '어제')],
      ['의료정보 없음', (j) => delete j.medical],
      ['질환이 배열이 아님', (j) => ((j.medical as Record<string, unknown>).conditions = '심장질환')],
      ['알레르기에 숫자', (j) => ((j.medical as Record<string, unknown>).allergies = [1])],
      ['약에 복용법 없음', (j) => ((j.medical as Record<string, unknown>).medications = [{ name: '심장약' }])],
    ]
    for (const [label, change] of breaks) {
      expect(parsePetProfile(variant(change)), label).toEqual({ ok: false, reason: 'corrupted' })
    }
  })

  it('모르는 필드는 무시하고 읽는다', () => {
    const raw = variant((json) => {
      json.weightKg = 30
      ;(json.medical as Record<string, unknown>).surgeries = ['슬개골']
    })
    expect(parsePetProfile(raw)).toEqual({ ok: true, profile: bori })
    expect(serializePetProfile((parsePetProfile(raw) as { profile: PetProfile }).profile)).not.toContain('weightKg')
  })

  it('birthDate 형식이 YYYY-MM-DD가 아니면 손상으로 읽지 않는다', () => {
    for (const birthDate of ['2014-8-1', '2014-08-01T00:00:00Z', '2023-02-29', 20140801]) {
      expect(parsePetProfile(variant((j) => (j.birthDate = birthDate))), String(birthDate)).toEqual({
        ok: false,
        reason: 'corrupted',
      })
    }
    expect(parsePetProfile(variant((j) => (j.birthDate = null))).ok).toBe(true)
  })
})
