/**
 * 고른 사진 → 긴 변 512px JPEG data URL. 브라우저 전용 (캔버스).
 * - 캔버스로 다시 그리므로 원본의 EXIF(촬영 위치 등)는 들어가지 않는다.
 * - 투명한 PNG가 검게 나오지 않게 흰색을 먼저 칠한다.
 */
import { LIMITS, PHOTO_DATA_URL_PREFIX } from '../domain/petProfile'
import { fitWithin } from './fitWithin'

export const PHOTO_MAX_EDGE = 512
const MAX_FILE_BYTES = 20 * 1024 * 1024
const JPEG_QUALITY = 0.8

export type PhotoFailure = 'too-large-file' | 'unreadable' | 'too-large-result'
export type PhotoResult = { ok: true; dataUrl: string } | { ok: false; reason: PhotoFailure }

export async function resizePhoto(file: File): Promise<PhotoResult> {
  // 아주 큰 파일은 폰 브라우저가 디코딩하다 멈출 수 있어 먼저 거른다.
  if (file.size > MAX_FILE_BYTES) return { ok: false, reason: 'too-large-file' }

  const url = URL.createObjectURL(file)
  try {
    const image = new Image()
    image.src = url
    try {
      await image.decode()
    } catch {
      return { ok: false, reason: 'unreadable' }
    }
    if (image.naturalWidth === 0 || image.naturalHeight === 0) return { ok: false, reason: 'unreadable' }

    const { width, height } = fitWithin(image.naturalWidth, image.naturalHeight, PHOTO_MAX_EDGE)
    const canvas = document.createElement('canvas')
    canvas.width = width
    canvas.height = height
    const context = canvas.getContext('2d')
    if (context === null) return { ok: false, reason: 'unreadable' }
    context.fillStyle = '#ffffff'
    context.fillRect(0, 0, width, height)
    context.imageSmoothingQuality = 'high'
    context.drawImage(image, 0, 0, width, height)

    const dataUrl = canvas.toDataURL('image/jpeg', JPEG_QUALITY)
    if (!dataUrl.startsWith(PHOTO_DATA_URL_PREFIX) || dataUrl.length > LIMITS.photoDataUrlChars) {
      return { ok: false, reason: 'too-large-result' }
    }
    return { ok: true, dataUrl }
  } finally {
    URL.revokeObjectURL(url)
  }
}
