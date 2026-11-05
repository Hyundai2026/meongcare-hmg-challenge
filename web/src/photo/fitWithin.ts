/** 긴 변이 maxEdge를 넘지 않게 비율을 지켜 줄인 크기. 키우지 않는다. */
export function fitWithin(width: number, height: number, maxEdge: number): { width: number; height: number } {
  if (width <= maxEdge && height <= maxEdge) return { width, height }
  const scale = maxEdge / Math.max(width, height)
  return {
    width: Math.max(1, Math.round(width * scale)),
    height: Math.max(1, Math.round(height * scale)),
  }
}
