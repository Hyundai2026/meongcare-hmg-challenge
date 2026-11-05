/**
 * 프로필 JSON 경계 테스트. 기준: docs/data/pet-profile.md "필드", "읽는 쪽 규칙", "버전 규칙".
 * 기본 케이스는 petProfileJson.test.ts에 있다.
 */
import { describe, expect, it } from 'vitest'
import example from '../../../docs/data/pet-profile.example.json'
import type { PetProfile } from './petProfile'
import { parsePetProfile, serializePetProfile } from './petProfileJson'

const bori: PetProfile = {
  schemaVersion: 1,
  name: '보리',
  breed: '골든리트리버',
  sex: 'male',
  birthDate: '2014-08-01',
  birthDateIsEstimated: false,
  photoDataUrl: 'data:image/jpeg;base64,AAAA',
  medical: {
    conditions: ['심장질환'],
    allergies: ['특정 단백질 알레르기'],
    medications: [{ name: '심장약', dosage: '1일 1회' }],
  },
  updatedAt: '2026-11-05T00:00:00.000Z',
}

type Json = Record<string, unknown>

function boriJson(): Json {
  return JSON.parse(serializePetProfile(bori)) as Json
}

function medicalOf(json: Json): Json {
  return json.medical as Json
}

function variant(change: (json: Json) => void): string {
  const json = boriJson()
  change(json)
  return JSON.stringify(json)
}

const CORRUPTED = { ok: false, reason: 'corrupted' }

describe('프로필 JSON 버전 경계', () => {
  it('v2 문서는 v1 필드가 없어도 손상이 아니라 버전 문제로 읽지 않는다', () => {
    const v2 = JSON.stringify({ schemaVersion: 2, id: 'pet-1', name: '보리', photoUrl: 'https://example.com/a.jpg' })
    expect(parsePetProfile(v2)).toEqual({ ok: false, reason: 'unsupported-version' })
  })

  it('schemaVersion이 1.5, true, [1], {}이면 버전 문제다', () => {
    for (const version of [1.5, true, [1], {}, -1]) {
      expect(parsePetProfile(variant((j) => (j.schemaVersion = version))), JSON.stringify(version)).toEqual({
        ok: false,
        reason: 'unsupported-version',
      })
    }
  })

  it('JSON에 1.0으로 적힌 schemaVersion은 1이다', () => {
    const raw = serializePetProfile(bori).replace('"schemaVersion":1', '"schemaVersion":1.0')
    expect(raw).toContain('"schemaVersion":1.0')
    expect(parsePetProfile(raw)).toEqual({ ok: true, profile: bori })
  })
})

