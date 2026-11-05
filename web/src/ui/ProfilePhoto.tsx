import { initialOf } from './profileText'

/** 사진, 없으면 이름 첫 글자. */
export function ProfilePhoto({ photoDataUrl, name, className }: { photoDataUrl: string | null; name: string; className: string }) {
  if (photoDataUrl !== null) {
    return <img className={`${className} photo`} src={photoDataUrl} alt={`${name} 사진`} />
  }
  return (
    <div className={`${className} photo photo--initial`} aria-hidden="true">
      {initialOf(name)}
    </div>
  )
}
