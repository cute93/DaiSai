# 다이사이 주사위 애니메이션 설계 문서

## 1. 개요

주사위 3개가 삼각형 배치(위 1개 + 아래 2개)로 동시에 굴러가다 착지하고,  
결과를 바탕으로 **최적 베팅 TOP 3**를 화면 하단에 추천하는 구현 계획입니다.

- **UI 프레임워크**: Jetpack Compose
- **렌더링**: `Canvas` API로 주사위 면(pip) 직접 드로잉
- **애니메이션**: `Animatable`, `LaunchedEffect`, `spring()`, `tween()`
- **패키지**: `com.ccteacher.daisai`

---

## 2. 생성할 파일 목록

```
app/src/main/
├── java/com/ccteacher/daisai/
│   ├── MainActivity.kt
│   └── ui/
│       ├── theme/
│       │   ├── Color.kt
│       │   └── Theme.kt
│       └── dice/
│           ├── DiceFace.kt               # 주사위 1개 Canvas 렌더링 Composable
│           ├── DiceRollAnimation.kt      # 단일 주사위 롤링 애니메이션 상태 머신
│           ├── ThreeDiceBoard.kt         # 삼각 배치 + 굴리기/끝내기 버튼 + 추천 패널
│           ├── BettingSuggestion.kt      # 베팅 추천 로직 + UI
│           └── DiceRollViewModel.kt      # 결과 생성, 롤링 상태, 추천 목록 관리
└── res/
    └── values/
        └── colors.xml
```

### 파일별 역할

| 파일 | 역할 |
|------|------|
| `MainActivity.kt` | Compose 진입점. `ThreeDiceBoard` 호스팅 |
| `DiceFace.kt` | Canvas로 주사위 1개 면 그리기 (눈금 포함) |
| `DiceRollAnimation.kt` | 단일 주사위 롤링·착지 애니메이션 |
| `ThreeDiceBoard.kt` | 주사위 3개 삼각 배치 + 굴리기/끝내기 버튼 + 추천 패널 |
| `BettingSuggestion.kt` | 결과 기반 베팅 추천 계산 로직 + 카드 UI |
| `DiceRollViewModel.kt` | 랜덤 결과 생성, 상태 관리, 추천 목록 생성 |
| `Color.kt` | 주사위·배경 색상 토큰 정의 |
| `Theme.kt` | MaterialTheme 설정 |

---

## 3. 화면 레이아웃

### 3-1. 전체 구조

```
┌─────────────────────────────┐
│                             │
│        [ 주사위 1 ]          │  ← 상단 중앙
│                             │
│  [ 주사위 2 ]  [ 주사위 3 ]  │  ← 하단 좌우
│                             │
│  [ 🎲 굴리기 ]  [ 끝내기 ]  │  ← 버튼 2개 가로 배치
│  ─────────────────────────  │
│  ★ 추천 베팅 TOP 3          │
│  ┌─────────────────────┐   │
│  │ 1위: 합계 10 | 6:1  │   │
│  │ 2위: 소(Small) | 1:1│   │
│  │ 3위: 싱글 3   | 1:1 │   │
│  └─────────────────────┘   │
└─────────────────────────────┘
```

### 3-2. 버튼 동작 정의

| 버튼 | 활성 조건 | 동작 |
|------|-----------|------|
| **굴리기** | 롤링 중 비활성 | 주사위 3개 랜덤 결과 생성 → 애니메이션 시작 |
| **끝내기** | 항상 활성 | `Activity.finish()` 호출 → 앱 종료 |

### 3-3. Compose 레이아웃 구조

```kotlin
// ThreeDiceBoard.kt
Column(horizontalAlignment = CenterHorizontally) {

    Spacer(32.dp)

    // 주사위 1 (상단 중앙)
    Row(horizontalArrangement = Center) {
        DiceRollAnimation(targetValue = results[0], ...)
    }

    Spacer(16.dp)

    // 주사위 2, 3 (하단 좌우)
    Row(horizontalArrangement = spacedBy(24.dp)) {
        DiceRollAnimation(targetValue = results[1], ...)
        DiceRollAnimation(targetValue = results[2], ...)
    }

    Spacer(24.dp)

    // 버튼 행
    Row(horizontalArrangement = spacedBy(16.dp)) {
        Button(
            onClick = { viewModel.roll() },
            enabled = !isRolling
        ) {
            Text("🎲 굴리기")
        }
        OutlinedButton(
            onClick = { (context as Activity).finish() }
        ) {
            Text("끝내기")
        }
    }

    // 베팅 추천 패널 (롤링 완료 후 표시)
    AnimatedVisibility(visible = !isRolling && suggestions.isNotEmpty()) {
        BettingSuggestionPanel(suggestions = suggestions)
    }
}
```

