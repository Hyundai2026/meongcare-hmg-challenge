import type { PetProfileRepository } from '../domain/petProfileRepository'
import { EmptyState } from './EmptyState'
import { Header } from './Header'
import { LoadErrorState } from './LoadErrorState'
import { StorageNotice } from './StorageNotice'
import { usePetProfile } from './usePetProfile'

export function App({ repository }: { repository: PetProfileRepository }) {
  const { state, edit } = usePetProfile(repository)

  if (state.kind === 'loading') {
    return (
      <div className="page">
        <Header />
      </div>
    )
  }

  const { profile, loadError } = state
  return (
    <div className="page">
      <Header />
      <main className="content">
        {profile === null &&
          (loadError !== null ? (
            <LoadErrorState reason={loadError} onStartNew={() => edit('profile')} />
          ) : (
            <EmptyState onRegister={() => edit('profile')} />
          ))}
        <StorageNotice />
      </main>
    </div>
  )
}
