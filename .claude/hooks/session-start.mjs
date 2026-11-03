#!/usr/bin/env node
// SessionStart 훅: 마감 D-day와 진행 중인 계획을 세션 컨텍스트에 넣는다. (stdout이 컨텍스트로 들어감)
import { readdirSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

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
console.log(lines.join('\n'));
