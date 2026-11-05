#!/usr/bin/env node
// 빌드 게이트. 다음을 막는다:
//   - 앱 코드가 빌드되지 않는 상태로 커밋되는 것
//   - 그런 상태로 턴이 끝나는 것
//   - 검증 파이프라인을 통과하지 않은 상태로 dev나 main에 push되는 것
//   - /promote를 거치지 않고 main에 push되는 것
//
// 브랜치 (docs/conventions/git.md)
//   dev   통합 브랜치. /ship이 작업 브랜치를 merge한다.
//   main  제출·시연용. /promote로만 바뀐다 (PROMOTE_MAIN=1). dev에 이미 있는 커밋만 올라간다.
//
// Claude Code 훅 (.claude/settings.json)
//   snapshot  (UserPromptSubmit) 턴 시작 시점의 앱 코드 해시를 기록한다.
//   precommit (PreToolUse: Bash|PowerShell) git commit 직전에 검사한다.
//             - 커밋 메시지나 PR 본문(gh pr create/edit)에 Claude 트레일러가 있으면 막는다.
//             - main·dev에 직접 커밋하면 막는다. 저장소의 첫 커밋은 예외. merge는 막지 않는다.
//             - 앱 코드가 바뀌었고 아직 빌드를 확인하지 않은 상태면 assembleDebug를 돌린다. 실패하면 커밋을 막는다.
//   stop      (Stop) 이번 턴에 앱 코드가 바뀌었고 아직 빌드를 확인하지 않았으면 assembleDebug를 돌린다.
//             실패하면 exit 2로 에러를 돌려줘서, Claude가 멈추지 않고 이어서 고치게 한다.
//
// 직접 실행 / git 훅
//   pipeline  (/ship 스킬) 검증 파이프라인(CI와 같은 작업)을 돌리고, 통과하면 기록한다.
//             - Android: gradlew assembleDebug testDebugUnitTest lintDebug (gradlew가 있을 때)
//             - 보호자 웹: npm --prefix web run check (web/package.json이 있을 때)
//               웹 의존성이 설치되지 않았거나 package.json과 다르면 멈추고 npm --prefix web ci를 하라고 알린다.
//               설치는 자동으로 하지 않는다 (패키지 설치 스크립트가 돌기 때문. 사람이 확인하고 한다).
//   prepush   (git pre-push 훅: .githooks/pre-push) dev·main에 push할 때 검사한다.
//             - main·dev 브랜치를 지우는 push는 막는다.
//             - main push는 PROMOTE_MAIN=1이어야 하고, origin/dev에 이미 있는 커밋이어야 한다.
//             - 검증 파이프라인 통과를 확인한다.
//             - 아래 경우에는 막는다. 검증한 대상과 push하는 대상이 달라지기 때문이다.
//               - 커밋 안 된 앱 코드나 웹 파일이 있을 때
//               - push할 커밋이 체크아웃돼 있지 않을 때
//             - pipeline으로 이미 통과한 상태면 다시 돌리지 않는다.
//
// 웹 파일(web/, docs/data/)은 pipeline과 prepush에서만 검사한다. precommit·stop은 Android 앱 코드만 본다
// (008 사용자 결정: 웹은 dev에 들어갈 때만 검사).
//
// 해시는 커밋 여부와 상관없이 "작업 트리에 있는 앱 코드 전체의 내용"으로 만든다.
// 그래서 턴 중에 커밋해도 Stop 검사를 건너뛰지 않는다.
// 파이프라인 통과 기록은 앱 코드 해시와 웹 파일 해시를 합친 값이다. 웹만 바뀌어도 다시 돈다.
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
const IGNORE = /^(\.claude|docs|apk|web)\//;
const TASKS = ['assembleDebug'];
const PIPELINE = ['assembleDebug', 'testDebugUnitTest', 'lintDebug']; // .github/workflows/android.yml과 같게 유지
const WEB_DIR = 'web';
const WEB_FILES = /^(web|docs\/data)\//; // 웹 테스트가 docs/data/의 예시 JSON을 읽는다
const WEB_CHECK = ['--prefix', WEB_DIR, 'run', 'check']; // .github/workflows/web.yml과 같게 유지
const MAX_FAILS = 3;
const RELEASE_BRANCHES = ['main', 'master']; // /promote로만 바뀐다
const INTEGRATION_BRANCH = 'dev'; // /ship이 merge한다
const PROTECTED_BRANCHES = [...RELEASE_BRANCHES, INTEGRATION_BRANCH]; // 직접 커밋·삭제 금지, push 전 검증
const GIT = String.raw`(^|[\s;&|(])git(\s+-[cC]\s+\S+)*\s+`;
const GIT_COMMIT = new RegExp(`${GIT}commit(\\s|$)`);
const GIT_ADD = new RegExp(`${GIT}add\\s`);
const COMMIT_ALL = /\scommit\b.*\s(--all|-[A-Za-z]*a[A-Za-z]*)(\s|$)/;
const GH_PR = /(^|[\s;&|(])gh\s+pr\s+(create|edit)\b/;
const CLAUDE_TRAILER = /Co-Authored-By:\s*Claude|Generated with \[?Claude Code|claude\.com\/claude-code/i;

const mode = process.argv[2] ?? 'stop';

function done(userMessage) {
  if (userMessage) process.stdout.write(JSON.stringify({ systemMessage: userMessage }));
  process.exit(0);
}

function block(message) {
  process.stderr.write(`${message}\n`);
  process.exit(2);
}

function say(message) {
  process.stderr.write(`${message}\n`);
}

// pipeline은 사람이나 Claude가 직접 실행한다. stdin을 읽으면 입력을 기다리며 멈출 수 있어 읽지 않는다.
let stdin = '';
if (mode !== 'pipeline') {
  try {
    stdin = readFileSync(0, 'utf8');
  } catch {}
}
let input = {};
try {
  input = JSON.parse(stdin || '{}');
} catch {}

// precommit은 모든 셸 명령마다 불린다. git commit이 아니면 가장 먼저 내보낸다.
const command = input.tool_input?.command ?? '';
if (mode === 'precommit' && (GIT_COMMIT.test(command) || GH_PR.test(command)) && CLAUDE_TRAILER.test(command)) {
  block(
    '[build-gate] 커밋 메시지와 PR 본문에 Claude 트레일러(Co-Authored-By: Claude, Generated with Claude Code)를 넣지 않는다 ' +
      '(docs/conventions/git.md). 그 줄을 빼고 다시 실행하라.',
  );
}
if (mode === 'precommit' && !GIT_COMMIT.test(command)) done();

function git(...args) {
  const r = spawnSync('git', ['-c', 'core.quotepath=false', ...args], { cwd: ROOT, encoding: 'utf8' });
  return r.status === 0 ? r.stdout : null;
}

const isAppCode = (f) => WATCH.test(f) && !IGNORE.test(f);
const isWebFile = (f) => WEB_FILES.test(f);
const isPipelineFile = (f) => isAppCode(f) || isWebFile(f);
const hasGradle = existsSync(join(ROOT, 'gradlew'));
const hasWeb = existsSync(join(ROOT, WEB_DIR, 'package.json'));

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

// prepush: git이 stdin으로 "<local ref> <local sha> <remote ref> <remote sha>" 줄을 넘긴다.
// 브랜치 규칙은 Android 프로젝트가 없어도 적용한다.
const pushes = []; // 보호 브랜치로 가는 push: { branch, sha }
if (mode === 'prepush') {
  for (const line of stdin.split(/\r?\n/)) {
    const [, localSha, remoteRef] = line.trim().split(/\s+/);
    if (!localSha || !remoteRef?.startsWith('refs/heads/')) continue;
    const branch = remoteRef.slice('refs/heads/'.length);
    if (!PROTECTED_BRANCHES.includes(branch)) continue;
    if (/^0+$/.test(localSha)) block(`[build-gate] ${branch} 브랜치는 지우지 않는다.`);
    pushes.push({ branch, sha: localSha });
  }
  if (pushes.length === 0) process.exit(0); // 작업 브랜치 push

  for (const { branch, sha } of pushes) {
    if (!RELEASE_BRANCHES.includes(branch)) continue;
    if (process.env.PROMOTE_MAIN !== '1') {
      block(
        `[build-gate] ${branch}에는 /promote로만 push한다 (docs/conventions/git.md). ` +
          `평소 작업은 /ship으로 ${INTEGRATION_BRANCH}에 반영한다.`,
      );
    }
    if (git('merge-base', '--is-ancestor', sha, `refs/remotes/origin/${INTEGRATION_BRANCH}`) === null) {
      block(
        `[build-gate] ${branch}에는 origin/${INTEGRATION_BRANCH}에 이미 있는 커밋만 올린다. ` +
          `${INTEGRATION_BRANCH}에 먼저 반영(/ship)하고, git fetch origin 후 /promote 한다.`,
      );
    }
  }
}

const isPipelineMode = mode === 'pipeline' || mode === 'prepush';
if (!hasGradle && !(isPipelineMode && hasWeb)) {
  // Android 프로젝트가 아직 없음 (pipeline·prepush는 웹이 있으면 웹만 검사한다)
  if (isPipelineMode) say('[build-gate] gradlew와 web/package.json이 없어 검증 파이프라인을 건너뛴다.');
  done();
}

// 작업 트리(추적 중인 파일 + gitignore되지 않은 새 파일)에서 조건에 맞는 파일 전체의 내용 해시.
// gitignore 대상(secrets.properties, local.properties, build/, web/node_modules/ 등)은 읽지 않는다.
// 줄바꿈(CRLF/LF)은 무시한다. Windows(autocrlf)에서는 브랜치 전환이나 merge 때 git이 파일을 CRLF로 다시 써서,
// 내용이 같아도 해시가 바뀌어 빌드·파이프라인을 다시 돌리게 되기 때문이다.
function appCodeDigest() {
  return digestOf(isAppCode);
}

function digestOf(predicate) {
  const out = git('ls-files', '-z', '--cached', '--others', '--exclude-standard');
  if (out === null) return null;
  const files = [...new Set(out.split('\0').filter((f) => f && predicate(f)))].sort();
  const h = createHash('sha256');
  for (const f of files) {
    h.update(`${f}\0`);
    try {
      h.update(readFileSync(join(ROOT, f), 'latin1').replace(/\r\n/g, '\n'), 'latin1');
    } catch {
      h.update('<deleted>');
    }
  }
  return h.digest('hex');
}

// HEAD 대비 바뀐 앱 코드 파일 (스테이징 여부 무관, 새 파일 포함)
function changedAppFiles() {
  return changedFiles(isAppCode);
}

function changedFiles(predicate) {
  const out = git('status', '--porcelain', '-z', '--untracked-files=all');
  if (out === null) return [];
  const entries = out.split('\0').filter(Boolean);
  const files = [];
  for (let i = 0; i < entries.length; i++) {
    const status = entries[i].slice(0, 2);
    files.push(entries[i].slice(3));
    if (status.includes('R') || status.includes('C')) i++; // -z에서는 rename 원본 경로가 다음 항목
  }
  return files.filter(predicate);
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

function build(tasks) {
  const args = [...tasks, '--console=plain', '-q'];
  const opts = { cwd: ROOT, timeout: 570_000, maxBuffer: 32 * 1024 * 1024 };
  // Windows: 현재 폴더 실행이 막힌 환경(NoDefaultCurrentDirectoryInExePath)이 있어 절대 경로로 실행한다.
  const run =
    process.platform === 'win32'
      ? spawnSync(`"${join(ROOT, 'gradlew.bat')}"`, args, { ...opts, shell: true })
      : spawnSync('sh', ['gradlew', ...args], opts);
  const tail = `${decode(run.stdout)}\n${decode(run.stderr)}`.trim().split(/\r?\n/).slice(-80).join('\n');
  return { ok: run.status === 0, tail, error: run.error?.message };
}

function npm(args) {
  const opts = { cwd: ROOT, timeout: 570_000, maxBuffer: 32 * 1024 * 1024 };
  // Windows: npm은 npm.cmd라서 셸로 실행한다.
  const run = spawnSync('npm', args, process.platform === 'win32' ? { ...opts, shell: true } : opts);
  const tail = `${decode(run.stdout)}\n${decode(run.stderr)}`.trim().split(/\r?\n/).slice(-80).join('\n');
  return { ok: run.status === 0, tail, error: run.error?.message };
}

// 웹 의존성이 package.json과 맞게 설치돼 있는지 먼저 본다 (npm ls는 없거나 버전이 다르면 실패한다).
function webCheck() {
  const installed = npm(['--prefix', WEB_DIR, 'ls', '--depth=0']);
  if (!installed.ok) {
    return {
      ...installed,
      hint: `웹 의존성이 설치되지 않았거나 web/package.json과 다르다. npm --prefix ${WEB_DIR} ci를 실행한 뒤 다시 하라 (설치는 자동으로 하지 않는다).`,
    };
  }
  return npm(WEB_CHECK);
}

const ENV_HINT =
  '코드로 고칠 수 없는 환경 문제(Android SDK 미설치, Node 미설치, 네트워크, Pleos 저장소 접근 등)라면 고치려 하지 말고 사용자에게 원인을 보고하고 멈춰라.';

const digest = appCodeDigest();

if (mode === 'snapshot') {
  save({ turnDigest: digest });
  done();
}

if (mode === 'pipeline' || mode === 'prepush') {
  if (mode === 'prepush') {
    const head = git('rev-parse', 'HEAD')?.trim();
    const off = pushes.find((p) => p.sha !== head);
    if (off) {
      block(
        `[build-gate] push하려는 ${off.branch} 커밋(${off.sha.slice(0, 7)})이 체크아웃돼 있지 않다. ` +
          `검증은 작업 트리 기준이라, git switch ${off.branch} 후 push한다.`,
      );
    }
    if (changedFiles(isPipelineFile).length > 0) {
      block('[build-gate] 커밋 안 된 앱 코드나 웹 파일 변경이 있다. 검증 대상과 push 대상이 달라지므로 커밋하거나 치운 뒤 push한다.');
    }
  }
  const webDigest = hasWeb ? digestOf(isWebFile) : 'no-web';
  const pipelineDigest = digest === null || webDigest === null ? null : `${hasGradle ? digest : 'no-gradle'}|${webDigest}`;
  if (pipelineDigest !== null && pipelineDigest === state.pipelineDigest) {
    say('[build-gate] 이 상태는 이미 검증 파이프라인을 통과했다.');
    process.exit(0);
  }
  const steps = [hasGradle && `gradlew ${PIPELINE.join(' ')}`, hasWeb && `npm ${WEB_CHECK.join(' ')}`].filter(Boolean);
  say(`[build-gate] 검증 파이프라인 실행: ${steps.join(' + ')}`);
  const failed = (part, result) =>
    block(
      [
        `[build-gate] 검증 파이프라인 실패 (${part}).${mode === 'prepush' ? ' push하지 않았다.' : ''} 아래 에러를 고친 뒤 다시 실행하라.`,
        result.hint ?? ENV_HINT,
        result.error ? `실행 오류: ${result.error}` : '',
        '',
        result.tail,
      ].join('\n'),
    );
  if (hasGradle) {
    const result = build(PIPELINE);
    if (!result.ok) failed('Android', result);
  }
  if (hasWeb) {
    const result = webCheck();
    if (!result.ok) failed('보호자 웹', result);
  }
  save({ pipelineDigest, ...(hasGradle ? { okDigest: digest } : {}) });
  say('[build-gate] 검증 파이프라인 통과.');
  process.exit(0);
}

if (mode === 'precommit') {
  // 이번 커밋에 앱 코드가 들어가는 경우만 검사한다.
  // 같은 명령 안에서 git add나 commit -a를 하면 아직 스테이징 전이므로, 바뀐 앱 코드가 있으면 들어간다고 본다.
  const addsInline = GIT_ADD.test(command) || COMMIT_ALL.test(command);
  const includesAppCode = stagedAppFiles().length > 0 || (addsInline && changedAppFiles().length > 0);
  if (!includesAppCode) done();
  if (digest !== null && digest === state.okDigest) done(); // 이 상태는 이미 빌드 통과

  const result = build(TASKS);
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

const result = build(TASKS);
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
