#!/usr/bin/env node
// SessionStart 훅: 마감 D-day와 진행 중인 계획을 세션 컨텍스트에 넣는다. (stdout이 컨텍스트로 들어감)
// 저장소의 git 훅(.githooks/)이 꺼져 있으면 켠다.
import { readdirSync, existsSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
import { spawnSync } from 'node:child_process';

const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..', '..');
const DEADLINES = [
  ['접수 마감', '2026-11-15'],
  ['2차 PT', '2026-12-03'],
];

const kst = new Date(Date.now() + 9 * 3600 * 1000);
const today = Date.UTC(kst.getUTCFullYear(), kst.getUTCMonth(), kst.getUTCDate());
const dday = DEADLINES.flatMap(([label, date]) => {
  const [y, m, d] = date.split('-').map(Number);
  const diff = Math.round((Date.UTC(y, m - 1, d) - today) / 86_400_000);
  return diff >= 0 ? [`${label}(${date}) D-${diff}`] : [];
});

let plans = [];
try {
  plans = readdirSync(join(ROOT, 'docs', 'plans', 'active')).filter((f) => f.endsWith('.md'));
} catch {}

const lines = [`[하네스] 오늘(KST) ${new Date(today).toISOString().slice(0, 10)}. ${dday.join(' / ')}`];
lines.push(
  plans.length
    ? `진행 중 계획: ${plans.map((p) => `docs/plans/active/${p}`).join(', ')}. 관련 작업이면 먼저 읽고 이어서 진행할 것.`
    : '진행 중 계획 없음.',
);

// 보호 브랜치(main·dev)에서 세션을 시작했으면 작업 브랜치부터 따라고 알린다.
const branch = spawnSync('git', ['branch', '--show-current'], { cwd: ROOT, encoding: 'utf8' }).stdout?.trim();
if (['main', 'master', 'dev'].includes(branch)) {
  lines.push(
    `현재 브랜치: ${branch}. 여기에는 직접 커밋하지 않는다. 작업은 dev에서 딴 브랜치에서 한다: ` +
      'git switch dev && git pull --ff-only && git switch -c feat/<이름>',
  );
}

// dev·main push 전 검증 훅(.githooks/pre-push)을 켠다. 이 저장소의 로컬 설정만 바꾼다.
// 다른 값이 이미 설정돼 있으면 덮어쓰지 않고 알리기만 한다.
if (existsSync(join(ROOT, '.githooks'))) {
  const current = spawnSync('git', ['config', '--get', 'core.hooksPath'], { cwd: ROOT, encoding: 'utf8' }).stdout?.trim();
  if (!current) {
    const r = spawnSync('git', ['config', 'core.hooksPath', '.githooks'], { cwd: ROOT });
    lines.push(
      r.status === 0
        ? 'git core.hooksPath를 .githooks로 설정했다 (main push 전 검증 파이프라인).'
        : '⚠ git 훅을 켜지 못했다. 직접 실행: git config core.hooksPath .githooks',
    );
  } else if (current !== '.githooks') {
    lines.push(`⚠ core.hooksPath가 ${current}로 설정돼 있어 main push 전 검증 훅이 꺼져 있다. 필요하면: git config core.hooksPath .githooks`);
  }
}
console.log(lines.join('\n'));
