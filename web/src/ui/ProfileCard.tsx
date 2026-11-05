import type { LocalDate } from '../domain/localDate'
import type { PetProfile } from '../domain/petProfile'
import { ProfilePhoto } from './ProfilePhoto'
import { profileSubtitle, SEX_SYMBOL, SEX_TEXT } from './profileText'

export function ProfileCard({ profile, today, onEdit }: { profile: PetProfile; today: LocalDate; onEdit: () => void }) {
  const subtitle = profileSubtitle(profile, today)
  return (
    <section className="card profile">
      <ProfilePhoto photoDataUrl={profile.photoDataUrl} name={profile.name} className="profile__photo" />
      <div className="profile__info">
        <h1 className="profile__name">
          {profile.name}
          {profile.sex !== null && (
            <>
              {' '}
              <span className={`profile__sex profile__sex--${profile.sex}`} title={SEX_TEXT[profile.sex]}>
                {SEX_SYMBOL[profile.sex]}
              </span>
            </>
          )}
        </h1>
        {subtitle !== null && <p className="profile__sub">{subtitle}</p>}
        <button type="button" className="button button--pill profile__edit" onClick={onEdit}>
          프로필 수정
        </button>
      </div>
    </section>
  )
}