---

## 4. 주사위 렌더링 방식 (Canvas)

### 4-1. 주사위 외형

```
크기: 80dp × 80dp
모서리 반지름: 12dp
배경색: White (#FFFFFF)
테두리: 2dp, DarkGray (#333333)
pip 색상: Black (#111111)
```

### 4-2. 눈금(pip) 좌표 정의

주사위 크기를 W로 표현할 때 7개 기준 위치:

| 식별자 | X | Y |
|--------|---|---|
| `TL` (top-left) | W × 0.25 | W × 0.25 |
| `TR` (top-right) | W × 0.75 | W × 0.25 |
| `ML` (mid-left) | W × 0.25 | W × 0.50 |
| `CT` (center) | W × 0.50 | W × 0.50 |
| `MR` (mid-right) | W × 0.75 | W × 0.50 |
| `BL` (bot-left) | W × 0.25 | W × 0.75 |
| `BR` (bot-right) | W × 0.75 | W × 0.75 |

pip 반지름: W × 0.08

### 4-3. 눈금별 pip 배치

| 눈금 | 사용 위치 | pip 수 |
|------|-----------|--------|
| 1 | CT | 1 |
| 2 | TL, BR | 2 |
| 3 | TL, CT, BR | 3 |
| 4 | TL, TR, BL, BR | 4 |
| 5 | TL, TR, CT, BL, BR | 5 |
| 6 | TL, TR, ML, MR, BL, BR | 6 |

### 4-4. DiceFace 코드 구조

```kotlin
// DiceFace.kt
@Composable
fun DiceFace(value: Int, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(80.dp)) {
        drawRoundRect(color = Color.White, cornerRadius = CornerRadius(12.dp.toPx()))
        drawRoundRect(
            color = Color.DarkGray,
            cornerRadius = CornerRadius(12.dp.toPx()),
            style = Stroke(width = 2.dp.toPx())
        )
        pipPositions(value, size).forEach { offset ->
            drawCircle(color = Color.Black, radius = size.width * 0.08f, center = offset)
        }
    }
}

fun pipPositions(value: Int, size: Size): List<Offset> {
    val w = size.width
    val pos = mapOf(
        "TL" to Offset(w*0.25f, w*0.25f), "TR" to Offset(w*0.75f, w*0.25f),
        "ML" to Offset(w*0.25f, w*0.50f), "CT" to Offset(w*0.50f, w*0.50f),
        "MR" to Offset(w*0.75f, w*0.50f), "BL" to Offset(w*0.25f, w*0.75f),
        "BR" to Offset(w*0.75f, w*0.75f)
    )
    val layout = mapOf(
        1 to listOf("CT"),
        2 to listOf("TL","BR"),
        3 to listOf("TL","CT","BR"),
        4 to listOf("TL","TR","BL","BR"),
        5 to listOf("TL","TR","CT","BL","BR"),
        6 to listOf("TL","TR","ML","MR","BL","BR")
    )
    return layout[value]!!.map { pos[it]!! }
}
```

---

## 5. 애니메이션 시퀀스

### 5-1. 3단계 구조

```
[IDLE] ──굴리기 클릭──▶ [ROLLING] ──1500ms──▶ [SETTLING] ──700ms──▶ [DONE]
                              │                       │                   │
                        360° 반복 회전          spring 감속          bounce 착지
                        100ms마다 랜덤 눈금     최종값 확정          추천 패널 표시
```

### 5-2. 단계별 동작 상세

#### Phase 1: ROLLING (0 ~ 1500ms)
- **회전**: `animateTo(720f, tween(1500, LinearEasing))` — 2바퀴 선형 회전
- **눈금**: 100ms 간격으로 `(1..6).random()` 반복
- **스케일**: 1.0 유지

#### Phase 2: SETTLING (1500 ~ 2200ms)
- **회전**: `animateTo(720f, spring(dampingRatio=0.6f, stiffness=200f))` — 튀며 정지
- **눈금**: 최종 결과값으로 고정 (ViewModel에서 미리 결정)
- **스케일**: `1.0 → 1.15 → 1.0` bounce

