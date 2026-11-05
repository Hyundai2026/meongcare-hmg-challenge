import { useState, type FormEvent, type ReactNode } from 'react'
import { earliestBirthDate } from '../domain/age'
import { formatLocalDate, type LocalDate } from '../domain/localDate'
import type { PetProfile } from '../domain/petProfile'
import type { SaveFailure } from '../domain/petProfileRepository'
import { SaveErrorBanner } from './SaveErrorBanner'
import {
  applyProfileForm,
  PROFILE_FIELD_ORDER,
  profileFormValuesOf,
  type ProfileField,
  type ProfileFieldError,
  type ProfileFormErrors,
  type ProfileFormValues,
} from './profileFormValues'

const FIELD_ERROR_MESSAGES: Record<ProfileFieldError, string> = {
  'name-required': '이름을 입력해 주세요',
  'name-too-long': '20자까지 쓸 수 있어요',
  'breed-too-long': '30자까지 쓸 수 있어요',
  'birth-date-invalid': '날짜를 다시 확인해 주세요',
  'birth-date-future': '오늘 이후 날짜는 고를 수 없어요',
  'birth-date-too-old': '30년 안쪽 날짜를 골라 주세요',
  'age-invalid': '0~30세, 0~11개월로 입력해 주세요',
  'age-too-old': '나이는 30세까지 입력할 수 있어요',
}

/** 오류가 나면 포커스할 칸의 id */
const FIELD_INPUT_ID: Record<ProfileField, (values: ProfileFormValues) => string> = {
  name: () => 'pf-name',
  breed: () => 'pf-breed',
  birth: (values) => (values.birthMode === 'date' ? 'pf-birth-date' : 'pf-age-years'),
}

interface Props {
  initial: PetProfile | null
  today: LocalDate
  now: () => Date
  saving: boolean
  saveError: SaveFailure | null
  /** 사진 칸 (단계 7). 값과 바꾸는 함수를 받는다 */
  renderPhoto?: (photoDataUrl: string | null, name: string, onChange: (photo: string | null) => void) => ReactNode
  onSave: (profile: PetProfile) => void
  onCancel: () => void
}

