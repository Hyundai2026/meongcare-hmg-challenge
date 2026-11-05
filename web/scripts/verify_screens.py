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
import json
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
    context_options: dict = field(default_factory=dict)


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


def save_button(r: Run):
    return r.page.get_by_role("button", name="저장", exact=True)


def open_register(r: Run) -> None:
    r.open()
    r.page.get_by_role("button", name="반려견 등록하기").click()
    expect(r.page.get_by_role("heading", name="반려견 등록")).to_be_visible()


def register_bori(r: Run, photo: Path | None = None) -> None:
    """빈 화면에서 보리를 등록한다 (S4·S5 값)."""
    open_register(r)
    r.page.get_by_label("이름").fill("보리")
    r.page.get_by_label("품종").fill("골든리트리버")
    r.page.get_by_label("생년월일").fill("2014-08-01")
    r.page.get_by_text("수컷", exact=True).click()
    if photo is not None:
        r.page.get_by_label("사진 선택").set_input_files(str(photo))
        expect(r.page.locator(".photo-picker__preview img")).to_be_visible()
    save_button(r).click()
    expect(r.page.get_by_role("button", name="프로필 수정")).to_be_visible()


def stored_json(r: Run) -> dict:
    raw = r.stored_raw()
    r.check(raw is not None, f"localStorage에 {STORAGE_KEY} 키가 없다")
    return json.loads(raw)


def s02_name_required(r: Run) -> None:
    open_register(r)
    save_button(r).click()
    expect(r.page.get_by_text("이름을 입력해 주세요")).to_be_visible()
    focused = r.page.evaluate("() => document.activeElement && document.activeElement.id")
    r.check(focused == "pf-name", f"첫 오류 칸(이름)에 포커스가 가지 않았다: {focused!r}")
    r.check(r.stored_raw() is None, "저장되면 안 되는데 저장됐다")
    r.shot("name-required")


def s03_limits(r: Run) -> None:
    open_register(r)
    r.page.get_by_label("이름").fill("가" * 21)
    save_button(r).click()
    expect(r.page.get_by_text("20자까지 쓸 수 있어요")).to_be_visible()
    r.page.get_by_label("이름").fill("보리")
    r.page.get_by_label("생년월일").fill("2026-12-01")
    save_button(r).click()
    expect(r.page.get_by_text("오늘 이후 날짜는 고를 수 없어요")).to_be_visible()
    r.page.get_by_text("나이로 입력", exact=True).click()
    r.page.get_by_label("나이(세)").fill("1")
    r.page.get_by_label("나이(개월)").fill("12")
    save_button(r).click()
    expect(r.page.get_by_text("0~30세, 0~11개월로 입력해 주세요")).to_be_visible()
    r.check(r.stored_raw() is None, "저장되면 안 되는데 저장됐다")
    r.shot("age-invalid")


def s10_cancel(r: Run) -> None:
    register_bori(r)
    before = r.stored_raw()
    r.page.get_by_role("button", name="프로필 수정").click()
    r.page.get_by_label("이름").fill("보리2")
    r.page.get_by_role("button", name="취소").click()
    expect(r.page.get_by_role("button", name="프로필 수정")).to_be_visible()
    expect(r.page.get_by_text("보리2")).to_have_count(0)
    r.check(r.stored_raw() == before, "취소했는데 저장된 값이 바뀌었다")


def expect_bori_card(r: Run) -> None:
    expect(r.page.locator(".profile__name")).to_have_text("보리 ♂")
    expect(r.page.locator(".profile__sub")).to_have_text("골든리트리버 · 12세 3개월")
    expect(r.page.get_by_role("button", name="프로필 수정")).to_be_visible()


def s04_s05_register(r: Run) -> None:
    register_bori(r)
    expect_bori_card(r)
    expect(r.page.get_by_role("tab", name="의료정보")).to_have_attribute("aria-selected", "true")
    expect(r.page.get_by_text("등록된 정보 없음")).to_have_count(3)
    expect(r.page.get_by_text(STORAGE_NOTICE)).to_be_visible()
    saved = stored_json(r)
    r.check(saved.get("schemaVersion") == 1, f"schemaVersion이 1이 아니다: {saved.get('schemaVersion')!r}")
    r.check(saved.get("birthDate") == "2014-08-01" and saved.get("sex") == "male", f"저장된 값이 다르다: {saved}")
    check_no_horizontal_scroll(r)
    r.shot("profile")


def s06_basic_tab(r: Run) -> None:
    register_bori(r)
    r.page.get_by_role("tab", name="기본정보").click()
    panel = r.page.get_by_role("tabpanel")
    for text in ["보리", "골든리트리버", "수컷", "2014년 8월 1일생", "12세 3개월"]:
        expect(panel.get_by_text(text, exact=True)).to_be_visible()
    r.shot("basic-tab")


def s09_reload(r: Run) -> None:
    register_bori(r)
    r.page.reload()
    expect_bori_card(r)


def s15_dark(r: Run) -> None:
    register_bori(r)
    bg = r.page.evaluate("() => getComputedStyle(document.body).backgroundColor")
    r.check(bg == "rgb(243, 246, 252)", f"다크 설정에서 배경이 바뀌었다: {bg}")
    expect_bori_card(r)
    r.shot("dark")


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
    Scenario("S02", "이름 없이 저장", s02_name_required),
    Scenario("S03", "글자 수·날짜·나이 범위", s03_limits),
    Scenario("S05", "보리 등록 → 프로필 화면", s04_s05_register, widths=(390, 360)),
    Scenario("S06", "기본정보 탭", s06_basic_tab),
    Scenario("S09", "새로고침해도 남는다", s09_reload),
    Scenario("S10", "수정하다 취소", s10_cancel),
    Scenario("S12", "저장된 정보가 깨졌을 때", s12_corrupted),
    Scenario("S15", "폰이 다크 모드일 때", s15_dark, context_options={"color_scheme": "dark"}),
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
        **scenario.context_options,
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
