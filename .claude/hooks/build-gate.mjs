#!/usr/bin/env node
// 빌드 게이트 훅. 앱 코드가 빌드되지 않는 상태로 커밋되거나 턴이 끝나는 것을 막는다.
//
//   snapshot  (UserPromptSubmit) 턴 시작 시점의 앱 코드 해시를 기록한다.
//   precommit (PreToolUse: Bash|PowerShell) git commit 직전에 검사한다.
//             - main/master에 직접 커밋하면 막는다. 저장소의 첫 커밋은 예외.
//             - 앱 코드가 바뀌었고 아직 빌드를 확인하지 않은 상태면 assembleDebug를 돌린다. 실패하면 커밋을 막는다.
//   stop      (Stop) 이번 턴에 앱 코드가 바뀌었고 아직 빌드를 확인하지 않았으면 assembleDebug를 돌린다.
//             실패하면 exit 2로 에러를 돌려줘서, Claude가 멈추지 않고 이어서 고치게 한다.
//
// 해시는 커밋 여부와 상관없이 "작업 트리에 있는 앱 코드 전체의 내용"으로 만든다.
// 그래서 턴 중에 커밋해도 Stop 검사를 건너뛰지 않는다.
//
// Stop의 무한 루프 방지:
//   - 실패한 뒤 아무것도 고치지 않고 다시 멈추면 놓아준다.
//   - 같은 세션에서 MAX_FAILS번 연속 실패하면 놓아준다.
import { existsSync, readFileSync, writeFileSync, mkdirSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
import { spawnSync } from 'node:child_process';
import { createHash } from 'node:crypto';

const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..', '..');
const STATE_DIR = join(ROOT, '.claude', '.cache');
const STATE_FILE = join(STATE_DIR, 'build-gate.json');
const WATCH = /\.(kt|kts|java|xml|gradle|toml|pro|properties)$/i;
const IGNORE = /^(\.claude|docs|apk)\//;
const TASKS = ['assembleDebug'];
const MAX_FAILS = 3;
const PROTECTED_BRANCHES = ['main', 'master'];
const GIT = String.raw`(^|[\s;&|(])git(\s+-[cC]\s+\S+)*\s+`;
const GIT_COMMIT = new RegExp(`${GIT}commit(\\s|$)`);
const GIT_ADD = new RegExp(`${GIT}add\\s`);
const COMMIT_ALL = /\scommit\b.*\s(--all|-[A-Za-z]*a[A-Za-z]*)(\s|$)/;

const mode = process.argv[2] ?? 'stop';

function done(userMessage) {
  if (userMessage) process.stdout.write(JSON.stringify({ systemMessage: userMessage }));
  process.exit(0);
}

function block(message) {
  process.stderr.write(`${message}\n`);
  process.exit(2);
}

let input = {};
try {
  input = JSON.parse(readFileSync(0, 'utf8') || '{}');
} catch {}

// precommit은 모든 셸 명령마다 불린다. git commit이 아니면 가장 먼저 내보낸다.
const command = input.tool_input?.command ?? '';
if (mode === 'precommit' && !GIT_COMMIT.test(command)) done();

function git(...args) {
  const r = spawnSync('git', ['-c', 'core.quotepath=false', ...args], { cwd: ROOT, encoding: 'utf8' });
  return r.status === 0 ? r.stdout : null;
}

const isAppCode = (f) => WATCH.test(f) && !IGNORE.test(f);

if (mode === 'precommit') {
  const branch = git('symbolic-ref', '--short', '-q', 'HEAD')?.trim();
  const hasCommits = git('rev-parse', '--verify', '-q', 'HEAD') !== null;
  if (hasCommits && PROTECTED_BRANCHES.includes(branch)) {
    block(
      `[build-gate] ${branch}에 직접 커밋하지 않는다 (docs/conventions/git.md). ` +
        '작업 브랜치를 먼저 만든다: git switch -c feat/<짧은-이름>  (변경 사항은 그대로 따라간다)',
    );
  }
}

if (!existsSync(join(ROOT, 'gradlew'))) done(); // Android 프로젝트가 아직 없음

// 작업 트리(추적 중인 파일 + gitignore되지 않은 새 파일)에 있는 앱 코드 전체의 내용 해시.
// gitignore 대상(secrets.properties, local.properties, build/ 등)은 읽지 않는다.
function appCodeDigest() {
  const out = git('ls-files', '-z', '--cached', '--others', '--exclude-standard');
  if (out === null) return null;
  const files = [...new Set(out.split('\0').filter((f) => f && isAppCode(f)))].sort();
  const h = createHash('sha256');
  for (const f of files) {
    h.update(`${f}\0`);
    try {
      h.update(readFileSync(join(ROOT, f)));
    } catch {
      h.update('<deleted>');
    }
  }
  return h.digest('hex');
}

// HEAD 대비 바뀐 앱 코드 파일 (스테이징 여부 무관, 새 파일 포함)
function changedAppFiles() {
  const out = git('status', '--porcelain', '-z', '--untracked-files=all');
  if (out === null) return [];
  const entries = out.split('\0').filter(Boolean);
  const files = [];
  for (let i = 0; i < entries.length; i++) {
    const status = entries[i].slice(0, 2);
    files.push(entries[i].slice(3));
    if (status.includes('R') || status.includes('C')) i++; // -z에서는 rename 원본 경로가 다음 항목
  }
  return files.filter(isAppCode);
}

function stagedAppFiles() {
  const out = git('diff', '--cached', '--name-only', '-z');
  return out === null ? [] : out.split('\0').filter((f) => f && isAppCode(f));
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

// Windows 콘솔 출력은 CP949일 수 있다. UTF-8로 안 읽히면 EUC-KR로 다시 읽는다.
function decode(buf) {
  if (!buf || buf.length === 0) return '';
  try {
    return new TextDecoder('utf-8', { fatal: true }).decode(buf);
  } catch {
    return new TextDecoder('euc-kr').decode(buf);
  }
}

function build() {
  const args = [...TASKS, '--console=plain', '-q'];
  const opts = { cwd: ROOT, timeout: 570_000, maxBuffer: 32 * 1024 * 1024 };
  // Windows: 현재 폴더 실행이 막힌 환경(NoDefaultCurrentDirectoryInExePath)이 있어 절대 경로로 실행한다.
  const run =
    process.platform === 'win32'
      ? spawnSync(`"${join(ROOT, 'gradlew.bat')}"`, args, { ...opts, shell: true })
      : spawnSync('sh', ['gradlew', ...args], opts);
  const tail = `${decode(run.stdout)}\n${decode(run.stderr)}`.trim().split(/\r?\n/).slice(-80).join('\n');
  return { ok: run.status === 0, tail, error: run.error?.message };
}

const ENV_HINT =
  '코드로 고칠 수 없는 환경 문제(Android SDK 미설치, 네트워크, Pleos 저장소 접근 등)라면 고치려 하지 말고 사용자에게 원인을 보고하고 멈춰라.';

const digest = appCodeDigest();

if (mode === 'snapshot') {
  save({ turnDigest: digest });
  done();
}

if (mode === 'precommit') {
  // 이번 커밋에 앱 코드가 들어가는 경우만 검사한다.
  // 같은 명령 안에서 git add나 commit -a를 하면 아직 스테이징 전이므로, 바뀐 앱 코드가 있으면 들어간다고 본다.
  const addsInline = GIT_ADD.test(command) || COMMIT_ALL.test(command);
  const includesAppCode = stagedAppFiles().length > 0 || (addsInline && changedAppFiles().length > 0);
  if (!includesAppCode) done();
  if (digest !== null && digest === state.okDigest) done(); // 이 상태는 이미 빌드 통과

  const result = build();
  if (result.ok) {
    save({ okDigest: digest });
    done();
  }
  block(
    [
      `[build-gate] 커밋 전 gradlew ${TASKS.join(' ')} 실패. 커밋하지 않았다. 아래 에러를 고친 뒤 다시 커밋하라.`,
      ENV_HINT,
      result.error ? `실행 오류: ${result.error}` : '',
      '',
      result.tail,
    ].join('\n'),
  );
}

// ---- stop ----
if (digest === null) done();
if (digest === state.turnDigest) done(); // 이번 턴에 앱 코드를 건드리지 않음
if (digest === state.okDigest) done(); // 이 상태는 이미 빌드 통과
if (input.stop_hook_active && digest === state.failDigest) {
  done('⚠ 빌드 실패 상태로 턴이 끝났습니다 (build-gate). gradlew assembleDebug로 확인하세요.');
}
const fails = state.session === input.session_id ? (state.fails ?? 0) : 0;
if (fails >= MAX_FAILS) {
  done(`⚠ 빌드가 ${MAX_FAILS}회 연속 실패해 build-gate가 더 이상 막지 않습니다.`);
}

const result = build();
if (result.ok) {
  save({ okDigest: digest, failDigest: null, fails: 0, session: input.session_id });
  done();
}

save({ failDigest: digest, fails: fails + 1, session: input.session_id });
block(
  [
    `[build-gate] gradlew ${TASKS.join(' ')} 실패 (${fails + 1}/${MAX_FAILS}). 아래 에러를 고친 뒤 마무리하라.`,
    ENV_HINT,
    result.error ? `실행 오류: ${result.error}` : '',
    '',
    result.tail,
  ].join('\n'),
);
