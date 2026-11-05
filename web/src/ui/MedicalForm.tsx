import { useState, type FormEvent, type ReactNode } from 'react'
import { isMedicalInfoEmpty, LIMITS, type PetProfile } from '../domain/petProfile'
import type { SaveFailure } from '../domain/petProfileRepository'
import {
  applyMedicalForm,
  MEDICAL_GROUPS,
  medicalFormValuesOf,
  rowKey,
  type MedicalFormErrors,
  type MedicalFormValues,
  type MedicalGroup,
  type MedicalRowError,
} from './medicalFormValues'
import { SaveErrorBanner } from './SaveErrorBanner'

const ROW_ERROR_MESSAGES: Record<MedicalRowError, string> = {
  'too-long': '50자까지 쓸 수 있어요',
  'dosage-too-long': '복용법은 50자까지 쓸 수 있어요',
  'medication-name-required': '약 이름을 입력해 주세요',
}

const GROUP_TEXT: Record<MedicalGroup, { title: string; label: string; placeholder: string; add: string }> = {
  conditions: { title: '주요 질환', label: '주요 질환', placeholder: '예: 심장질환', add: '질환 추가' },
  allergies: { title: '알레르기', label: '알레르기', placeholder: '예: 닭고기', add: '알레르기 추가' },
  medications: { title: '복용 중인 약', label: '약 이름', placeholder: '약 이름', add: '약 추가' },
}

const EMPTY_ERRORS: MedicalFormErrors = { rows: {}, tooMany: [] }

const inputId = (group: MedicalGroup, index: number) => `mf-${group}-${index}`

interface Props {
  profile: PetProfile
  now: () => Date
  saving: boolean
  saveError: SaveFailure | null
  onSave: (profile: PetProfile) => void
  onCancel: () => void
}

export function MedicalForm({ profile, now, saving, saveError, onSave, onCancel }: Props) {
  const [values, setValues] = useState(() => medicalFormValuesOf(profile.medical))
  const [errors, setErrors] = useState<MedicalFormErrors>(EMPTY_ERRORS)

  const change = (next: MedicalFormValues) => {
    setValues(next)
    setErrors(EMPTY_ERRORS)
  }

  const submit = (event: FormEvent) => {
    event.preventDefault()
    if (saving) return
    const result = applyMedicalForm(values, profile, now())
    if (!result.ok) {
      setErrors(result.errors)
      const firstRow = Object.keys(result.errors.rows)[0]
      if (firstRow !== undefined) {
        const [group, index] = firstRow.split('.')
        const input = document.getElementById(inputId(group as MedicalGroup, Number(index)))
        input?.scrollIntoView({ block: 'center' })
        input?.focus({ preventScroll: true })
      }
      return
    }
    onSave(result.profile)
  }

  return (
    <form className="form card" onSubmit={submit} noValidate>
      <h1 className="form__title">{isMedicalInfoEmpty(profile.medical) ? '의료정보 추가' : '의료정보 수정'}</h1>
      <p className="form__lead">병원에서 수의사에게 보여 줄 정보예요. 모르는 칸은 비워 두세요.</p>
      {saveError !== null && <SaveErrorBanner reason={saveError} />}

      {MEDICAL_GROUPS.map((group) => (
        <fieldset className="field medical-group" key={group}>
          <legend className="field__label">{GROUP_TEXT[group].title}</legend>
          {group === 'medications'
            ? values.medications.map((row, index) => (
                <Row key={index} error={errors.rows[rowKey(group, index)]} onRemove={() => change({ ...values, medications: values.medications.filter((_, i) => i !== index) })} removeLabel={`약 ${index + 1} 삭제`}>
                  <div className="medication-inputs">
                    <TextInput id={inputId(group, index)} label={`약 이름 ${index + 1}`} placeholder="약 이름" value={row.name} invalid={errors.rows[rowKey(group, index)] !== undefined} onChange={(name) => change({ ...values, medications: values.medications.map((m, i) => (i === index ? { ...m, name } : m)) })} />
                    <TextInput id={`${inputId(group, index)}-dosage`} label={`복용법 ${index + 1}`} placeholder="예: 1일 1회" value={row.dosage} invalid={errors.rows[rowKey(group, index)] === 'dosage-too-long'} onChange={(dosage) => change({ ...values, medications: values.medications.map((m, i) => (i === index ? { ...m, dosage } : m)) })} />
                  </div>
                </Row>
              ))
            : values[group].map((text, index) => (
                <Row key={index} error={errors.rows[rowKey(group, index)]} onRemove={() => change({ ...values, [group]: values[group].filter((_, i) => i !== index) })} removeLabel={`${GROUP_TEXT[group].label} ${index + 1} 삭제`}>
                  <TextInput id={inputId(group, index)} label={`${GROUP_TEXT[group].label} ${index + 1}`} placeholder={GROUP_TEXT[group].placeholder} value={text} invalid={errors.rows[rowKey(group, index)] !== undefined} onChange={(next) => change({ ...values, [group]: values[group].map((t, i) => (i === index ? next : t)) })} />
                </Row>
              ))}
          {errors.tooMany.includes(group) && <p className="field__error">{LIMITS.medicalItemCount}개까지 등록할 수 있어요</p>}
          {values[group].length < LIMITS.medicalItemCount ? (
            <button
              type="button"
              className="button button--text medical-group__add"
              onClick={() =>
                change(
                  group === 'medications'
                    ? { ...values, medications: [...values.medications, { name: '', dosage: '' }] }
                    : { ...values, [group]: [...values[group], ''] },
                )
              }
            >
              + {GROUP_TEXT[group].add}
            </button>
          ) : (
            <p className="field__hint">{LIMITS.medicalItemCount}개까지 등록할 수 있어요</p>
          )}
        </fieldset>
      ))}

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

function Row({ children, error, removeLabel, onRemove }: { children: ReactNode; error?: MedicalRowError; removeLabel: string; onRemove: () => void }) {
  return (
    <div className="medical-row">
      <div className="medical-row__line">
        {children}
        <button type="button" className="button button--icon" aria-label={removeLabel} onClick={onRemove}>
          <svg viewBox="0 0 20 20" aria-hidden="true">
            <path d="M5 5l10 10M15 5L5 15" />
          </svg>
        </button>
      </div>
      {error !== undefined && <p className="field__error">{ROW_ERROR_MESSAGES[error]}</p>}
    </div>
  )
}

function TextInput({ id, label, placeholder, value, invalid, onChange }: { id: string; label: string; placeholder: string; value: string; invalid: boolean; onChange: (value: string) => void }) {
  return (
    <>
      <label className="visually-hidden" htmlFor={id}>
        {label}
      </label>
      <input id={id} className="input" type="text" autoComplete="off" placeholder={placeholder} value={value} aria-invalid={invalid} onChange={(e) => onChange(e.target.value)} />
    </>
  )
}
