/**
 * 사진 크기 경계 테스트. 기준: 계획 008 설계 결정 7 ("fitWithin(w, h, 512)로 크기를 정한다. 키우지 않는다"),
 * docs/data/pet-profile.md (긴 변 512px 이하).
 */
import { describe, expect, it } from 'vitest'
import { fitWithin } from './fitWithin'

describe('사진 크기 경계', () => {
  it('긴 변이 정확히 최대 길이면 그대로다', () => {
    expect(fitWithin(512, 300, 512)).toEqual({ width: 512, height: 300 })
    expect(fitWithin(300, 512, 512)).toEqual({ width: 300, height: 512 })
    expect(fitWithin(512, 512, 512)).toEqual({ width: 512, height: 512 })
  })

  it('긴 변이 최대 길이보다 1px 크면 최대 길이로 줄인다', () => {
    const landscape = fitWithin(513, 100, 512)
    expect(landscape.width).toBe(512)
    expect(landscape.height).toBeGreaterThanOrEqual(99)
    expect(landscape.height).toBeLessThanOrEqual(100)
    const portrait = fitWithin(100, 513, 512)
    expect(portrait.height).toBe(512)
  })

  it('폰 카메라 사진 크기를 비율대로 줄인다', () => {
    expect(fitWithin(4032, 3024, 512)).toEqual({ width: 512, height: 384 })
    expect(fitWithin(3024, 4032, 512)).toEqual({ width: 384, height: 512 })
    expect(fitWithin(4000, 3000, 512)).toEqual({ width: 512, height: 384 })
    expect(fitWithin(1920, 1080, 512)).toEqual({ width: 512, height: 288 })
  })

  it('아주 가늘고 긴 사진도 짧은 변이 1px보다 작아지지 않는다', () => {
    expect(fitWithin(10000, 1, 512)).toEqual({ width: 512, height: 1 })
    expect(fitWithin(1, 10000, 512)).toEqual({ width: 1, height: 512 })
    expect(fitWithin(100000, 3, 512)).toEqual({ width: 512, height: 1 })
  })

  it('1×1 사진은 그대로다', () => {
    expect(fitWithin(1, 1, 512)).toEqual({ width: 1, height: 1 })
  })

  it('최대 길이를 768로 바꿔도 같은 규칙이다', () => {
    expect(fitWithin(2000, 1500, 768)).toEqual({ width: 768, height: 576 })
    expect(fitWithin(700, 500, 768)).toEqual({ width: 700, height: 500 })
  })

  it('여러 크기에서: 정수, 1px 이상, 긴 변은 min(최대, 원래 긴 변), 어느 변도 원본보다 크지 않고 비율은 1px 안쪽', () => {
    const failures: string[] = []
    const sizes = [1, 2, 3, 7, 100, 333, 511, 512, 513, 640, 767, 1000, 1023, 1024, 1500, 2000, 3024, 4032, 9999]
    for (const maxEdge of [512, 768]) {
      for (const w of sizes) {
        for (const h of sizes) {
          const r = fitWithin(w, h, maxEdge)
          const label = `${w}×${h} → ${r.width}×${r.height} (최대 ${maxEdge})`
          const scale = Math.min(1, maxEdge / Math.max(w, h))
          if (!Number.isInteger(r.width) || !Number.isInteger(r.height)) failures.push(`정수 아님: ${label}`)
          else if (r.width < 1 || r.height < 1) failures.push(`1px 미만: ${label}`)
          else if (Math.max(r.width, r.height) !== Math.min(maxEdge, Math.max(w, h))) failures.push(`긴 변: ${label}`)
          else if (r.width > w || r.height > h) failures.push(`키웠다: ${label}`)
          else if (Math.abs(r.width - w * scale) > 1 || Math.abs(r.height - h * scale) > 1) failures.push(`비율: ${label}`)
          else if ((w > h && r.width < r.height) || (w < h && r.width > r.height)) failures.push(`방향 바뀜: ${label}`)
        }
      }
    }
    expect(failures.slice(0, 10)).toEqual([])
  })
})
