// Firebase Functions: 근처 동물병원 검색 (계획 015). PC의 Firebase 에뮬레이터에서만 돌린다 (배포하지 않음, D1).
// 카카오 REST 키는 이 함수만 안다. 앱·웹에는 없다. 형식: docs/data/nearby-vets.md

import { initializeApp } from "firebase-admin/app";
import { defineSecret } from "firebase-functions/params";
import { HttpsError, onCall } from "firebase-functions/v2/https";
import * as logger from "firebase-functions/logger";
import {
  InputError,
  KAKAO_TIMEOUT_MS,
  RateLimiter,
  kakaoSearchUrl,
  parseInput,
  resultOf,
  toVets,
} from "./nearby.mjs";

initializeApp();

/** 에뮬레이터는 functions/.secret.local에서 읽는다 (F3). 커밋하지 않는다. */
const kakaoKey = defineSecret("KAKAO_REST_API_KEY");

const limiter = new RateLimiter();

// 로그에는 결과 종류·개수·걸린 ms·카카오 HTTP 상태만 남긴다 (S9). 좌표·병원 이름·전화·uid·키는 남기지 않는다.
export const nearbyVets = onCall(
  { region: "asia-northeast3", secrets: [kakaoKey], timeoutSeconds: 10 },
  async (request) => {
    const started = Date.now();
    if (!request.auth) throw new HttpsError("unauthenticated", "로그인이 필요해요.");

    let center;
    try {
      center = parseInput(request.data);
    } catch (e) {
      if (e instanceof InputError) throw new HttpsError("invalid-argument", "위치 형식이 맞지 않아요.");
      throw e;
    }

    const key = (kakaoKey.value() ?? "").trim();
    if (key === "") {
      logger.warn("nearbyVets: 키 없음");
      throw new HttpsError("failed-precondition", "장소 검색 설정이 없어요.");
    }

    if (!limiter.tryAcquire(request.auth.uid)) {
      throw new HttpsError("resource-exhausted", "잠시 뒤에 다시 찾아 주세요.");
    }

    let response;
    try {
      response = await fetch(kakaoSearchUrl(center), {
        headers: { Authorization: `KakaoAK ${key}` },
        signal: AbortSignal.timeout(KAKAO_TIMEOUT_MS),
      });
    } catch {
      logger.info("nearbyVets: 실패 (네트워크·시간 초과)", { ms: Date.now() - started });
      throw new HttpsError("unavailable", "장소 검색에 연결하지 못했어요.");
    }
    if (!response.ok) {
      // 카카오 응답 본문은 돌려주지도 남기지도 않는다 (S6)
      logger.info("nearbyVets: 실패", { kakaoStatus: response.status, ms: Date.now() - started });
      throw new HttpsError("unavailable", "장소 검색에 실패했어요.");
    }

    let json;
    try {
      json = await response.json();
    } catch {
      logger.info("nearbyVets: 실패 (형식)", { ms: Date.now() - started });
      throw new HttpsError("unavailable", "장소 검색 응답을 읽지 못했어요.");
    }
    const vets = toVets(json);
    const kakaoCount = Array.isArray(json?.documents) ? json.documents.length : 0;
    logger.info("nearbyVets: 성공", { count: vets.length, kakaoCount, ms: Date.now() - started });
    return resultOf(vets);
  },
);