describe('프로필 JSON 필드 경계', () => {
  it('어느 필드든 빠지면 손상이다 (null을 허용하는 필드도 키는 있어야 한다)', () => {
    for (const key of ['name', 'breed', 'sex', 'birthDate', 'birthDateIsEstimated', 'photoDataUrl', 'medical', 'updatedAt']) {
      expect(parsePetProfile(variant((j) => delete j[key])), key).toEqual(CORRUPTED)
    }
    for (const key of ['conditions', 'allergies', 'medications']) {
      expect(parsePetProfile(variant((j) => delete medicalOf(j)[key])), `medical.${key}`).toEqual(CORRUPTED)
    }
  })

  it('null을 허용하지 않는 필드가 null이면 손상이다', () => {
    const breaks: Array<[string, (j: Json) => void]> = [
      ['name', (j) => (j.name = null)],
      ['birthDateIsEstimated', (j) => (j.birthDateIsEstimated = null)],
      ['medical', (j) => (j.medical = null)],
      ['updatedAt', (j) => (j.updatedAt = null)],
      ['medical.conditions', (j) => (medicalOf(j).conditions = null)],
      ['medical.allergies', (j) => (medicalOf(j).allergies = null)],
      ['medical.medications', (j) => (medicalOf(j).medications = null)],
      ['medical.conditions[0]', (j) => (medicalOf(j).conditions = [null])],
      ['medical.medications[0]', (j) => (medicalOf(j).medications = [null])],
      ['medical.medications[0].name', (j) => (medicalOf(j).medications = [{ name: null, dosage: '1일 1회' }])],
      ['medical.medications[0].dosage', (j) => (medicalOf(j).medications = [{ name: '심장약', dosage: null }])],
    ]
    for (const [label, change] of breaks) {
      expect(parsePetProfile(variant(change)), label).toEqual(CORRUPTED)
    }
  })

  it('null을 허용하는 필드는 모두 null이어도 읽는다', () => {
    const minimal: PetProfile = {
      ...bori,
      breed: null,
      sex: null,
      birthDate: null,
      photoDataUrl: null,
      medical: { conditions: [], allergies: [], medications: [] },
    }
    expect(parsePetProfile(serializePetProfile(minimal))).toEqual({ ok: true, profile: minimal })
  })

  it('타입이 다른 값은 손상이다: 배열 자리에 객체, 객체 자리에 배열, 불리언 자리에 숫자', () => {
    const breaks: Array<[string, (j: Json) => void]> = [
      ['medical이 배열', (j) => (j.medical = [])],
      ['conditions가 객체', (j) => (medicalOf(j).conditions = { 0: '심장질환' })],
      ['medications 항목이 문자열', (j) => (medicalOf(j).medications = ['심장약'])],
      ['medications 항목이 배열', (j) => (medicalOf(j).medications = [['심장약', '1일 1회']])],
      ['dosage가 숫자', (j) => (medicalOf(j).medications = [{ name: '심장약', dosage: 1 }])],
      ['birthDateIsEstimated가 0', (j) => (j.birthDateIsEstimated = 0)],
      ['sex가 불리언', (j) => (j.sex = true)],
      ['photoDataUrl이 숫자', (j) => (j.photoDataUrl = 0)],
      ['updatedAt이 숫자', (j) => (j.updatedAt = 1793836800000)],
    ]
    for (const [label, change] of breaks) {
      expect(parsePetProfile(variant(change)), label).toEqual(CORRUPTED)
    }
  })

  it('성별은 소문자 male과 female만 받는다', () => {
    for (const sex of ['Male', 'FEMALE', 'm', '수컷', '']) {
      expect(parsePetProfile(variant((j) => (j.sex = sex))), sex).toEqual(CORRUPTED)
    }
    expect(parsePetProfile(variant((j) => (j.sex = 'female'))).ok).toBe(true)
  })

  it('birthDate의 윤년·말일·공백 경계', () => {
    for (const birthDate of ['2000-02-29', '2024-02-29']) {
      expect(parsePetProfile(variant((j) => (j.birthDate = birthDate))).ok, birthDate).toBe(true)
    }
    for (const birthDate of ['2100-02-29', '2026-04-31', '2026-00-01', '2026-13-01', '2026-01-00', ' 2026-01-01', '2026-01-01\n', '']) {
      expect(parsePetProfile(variant((j) => (j.birthDate = birthDate))), JSON.stringify(birthDate)).toEqual(CORRUPTED)
    }
  })

  it('photoDataUrl은 정확히 data:image/jpeg;base64, 로 시작해야 한다', () => {
    for (const photo of ['data:image/png;base64,AAAA', '', ' data:image/jpeg;base64,AAAA', 'data:image/jpeg,AAAA']) {
      expect(parsePetProfile(variant((j) => (j.photoDataUrl = photo))), JSON.stringify(photo)).toEqual(CORRUPTED)
    }
  })

  it('updatedAt이 ISO 8601 시각이 아니면 손상이다', () => {
    // 형식 문서: updatedAt은 "문자열 (ISO 8601, UTC)". 차 앱(Kotlin Instant)도 같은 값을 읽는다.
    const notIso = ['1', '보리 1', '2026/11/05 09:00', 'Thu Nov 05 2026', 'November 5, 2026', '']
    const readAnyway = notIso.filter((updatedAt) => parsePetProfile(variant((j) => (j.updatedAt = updatedAt))).ok)
    expect(readAnyway, '손상이 아니라 읽어 버린 updatedAt 값').toEqual([])
  })

  it('updatedAt이 없는 날짜(2월 30일)면 손상이다', () => {
    expect(parsePetProfile(variant((j) => (j.updatedAt = '2026-02-30T00:00:00.000Z')))).toEqual(CORRUPTED)
  })

  it('ISO 8601 UTC 시각은 밀리초가 있든 없든 읽는다', () => {
    for (const updatedAt of ['2026-11-05T00:00:00.000Z', '2026-11-05T00:00:00Z']) {
      expect(parsePetProfile(variant((j) => (j.updatedAt = updatedAt))), updatedAt).toEqual({
        ok: true,
        profile: { ...bori, updatedAt },
      })
    }
  })
})

