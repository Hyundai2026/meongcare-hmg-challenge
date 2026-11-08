// 근처 동물병원 검색의 순수 함수 (계획 015 "설계 결정 5"). 네트워크와 Firebase를 모른다.
// 형식: docs/data/nearby-vets.md

/** 검색 반경 (미터, 사용자 결정 D2). 앱의 NEARBY_RADIUS_METERS와 같다. 클라이언트가 바꾸지 못한다. */
export const RADIUS_METERS = 5000;

/** 카카오 키워드 검색 한 페이지 최대 (K3: size 1~15) */
export const MAX_VETS = 15;

export const SCHEMA_VERSION = 1;

/** 카카오 호출 시간 초과 (ms) */
export const KAKAO_TIMEOUT_MS = 4000;

/** 같은 계정이 다시 부르기까지 기다릴 시간 (ms) */
export const MIN_INTERVAL_MS = 5000;

/** 같은 계정이 1시간에 부를 수 있는 횟수 */
export const MAX_PER_HOUR = 30;

/** 잘못된 입력. index.js가 callable 오류 코드로 바꾼다. */
export class InputError extends Error {}

/**
 * callable 몸통의 `data`를 읽는다. 위도·경도가 유한한 숫자이고 범위 안이어야 한다.
 * 다른 필드(반경, 개수 등)는 무시한다 (S5: 클라이언트가 정하지 못한다).
 * @returns {{latitude: number, longitude: number}}
 */
export function parseInput(data) {
  if (data === null || typeof data !== "object") throw new InputError("data가 객체가 아니다");
  const { latitude, longitude } = data;
  if (typeof latitude !== "number" || !Number.isFinite(latitude) || latitude < -90 || latitude > 90) {
    throw new InputError("latitude");
  }
  if (typeof longitude !== "number" || !Number.isFinite(longitude) || longitude < -180 || longitude > 180) {
    throw new InputError("longitude");
  }
  return { latitude, longitude };
}

/** 좌표를 소수 셋째 자리(위도 약 111m)로 반올림한다 (사용자 결정 U2: 정확한 위치를 카카오에 보내지 않는다). */
export function roundCoordinate(value) {
  return Math.round(value * 1000) / 1000;
}

/** 카카오 키워드 검색 주소 (K1·K3). x=경도, y=위도. */
export function kakaoSearchUrl({ latitude, longitude }) {
  const params = new URLSearchParams({
    query: "동물병원",
    x: String(roundCoordinate(longitude)),
    y: String(roundCoordinate(latitude)),
    radius: String(RADIUS_METERS),
    sort: "distance",
    size: String(MAX_VETS),
    page: "1",
  });
  return `https://dapi.kakao.com/v2/local/search/keyword.json?${params}`;
}

/**
 * 동물병원인지. 카카오에는 동물병원 카테고리 코드가 없어서(K2) 키워드 검색 결과의 `category_name`으로 거른다.
 * (예: 용품점·미용실이 "동물병원" 키워드에 걸릴 수 있다.)
 */
export function isVet(document) {
  return typeof document?.category_name === "string" && document.category_name.includes("동물병원");
}

/**
 * 카카오 응답 → 우리 형식의 병원 목록 (S7). 결과를 저장하지 않는다 (K6).
 * - 이름이 비면 뺀다. 좌표가 숫자로 바뀌지 않거나 범위 밖이면 뺀다.
 * - 전화가 비면 null. 주소는 도로명 → 지번 → null.
 * - 동물병원이 아니면 뺀다. 최대 [MAX_VETS]곳.
 */
export function toVets(kakaoJson) {
  const documents = Array.isArray(kakaoJson?.documents) ? kakaoJson.documents : [];
  const vets = [];
  for (const document of documents) {
    if (!isVet(document)) continue;
    const name = typeof document.place_name === "string" ? document.place_name.trim() : "";
    const latitude = toNumber(document.y);
    const longitude = toNumber(document.x);
    if (name === "") continue;
    if (!Number.isFinite(latitude) || latitude < -90 || latitude > 90) continue;
    if (!Number.isFinite(longitude) || longitude < -180 || longitude > 180) continue;
    vets.push({
      name,
      phone: textOrNull(document.phone),
      latitude,
      longitude,
      address: textOrNull(document.road_address_name) ?? textOrNull(document.address_name),
    });
    if (vets.length >= MAX_VETS) break;
  }
  return vets;
}

/** 성공 응답 (`result`) */
export function resultOf(vets) {
  return { schemaVersion: SCHEMA_VERSION, radiusMeters: RADIUS_METERS, vets };
}

/** 카카오 좌표 문자열 → 숫자. 문자열 전체가 숫자여야 한다 ("127.09abc"는 NaN). 빈 값·숫자가 아닌 타입은 NaN */
function toNumber(value) {
  if (typeof value === "number") return value;
  if (typeof value !== "string" || value.trim() === "") return Number.NaN;
  return Number(value.trim());
}

function textOrNull(value) {
  if (typeof value !== "string") return null;
  const trimmed = value.trim();
  return trimmed === "" ? null : trimmed;
}

/**
 * 계정별 호출 제한 (S3). 프로세스 메모리에만 기억한다 (에뮬레이터는 한 프로세스라 충분. 배포하면 다시 정한다).
 * 남용 방지의 최소한이다: 같은 계정이 [MIN_INTERVAL_MS] 안에 다시 부르거나, 1시간에 [MAX_PER_HOUR]번을 넘으면 막는다.
 */
export class RateLimiter {
  constructor({ now = () => Date.now(), minIntervalMs = MIN_INTERVAL_MS, maxPerHour = MAX_PER_HOUR } = {}) {
    this.now = now;
    this.minIntervalMs = minIntervalMs;
    this.maxPerHour = maxPerHour;
    /** @type {Map<string, number[]>} uid → 최근 1시간 호출 시각 */
    this.calls = new Map();
  }

  /** 부를 수 있으면 기록하고 true, 막히면 false. */
  tryAcquire(uid) {
    const now = this.now();
    const recent = (this.calls.get(uid) ?? []).filter((t) => now - t < 3_600_000);
    const last = recent.at(-1);
    if (last !== undefined && now - last < this.minIntervalMs) {
      this.calls.set(uid, recent);
      return false;
    }
    if (recent.length >= this.maxPerHour) {
      this.calls.set(uid, recent);
      return false;
    }
    recent.push(now);
    this.calls.set(uid, recent);
    return true;
  }
}
