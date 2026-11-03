#!/usr/bin/env node
// 빌드 게이트 훅.
//   UserPromptSubmit: `stop-verify.mjs snapshot`. 턴 시작 시점의 앱 코드 상태(해시)를 기록한다.
//   Stop:             `stop-verify.mjs`. 이번 턴에 앱 코드/빌드 설정이 바뀌었으면 assembleDebug를 돌린다.
//                     실패하면 exit 2 + stderr로 에러를 돌려줘서 Claude가 멈추지 않고 이어서 고치게 한다.
//
// 무한 루프 방지:
//   - 실패 후 아무것도 안 고치고 다시 멈추면 놓아준다.
//   - 같은 세션에서 MAX_FAILS번 연속 실패하면 놓아준다.
import { existsSync, readFileSync, writeFileSync, mkdirSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
import { spawnSync } from 'node:child_process';
import { createHash } from 'node:crypto';

const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..', '..');
const STATE_DIR = join(ROOT, '.claude', '.cache');
const STATE_FILE = join(STATE_DIR, 'stop-verify.json');
const WATCH = /\.(kt|kts|java|xml|gradle|toml|pro|properties)$/i;
const IGNORE = /^(\.claude|docs|apk)\//;
const TASKS = ['assembleDebug'];
const MAX_FAILS = 3;

const mode = process.argv[2] ?? 'stop';

function done(userMessage) {
  if (userMessage) process.stdout.write(JSON.stringify({ systemMessage: userMessage }));
  process.exit(0);
}

let input = {};
try {
  input = JSON.parse(readFileSync(0, 'utf8') || '{}');
} catch {}

if (!existsSync(join(ROOT, 'gradlew'))) done(); // Android 프로젝트가 아직 없음

// 변경된 앱 코드/빌드 파일의 내용 해시. 변경이 없으면 null.
function digestOfChanges() {
  const git = spawnSync(
    'git',
    ['-c', 'core.quotepath=false', 'status', '--porcelain', '-z', '--untracked-files=all'],
    { cwd: ROOT, encoding: 'utf8' },
  );
  if (git.status !== 0) return null;
  const entries = git.stdout.split('\0').filter(Boolean);
  const files = [];
  for (let i = 0; i < entries.length; i++) {
    const status = entries[i].slice(0, 2);
    files.push(entries[i].slice(3));
    if (status.includes('R') || status.includes('C')) i++; // -z에서는 rename 원본 경로가 다음 항목
  }
  const relevant = files.filter((f) => WATCH.test(f) && !IGNORE.test(f)).sort();
  if (relevant.length === 0) return null;
  const h = createHash('sha256');
  for (const f of relevant) {
    h.update(`${f}\0`);
    try {
      h.update(readFileSync(join(ROOT, f)));
    } catch {
      h.update('<deleted>');
    }
  }
  return h.digest('hex');
}

let state = {};
try {
  state = JSON.parse(readFileSync(STATE_FILE, 'utf8'));
} catch {}
function save(patch) {
  state = { ...state, ...patch };
  mkdirSync(STATE_DIR, { recursive: true });
  writeFileSync(STATE_FILE, JSON.stringify(state, null, 2));
}

const digest = digestOfChanges();

if (mode === 'snapshot') {
  save({ turnDigest: digest });
  done();
}

// ---- Stop ----
if (digest === null) done(); // 커밋되지 않은 앱 코드 변경 없음
if (digest === state.turnDigest) done(); // 이번 턴에 앱 코드를 건드리지 않음
if (digest === state.okDigest) done(); // 이 상태는 이미 빌드 통과
if (input.stop_hook_active && digest === state.failDigest) {
  done('⚠ 빌드 실패 상태로 턴이 끝났습니다 (Stop 훅). gradlew assembleDebug로 확인하세요.');
}
const fails = state.session === input.session_id ? (state.fails ?? 0) : 0;
if (fails >= MAX_FAILS) {
  done(`⚠ 빌드가 ${MAX_FAILS}회 연속 실패해 Stop 훅이 더 이상 막지 않습니다.`);
}

const args = [...TASKS, '--console=plain', '-q'];
const opts = { cwd: ROOT, timeout: 570_000, maxBuffer: 32 * 1024 * 1024 };
// Windows: 현재 폴더 실행이 막힌 환경(NoDefaultCurrentDirectoryInExePath)이 있어 절대 경로로 실행한다.
const run =
  process.platform === 'win32'
    ? spawnSync(`"${join(ROOT, 'gradlew.bat')}"`, args, { ...opts, shell: true })
    : spawnSync('sh', ['gradlew', ...args], opts);

// Windows 콘솔 출력은 CP949일 수 있다. UTF-8로 안 읽히면 EUC-KR로 다시 읽는다.
function decode(buf) {
  if (!buf || buf.length === 0) return '';
  try {
    return new TextDecoder('utf-8', { fatal: true }).decode(buf);
  } catch {
    return new TextDecoder('euc-kr').decode(buf);
  }
}

if (run.status === 0) {
  save({ okDigest: digest, failDigest: null, fails: 0, session: input.session_id });
  done();
}

save({ failDigest: digest, fails: fails + 1, session: input.session_id });
const tail = `${decode(run.stdout)}\n${decode(run.stderr)}`.trim().split(/\r?\n/).slice(-80).join('\n');
process.stderr.write(
  [
    `[Stop 훅] gradlew ${TASKS.join(' ')} 실패 (${fails + 1}/${MAX_FAILS}). 아래 에러를 고친 뒤 마무리하라.`,
    '코드로 고칠 수 없는 환경 문제(Android SDK 미설치, 네트워크, Pleos 저장소 접근 등)라면 고치려 하지 말고 사용자에게 원인을 보고하고 멈춰라.',
    run.error ? `실행 오류: ${run.error.message}` : '',
    '',
    tail,
  ].join('\n') + '\n',
);
process.exit(2);
