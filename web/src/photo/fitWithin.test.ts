import { describe, expect, it } from 'vitest'
import { fitWithin } from './fitWithin'

describe('사진 크기 맞추기', () => {
  it('가로가 긴 사진은 가로를 최대 길이에 맞춘다', () => {
    expect(fitWithin(2000, 1500, 512)).toEqual({ width: 512, height: 384 })
  })

  it('세로가 긴 사진은 세로를 최대 길이에 맞춘다', () => {
    expect(fitWithin(1500, 2000, 512)).toEqual({ width: 384, height: 512 })
  })

  it('이미 작은 사진은 키우지 않는다', () => {
    expect(fitWithin(300, 200, 512)).toEqual({ width: 300, height: 200 })
    expect(fitWithin(512, 100, 512)).toEqual({ width: 512, height: 100 })
  })

  it('정사각형은 두 변 모두 최대 길이다', () => {
    expect(fitWithin(4000, 4000, 512)).toEqual({ width: 512, height: 512 })
  })

  it('줄인 크기는 정수이고 1px보다 작아지지 않는다', () => {
    const { width, height } = fitWithin(10000, 7, 512)
    expect(width).toBe(512)
    expect(height).toBe(1)
    const odd = fitWithin(1999, 1333, 512)
    expect(Number.isInteger(odd.width) && Number.isInteger(odd.height)).toBe(true)
  })
})
