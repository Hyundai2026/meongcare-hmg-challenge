// node --test. 손으로 만든 가짜 카카오 응답만 쓴다 (실제 응답을 저장하지 않는다, K6).
import assert from "node:assert/strict";
import { test } from "node:test";
import {
  InputError,
  MAX_VETS,
  RADIUS_METERS,
  RateLimiter,
  isVet,
  kakaoSearchUrl,
  parseInput,
  resultOf,
  roundCoordinate,
  toVets,
} from "./nearby.mjs";

const doc = (overrides = {}) => ({
  place_name: "가상 동물병원",
  category_name: "가정,생활 > 반려동물 > 동물병원",
  phone: "031-000-0000",
  x: "127.0947",
  y: "37.4127",
  road_address_name: "가상시 가상로 1",
  address_name: "가상시 가상동 1",
  ...overrides,
});

test("S2: 위도·경도가 숫자이고 범위 안이어야 한다", () => {
  assert.deepEqual(parseInput({ latitude: 37.4, longitude: 127.1, radius: 99999 }), { latitude: 37.4, longitude: 127.1 });
  for (const bad of [null, "x", {}, { latitude: "37", longitude: 127 }, { latitude: 91, longitude: 0 },
    { latitude: 0, longitude: -181 }, { latitude: Number.NaN, longitude: 0 }, { latitude: 0, longitude: Infinity }]) {
    assert.throws(() => parseInput(bad), InputError, JSON.stringify(bad));
  }
  assert.deepEqual(parseInput({ latitude: -90, longitude: 180 }), { latitude: -90, longitude: 180 });
});

test("U2: 좌표를 소수 셋째 자리로 반올림한다", () => {
  assert.equal(roundCoordinate(37.4127212), 37.413);
  assert.equal(roundCoordinate(127.094681), 127.095);
  assert.equal(roundCoordinate(-0.0004), -0);
});

test("S5: 카카오 주소는 반올림한 좌표, 반경 5000, 거리순, 15개 한 페이지다", () => {
  const url = new URL(kakaoSearchUrl({ latitude: 37.4127212, longitude: 127.094681 }));
  assert.equal(url.origin + url.pathname, "https://dapi.kakao.com/v2/local/search/keyword.json");
  assert.equal(url.searchParams.get("query"), "동물병원");
  assert.equal(url.searchParams.get("x"), "127.095");
  assert.equal(url.searchParams.get("y"), "37.413");
  assert.equal(url.searchParams.get("radius"), String(RADIUS_METERS));
  assert.equal(url.searchParams.get("sort"), "distance");
  assert.equal(url.searchParams.get("size"), "15");
  assert.equal(url.searchParams.get("page"), "1");
});

test("S7: 동물병원만, 이름·좌표가 맞는 것만 남긴다", () => {
  const vets = toVets({
    documents: [
      doc(),
      doc({ place_name: "펫샵", category_name: "가정,생활 > 반려동물 > 반려동물용품" }),
      doc({ place_name: "  " }),
      doc({ x: "abc" }),
      doc({ y: "95" }),
      doc({ place_name: " 전화 없는 동물병원 ", phone: "", road_address_name: "" }),
    ],
  });
  assert.deepEqual(vets, [
    { name: "가상 동물병원", phone: "031-000-0000", latitude: 37.4127, longitude: 127.0947, address: "가상시 가상로 1" },
    { name: "전화 없는 동물병원", phone: null, latitude: 37.4127, longitude: 127.0947, address: "가상시 가상동 1" },
  ]);
});

test("S7: 최대 15곳, 형식이 이상하면 빈 목록", () => {
  const many = { documents: Array.from({ length: 20 }, (_, i) => doc({ place_name: `병원${i}` })) };
  assert.equal(toVets(many).length, MAX_VETS);
  assert.deepEqual(toVets(null), []);
  assert.deepEqual(toVets({ documents: "x" }), []);
  assert.equal(isVet({}), false);
});

test("응답 형식: schemaVersion 1, radiusMeters 5000", () => {
  assert.deepEqual(resultOf([]), { schemaVersion: 1, radiusMeters: 5000, vets: [] });
});

test("S3: 같은 계정은 5초 안에 다시 못 부르고, 1시간에 30번까지다", () => {
  let now = 0;
  const limiter = new RateLimiter({ now: () => now });
  assert.equal(limiter.tryAcquire("a"), true);
  now = 4999;
  assert.equal(limiter.tryAcquire("a"), false);
  assert.equal(limiter.tryAcquire("b"), true, "다른 계정은 따로 센다");
  now = 5000;
  assert.equal(limiter.tryAcquire("a"), true);
  for (let i = 2; i < 30; i++) {
    now += 5000;
    assert.equal(limiter.tryAcquire("a"), true, `${i + 1}번째`);
  }
  now += 5000;
  assert.equal(limiter.tryAcquire("a"), false, "31번째는 막힌다");
  now += 3_600_000;
  assert.equal(limiter.tryAcquire("a"), true, "1시간이 지나면 다시 된다");
});
