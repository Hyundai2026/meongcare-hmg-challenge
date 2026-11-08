// node --test. 계획 015 "설계 결정 5" S2·S3·S5·S7의 경계 (tester).
// 손으로 만든 가짜 카카오 응답만 쓴다 (실제 응답을 저장하지 않는다, K6).
import assert from "node:assert/strict";
import { test } from "node:test";
import {
  InputError,
  MAX_VETS,
  RateLimiter,
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

test("S2: 범위 바로 밖의 위도·경도는 invalid-argument 감이다", () => {
  for (const bad of [
    { latitude: 90.000001, longitude: 0 },
    { latitude: -90.000001, longitude: 0 },
    { latitude: 0, longitude: 180.000001 },
    { latitude: 0, longitude: -180.000001 },
    { latitude: -Infinity, longitude: 0 },
  ]) {
    assert.throws(() => parseInput(bad), InputError, JSON.stringify(bad));
  }
});

test("S2: 숫자가 아닌 위도·경도는 모두 거절한다 (문자열, 참거짓, null, 배열, BigInt)", () => {
  for (const bad of [
    { latitude: true, longitude: 127 },
    { latitude: 37, longitude: null },
    { latitude: [37], longitude: 127 },
    { latitude: 37, longitude: "127" },
    { latitude: 37n, longitude: 127 },
    { latitude: 37 },
    [],
    undefined,
    42,
  ]) {
    assert.throws(() => parseInput(bad), InputError, String(JSON.stringify(bad, (_, v) => (typeof v === "bigint" ? `${v}n` : v))));
  }
});

test("S2: 경계값과 0은 받는다", () => {
  assert.deepEqual(parseInput({ latitude: 90, longitude: -180 }), { latitude: 90, longitude: -180 });
  assert.deepEqual(parseInput({ latitude: 0, longitude: 0 }), { latitude: 0, longitude: 0 });
});

test("S5: 반경·개수·쪽·검색어를 클라이언트가 보내도 카카오 주소는 바뀌지 않는다", () => {
  const sneaky = { latitude: 37.4127212, longitude: 127.094681, radius: 20000, size: 1, page: 45, query: "약국", sort: "accuracy" };
  const url = new URL(kakaoSearchUrl(parseInput(sneaky)));
  assert.equal(url.searchParams.get("radius"), "5000");
  assert.equal(url.searchParams.get("size"), "15");
  assert.equal(url.searchParams.get("page"), "1");
  assert.equal(url.searchParams.get("query"), "동물병원");
  assert.equal(url.searchParams.get("sort"), "distance");
  // parseInput 없이 바로 넘겨도 같다
  const direct = new URL(kakaoSearchUrl(sneaky));
  assert.equal(direct.searchParams.get("radius"), "5000");
  assert.equal(direct.searchParams.get("size"), "15");
  assert.equal(direct.searchParams.get("query"), "동물병원");
});

test("S5: 카카오 주소에는 소수 셋째 자리까지만 가고 정확한 좌표는 없다", () => {
  const precise = { latitude: 37.4127212, longitude: 127.094681 };
  const raw = kakaoSearchUrl(precise);
  assert.ok(!raw.includes("37.4127212") && !raw.includes("127.094681"), raw);
  const url = new URL(raw);
  for (const key of ["x", "y"]) {
    const decimals = url.searchParams.get(key).split(".")[1] ?? "";
    assert.ok(decimals.length <= 3, `${key}=${url.searchParams.get(key)}`);
  }
});

test("U2: 반올림한 좌표는 원래 좌표와 0.0005도 넘게 차이 나지 않는다", () => {
  for (let i = 0; i < 1000; i++) {
    const v = -180 + (360 * i) / 997 + 0.0000123;
    const r = roundCoordinate(v);
    assert.ok(Math.abs(r - v) <= 0.0005 + 1e-12, `${v} → ${r}`);
    assert.ok(Math.abs(r * 1000 - Math.round(r * 1000)) < 1e-6, `${r}는 소수 셋째 자리까지`);
  }
});

test("S7: 동물병원이 아닌 항목을 빼고도 15곳을 채운다", () => {
  const documents = [
    ...Array.from({ length: 5 }, (_, i) => doc({ place_name: `용품${i}`, category_name: "가정,생활 > 반려동물 > 반려동물용품" })),
    ...Array.from({ length: 20 }, (_, i) => doc({ place_name: `병원${i}` })),
  ];
  const vets = toVets({ documents });
  assert.equal(vets.length, MAX_VETS);
  assert.deepEqual(vets.map((v) => v.name), Array.from({ length: 15 }, (_, i) => `병원${i}`));
});

test("S7: category_name이 없거나 문자열이 아니면 동물병원이 아니다", () => {
  const vets = toVets({ documents: [doc({ category_name: undefined }), doc({ category_name: null }), doc({ category_name: 1 })] });
  assert.deepEqual(vets, []);
});

test("S7: 공백뿐인 전화는 null이고 전화 필드가 없어도 null이다", () => {
  const vets = toVets({ documents: [doc({ phone: "   " }), doc({ phone: undefined, place_name: "둘" })] });
  assert.deepEqual(vets.map((v) => v.phone), [null, null]);
});

test("S7: 도로명·지번 주소가 둘 다 없으면 address는 null이다", () => {
  const [vet] = toVets({ documents: [doc({ road_address_name: "", address_name: " " })] });
  assert.equal(vet.address, null);
});

test("S7: 좌표가 빈 문자열이거나 없으면 뺀다", () => {
  const vets = toVets({ documents: [doc({ x: "" }), doc({ y: undefined }), doc({ x: null }), doc({ place_name: "좋음" })] });
  assert.deepEqual(vets.map((v) => v.name), ["좋음"]);
});

test("S7: 숫자 뒤에 글자가 붙은 좌표는 숫자로 바뀌지 않는 것으로 보고 뺀다", () => {
  const vets = toVets({ documents: [doc({ x: "127.09abc" }), doc({ y: "37.4 north" }), doc({ place_name: "좋음" })] });
  assert.deepEqual(vets.map((v) => v.name), ["좋음"]);
});

test("S7: 결과 좌표는 숫자 타입이고 이름은 앞뒤 공백을 뺀다", () => {
  const [vet] = toVets({ documents: [doc({ place_name: "  가 동물병원\t" })] });
  assert.equal(vet.name, "가 동물병원");
  assert.equal(typeof vet.latitude, "number");
  assert.equal(typeof vet.longitude, "number");
  assert.equal(vet.latitude, 37.4127);
  assert.equal(vet.longitude, 127.0947);
});

test("S7: 결과에는 형식 문서의 다섯 필드만 있다 (카카오 id·url·distance를 넘기지 않는다)", () => {
  const [vet] = toVets({ documents: [doc({ id: "123", place_url: "http://place", distance: "120", category_group_code: "" })] });
  assert.deepEqual(Object.keys(vet).sort(), ["address", "latitude", "longitude", "name", "phone"]);
});

test("S7: documents 안의 null이나 원시값은 건너뛴다", () => {
  const vets = toVets({ documents: [null, 1, "x", doc()] });
  assert.equal(vets.length, 1);
});

test("응답 형식: resultOf는 받은 목록을 그대로 vets에 담는다", () => {
  const vets = toVets({ documents: [doc()] });
  assert.deepEqual(resultOf(vets), { schemaVersion: 1, radiusMeters: 5000, vets });
});

test("S3: 막힌 호출은 기록되지 않아 5초 간격을 늘리지 않는다", () => {
  let now = 0;
  const limiter = new RateLimiter({ now: () => now });
  assert.equal(limiter.tryAcquire("a"), true);
  for (const t of [1000, 2000, 3000, 4000, 4999]) {
    now = t;
    assert.equal(limiter.tryAcquire("a"), false, `t=${t}`);
  }
  now = 5000;
  assert.equal(limiter.tryAcquire("a"), true, "첫 호출에서 5초 뒤면 된다");
});

test("S3: 1시간 창은 미끄러진다 (가장 오래된 호출이 1시간 지나면 한 번 더 된다)", () => {
  let now = 0;
  const limiter = new RateLimiter({ now: () => now });
  const times = [];
  for (let i = 0; i < 30; i++) {
    now = i * 10_000;
    times.push(now);
    assert.equal(limiter.tryAcquire("a"), true, `${i + 1}번째`);
  }
  now = 3_599_999;
  assert.equal(limiter.tryAcquire("a"), false, "첫 호출에서 1시간이 아직 안 됐다");
  now = 3_600_000;
  assert.equal(limiter.tryAcquire("a"), true, "첫 호출이 정확히 1시간 지나면 창에서 빠진다");
  now = 3_605_000;
  assert.equal(limiter.tryAcquire("a"), false, "다시 30번이 찼다");
});

test("S3: 한 계정이 막혀도 다른 계정은 영향이 없다", () => {
  let now = 0;
  const limiter = new RateLimiter({ now: () => now });
  for (let i = 0; i < 30; i++) {
    now = i * 5000;
    limiter.tryAcquire("a");
  }
  now += 5000;
  assert.equal(limiter.tryAcquire("a"), false);
  assert.equal(limiter.tryAcquire("b"), true);
});