#### Phase 3: DONE (2200ms~)
- 최종 눈금 안정 표시
- `onDone()` 콜백 → ViewModel의 doneCount 증가
- 3개 모두 완료 시 `isRolling = false` → 추천 패널 표시

### 5-3. DiceRollAnimation 코드 구조

```kotlin
// DiceRollAnimation.kt
@Composable
fun DiceRollAnimation(
    targetValue: Int,
    isRolling: Boolean,
    onDone: () -> Unit
) {
    var displayValue by remember { mutableIntStateOf(1) }
    val rotation = remember { Animatable(0f) }
    val scale = remember { Animatable(1f) }

    LaunchedEffect(isRolling) {
        if (!isRolling) return@LaunchedEffect

        // Phase 1: Rolling
        launch { rotation.animateTo(720f, tween(1500, easing = LinearEasing)) }
        val rollStart = System.currentTimeMillis()
        while (System.currentTimeMillis() - rollStart < 1500L) {
            displayValue = (1..6).random()
            delay(100)
        }

        // Phase 2: Settling
        displayValue = targetValue
        launch { rotation.animateTo(720f, spring(dampingRatio = 0.6f, stiffness = 200f)) }
        launch {
            scale.animateTo(1.15f, tween(150))
            scale.animateTo(1.0f, spring(dampingRatio = 0.4f))
        }
        delay(700)

        // Phase 3: Done
        onDone()
    }

    Box(
        modifier = Modifier.graphicsLayer {
            rotationZ = rotation.value % 360f
            scaleX = scale.value
            scaleY = scale.value
        }
    ) {
        DiceFace(value = displayValue)
    }
}
```

---

## 6. 베팅 추천 로직

### 6-1. 추천 데이터 구조

```kotlin
data class BettingRecommendation(
    val name: String,        // 베팅 이름 (예: "합계 10")
    val payout: Int,         // 배당률 (예: 6)
    val description: String  // 설명 (예: "세 주사위 합이 10")
)
```

### 6-2. 결과 기반 당첨 베팅 계산

결과 `[d1, d2, d3]`에 대해 모든 베팅 종류를 평가하여 당첨 베팅을 추출합니다.  
**배당률 내림차순** 정렬 후 TOP 3 반환합니다.

```kotlin
// BettingSuggestion.kt
fun evaluateBets(d1: Int, d2: Int, d3: Int): List<BettingRecommendation> {
    val sum = d1 + d2 + d3
    val dice = listOf(d1, d2, d3)
    val results = mutableListOf<BettingRecommendation>()
    val isTriple = d1 == d2 && d2 == d3

    // 스페시픽 트리플 (150:1)
    if (isTriple) {
        results += BettingRecommendation("트리플 $d1", 150, "주사위 3개 모두 $d1")
    }

    // 애니 트리플 (24:1)
    if (isTriple) {
        results += BettingRecommendation("애니 트리플", 24, "3개 모두 동일 숫자")
    }

    // 합계 베팅
    val totalPayout = mapOf(
        4 to 50, 5 to 18, 6 to 14, 7 to 12, 8 to 8,
        9 to 6, 10 to 6, 11 to 6, 12 to 6, 13 to 8,
        14 to 12, 15 to 14, 16 to 18, 17 to 50
    )
    totalPayout[sum]?.let { payout ->
        results += BettingRecommendation("합계 $sum", payout, "세 주사위 합이 $sum")
    }

    // 더블 (8:1)
    val counts = dice.groupingBy { it }.eachCount()
    counts.filter { it.value >= 2 }.forEach { (num, _) ->
        results += BettingRecommendation("더블 $num", 8, "주사위 2개가 $num")
    }

    // 도미노 (5:1)
    setOf(setOf(d1,d2), setOf(d1,d3), setOf(d2,d3))
        .filter { it.size == 2 }
        .forEach { pair ->
            val (a, b) = pair.toList()
            results += BettingRecommendation("도미노 $a-$b", 5, "$a 와 $b 조합")
        }

    // 싱글 (등장 횟수별 배당)
    counts.forEach { (num, cnt) ->
        results += BettingRecommendation("싱글 $num (${cnt}개)", cnt, "$num 이 ${cnt}개 등장")
    }

    // 대/소, 홀/짝 — 트리플 제외
    if (!isTriple) {
        if (sum in 11..17) results += BettingRecommendation("대 (Big)", 1, "합계 11~17")
        if (sum in 4..10)  results += BettingRecommendation("소 (Small)", 1, "합계 4~10")
        if (sum % 2 == 1)  results += BettingRecommendation("홀 (Odd)", 1, "합계 홀수")
        if (sum % 2 == 0)  results += BettingRecommendation("짝 (Even)", 1, "합계 짝수")
    }

    return results.sortedByDescending { it.payout }.take(3)
}
```

