# BMAD로 기획 시작하기 (처음 쓰는 사람용)

이 문서는 개발을 몰라도 BMAD로 공모전 기획을 진행할 수 있도록 쓴 안내서예요.
순서대로 따라 하면 됩니다.

0. [5zum 전용: 처음 세팅 총정리](#0-5zum-전용-처음-세팅-총정리) ← 여기부터 따라 하세요
1. [BMAD가 뭐예요?](#1-bmad가-뭐예요)
2. [이 저장소 시작하기](#2-이-저장소-시작하기-처음-한-번만)
3. [BMAD 시작하기](#3-bmad-시작하기)
4. [쓰면 좋은 스킬](#4-쓰면-좋은-스킬)
5. [추천 진행 순서](#5-추천-진행-순서-1115-마감-기준)
6. [지켜야 할 것](#6-지켜야-할-것)
7. [자주 막히는 곳](#7-자주-막히는-곳)

---

## 0. 5zum 전용: 처음 세팅 총정리

아래 두 방법 중 **하나만** 하면 돼요. 제목을 누르면 펼쳐져요.

- **방법 1. 직접 입력 버전**: PowerShell에 명령어를 하나씩 붙여 넣어요. 무엇이 설치되는지 하나하나 보여요.
- **방법 2. 클로드코드 딸깍 버전**: Claude Code만 설치하면, 나머지는 붙여넣기 한 번으로 Claude가 해 줘요. 처음이라면 이쪽이 편해요.

각 단계가 무슨 뜻인지는 [2. 이 저장소 시작하기](#2-이-저장소-시작하기-처음-한-번만)에 자세히 있어요.

<details>
<summary><b>방법 1. 직접 입력 버전</b>: PowerShell에 명령어를 순서대로 붙여 넣기</summary>

<br>

**어디에 입력하나요?**

- 시작 메뉴에서 `PowerShell`을 검색해서 **Windows PowerShell**을 열어요.
- 명령을 복사해서 창에 붙여 넣고(마우스 오른쪽 클릭) **Enter**를 누르면 돼요.

### 1단계: 프로그램 설치

```powershell
winget install --id Microsoft.VisualStudioCode -e
winget install --id Git.Git -e
winget install --id OpenJS.NodeJS.LTS -e
winget install --id astral-sh.uv -e
```

- 한 줄씩 붙여 넣고 Enter를 눌러요.
- 약관 동의를 물으면 `Y`를 입력해요.
- "이미 설치되어 있음"이라고 나오는 건 괜찮아요. 넘어가면 돼요.
- 다 끝나면 **PowerShell 창을 닫고 새로 열어요.** 새 창에서 계속해요.

### 2단계: 설치 확인

```powershell
git --version
node --version
uv --version
uv run python --version
```

- 네 줄 모두 버전 번호(예: `git version 2.x`)가 나오면 성공이에요.
- 마지막 줄은 처음 실행할 때 Python을 자동으로 받느라 조금 걸릴 수 있어요.
- "인식되지 않습니다"라는 오류가 나오면 컴퓨터를 재시작한 뒤 다시 해 보세요.

### 3단계: Claude Code 설치

```powershell
code --install-extension anthropic.claude-code
```

- `code`를 찾을 수 없다고 나오면, VS Code를 한 번 실행했다가 닫고 PowerShell을 새로 연 뒤 다시 해 보세요.

### 4단계: git에 내 정보 등록

```powershell
git config --global user.name "5zum"
git config --global user.email "190751656+5zum@users.noreply.github.com"
```

- 이메일은 GitHub가 주는 **비공개용 주소**예요. 내 실제 이메일이 작업 기록에 남지 않아요.

### 5단계: 저장소 받기

```powershell
New-Item -ItemType Directory -Force $HOME\projects | Out-Null
cd $HOME\projects
git clone https://github.com/Hyundai2026/meongcare-hmg-challenge.git
cd meongcare-hmg-challenge
git switch dev
```

- clone할 때 GitHub 로그인 창이 뜨면 **5zum 계정**으로 로그인해요.
- `Repository not found`가 나오면 아직 저장소 권한이 없는 거예요.
  - 팀장에게 초대를 요청하고, 메일로 온 초대를 수락한 뒤 다시 해요.
- 저장소는 `내 사용자 폴더\projects\meongcare-hmg-challenge`에 받아져요.

### 6단계: BMAD가 내 이름을 부르게 하기

아래 **4줄을 한 번에** 복사해서 붙여 넣어요.

```powershell
@"
[core]
user_name = "5zum"
"@ | Set-Content -Encoding ascii _bmad\custom\config.user.toml
```

- 아무 메시지도 안 나오면 성공이에요.
- 이 파일은 내 컴퓨터에만 있고, 팀 저장소에는 올라가지 않아요.

### 7단계: VS Code로 열기

```powershell
code .
```

1. VS Code가 열리면 오른쪽 위 **Claude 아이콘**을 눌러 Claude Code 패널을 열어요.
2. 팀에서 쓰는 Claude 계정으로 로그인해요.
3. 첫 메시지에 남은 마감 일수(D-day)가 보이면 **세팅 끝!**
4. 입력창에 `/bmad-help`를 써 보세요. 여기서부터는 [3. BMAD 시작하기](#3-bmad-시작하기)를 보면 돼요.

</details>

<details>
<summary><b>방법 2. 클로드코드 딸깍 버전</b>: Claude Code에 한 번 붙여 넣으면 알아서 세팅</summary>

<br>

**직접 할 일은 이것뿐이에요**

1. **VS Code 설치**
   - https://code.visualstudio.com 에서 받아서 설치해요.
   - PowerShell을 쓸 줄 알면 `winget install --id Microsoft.VisualStudioCode -e` 한 줄로도 돼요.
2. **Claude Code 설치**
   - VS Code를 열고, 왼쪽의 **확장**(네모 4개 아이콘)을 눌러요.
   - `Claude Code`를 검색해서 설치하고, 팀에서 쓰는 Claude 계정으로 로그인해요.
3. **저장소 초대 수락**
   - 팀장에게 GitHub 저장소 초대를 요청해요.
   - 메일로 온 초대를 **5zum 계정**으로 수락해요.
4. **작업 위치 열기**
   - VS Code에서 **파일 → 폴더 열기**를 누르고, 내 사용자 폴더(`C:\Users\<내 이름>`)를 선택해요.
   - Claude가 여기서 작업을 시작해요.
5. **붙여넣기**
   - 오른쪽 위 **Claude 아이콘**을 눌러 Claude Code 패널을 열어요.
   - 아래 상자의 내용을 **통째로** 복사해서 붙여 넣고 Enter를 눌러요.

**붙여 넣을 내용**

```text
이 PC(Windows)에 우리 팀 BMAD 기획 환경을 세팅해줘. PowerShell로 진행하고, 단계마다 결과를 한두 줄로 알려줘.

1. 설치: Git, Node.js LTS, uv가 있는지 확인하고, 없는 것만 winget으로 설치해줘.
   - 패키지 ID: Git.Git, OpenJS.NodeJS.LTS, astral-sh.uv
   - winget 옵션: -e --accept-source-agreements --accept-package-agreements
   - 설치한 뒤에는 아래 명령으로 PATH에 새 경로를 더해서, 새로 설치한 프로그램이 바로 인식되게 해줘.
     명령마다 셸이 새로 열리면, 이후 명령 앞에도 이 줄을 붙여서 실행해줘.
     $env:Path += ';' + [Environment]::GetEnvironmentVariable('Path','Machine') + ';' + [Environment]::GetEnvironmentVariable('Path','User')
2. 확인: git, node, uv 버전과 `uv run python --version` 결과를 보여줘.
3. git 사용자 정보를 전역으로 설정해줘.
   - user.name: 5zum
   - user.email: 190751656+5zum@users.noreply.github.com
4. 저장소 받기:
   - $HOME\projects 폴더를 만들고, 거기에 https://github.com/Hyundai2026/meongcare-hmg-challenge.git 을 clone해줘.
   - clone한 뒤 dev 브랜치로 전환해줘.
   - 이미 받아져 있으면 clone하지 말고 dev로 전환한 뒤 git pull만 해줘.
   - clone 전에 GitHub 로그인 창이 뜰 수 있다고 미리 알려줘.
   - "Repository not found"가 나오면 멈추고, 저장소 초대를 수락하라고 알려줘.
5. BMAD 개인 설정:
   - 저장소 안에 _bmad\custom\config.user.toml 파일을 아래 두 줄로 만들어줘. UTF-8(BOM 없음)으로 저장해야 해.
     [core]
     user_name = "5zum"
   - 만든 뒤 저장소 폴더에서 `uv run _bmad/scripts/resolve_config.py --project-root . --key core`를 실행해서, user_name이 5zum이고 communication_language가 Korean인지 확인해줘.
6. 저장소의 다른 파일은 고치지 말고, 커밋이나 push도 하지 마. 세팅만 해줘.
7. 끝나면 결과를 요약하고, VS Code에서 그 저장소 폴더를 다시 여는 방법과 그다음에 /bmad-help를 입력하라고 알려줘.
```

**붙여 넣은 다음에**

1. 승인 창이 몇 번 떠요. 프로그램 설치, 명령 실행, GitHub 로그인 때예요.
   - 내용을 보고 **승인**을 눌러요.
   - GitHub 로그인 창에서는 **5zum 계정**으로 로그인해요.
2. Claude가 끝났다고 하면, VS Code에서 **파일 → 폴더 열기**로 `C:\Users\<내 이름>\projects\meongcare-hmg-challenge`를 열어요.
3. Claude 패널을 다시 열어요. 첫 메시지에 남은 마감 일수(D-day)가 보이면 **세팅 끝!**
4. 입력창에 `/bmad-help`를 써 보세요. 여기서부터는 [3. BMAD 시작하기](#3-bmad-시작하기)를 보면 돼요.

**잘 안 될 때**

- Claude Code가 "Git이 필요하다"는 식으로 말하며 시작되지 않을 수 있어요.
  - 그럴 때는 https://git-scm.com 에서 Git을 먼저 설치해요.
  - 그다음 VS Code를 껐다 켜고, 다시 붙여 넣어요.
- 중간에 막히면 Claude에게 "어디까지 됐어? 남은 거 이어서 해줘"라고 말해요.
- 그래도 안 되면 **방법 1**로 직접 입력하면 돼요.

</details>

### 매일 작업을 시작할 때

방법 1과 방법 2 모두 같아요. 둘 중 편한 쪽으로 하면 돼요.

- **VS Code에서**: **파일 → 최근 항목 열기**로 `meongcare-hmg-challenge` 폴더를 열고, Claude에게 "dev 최신으로 받아줘"라고 말해요.
- **PowerShell에서**:

  ```powershell
  cd $HOME\projects\meongcare-hmg-challenge
  git switch dev
  git pull
  code .
  ```

오류가 나면 VS Code를 연 다음 Claude에게 "지금 상태 확인하고 정리해줘"라고 말하세요.

---

## 1. BMAD가 뭐예요?

**BMAD(BMad Method)는 AI가 분석가, 기획자(PM), UX 디자이너, 아키텍트 역할을 나눠 맡아서, 아이디어를 기획 문서로 만들어 가는 도구 모음이에요.**

- 혼자 빈 문서를 채우는 대신, AI가 **질문을 던지고** 내가 답하면서 생각을 정리해요.
- 답한 내용을 바탕으로 **아이디어 목록, 시장 조사, 제품 개요, 요구사항 문서**가 차례로 만들어져요.
- 결과물은 모두 파일로 저장돼요. 다음 날 이어서 하거나 팀원과 공유할 수 있어요.

### 등장하는 AI 역할

| 이름 | 역할 | 이럴 때 불러요 |
|---|---|---|
| **Mary** | 비즈니스 분석가 | 시장, 경쟁, 사용자 조사 |
| **John** | 제품 기획자(PM) | 무엇을 왜 만드는지, 요구사항 정리 |
| **Sally** | UX 디자이너 | 화면과 사용 흐름, 운전 중 경험 |
| **Winston** | 아키텍트 | 기술 구조 (개발팀과 이야기할 때) |

"Mary 불러줘"처럼 이름으로 부르면 그 역할로 대화해요.

### 이 문서에서 다루는 것

- 이 문서는 **기획 담당**을 위한 안내서예요.
- 그래서 BMAD 기능 중 **기획 단계**에서 쓰는 것만 다뤄요: 아이디어 → 시장 조사 → 제품 개요 → 요구사항 → 화면 설계.
- 개발 단계 기능은 개발팀이 다뤄요.
- 우리가 내야 하는 필수 제출물은 **기획서 DOCX 하나**예요 (11/15 마감). BMAD로 만든 문서가 이 기획서의 재료가 돼요.

---

## 2. 이 저장소 시작하기 (처음 한 번만)

> 모르는 단계가 있으면 개발팀에 화면 공유를 요청하세요. 10~20분이면 끝나요.

### 2-1. 설치할 프로그램

> 설치 명령은 [0번 총정리](#0-5zum-전용-처음-세팅-총정리)의 1단계를 그대로 쓰면 돼요. 여기서는 각 프로그램이 왜 필요한지만 설명해요.

| 프로그램 | 왜 필요해요? |
|---|---|
| **VS Code** | 문서를 보고 Claude Code를 쓰는 편집기 |
| **Claude Code** (VS Code 확장) | AI와 대화하며 BMAD를 쓰는 도구 |
| **Git** | 저장소를 받고, 변경 내용을 팀과 공유 |
| **Node.js** | 저장소의 자동 검사 도구가 사용 |
| **uv** | BMAD가 사용. BMAD에 필요한 Python도 uv가 알아서 설치해요 |

설치가 끝나면 **열려 있는 PowerShell과 VS Code를 모두 껐다 켜세요.** 그래야 새로 설치한 프로그램이 인식돼요.

### 2-2. 내 이름 등록 (Git)

터미널에 아래 두 줄을 입력해요. 5zum 계정 기준으로 이미 채워 두었어요.

```
git config --global user.name "5zum"
git config --global user.email "190751656+5zum@users.noreply.github.com"
```

- 이메일은 GitHub가 계정마다 주는 비공개용 주소예요.
- 실제 이메일 대신 이 주소가 작업 기록에 남아요. 그래도 GitHub에서는 5zum의 작업으로 표시돼요.

여기서 정한 이름으로 작업 기록 폴더(`docs/notes/<이름>/`)가 만들어져요.

### 2-3. 저장소 받기

저장소 접근 권한이 없으면 먼저 팀장에게 GitHub 초대를 요청하세요.

```
New-Item -ItemType Directory -Force $HOME\projects | Out-Null
cd $HOME\projects
git clone https://github.com/Hyundai2026/meongcare-hmg-challenge.git
cd meongcare-hmg-challenge
git switch dev
```

- 처음 두 줄은 내 사용자 폴더 아래에 `projects` 폴더를 만들고 그리로 이동하는 명령이에요.

- 마지막 줄이 중요해요. 우리 팀은 평소 작업을 **dev** 브랜치에 모아요.
- **main**은 심사위원이 보는 제출용이에요. 건드리지 않아요.

### 2-4. VS Code로 열기

1. VS Code 메뉴 **파일 → 폴더 열기**에서 `meongcare-hmg-challenge` 폴더를 열어요.
2. Claude Code 패널을 열어요. 오른쪽 위 Claude 아이콘을 누르면 돼요.
3. 첫 메시지로 남은 마감 일수(D-day)가 보이면 준비 끝이에요.

### 2-5. (선택) BMAD가 내 이름을 부르게 하기

`_bmad/custom/` 폴더에 `config.user.toml` 파일을 새로 만들고 아래처럼 써요.
이 파일은 내 컴퓨터에만 있고 공유되지 않아요.

```
[core]
user_name = "5zum"
```

- 0번 총정리의 6단계 명령을 쓰면 이 파일이 자동으로 만들어져요.
- 직접 만들 때는 **UTF-8(BOM 없음)** 으로 저장해야 BMAD가 읽을 수 있어요.

---

## 3. BMAD 시작하기

### 3-1. 첫 마디

Claude Code 입력창에 이렇게 써 보세요.

```
/bmad-help
```

그다음 이어서 말해요.

```
차량용 앱 공모전 기획을 처음부터 하려고 해. 뭐부터 하면 돼?
```

지금 상태를 보고 다음에 할 일을 추천해 줘요. **막힐 때마다 `/bmad-help`를 쓰면 돼요.**

### 3-2. 대화하는 방식

- `/`로 시작하는 이름(예: `/bmad-brainstorming`)을 입력하거나, **그냥 말로 시켜도 돼요.** 예: "bmad로 브레인스토밍 하자"
- AI가 질문을 하나씩 던져요. **짧게라도 솔직하게 답하는 게 핵심이에요.** 모르면 "모르겠어, 네가 몇 가지 제안해줘"라고 해도 돼요.
- 중간에 멈춰도 괜찮아요. 다음에 "지난번 브레인스토밍 이어서 하자"라고 하면 이어서 해요.
- 결과가 마음에 안 들면 그냥 말하면 돼요. 예: "너무 뻔해. 더 과감하게", "운전자 입장에서 다시"

### 3-3. 결과물은 어디에?

VS Code 왼쪽 탐색기에서 아래 폴더를 열면 돼요.

```
docs/product/bmad/planning-artifacts/
```

- 문서는 마크다운(`.md`) 파일이에요. 파일을 열고 오른쪽 위 미리보기 버튼을 누르면 보기 좋게 보여요.
- 확정된 내용은 `docs/product/idea.md`로 옮겨서 기획서의 원천으로 써요. Claude에게 시키면 돼요.

### 3-4. 저장하고 팀에 공유하기

- 파일 저장과 기록(커밋)은 **Claude가 알아서** 해요. 작업용 브랜치도 Claude가 만들어요.
- 작업이 끝나면 **"dev에 반영해줘"** 라고 말하세요.
- **"push" 승인 창이 뜨면 승인**을 누르세요. 그러면 팀원 모두가 볼 수 있어요.
- main(제출용)에 올리는 건 제출 직전에 팀장이 `/promote`로 해요.

---

## 4. 쓰면 좋은 스킬

### 기획 흐름대로

| 스킬 | 언제 써요? | 이렇게 말해 보세요 | 나오는 것 |
|---|---|---|---|
| `/bmad-brainstorming` | 아이디어가 없거나 더 넓히고 싶을 때 | "운전자가 주차할 때 불편한 점 위주로 브레인스토밍 하자" | 아이디어 후보 목록 |
| `/bmad-forge-idea` | 마음에 드는 아이디어가 생겼을 때 검증용 | "이 아이디어 약점 찔러줘: 주차 후 차 안에 두고 내린 물건 알림" | 약점, 보완점, 짧은 요약 |
| `/bmad-deep-recon` | 시장 규모, 경쟁 서비스, 사용자 반응 조사 | "이 아이디어 시장이랑 경쟁 서비스 조사해줘. 출처 달아서" | 출처가 달린 조사 요약 |
| `/bmad-prfaq` | 아이디어가 정말 매력적인지 시험할 때 | "이 아이디어로 PRFAQ 해보자" | 출시 보도자료 + 예상 질문과 답 (PT 스토리로도 좋아요) |
| `/bmad-product-brief` | 아이디어를 한 문서로 정리할 때 | "지금까지 내용으로 제품 개요 써줘" | 문제, 타깃, 해결책을 담은 1~2쪽 개요 |
| `/bmad-prd` | 기능 목록과 우선순위를 정할 때 | "PRD 만들어줘. 꼭 필요한 기능이랑 있으면 좋은 기능 나눠서" | 요구사항 문서 (개발팀에 넘기는 기준) |
| `/bmad-ux` | 화면과 사용 흐름을 정할 때 | "운전 중 화면이랑 정차 중 화면 나눠서 UX 정리해줘" | 화면 구성과 사용 흐름 문서 |

### 언제든 도움이 되는 것

| 스킬 | 언제 써요? | 이렇게 말해 보세요 |
|---|---|---|
| `/bmad-help` | 다음에 뭘 해야 할지 모를 때 | "지금 뭐 하면 돼?" |
| `/bmad-party-mode` | 여러 관점의 의견이 듣고 싶을 때 | "Mary, John, Sally 다 불러서 이 아이디어 토론시켜줘" |
| `/bmad-advanced-elicitation` | 결과물을 더 날카롭게 다듬고 싶을 때 | "이 제품 개요 더 비판적으로 다시 봐줘" |
| `/bmad-review` | 다 쓴 문서를 점검받고 싶을 때 | "이 PRD 리뷰해줘" |

### 우리 팀 전용 도구

| 도구 | 언제 써요? | 이렇게 말해 보세요 |
|---|---|---|
| **contest-judge** (심사위원 역할) | 아이디어 후보 비교, 기획서 제출 전 점검 | "contest-judge로 아이디어 후보 3개 비교해줘", "기획서 심사위원 관점으로 채점해줘" |
| **stack-diagram** (구조도) | 기획서와 PT에 넣을 서비스 구조 그림 | "우리 앱 구조도 그려줘" |
| **Pleos SDK 확인** | 차량 데이터(속도, 문, 주행 상태 등)를 쓰는 기능이 실제로 가능한지 | "이 기능 Pleos SDK로 가능한지 확인해줘" |

> **심사 기준 4가지**를 기억하세요: **차량특화성**(폰이 아니라 차라서 생기는 가치), **창의성**(분명한 불편과 해결책), **기술 활용성**(Pleos SDK를 핵심에 사용), **사업성**(타깃, 예상 사용자 수, 재사용 이유).
> 자세한 내용: [docs/contest/requirements.md](../contest/requirements.md)

---

## 5. 추천 진행 순서 (11/15 마감 기준)

| 날짜 | 할 일 | 쓰는 스킬 |
|---|---|---|
| 1일차 | 아이디어 후보 3~5개 만들기 → 2~3개로 좁히기 | `/bmad-brainstorming` → `/bmad-forge-idea` → contest-judge 비교 |
| 2일차 | 최종 아이디어 1개 정하기, 시장·경쟁 조사 | `/bmad-deep-recon` |
| 3일차 | 제품 개요 쓰기 (매력도 시험은 선택) | `/bmad-product-brief` (+ `/bmad-prfaq`) |
| 4일차 | 기능 목록, 운전 중 화면 정리 | `/bmad-prd` → `/bmad-ux` |
| 5일차 | `idea.md` 정리 → 대회 접수양식(기획서 DOCX)에 옮기기 → 심사위원 점검 | "BMAD 결과로 idea.md 채워줘" → contest-judge |

- 아이디어가 정해지면 바로 개발팀에 알려 주세요. 개발도 같이 시작해야 12/3 PT에 동작하는 앱을 보여줄 수 있어요.
- 기획서의 5번 "개발 계획"은 개발팀과 같이 써요. 무엇을 어떻게 만들지, 어떤 SDK를 쓸지가 들어가요.

---

## 6. 지켜야 할 것

- **차량 데이터를 쓰는 기능은 꼭 "가능한지" 확인해요.**
  - Pleos SDK 중 일부는 아직 "준비 중"이라 쓸 수 없어요.
  - 핵심 기능이 준비 중인 기능에 기대면 심사에서 불리해요.
  - "이 기능 Pleos SDK로 가능한지 확인해줘"라고 물어보세요.
- **운전 중에는 화면을 최소로.** 운전 중에는 글자 입력이 없어야 하고, 정보는 적게, 버튼은 크게 해야 해요. 운전 중과 정차 중에 할 수 있는 일을 나눠서 기획해요.
- **비밀번호나 개인 정보는 문서에 쓰지 않아요.** 문서는 팀 저장소에 올라가요.
- **main 브랜치는 건드리지 않아요.** 제출용이에요. 실수로 main에서 작업하려 하면 저장소가 막아 줘요.

---

## 7. 자주 막히는 곳

| 증상 | 해결 |
|---|---|
| `uv`를 찾을 수 없다고 나와요 | uv를 설치한 뒤 VS Code를 완전히 껐다 켜세요. |
| AI가 영어로 대답해요 | "한국어로 해줘"라고 말하세요. 팀 설정은 한국어로 돼 있어요. |
| 내 결과물이 안 보여요 | `docs/product/bmad/planning-artifacts/` 폴더를 확인하세요. 없으면 "방금 만든 문서 어디 저장했어?"라고 물어보세요. |
| `/bmad-help`가 build, sprint, story 같은 개발 단계를 추천해요 | 개발 단계는 개발팀이 맡아요. 기획에서는 이 문서의 스킬까지만 진행하고, 결과물을 개발팀에 넘기면 돼요. |
| push가 거절됐다고 나와요 | 그사이 다른 팀원이 먼저 올린 거예요. "다시 dev에 반영해줘"라고 하면 돼요. |
| 지금 어느 브랜치에 있는지 모르겠어요 | "지금 어떤 브랜치야?"라고 물어보세요. 작업은 dev에서 딴 작업용 브랜치에서 해요. |
| 뭘 해야 할지 모르겠어요 | `/bmad-help` 또는 개발팀에 물어보세요. |

---

더 알고 싶다면: [README의 팀원 개발 가이드](../../README.md#팀원-개발-가이드), [CLAUDE.md의 "기획 단계: BMAD"](../../CLAUDE.md)
