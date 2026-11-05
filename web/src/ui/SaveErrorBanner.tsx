import type { SaveFailure } from '../domain/petProfileRepository'
import { SAVE_FAILURE_MESSAGES } from './messages'

export function SaveErrorBanner({ reason }: { reason: SaveFailure }) {
  return (
    <p className="banner banner--error" role="alert">
      {SAVE_FAILURE_MESSAGES[reason]}
    </p>
  )
}
