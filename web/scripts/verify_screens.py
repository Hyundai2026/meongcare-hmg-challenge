"""보호자 웹 화면 확인: 폰 크기 브라우저로 시나리오를 돌리고 스크린샷을 남긴다.

사용법 (저장소 루트에서, 미리보기 서버를 먼저 띄운다. web/README.md "화면 확인"):
    python web/scripts/verify_screens.py --base-url http://127.0.0.1:4173 --out .claude/.cache/web

- 표준 라이브러리 + playwright만 쓴다. Chromium만 쓴다.
- 날짜는 2026-11-05로 고정한다 (나이 표시가 매번 같게).
- 시나리오마다 새 브라우저 컨텍스트(빈 localStorage)로 시작한다.
- 콘솔 에러, 페이지 에러, 같은 출처 밖으로 가는 요청이 있으면 그 시나리오는 실패다 (S16).
- 모두 맞으면 종료 코드 0, 하나라도 틀리면 1.
"""

from __future__ import annotations

import argparse
import re
import sys
from dataclasses import dataclass, field
from pathlib import Path
from typing import Callable

from playwright.sync_api import Browser, BrowserContext, Page, expect, sync_playwright

ROOT = Path(__file__).resolve().parents[2]
CONFIG_TS = ROOT / "web" / "src" / "config.ts"

FIXED_TIME = "2026-11-05T09:00:00"
STORAGE_KEY = "owner-web.pet-profile"
VIEWPORTS = {390: (390, 844), 360: (360, 800)}
TIMEOUT_MS = 5_000


def read_app_name() -> str:
    match = re.search(r"APP_NAME\s*=\s*'([^']+)'", CONFIG_TS.read_text(encoding="utf-8"))
    if match is None:
        raise SystemExit(f"{CONFIG_TS}에서 APP_NAME을 찾지 못했다")
    return match.group(1)


APP_NAME = read_app_name()


class ScenarioFailure(Exception):
    pass


@dataclass
class Run:
    """시나리오 하나가 쓰는 것: 페이지, 스크린샷, 확인."""

    page: Page
    context: BrowserContext
    base_url: str
    out_dir: Path
    scenario_id: str
    width: int
    problems: list[str] = field(default_factory=list)

    def open(self, path: str = "/") -> None:
        self.page.goto(self.base_url + path)

    def shot(self, name: str) -> Path:
        path = self.out_dir / f"{self.scenario_id.lower()}-{name}-{self.width}.png"
        self.page.screenshot(path=str(path), full_page=True)
        return path

    def check(self, condition: bool, message: str) -> None:
        if not condition:
            raise ScenarioFailure(message)

    def stored_raw(self) -> str | None:
        return self.page.evaluate(f"() => localStorage.getItem({STORAGE_KEY!r})")


@dataclass
class Scenario:
    id: str
    title: str
    run: Callable[[Run], None]
    widths: tuple[int, ...] = (390,)


# ---------------------------------------------------------------- 시나리오

STORAGE_NOTICE = "이 정보는 지금 쓰는 브라우저에만 저장돼요. 차 앱과의 연결은 준비 중이에요."


def check_no_horizontal_scroll(r: Run) -> None:
    over = r.page.evaluate("() => document.documentElement.scrollWidth - window.innerWidth")
    r.check(over <= 0, f"가로 스크롤이 생겼다 ({over}px 넘침)")


def s01_empty(r: Run) -> None:
    r.open()
    expect(r.page.locator("header")).to_contain_text(APP_NAME)
    r.check(r.page.title() == APP_NAME, f"document.title이 {APP_NAME!r}가 아니다: {r.page.title()!r}")
    expect(r.page.get_by_text("아직 등록된 반려견이 없어요")).to_be_visible()
    expect(r.page.get_by_role("button", name="반려견 등록하기")).to_be_visible()
    expect(r.page.get_by_text(STORAGE_NOTICE)).to_be_visible()
    check_no_horizontal_scroll(r)
    r.shot("empty")


