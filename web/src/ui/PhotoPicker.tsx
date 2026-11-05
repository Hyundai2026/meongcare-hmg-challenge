import { useState, type ChangeEvent } from 'react'
import { resizePhoto, type PhotoFailure } from '../photo/resizePhoto'
import { ProfilePhoto } from './ProfilePhoto'

const PHOTO_FAILURE_MESSAGES: Record<PhotoFailure, string> = {
  'too-large-file': '20MB보다 작은 사진을 골라 주세요',
  unreadable: '이 사진은 열 수 없어요. 다른 사진을 골라 주세요',
  'too-large-result': '사진을 줄이지 못했어요. 다른 사진을 골라 주세요',
}

interface Props {
  photoDataUrl: string | null
  name: string
  onChange: (photoDataUrl: string | null) => void
}

/** 사진 고르기. 고르면 바로 줄여서 미리 보여 준다. 실패해도 이전 사진은 그대로다. */
export function PhotoPicker({ photoDataUrl, name, onChange }: Props) {
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<PhotoFailure | null>(null)

  const pick = async (event: ChangeEvent<HTMLInputElement>) => {
    const input = event.currentTarget
    const file = input.files?.[0]
    input.value = '' // 같은 파일을 다시 골라도 change가 오게
    if (file === undefined) return
    setBusy(true)
    setError(null)
    const result = await resizePhoto(file)
    setBusy(false)
    if (result.ok) onChange(result.dataUrl)
    else setError(result.reason)
  }

  return (
    <div className="field">
      <span className="field__label">사진</span>
      <div className="photo-picker">
        <ProfilePhoto photoDataUrl={photoDataUrl} name={name.trim() || '?'} className="photo-picker__preview" />
        <div className="photo-picker__actions">
          <label className="button button--secondary photo-picker__choose">
            {busy ? '사진을 줄이는 중…' : '사진 선택'}
            <input className="visually-hidden" type="file" accept="image/*" disabled={busy} onChange={pick} aria-label="사진 선택" />
          </label>
          {photoDataUrl !== null && (
            <button type="button" className="button button--text" onClick={() => onChange(null)} disabled={busy}>
              사진 빼기
            </button>
          )}
        </div>
      </div>
      {error !== null && (
        <p className="field__error" role="alert">
          {PHOTO_FAILURE_MESSAGES[error]}
        </p>
      )}
    </div>
  )
}