describe('읽는 쪽은 타입과 형식만 확인한다', () => {
  it('글자 수·개수 제한을 넘은 값도 형식이 맞으면 읽는다', () => {
    const overflow: PetProfile = {
      ...bori,
      name: '가'.repeat(21),
      breed: '나'.repeat(31),
      photoDataUrl: 'data:image/jpeg;base64,' + 'A'.repeat(400_001 - 'data:image/jpeg;base64,'.length),
      medical: {
        conditions: Array.from({ length: 21 }, (_, i) => `질환${i}`),
        allergies: ['다'.repeat(51)],
        medications: [{ name: '라'.repeat(51), dosage: '마'.repeat(51) }],
      },
    }
    expect(overflow.photoDataUrl).toHaveLength(400_001)
    expect(parsePetProfile(serializePetProfile(overflow))).toEqual({ ok: true, profile: overflow })
  })

  it('미래 생년월일, 30년보다 앞선 생년월일, null 생년월일의 추정 표시도 형식이 맞으면 읽는다', () => {
    const patches: Array<Partial<PetProfile>> = [
      { birthDate: '2099-01-01' },
      { birthDate: '1900-01-01' },
      { birthDate: null, birthDateIsEstimated: true },
    ]
    for (const patch of patches) {
      const profile: PetProfile = { ...bori, ...patch }
      expect(parsePetProfile(serializePetProfile(profile)), JSON.stringify(patch)).toEqual({ ok: true, profile })
    }
  })

  it('빈 품종 문자열과 빈 항목 문자열도 타입이 맞으면 읽는다', () => {
    const profile: PetProfile = { ...bori, breed: '', medical: { conditions: [''], allergies: [], medications: [{ name: '', dosage: '' }] } }
    expect(parsePetProfile(serializePetProfile(profile))).toEqual({ ok: true, profile })
  })

  it('약 항목과 의료정보 안의 모르는 필드도 무시하고 읽는다', () => {
    const raw = variant((j) => {
      medicalOf(j).medications = [{ name: '심장약', dosage: '1일 1회', timesPerDay: 1 }]
      medicalOf(j).confirmedNoAllergy = true
    })
    expect(parsePetProfile(raw)).toEqual({ ok: true, profile: bori })
  })
})

describe('쓰는 쪽 형식', () => {
  it('쓴 JSON의 필드 이름은 형식 문서 예시와 같고 null 필드도 빠뜨리지 않는다', () => {
    const minimal: PetProfile = { ...bori, breed: null, sex: null, birthDate: null, photoDataUrl: null }
    const json = JSON.parse(serializePetProfile(minimal)) as Json
    expect(Object.keys(json).sort()).toEqual(Object.keys(example).sort())
    expect(Object.keys(medicalOf(json)).sort()).toEqual(Object.keys(example.medical).sort())
    expect(json.breed).toBeNull()
    expect(json.sex).toBeNull()
    expect(json.birthDate).toBeNull()
    expect(json.photoDataUrl).toBeNull()
    expect(json.schemaVersion).toBe(1)
  })

  it('프로필 객체에 붙은 다른 값은 쓰지 않는다', () => {
    const withExtra = { ...bori, weightKg: 30, medical: { ...bori.medical, surgeries: ['슬개골'] } } as PetProfile
    const json = JSON.parse(serializePetProfile(withExtra)) as Json
    expect(json).not.toHaveProperty('weightKg')
    expect(medicalOf(json)).not.toHaveProperty('surgeries')
  })

  it('따옴표·역슬래시·줄바꿈·이모지가 든 값도 쓴 뒤 그대로 읽는다', () => {
    const tricky: PetProfile = {
      ...bori,
      name: '보리🐶 "멍"',
      breed: '골든\\리트리버\n믹스',
      medical: {
        conditions: ['심장질환 </script>'],
        allergies: ['닭고기, 소고기'],
        medications: [{ name: '심장약\t', dosage: '1일 1회 (아침)' }],
      },
    }
    expect(parsePetProfile(serializePetProfile(tricky))).toEqual({ ok: true, profile: tricky })
  })
})
