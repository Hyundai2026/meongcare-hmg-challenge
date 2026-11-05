import { isMedicalInfoEmpty, type MedicalInfo } from '../domain/petProfile'
import { InfoTable } from './InfoTable'
import { medicationText } from './profileText'

export function MedicalInfoTab({ medical, onEdit }: { medical: MedicalInfo; onEdit: () => void }) {
  return (
    <div className="medical">
      <h2 className="section-title">
        <svg className="section-title__icon" viewBox="0 0 24 24" aria-hidden="true">
          <path d="M12 20.5s-7.5-4.6-7.5-10.1C4.5 7.4 6.7 5.5 9 5.5c1.4 0 2.4.7 3 1.7.6-1 1.6-1.7 3-1.7 2.3 0 4.5 1.9 4.5 4.9 0 5.5-7.5 10.1-7.5 10.1Z" />
        </svg>
        건강 정보
      </h2>
      <InfoTable
        emptyText="등록된 정보 없음"
        rows={[
          { label: '주요 질환', values: medical.conditions },
          { label: '알레르기', values: medical.allergies },
          { label: '복용 중인 약', values: medical.medications.map(medicationText) },
        ]}
      />
      <button type="button" className="button button--primary button--block medical__edit" onClick={onEdit}>
        {isMedicalInfoEmpty(medical) ? '+ 의료정보 추가하기' : '의료정보 수정하기'}
      </button>
    </div>
  )
}
