# DaiSai (大細) 프로젝트 현황 개요

## 1. 프로젝트 정보

| 항목 | 내용 |
|------|------|
| **앱 이름** | DaiSai (大細) |
| **패키지** | `com.ccteacher.daisai` |
| **언어** | Kotlin |
| **UI 프레임워크** | Jetpack Compose (Material3) |
| **최소 SDK** | 26 (Android 8.0) |
| **대상 SDK** | 35 (Android 15) |
| **Kotlin 버전** | 2.0.0 |
| **AGP 버전** | 8.5.2 |
| **Compose BOM** | 2024.09.00 |
| **Gradle** | 8.7 |

---

## 2. 참고 문서

| 파일 | 경로 | 역할 |
|------|------|------|
| `rule.md` | `DaiSai/rule.md` | 다이사이 게임 규칙, 베팅 종류, 배당률, 확률 마스터 레퍼런스 |
| `animation.md` | `DaiSai/animation.md` | 주사위 애니메이션·렌더링·베팅 추천 기술 설계 명세 |
| `overview.md` | `DaiSai/overview.md` | 프로젝트 전체 현황 요약 (이 파일) |

---

## 3. 전체 파일 트리

```
DaiSai/
├── overview.md                          # 프로젝트 현황 (이 파일)
├── rule.md                              # 게임 규칙 문서
├── animation.md                         # 애니메이션 설계 문서
├── CLAUDE.md                            # Claude 작업 규칙
├── settings.gradle.kts                  # 프로젝트 설정
├── build.gradle.kts                     # 루트 빌드 스크립트
├── gradle.properties                    # Gradle 프로퍼티
├── gradle/
│   ├── libs.versions.toml              # 버전 카탈로그 (의존성 관리)
│   └── wrapper/
│       └── gradle-wrapper.properties   # Gradle 8.7 래퍼 설정
└── app/
    ├── build.gradle.kts                # 앱 모듈 빌드 스크립트
    ├── proguard-rules.pro              # ProGuard 규칙
    └── src/main/
        ├── AndroidManifest.xml
        ├── res/values/
        │   ├── strings.xml             # 앱 이름 리소스
        │   └── themes.xml              # 기본 Android 테마
        └── java/com/ccteacher/daisai/
            ├── MainActivity.kt         # Compose 진입점
            └── ui/
                ├── theme/
                │   ├── Color.kt        # 색상 토큰 정의
                │   ├── Theme.kt        # Material3 테마 설정
                │   └── Type.kt         # 타이포그래피 설정
                └── dice/
                    ├── DiceFace.kt           # 주사위 Canvas 렌더링
                    ├── DiceRollAnimation.kt  # 롤링 애니메이션 상태 머신
                    ├── BettingSuggestion.kt  # 베팅 추천 로직 + 카드 UI
                    ├── DiceRollViewModel.kt  # 상태 관리
                    └── ThreeDiceBoard.kt     # 메인 게임 보드 UI
```

---

## 4. 파일별 역할 요약

### 빌드 설정

| 파일 | 역할 | 핵심 내용 |
|------|------|-----------|
| `gradle/libs.versions.toml` | 버전 카탈로그 | AGP 8.5.2, Kotlin 2.0.0, Compose BOM 2024.09.00 |
| `settings.gradle.kts` | 프로젝트 설정 | 모듈 `:app` 포함, 레포지토리 설정 |
| `build.gradle.kts` (root) | 루트 빌드 | 플러그인 선언 (android, kotlin, compose) |
| `app/build.gradle.kts` | 앱 빌드 | compileSdk 35, minSdk 26, Compose 의존성 전체 |

### Kotlin 소스

| 파일 | 역할 | 주요 함수/클래스 |
|------|------|-----------------|
| `MainActivity.kt` | 앱 진입점 | `MainActivity : ComponentActivity`, `enableEdgeToEdge()` |
| `Color.kt` | 색상 토큰 | `DiceWhite`, `DiceBorder`, `DicePip`, `TableGreen`, `GoldAccent` |
| `Theme.kt` | Material3 테마 | `DaiSaiTheme()`, Dynamic Color 지원 |
| `Type.kt` | 타이포그래피 | `Typography` (bodyLarge 정의) |
| `DiceFace.kt` | 주사위 1개 렌더링 | `DiceFace()`, `pipPositions()` |
| `DiceRollAnimation.kt` | 롤링 애니메이션 | `DiceRollAnimation()`, `Animatable`, `coroutineScope` |
| `BettingSuggestion.kt` | 베팅 추천 | `BettingRecommendation`, `evaluateBets()`, `BettingSuggestionPanel()` |
| `DiceRollViewModel.kt` | 상태 관리 | `DiceUiState`, `DiceRollViewModel`, `roll()`, `onDiceDone()` |
| `ThreeDiceBoard.kt` | 게임 보드 UI | `ThreeDiceBoard()`, 삼각 배치, 굴리기/끝내기 버튼 |

---

## 5. 아키텍처 흐름

```
MainActivity
    └── DaiSaiTheme (Material3 + Dynamic Color)
            └── ThreeDiceBoard
                    │
                    ├── [상태 구독] DiceRollViewModel (StateFlow<DiceUiState>)
                    │       ├── results: List<Int>       // 주사위 3개 결과 (1~6)
                    │       ├── rollKey: Int             // 롤 트리거 카운터
                    │       ├── isRolling: Boolean       // 애니메이션 진행 중 여부
                    │       ├── doneCount: Int           // 완료된 주사위 수
                    │       └── suggestions: List<...>  // 추천 베팅 TOP 3
                    │
                    ├── [렌더링] DiceRollAnimation × 3개 (삼각형 배치)
                    │       └── DiceFace (Canvas 렌더링)
                    │               ├── drawRoundRect()   // 주사위 배경
                    │               └── drawCircle() × N  // 눈금 pip
                    │
                    ├── [버튼] 굴리기 → vm.roll() → rollKey++ → 애니메이션 시작
                    ├── [버튼] 끝내기 → Activity.finish()
                    │
                    └── [추천] BettingSuggestionPanel
                            └── evaluateBets(d1, d2, d3) → TOP 3 반환
```