export function ProfileForm({ initial, today, now, saving, saveError, renderPhoto, onSave, onCancel }: Props) {
  const [values, setValues] = useState(() => profileFormValuesOf(initial, today))
  const [errors, setErrors] = useState<ProfileFormErrors>({})

  const update = (patch: Partial<ProfileFormValues>, field?: ProfileField) => {
    setValues((v) => ({ ...v, ...patch }))
    if (field !== undefined && errors[field] !== undefined) setErrors((e) => ({ ...e, [field]: undefined }))
  }

  const submit = (event: FormEvent) => {
    event.preventDefault()
    if (saving) return
    const result = applyProfileForm(values, initial, today, now())
    if (!result.ok) {
      setErrors(result.errors)
      const first = PROFILE_FIELD_ORDER.find((f) => result.errors[f] !== undefined)
      if (first !== undefined) {
        const input = document.getElementById(FIELD_INPUT_ID[first](values))
        input?.scrollIntoView({ block: 'center' })
        input?.focus({ preventScroll: true })
      }
      return
    }
    setErrors({})
    onSave(result.profile)
  }

  return (
    <form className="form card" onSubmit={submit} noValidate>
      <h1 className="form__title">{initial === null ? '반려견 등록' : '프로필 수정'}</h1>
      {saveError !== null && <SaveErrorBanner reason={saveError} />}

      {renderPhoto?.(values.photoDataUrl, values.name, (photoDataUrl) => update({ photoDataUrl }))}

      <Field id="pf-name" label="이름" required error={errors.name}>
        <input
          id="pf-name"
          className="input"
          type="text"
          autoComplete="off"
          value={values.name}
          aria-invalid={errors.name !== undefined}
          aria-describedby={errors.name ? 'pf-name-error' : undefined}
          onChange={(e) => update({ name: e.target.value }, 'name')}
        />
      </Field>

      <Field id="pf-breed" label="품종" error={errors.breed}>
        <input
          id="pf-breed"
          className="input"
          type="text"
          autoComplete="off"
          placeholder="예: 골든리트리버"
          value={values.breed}
          aria-invalid={errors.breed !== undefined}
          aria-describedby={errors.breed ? 'pf-breed-error' : undefined}
          onChange={(e) => update({ breed: e.target.value }, 'breed')}
        />
      </Field>

      <fieldset className="field">
        <legend className="field__label">생일</legend>
        <div className="segmented">
          <Segment name="pf-birth-mode" label="날짜로 입력" checked={values.birthMode === 'date'} onSelect={() => update({ birthMode: 'date' }, 'birth')} />
          <Segment name="pf-birth-mode" label="나이로 입력" checked={values.birthMode === 'age'} onSelect={() => update({ birthMode: 'age' }, 'birth')} />
        </div>
        {values.birthMode === 'date' ? (
          <>
            <label className="visually-hidden" htmlFor="pf-birth-date">
              생년월일
            </label>
            <input
              id="pf-birth-date"
              className="input"
              type="date"
              min={formatLocalDate(earliestBirthDate(today))}
              max={formatLocalDate(today)}
              value={values.birthDate}
              aria-invalid={errors.birth !== undefined}
              aria-describedby={errors.birth ? 'pf-birth-error' : 'pf-birth-hint'}
              onChange={(e) => update({ birthDate: e.target.value }, 'birth')}
            />
            <p className="field__hint" id="pf-birth-hint">
              모르면 비워 두거나 나이로 입력하세요.
            </p>
          </>
        ) : (
          <div className="age-inputs">
            <AgeInput id="pf-age-years" label="나이(세)" suffix="세" value={values.ageYears} invalid={errors.birth !== undefined} onChange={(ageYears) => update({ ageYears }, 'birth')} />
            <AgeInput id="pf-age-months" label="나이(개월)" suffix="개월" value={values.ageMonths} invalid={errors.birth !== undefined} onChange={(ageMonths) => update({ ageMonths }, 'birth')} />
          </div>
        )}
        {errors.birth !== undefined && (
          <p className="field__error" id="pf-birth-error">
            {FIELD_ERROR_MESSAGES[errors.birth]}
          </p>
        )}
      </fieldset>

      <fieldset className="field">
        <legend className="field__label">성별</legend>
        <div className="segmented">
          <Segment name="pf-sex" label="수컷" checked={values.sex === 'male'} onSelect={() => update({ sex: 'male' })} />
          <Segment name="pf-sex" label="암컷" checked={values.sex === 'female'} onSelect={() => update({ sex: 'female' })} />
          <Segment name="pf-sex" label="선택 안 함" checked={values.sex === ''} onSelect={() => update({ sex: '' })} />
        </div>
      </fieldset>

      <div className="form__actions">
        <button type="button" className="button button--secondary" onClick={onCancel} disabled={saving}>
          취소
        </button>
        <button type="submit" className="button button--primary" disabled={saving}>
          {saving ? '저장 중…' : '저장'}
        </button>
      </div>
    </form>
  )
}

function Field({ id, label, required, error, children }: { id: string; label: string; required?: boolean; error?: ProfileFieldError; children: ReactNode }) {
  return (
    <div className="field">
      <label className="field__label" htmlFor={id}>
        {label}
        {required && <span className="field__required"> (필수)</span>}
      </label>
      {children}
      {error !== undefined && (
        <p className="field__error" id={`${id}-error`}>
          {FIELD_ERROR_MESSAGES[error]}
        </p>
      )}
    </div>
  )
}

function Segment({ name, label, checked, onSelect }: { name: string; label: string; checked: boolean; onSelect: () => void }) {
  return (
    <label className="segmented__option">
      <input type="radio" name={name} checked={checked} onChange={onSelect} />
      <span>{label}</span>
    </label>
  )
}

function AgeInput({ id, label, suffix, value, invalid, onChange }: { id: string; label: string; suffix: string; value: string; invalid: boolean; onChange: (value: string) => void }) {
  return (
    <div className="age-input">
      <label className="visually-hidden" htmlFor={id}>
        {label}
      </label>
      <input
        id={id}
        className="input age-input__field"
        type="text"
        inputMode="numeric"
        autoComplete="off"
        value={value}
        aria-invalid={invalid}
        onChange={(e) => onChange(e.target.value)}
      />
      <span className="age-input__suffix" aria-hidden="true">
        {suffix}
      </span>
    </div>
  )
}
