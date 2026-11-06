# 환자 건 형식 (v1): 차에서 병원으로 보내는 진료 카드

차 앱이 병원을 고르고 [출발]하면 만드는 문서다. 병원 웹이 실시간으로 읽어 "들어온 환자" 목록에 보인다.
코드가 아니라 이 문서가 기준이다. 형식을 바꾸면 이 문서와 예시 JSON부터 고친다.

- 예시: [emergency-case.example.json](emergency-case.example.json). 샘플 반려견 "보리"가 호흡 곤란·기침으로 오는 중이다. 테스트가 이 파일을 읽어서 대조한다.
- 경로: `hospitals/{hospitalId}/cases/{caseId}` ([hospital-status.md](hospital-status.md) "Firestore 경로"). `caseId`는 Firestore가 만든다.
- 계획: [docs/plans/completed/012-fastest-hospital.md](../plans/completed/012-fastest-hospital.md) "설계 결정 5·6"

## 보내는 것과 보내지 않는 것 (사용자 결정 2026-11-06, 계획 012 U3)

- 보낸다: 반려견 이름·품종·성별·생년월일(나이 계산용)·주요 질환·알레르기·복용 약, 차에서 고른 증상, 도착 예정 시간
- **보내지 않는다: 반려견 사진(`photoDataUrl`), 차의 위치.** 병원에는 도착 예정만 필요하다.
- 차 확인 화면에서 [출발]을 누르는 것이 이번 전송의 동의다. [카드 없이 길안내만]을 고르면 아무것도 보내지 않는다 (U2).

## 필드

| 필드 | 타입 | 뜻 | 쓰는 쪽 |
|---|---|---|---|
| `schemaVersion` | 숫자 | 형식 버전. 지금은 `1` | 차 |
| `createdBy` | 문자열 | 보낸 차의 익명 계정 uid. 보안 규칙이 `request.auth.uid`와 같은지 본다 | 차 |
| `createdAt` | 타임스탬프 | 보낸 시각 (차 시계) | 차 |
| `status` | `"en_route"`, `"arrived"`, `"cancelled"` | 오는 중 / 도착 / 취소 | 차 |
| `remainingMinutes` | 정수 또는 `null` | 남은 시간(분). 차 화면과 같은 값(올림, 최소 1). `null`이면 경로를 찾는 중 | 차 |
| `etaAt` | 타임스탬프 또는 `null` | 도착 예정 시각 = 보낸 순간 + 남은 시간 (차 시계). `remainingMinutes`가 `null`이면 `null` | 차 |
| `etaUpdatedAt` | 타임스탬프 | 남은 시간을 마지막으로 보낸 시각 (차 시계) | 차 |
| `card.pet` | 객체 또는 `null` | 반려견 정보. 아래 "card.pet". 차가 프로필을 받지 못했으면 `null` | 차 |
| `card.symptoms` | `{ code, label }` 배열 | 고른 증상. 0~12개, 프리셋 순서 | 차 |
| `acknowledgedAt` | 타임스탬프 또는 `null` | 병원이 [확인함]을 누른 시각 (계획 012 있으면 좋음 A) | 병원 웹 |

- 타임스탬프의 논리 형식은 [hospital-status.md](hospital-status.md)와 같다 (예시 JSON에서는 ISO 8601 UTC 문자열).

### card.pet

[pet-profile.md](pet-profile.md) v1에서 아래 필드만 같은 이름·같은 형식으로 옮긴다.

`name`, `breed`, `sex`, `birthDate`, `birthDateIsEstimated`, `medical`(`conditions`, `allergies`, `medications`), `updatedAt`

- **`photoDataUrl`과 `schemaVersion`은 넣지 않는다.** 형식 버전은 이 문서의 `schemaVersion` 하나다.
- 병원 웹은 pet-profile의 읽는 쪽 규칙과 나이 계산으로 보인다 ("12세 3개월"). 나이는 병원 웹 기기의 오늘 날짜로 계산한다.

### card.symptoms

| 필드 | 타입 | 뜻 |
|---|---|---|
| `code` | 문자열 | 증상 코드. 차 앱 `domain/card/Symptom.kt`의 이름 (예: `COUGH`) |
| `label` | 문자열 | 차 화면에 보인 글자 (예: "기침", `strings.xml`의 `symptom_*`) |

- 순서는 프리셋 순서(`Symptom.kt` 선언 순서)다. 누른 순서가 아니다.
- 병원 웹은 `label`을 보인다. `label`이 없거나 빈 문자열이면 `code`를 보인다.

## 상태 흐름

```
(차 [출발]) → en_route ──(남은 시간 갱신, 1분에 1번 이하)──→ en_route
                 ├──(도착)──→ arrived
                 └──(앱 [안내 취소] 또는 내비 "안내 종료")──→ cancelled
(병원 웹 [완료]) → 문서 삭제
```

- 차는 남은 분이 바뀌었고 마지막으로 보낸 지 60초가 지났을 때만 `remainingMinutes`·`etaAt`·`etaUpdatedAt`을 바꾼다.
- 차는 도착·취소를 상태로 알리고 문서를 지우지 않는다 (병원이 기록을 봐야 한다). 지우는 것은 병원 웹 [완료]다.
- 차가 바꿀 수 있는 필드는 `status`, `remainingMinutes`, `etaAt`, `etaUpdatedAt`뿐이다. 병원 웹이 바꿀 수 있는 필드는 `acknowledgedAt`뿐이다 (보안 규칙).

## 읽는 쪽 규칙 (병원 웹)

| 받은 것 | 결과 |
|---|---|
| `schemaVersion`이 `1`이 아님 | 그 건을 "읽지 못한 환자 건"으로 한 줄만 보인다 (지우기는 된다) |
| `status`가 모르는 값 | 오는 중으로 보인다 |
| `card.pet`이 `null`이거나 pet-profile 규칙으로 읽지 못함 | "반려견 정보를 받지 못했어요" + 증상만 |
| `card.symptoms`가 배열이 아님 | 증상 없음 |
| `remainingMinutes`·`etaAt`이 `null` | "경로를 찾는 중" |
| 모르는 필드 | 무시하고 읽는다 |

## 알려진 한계

- **시계 차이**: `etaAt`은 차 시계로 계산한다. 차와 병원 PC의 시계가 다르면 그만큼 틀린다. 병원 웹은 "도착 예정 14:32 · 약 12분 후"의 "약 N분 후"를 자기 시계로 계산한다.
- 자동 삭제가 없다 (Functions를 쓰지 않는다). 진짜 프로젝트에서는 남은 건을 정리하는 방법을 정해야 한다 (docs/architecture.md "Firebase로 옮길 때 할 일").
- 익명 토큰은 약 1시간 유효로 알고 있다. 갱신 전에는 안내가 길면 남은 시간 갱신이 실패할 수 있다 (계획 012 있으면 좋음 C).