### 데이터 흐름 순서

```
1. "굴리기" 클릭
        ↓
2. vm.roll() : 결과 [d1,d2,d3] 랜덤 생성, rollKey++, isRolling=true
        ↓
3. LaunchedEffect(rollKey) 트리거 → 3개 DiceRollAnimation 동시 시작
        ↓
4. Phase1 ROLLING (1500ms): 720° 선형 회전 + 80ms마다 랜덤 눈금
        ↓
5. Phase2 SETTLING (700ms): spring 감속 + bounce 착지 + 최종 눈금 확정
        ↓
6. onDone() 콜백 × 3회 → vm.onDiceDone() → doneCount 증가
        ↓
7. doneCount == 3 → evaluateBets() 호출 → isRolling=false
        ↓
8. BettingSuggestionPanel 표시 (AnimatedVisibility fadeIn + slideIn)
```

---

## 6. 핵심 설계 결정

### Canvas 기반 주사위 렌더링

외부 이미지 없이 `Canvas API`로 직접 드로잉합니다.

```
눈금(pip) 7개 기준 위치 (주사위 크기 W 기준):
  TL(0.28W, 0.28W)  TR(0.72W, 0.28W)
  ML(0.28W, 0.50W)  CT(0.50W, 0.50W)  MR(0.72W, 0.50W)
  BL(0.28W, 0.72W)  BR(0.72W, 0.72W)

눈금별 pip 배치:
  1 → CT
  2 → TL, BR
  3 → TL, CT, BR
  4 → TL, TR, BL, BR
  5 → TL, TR, CT, BL, BR
  6 → TL, TR, ML, MR, BL, BR
```

### rollKey 기반 애니메이션 트리거

`isRolling: Boolean` 대신 `rollKey: Int`를 사용합니다.  
같은 결과가 연속으로 나와도 rollKey가 증가하므로 `LaunchedEffect`가 항상 재실행됩니다.

### coroutineScope 병렬 실행

```kotlin
coroutineScope {
    launch { rotation.animateTo(...) }  // 회전
    launch { /* 눈금 80ms 간격 변경 */ } // 눈금
}
// 두 코루틴 모두 완료 후 다음 단계 진행
```

### 베팅 추천 평가 우선순위

```
스페시픽 트리플 (150:1) > 합계 4·17 (50:1) > 애니 트리플 (24:1) >
합계 5·16 (18:1) > 합계 6·15 (14:1) > 합계 7·14 (12:1) >
더블 (8:1) = 합계 8·13 (8:1) > 합계 9·12 (6:1) > 도미노 (5:1) >
싱글3개 (3:1) > 싱글2개 (2:1) > 싱글1개 (1:1) = 대/소/홀/짝 (1:1)
```

트리플 발생 시 대/소/홀/짝 추천 자동 제외 (rule.md 규칙 준수).

---

## 7. 색상 토큰

| 토큰명 | 색상 코드 | 용도 |
|--------|----------|------|
| `DiceWhite` | `#FFFFFF` | 주사위 배경 |
| `DiceBorder` | `#333333` | 주사위 테두리 |
| `DicePip` | `#111111` | 주사위 눈금 점 |
| `TableGreen` | `#1B5E20` | 게임 테이블 배경 (다크 그린) |
| `GoldAccent` | `#FFD700` | "★ 추천 베팅 TOP 3" 타이틀 강조 |

---

## 8. 다음 단계 TODO

### 기능 추가
- [ ] 앱 아이콘 (mipmap) 추가 — 현재 빌드 시 기본 아이콘 없어서 오류 발생
- [ ] 사운드 효과 — 주사위 굴리는 소리, 착지 효과음
- [ ] 베팅 내역 기록 — 이전 결과 히스토리 표시
- [ ] 베팅 금액 입력 — 실제 게임처럼 베팅 후 결과 계산
- [ ] 홀/짝 베팅 페이지 추가

### 품질 개선
- [ ] 단위 테스트 — `evaluateBets()` 함수 결과 검증
- [ ] 계측 테스트 — 버튼 클릭 후 애니메이션 완료 확인
- [ ] 다크 모드 대응 — 다크 테마에서 주사위 색상 조정
- [ ] 태블릿 레이아웃 — 대형 화면에서 주사위 크기 조정

### 빌드
- [ ] gradlew 래퍼 생성 — Android Studio에서 Sync 후 자동 생성됨
- [ ] 앱 아이콘 리소스 추가 후 릴리즈 빌드 테스트

---

## 9. 빌드 및 실행 방법

### Android Studio에서 열기
1. Android Studio 실행
2. **File → Open** → `DaiSai` 폴더 선택
3. Gradle Sync 완료 대기 (최초 1~3분 소요)
4. 에뮬레이터 또는 실제 기기 연결
5. **Run** (▶) 클릭

### 명령줄 빌드 (Sync 후)
```bash
# 디버그 APK 빌드
./gradlew assembleDebug

# 릴리스 APK 빌드
./gradlew assembleRelease

# 린트 검사
./gradlew lint

# 단위 테스트
./gradlew test
```

> **주의**: 최초 실행 시 `mipmap/ic_launcher` 리소스가 없으면 빌드 오류가 발생합니다.  
> Android Studio에서 **File → New → Image Asset**으로 기본 아이콘을 생성하거나,  
> Sync 후 자동 생성된 기본 아이콘을 사용하세요.