### 6-3. 추천 패널 UI

```kotlin
// BettingSuggestion.kt
@Composable
fun BettingSuggestionPanel(suggestions: List<BettingRecommendation>) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("★ 추천 베팅 TOP 3", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        suggestions.forEachIndexed { i, rec ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${i + 1}위",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(36.dp)
                    )
                    Column {
                        Text(rec.name, fontWeight = FontWeight.Bold)
                        Text(
                            "배당 ${rec.payout}:1  |  ${rec.description}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}
```

---

## 7. DiceRollViewModel 구조

```kotlin
// DiceRollViewModel.kt
data class DiceUiState(
    val results: List<Int> = listOf(1, 1, 1),
    val isRolling: Boolean = false,
    val doneCount: Int = 0,
    val suggestions: List<BettingRecommendation> = emptyList()
)

class DiceRollViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(DiceUiState())
    val uiState: StateFlow<DiceUiState> = _uiState.asStateFlow()

    fun roll() {
        val newResults = List(3) { (1..6).random() }
        _uiState.update {
            it.copy(
                results = newResults,
                isRolling = true,
                doneCount = 0,
                suggestions = emptyList()
            )
        }
    }

    fun onDiceDone(index: Int) {
        _uiState.update { state ->
            val newCount = state.doneCount + 1
            val finished = newCount >= 3
            state.copy(
                doneCount = newCount,
                isRolling = !finished,
                suggestions = if (finished) {
                    evaluateBets(state.results[0], state.results[1], state.results[2])
                } else emptyList()
            )
        }
    }
}
```

---

## 8. Gradle 의존성 설정

### build.gradle.kts (:app)

```kotlin
android {
    compileSdk = 35
    defaultConfig {
        applicationId = "com.ccteacher.daisai"
        minSdk = 26
        targetSdk = 35
    }
    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
```

---

## 9. 색상 정의

```kotlin
// ui/theme/Color.kt
val DiceWhite  = Color(0xFFFFFFFF)  // 주사위 배경
val DiceBorder = Color(0xFF333333)  // 주사위 테두리
val DicePip    = Color(0xFF111111)  // 눈금 점
val TableGreen = Color(0xFF1B5E20)  // 게임 테이블 배경 (다크 그린)
val GoldAccent = Color(0xFFFFD700)  // 추천 패널 강조색
```

---

## 10. 구현 순서 및 검증

| 순서 | 파일 | 검증 방법 |
|------|------|-----------|
| 1 | `Color.kt`, `Theme.kt` | Compose Preview 색상 확인 |
| 2 | `DiceFace.kt` | Preview로 1~6 전 눈금 확인 |
| 3 | `DiceRollViewModel.kt` | 단위 테스트: roll() → 1~6 범위, evaluateBets() → TOP 3 반환 |
| 4 | `DiceRollAnimation.kt` | 에뮬레이터에서 회전·착지·bounce 확인 |
| 5 | `BettingSuggestion.kt` | 결과 [1,1,1] → 트리플 150:1 1위 확인 |
| 6 | `ThreeDiceBoard.kt` | 삼각 배치 확인, 굴리기/끝내기 버튼 동작 확인 |
| 7 | `MainActivity.kt` | 전체 E2E 흐름 + 끝내기 버튼 앱 종료 확인 |

---

## 11. UX 체크리스트

- [ ] 굴리기 중 굴리기 버튼 비활성화 (중복 클릭 방지)
- [ ] 끝내기 버튼은 항상 활성 (언제든 종료 가능)
- [ ] 주사위 3개 동시 시작, 독립적 착지
- [ ] 착지 bounce 효과 자연스러움
- [ ] 애니메이션 총 시간 2~3초 이내
- [ ] 착지 완료 후에만 추천 패널 표시 (AnimatedVisibility)
- [ ] 추천 베팅이 실제 당첨 조건과 일치
- [ ] 트리플 발생 시 대/소/홀/짝 추천 제외 (규칙 준수)