def s12_corrupted(r: Run) -> None:
    r.open()
    r.page.evaluate(f"() => localStorage.setItem({STORAGE_KEY!r}, '{{broken')")
    r.page.reload()
    expect(r.page.get_by_text("저장된 정보를 읽지 못했어요")).to_be_visible()
    expect(r.page.get_by_role("button", name="새로 등록하기")).to_be_visible()
    r.check(r.stored_raw() == "{broken", "읽기만 했는데 저장된 값이 바뀌었다")
    r.shot("corrupted")


SCENARIOS: list[Scenario] = [
    Scenario("S01", "빈 화면", s01_empty, widths=(390, 360)),
    Scenario("S12", "저장된 정보가 깨졌을 때", s12_corrupted),
]


# ---------------------------------------------------------------- 실행

def is_same_origin(url: str, base_url: str) -> bool:
    return url.startswith(base_url) or url.startswith("data:") or url.startswith("blob:")


def run_scenario(browser: Browser, scenario: Scenario, width: int, base_url: str, out_dir: Path) -> list[str]:
    vw, vh = VIEWPORTS[width]
    context = browser.new_context(
        viewport={"width": vw, "height": vh},
        is_mobile=True,
        has_touch=True,
        locale="ko-KR",
        device_scale_factor=2,
    )
    context.set_default_timeout(TIMEOUT_MS)
    page = context.new_page()
    page.clock.set_fixed_time(FIXED_TIME)

    noise: list[str] = []
    page.on("console", lambda msg: noise.append(f"콘솔 에러: {msg.text}") if msg.type == "error" else None)
    page.on("pageerror", lambda err: noise.append(f"페이지 에러: {err}"))
    context.on(
        "request",
        lambda req: None if is_same_origin(req.url, base_url) else noise.append(f"밖으로 가는 요청: {req.url}"),
    )

    run = Run(page=page, context=context, base_url=base_url, out_dir=out_dir, scenario_id=scenario.id, width=width)
    problems: list[str] = []
    try:
        scenario.run(run)
    except ScenarioFailure as e:
        problems.append(str(e))
    except AssertionError as e:  # playwright expect
        problems.append(str(e).splitlines()[0] if str(e) else "expect 실패")
    except Exception as e:  # noqa: BLE001 - 어떤 확인이 틀렸는지 보이고 다음 시나리오로 간다
        problems.append(f"{type(e).__name__}: {str(e).splitlines()[0] if str(e) else ''}")
        try:
            run.shot("error")
        except Exception:  # noqa: BLE001
            pass
    problems.extend(noise)
    context.close()
    return problems


def main() -> int:
    sys.stdout.reconfigure(encoding="utf-8")
    parser = argparse.ArgumentParser(description="보호자 웹 화면 확인")
    parser.add_argument("--base-url", default="http://127.0.0.1:4173")
    parser.add_argument("--out", default=str(ROOT / ".claude" / ".cache" / "web"))
    parser.add_argument("--only", nargs="*", help="이 시나리오만 (예: S01 S05)")
    args = parser.parse_args()

    base_url = args.base_url.rstrip("/")
    out_dir = Path(args.out)
    out_dir.mkdir(parents=True, exist_ok=True)
    selected = [s for s in SCENARIOS if not args.only or s.id in {x.upper() for x in args.only}]

    failed = 0
    with sync_playwright() as p:
        browser = p.chromium.launch()
        for scenario in selected:
            for width in scenario.widths:
                problems = run_scenario(browser, scenario, width, base_url, out_dir)
                label = f"{scenario.id} {scenario.title} ({width}px)"
                if problems:
                    failed += 1
                    print(f"실패  {label}")
                    for problem in problems:
                        print(f"      - {problem}")
                else:
                    print(f"통과  {label}")
        browser.close()

    total = sum(len(s.widths) for s in selected)
    print(f"\n{total - failed}/{total} 통과. 스크린샷: {out_dir}")
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
