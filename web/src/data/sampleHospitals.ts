/**
 * 시연용 제휴 병원 5곳과 예시 환자 1건 (메모리 모드). 모두 가상이다.
 * **값은 docs/data/hospitals.sample.json, docs/data/emergency-case.example.json과 같다.**
 * 그 파일을 바꾸면 여기도 바꾼다 (sampleHospitals.test.ts가 대조한다).
 * 앱 번들이 web/ 밖의 파일을 읽지 않게 값을 옮겨 두었다.
 */
export interface SampleHospitalEntry {
  id: string
  seedUpdatedMinutesAgo: number
  document: {
    schemaVersion: number
    name: string
    phone: string | null
    latitude: number
    longitude: number
    address: string | null
    isSample: boolean
    status: { waitMinutes: number; congestion: string; acceptingEmergency: boolean }
  }
}

export const SAMPLE_HOSPITALS: readonly SampleHospitalEntry[] = [
  {
    id: 'demo-fast',
    seedUpdatedMinutesAgo: 2,
    document: {
      schemaVersion: 1,
      name: '시연용 바른 동물병원',
      phone: '02-0000-0001',
      latitude: 37.367,
      longitude: 127.1083,
      address: null,
      isSample: true,
      status: { waitMinutes: 10, congestion: 'normal', acceptingEmergency: true },
    },
  },
  {
    id: 'demo-main',
    seedUpdatedMinutesAgo: 5,
    document: {
      schemaVersion: 1,
      name: '시연용 동물병원',
      phone: '02-0000-0000',
      latitude: 37.48544722,
      longitude: 127.03636666,
      address: null,
      isSample: true,
      status: { waitMinutes: 5, congestion: 'low', acceptingEmergency: true },
    },
  },
  {
    id: 'demo-near',
    seedUpdatedMinutesAgo: 3,
    document: {
      schemaVersion: 1,
      name: '시연용 가까운 동물병원',
      phone: '02-0000-0002',
      latitude: 37.3947,
      longitude: 127.1112,
      address: null,
      isSample: true,
      status: { waitMinutes: 60, congestion: 'high', acceptingEmergency: true },
    },
  },
  {
    id: 'demo-stale',
    seedUpdatedMinutesAgo: 90,
    document: {
      schemaVersion: 1,
      name: '시연용 정보 지난 동물병원',
      phone: '02-0000-0003',
      latitude: 37.4873,
      longitude: 127.1017,
      address: null,
      isSample: true,
      status: { waitMinutes: 5, congestion: 'low', acceptingEmergency: true },
    },
  },
  {
    id: 'demo-closed',
    seedUpdatedMinutesAgo: 10,
    document: {
      schemaVersion: 1,
      name: '시연용 응급 휴진 동물병원',
      phone: '02-0000-0004',
      latitude: 37.4113,
      longitude: 127.1275,
      address: null,
      isSample: true,
      status: { waitMinutes: 0, congestion: 'normal', acceptingEmergency: false },
    },
  },
]

/** docs/data/emergency-case.example.json과 같은 값. 메모리 모드는 시각만 지금에 맞춰 바꿔 쓴다 */
export const EXAMPLE_CASE_DOCUMENT = {
  schemaVersion: 1,
  createdBy: 'anon-uid-example',
  createdAt: '2026-11-06T05:00:00Z',
  status: 'en_route',
  remainingMinutes: 12,
  etaAt: '2026-11-06T05:12:00Z',
  etaUpdatedAt: '2026-11-06T05:00:00Z',
  card: {
    pet: {
      name: '보리',
      breed: '골든리트리버',
      sex: 'male',
      birthDate: '2014-08-01',
      birthDateIsEstimated: false,
      medical: {
        conditions: ['심장질환'],
        allergies: ['특정 단백질 알레르기'],
        medications: [{ name: '심장약', dosage: '1일 1회' }],
      },
      updatedAt: '2026-11-05T00:00:00.000Z',
    },
    symptoms: [
      { code: 'BREATHING_DIFFICULTY', label: '호흡 곤란' },
      { code: 'COUGH', label: '기침' },
    ],
  },
  acknowledgedAt: null,
} as const
