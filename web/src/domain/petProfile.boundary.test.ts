/**
 * 형식 상수와 글자 수 규칙. 기준: docs/data/pet-profile.md "필드" 표와 그 아래 글자 수·정규화 규칙.
 */
import { describe, expect, it } from 'vitest'
import { charCount, emptyMedicalInfo, isMedicalInfoEmpty, LIMITS, normalizeText, PET_PROFILE_SCHEMA_VERSION, PHOTO_DATA_URL_PREFIX } from './petProfile'

describe('형식 상수', () => {
  it('제한 값이 형식 문서와 같다', () => {
    expect(PET_PROFILE_SCHEMA_VERSION).toBe(1)
    expect(LIMITS).toEqual({
      nameChars: 20,
      breedChars: 30,
      medicalItemChars: 50,
      dosageChars: 50,
      medicalItemCount: 20,
      photoDataUrlChars: 400_000,
      maxAgeYears: 30,
    })
    expect(PHOTO_DATA_URL_PREFIX).toBe('data:image/jpeg;base64,')
  })
})

describe('글자 수와 정규화', () => {
  it('글자 수는 코드 포인트 수라 이모지 하나가 한 글자다', () => {
    expect('🐶'.length).toBe(2)
    expect(charCount('🐶')).toBe(1)
    expect(charCount('보리🐶')).toBe(3)
  })

  it('피부색이 붙은 이모지는 코드 포인트대로 두 글자다 (Kotlin codePointCount와 같다)', () => {
    expect(charCount('👍🏽')).toBe(2)
  })

  it('풀어 쓴 한글은 NFC로 합치면 음절 수가 글자 수다', () => {
    const decomposed = '보리'.normalize('NFD')
    expect(charCount(decomposed)).toBe(4)
    expect(normalizeText(decomposed)).toBe('보리')
    expect(charCount(normalizeText(decomposed))).toBe(2)
  })

  it('앞뒤의 공백·탭·줄바꿈·전각 공백을 지우고 가운데 공백은 둔다', () => {
    expect(normalizeText(' \t\n　특정 단백질 알레르기　\n ')).toBe('특정 단백질 알레르기')
    expect(normalizeText(' \t\n　 ')).toBe('')
  })

  it('세 묶음이 모두 비어야 의료정보가 비었다', () => {
    expect(isMedicalInfoEmpty(emptyMedicalInfo())).toBe(true)
    expect(isMedicalInfoEmpty({ ...emptyMedicalInfo(), conditions: ['심장질환'] })).toBe(false)
    expect(isMedicalInfoEmpty({ ...emptyMedicalInfo(), allergies: ['닭고기'] })).toBe(false)
    expect(isMedicalInfoEmpty({ ...emptyMedicalInfo(), medications: [{ name: '심장약', dosage: '' }] })).toBe(false)
  })

  it('빈 의료정보는 부를 때마다 새 배열이다', () => {
    const a = emptyMedicalInfo()
    a.conditions.push('심장질환')
    expect(emptyMedicalInfo().conditions).toEqual([])
  })
})
