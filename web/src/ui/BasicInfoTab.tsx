import { ageLabel } from '../domain/age'
import type { LocalDate } from '../domain/localDate'
import type { PetProfile } from '../domain/petProfile'
import { InfoTable } from './InfoTable'
import { birthDateText, SEX_TEXT } from './profileText'

export function BasicInfoTab({ profile, today }: { profile: PetProfile; today: LocalDate }) {
  const value = (text: string | null) => (text === null ? [] : [text])
  return (
    <InfoTable
      emptyText="입력 안 함"
      rows={[
        { label: '이름', values: [profile.name] },
        { label: '품종', values: value(profile.breed) },
        { label: '성별', values: value(profile.sex === null ? null : SEX_TEXT[profile.sex]) },
        { label: '생년월일', values: value(birthDateText(profile)) },
        { label: '나이', values: value(ageLabel(profile.birthDate, today)) },
      ]}
    />
  )
}
