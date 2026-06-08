# 틈새 중국어 단어장 앱 — 기술 설계 & 아키텍처

> 직장인 맞춤형, 잠금화면/백그라운드 활용 극대화 중국어 학습 앱
> 작성일: 2026-06-08 · 대상 플랫폼: Android (minSdk 26 / target 35)

---

## 0. 요약 (TL;DR)

- **언어/UI**: Kotlin + Jetpack Compose, 단일 모듈 + 패키지 분리(기능 성장 시 멀티모듈 전환).
- **로컬 저장**: Room. 단원(Unit) → 단어/회화/노트를 외래키로 묶는 구조.
- **OCR 파이프라인**: ML Kit(온디바이스, 무료) 로 텍스트 추출 → OpenAI GPT-4o(JSON 모드) 로 구조화.
- **잠금화면**: ⚠️ OS 보안 잠금화면을 "덮어쓰는" 것은 불가능. `ACTION_SCREEN_ON` 수신 → 자체 전체화면 액티비티를 띄우는 **우회 방식**으로 구현. (3장 상세)
- **TTS/STT**: 안드로이드 내장 `TextToSpeech` / `SpeechRecognizer` (네트워크/비용 0).
- **권장 개발 순서**: DB → OCR 파이프라인 → 단어장 UI → TTS → 잠금화면 → 게이미피케이션.

---

## 1. 기술 스택 결정

| 영역 | 선택 | 이유 / 비고 |
|---|---|---|
| 언어 | Kotlin | Android 1순위, 코루틴 비동기 |
| UI | Jetpack Compose | 선언형, 잠금화면 같은 동적 UI에 유리 |
| 로컬 DB | Room (SQLite) | 오프라인 우선, 단원-단어 관계형 구조에 적합 |
| 비동기 | Coroutines + Flow | DB 관찰, 네트워크 호출 |
| DI | Hilt | Repository/Service 주입, 테스트 용이 |
| OCR | **ML Kit Text Recognition v2 (Chinese 스크립트)** | 온디바이스, 무료, 오프라인. 중국어 인식 모델 별도 |
| 구조화 | **OpenAI GPT-4o** (`response_format: json_object`) | OCR raw text → 표 구조 변환 |
| 이미지 로딩 | Coil | Compose 친화 |
| 네트워크 | Retrofit + OkHttp + kotlinx.serialization | OpenAI REST 호출 |
| 백그라운드 | Foreground Service + BroadcastReceiver | 잠금화면 트리거 |
| 알림/스케줄 | WorkManager + AlarmManager | 미션 푸시 |
| TTS | `android.speech.tts.TextToSpeech` | `Locale.CHINA`, 성조 재생 |
| STT | `android.speech.SpeechRecognizer` | 미션 음성 인식 |

> **비용 주의**: GPT-4o는 호출당 과금. OCR 구조화는 "촬영 시 1회"만 호출하고 결과를 DB에 영구 저장하므로, 학습/잠금화면 단계에서는 추가 API 비용이 0이 되도록 설계함. (5장 비용 항목 참고)

---

## 2. 전체 아키텍처

### 2.1 레이어 (Clean-ish / MVVM)

```
┌─────────────────────────────────────────────┐
│  UI (Compose)  ── ViewModel (StateFlow)       │  Presentation
├─────────────────────────────────────────────┤
│  UseCase / Repository                          │  Domain
├─────────────────────────────────────────────┤
│  Room DAO   │  ML Kit   │  OpenAI API  │ TTS/STT│  Data / Platform
└─────────────────────────────────────────────┘
```

### 2.2 패키지 구조 (단일 모듈, 기능별 슬라이스)

```
com.example.chineselock
├── core
│   ├── db            # Room: entities, dao, AppDatabase, converters
│   ├── network       # Retrofit, OpenAI service, dto
│   ├── tts           # TtsManager (싱글톤)
│   └── di            # Hilt 모듈
├── feature
│   ├── capture       # 기능1: 카메라/OCR/구조화
│   ├── vocab         # 단어장 리스트/상세
│   ├── conversation  # 회화 리스트
│   ├── note          # 기능2: 커스텀 노트
│   ├── lockscreen    # 기능3: 잠금화면 서비스/액티비티
│   └── mission       # 기능5: 게이미피케이션 STT 미션
└── MainApplication.kt
```

> 초기에는 단일 모듈로 시작하고, `lockscreen`/`capture`가 무거워지면 `:feature:lockscreen` 등 멀티모듈로 분리.

---

## 3. 데이터 모델 (Room 스키마)

핵심 아이디어: **모든 학습 콘텐츠는 `StudyUnit`(예: "3권 1과")에 매달린다.** 잠금화면/미션은 "오늘의 단원"을 골라 거기 속한 단어/회화를 노출.

### 3.1 엔티티

```kotlin
// 단원: "3권 1과", "3-2" 등. OCR 업로드 시 사용자가 입력/인식한 대단원 분류 단위
@Entity(tableName = "study_unit")
data class StudyUnit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val book: Int,            // 권 (예: 3)
    val lesson: Int,          // 과 (예: 1)
    val title: String,        // 표시용 "3권 1과"
    val createdAt: Long
)

// 기능1-A 단어장: 교재 구조 그대로 [한자 | 병음 | 품사 | 뜻] + 카테고리
//  ※ 실제 교재 단어 페이지를 보고 확정: '한자풀이'는 교재에 없고 '품사'가 있음.
//    단어는 '회화 단어 / 여러 단어 / 고유명사' 섹션으로 묶임 → category로 보관.
@Entity(
    tableName = "vocab",
    foreignKeys = [ForeignKey(entity = StudyUnit::class, parentColumns = ["id"],
        childColumns = ["unitId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("unitId")]
)
data class Vocab(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val unitId: Long,
    val hanzi: String,         // 한자 (간체)
    val pinyin: String,        // 병음 (성조 포함, 예: yuánzhūbǐ)
    val partOfSpeech: String?, // 품사 (명사/동사/양사/부사 등)
    val meaning: String,       // 한국어 뜻
    val category: String?,     // 회화 단어 / 여러 단어 / 고유명사 등
    val orderInUnit: Int       // 페이지 내 순서 유지
)

// 기능1-B 회화: [화자 | 중국어 | 병음] + 섹션 제목/오디오 트랙
//  ※ 실제 교재 회화 페이지엔 한국어 해석이 없음 → korean은 처음 null.
//    한국어는 '해석 페이지를 따로 촬영'해 순서로 매칭하여 채운다(아래 매칭 플로우).
@Entity(
    tableName = "dialogue",
    foreignKeys = [ForeignKey(entity = StudyUnit::class, parentColumns = ["id"],
        childColumns = ["unitId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("unitId")]
)
data class Dialogue(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val unitId: Long,
    val sectionTitle: String?, // 회화 섹션 제목 (예: "작별 인사를 해요")
    val audioTrack: String?,   // 오디오 트랙 번호 (예: "01-03")
    val speaker: String?,      // 화자 라벨 (예: "A", "B")
    val chinese: String,       // 중국어 문장
    val pinyin: String?,       // 병음
    val korean: String?,       // 한국어 해석 (해석 페이지 매칭으로 채움, 처음 null)
    val orderInUnit: Int
)

// 기능2 커스텀 노트: 문법/뉘앙스 자유 텍스트. 특정 단원에 붙이거나(unitId) 전역(null)
@Entity(tableName = "custom_note")
data class CustomNote(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val unitId: Long?,         // null이면 단원 무관 전역 노트
    val title: String,         // 예: "觉得 / 想 / 猜 / 估计 차이"
    val body: String,          // 마크다운 허용 자유 텍스트
    val updatedAt: Long
)

// 학습 상태: 잠금화면/미션 노출 우선순위(간이 SRS)용
@Entity(tableName = "learning_state")
data class LearningState(
    @PrimaryKey val vocabId: Long,
    val seenCount: Int = 0,
    val correctCount: Int = 0,
    val lastSeenAt: Long = 0,
    val isMastered: Boolean = false
)

// 기능5 미션 진행
@Entity(tableName = "mission_progress")
data class MissionProgress(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateKey: String,       // "2026-06-08"
    val targetWords: String,   // JSON 배열: ["因为","所以"]
    val targetCount: Int = 10,
    val currentCount: Int = 0,
    val completed: Boolean = false
)
```

### 3.2 관계 조회 (예시 DAO)

```kotlin
@Dao
interface VocabDao {
    @Insert fun insertAll(items: List<Vocab>): List<Long>
    @Query("SELECT * FROM vocab WHERE unitId = :unitId ORDER BY orderInUnit")
    fun observeByUnit(unitId: Long): Flow<List<Vocab>>
    // 잠금화면: 오늘 단원에서 무작위 N개 (미숙 단어 가중치)
    @Query("""SELECT v.* FROM vocab v
              LEFT JOIN learning_state s ON v.id = s.vocabId
              WHERE v.unitId = :unitId
              ORDER BY COALESCE(s.seenCount,0) ASC, RANDOM() LIMIT :n""")
    fun pickForLockscreen(unitId: Long, n: Int): List<Vocab>
}
```

---

## 4. 기능별 설계

### 기능 1 — 카메라 촬영 → OCR → GPT 구조화 → DB 저장

**파이프라인**

```
[카메라/갤러리] → Bitmap
   → ML Kit Text Recognition (Chinese) → rawText
   → 사용자 입력/인식한 "권·과" + 콘텐츠 타입(단어장/회화) 선택
   → OpenAI GPT-4o (JSON 모드, 타입별 프롬프트)
   → 구조화 JSON 검증/파싱
   → 사용자 확인·수정 화면 (중요!) 
   → Room 저장
```

**회화 한국어 해석 매칭 플로우** (교재 회화 페이지엔 해석이 없음)
1. 회화 페이지 촬영 → 화자/중국어/병음 추출 → `Dialogue`로 저장 (korean=null).
2. 별도로 **해석 페이지 촬영** → `structureTranslation()`이 한국어 문장만 순서대로 추출.
3. 같은 단원/섹션 내에서 **순서(orderInUnit)로 1:1 매칭** → `korean` 채움.
4. 검수 화면에서 어긋난 쌍을 사용자가 드래그로 정렬·수정 후 확정.

**단어 암기 모드 (가리기 토글)** — 단어장 화면에서
- **한자만 보기**: 한자만 남기고 병음·뜻·품사를 임시로 가림(블러/공백) → 뜻을 떠올린 뒤 탭하면 공개.
- **뜻만 보기**: 뜻만 남기고 한자·병음을 가림 → 한자를 떠올린 뒤 공개.
- 가리기는 UI 상태일 뿐 DB 변경 없음. 행 탭으로 개별 공개, 토글로 전체 모드 전환.

**단어찾기 (품사별 모아보기)** — 홈 메뉴 → 명사/형용사/부사/양사 칩 선택 시
- `VocabDao.observeByPartOfSpeech(pos)`로 **단원과 무관하게** 등록된 모든 단어를 품사로 필터해 표시.
- 품사 칩과 개수는 `observePartOfSpeechCounts()`로 동적 생성(데이터에 있는 품사만 노출).

**오늘의 단어 (홈 카드)** — 매일 다른 단어 + 그 단어 뜻에 맞는 사진
- 날짜-결정형 선택: `index = epochDay % VocabDao.count()` → `getAt(index)`. 같은 날엔 항상 같은 단어.
- 배경 사진: 단어의 뜻(meaning) 키워드로 이미지 검색(예: Unsplash/Pixabay API) 후 표시. 실패 시 그라데이션 폴백.
- 사진은 캐시해 하루 단위로 유지(매 호출마다 새로 받지 않음).

**핵심 설계 포인트**
1. **타입을 사용자가 먼저 지정** (단어장 / 회화 / 회화 해석). 페이지 레이아웃이 달라 프롬프트를 분기해야 정확도가 높음.
2. **GPT는 "raw OCR 정리·구조화" 역할만**. OCR이 깨진 병음/한자를 GPT가 문맥으로 보정하도록 프롬프트에 명시.
3. **저장 전 사용자 검수 화면 필수**. OCR+LLM은 오인식이 있으므로, 표 형태로 보여주고 인라인 수정 후 저장.
4. **GPT 호출은 촬영 시 1회**. 이후 모든 학습은 DB만 사용 → 추가 비용 0.

**GPT 프롬프트 — 단어장 (요지)**
```
System: 너는 중국어 교재 OCR 텍스트를 표로 구조화하는 도우미다.
출력은 반드시 JSON. 각 항목: meaning(한국어 뜻), pinyin(성조부호 포함),
hanzi(간체), hanziGloss(각 한자 의미 풀이, 없으면 null).
OCR 오타로 보이는 병음/성조는 한자 기준으로 교정하라. 추측이면 추측이라고
별도 필드로 표시하지 말고 자연스럽게 교정하되, 한자 자체는 임의 변경 금지.

Output schema:
{ "items": [ { "meaning": "...", "pinyin": "...", "hanzi": "...", "hanziGloss": "..." } ] }
```

**GPT 프롬프트 — 회화 (요지)**
```
한국어 문장과 중국어 문장을 쌍으로 정렬. label은 상황/번호.
{ "items": [ { "label": "...", "korean": "...", "chinese": "...", "pinyin": "..." } ] }
```

**DTO → Entity 매핑**: 응답 JSON을 `kotlinx.serialization`으로 파싱 → `unitId` 주입 → `insertAll`.

**에러 처리**: ① OCR 결과 공백 → 재촬영 안내. ② GPT JSON 파싱 실패 → 1회 재시도 후 raw 텍스트 수기 편집 모드. ③ 네트워크 실패 → 사진+권/과를 큐에 보관, 재시도.

---

### 기능 2 — 커스텀 학습 노트

- `CustomNote` 엔티티에 자유 텍스트(마크다운) 누적. 단원에 귀속(`unitId`) 또는 전역(`null`).
- UI: 노트 목록 / 작성·편집(멀티라인 TextField, 간단 마크다운 프리뷰).
- 학습 시스템 포함: 잠금화면 카드 풀에 "노트 카드" 타입을 섞어 노출(예: 뉘앙스 비교 카드).
- 검색: `FTS4`(Room `@Fts4`) 적용 시 노트/단어 통합 검색 가능 (2차 고도화).

---

### 기능 3 — 커스텀 잠금화면 ⚠️ (가장 주의 필요)

#### 현실적 제약 (반드시 사전 합의)
- **OS 보안 잠금화면(PIN/패턴/지문)을 대체하거나 그 위에 오버레이할 수 없다.** `SYSTEM_ALERT_WINDOW`는 보안 키가드 위에 그려지지 않음 (Android 정책).
- 백그라운드에서 임의로 액티비티를 띄우는 것도 Android 10+에서 차단됨 (background activity launch 제한).

#### 채택 방식: "화면 켜짐 트리거 + 전용 전체화면 액티비티"
```
Foreground Service (상시, 알림 1개 노출)
   └ BroadcastReceiver: ACTION_SCREEN_ON / ACTION_USER_PRESENT 수신
        └ Intent로 LockLearnActivity 시작
             - setShowWhenLocked(true) / setTurnScreenOn(true)
             - FLAG_KEEP_SCREEN_ON
             - 단어/회화 카드 표시, 슬라이드로 정답 공개, 스피커로 TTS
```

- **두 가지 UX 옵션** (사용자 선택 제공):
  - (A) *기기 잠금 없음* 사용자: `setShowWhenLocked`로 잠금 위에 바로 학습 카드 표시 → 원래 구상에 가장 가까움.
  - (B) *기기 잠금 있음* 사용자: `ACTION_USER_PRESENT`(잠금 해제 직후) 트리거 → 잠금 해제하면 학습 카드가 먼저 뜨는 방식.
- **필요 권한/설정**: `FOREGROUND_SERVICE`, `POST_NOTIFICATIONS`(13+), `SYSTEM_ALERT_WINDOW`(옵션 표시용), 배터리 최적화 예외 안내, 일부 제조사(삼성/샤오미) 자동시작 허용 가이드.
- **카드 내용**: `pickForLockscreen()`로 오늘 단원에서 미숙 단어 가중 무작위 추출. 순차/무작위 토글 설정.
- **상호작용**: 좌우 슬라이드=다음/이전, 위로 슬라이드=정답(뜻·병음) 공개, 스피커 탭=TTS 재생, 길게=마스터 표시.

> 이 기능은 "OS를 진짜 잠그는" 게 아니라 "화면을 켤 때마다 학습 카드를 먼저 보게 만드는" 것임을 제품 설명에서 명확히 해야 함.

---

### 기능 4 — 네이티브 TTS (중국어 성조)

```kotlin
class TtsManager(context: Context) {
    private val tts = TextToSpeech(context) { status ->
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.CHINA          // zh-CN, 성조 재생
            tts.setSpeechRate(0.9f)              // 학습용으로 살짝 느리게
        }
    }
    fun speak(text: String) =
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, text.hashCode().toString())
}
```
- 단어장/회화/잠금화면/노트 어디서나 스피커 아이콘 → `speak(hanzi)` 또는 `speak(chinese)`.
- **주의**: 일부 기기에 중국어 TTS 데이터 미설치 → `isLanguageAvailable` 체크 후 `ACTION_INSTALL_TTS_DATA` 안내.
- 싱글톤(Hilt)으로 관리, 앱 종료 시 `shutdown()`.

---

### 기능 5 — 게이미피케이션 "오늘의 말하기 미션"

```
AlarmManager/WorkManager (출퇴근 지정 시각)
   → Notification 푸시: "오늘 핵심 패턴(因为, 所以)으로 10번 말하기!"
   → 탭 → MissionActivity
        - 마이크 버튼 → SpeechRecognizer(Locale.CHINA, STT)
        - 인식 결과 텍스트에 targetWords 포함 여부 대조
        - 포함 시 currentCount + 1, 진행바 갱신, TTS로 모범 발음 피드백
        - 10회 도달 → 축하 팝업 + MissionProgress.completed=true
```
- **타깃 매칭**: 인식 문자열 정규화(공백/문장부호 제거) 후 한자 substring 또는 병음 매칭. 발음 유사 오인식 대비, 한자/병음 둘 다로 매칭.
- **STT 제약**: `SpeechRecognizer`는 무료지만 네트워크 필요(기기별). 오프라인 인식은 기기 지원 시에만. 실패 시 텍스트 입력 폴백 제공.
- 미션 데이터는 `MissionProgress`에 날짜별 저장 → 연속 달성(streak) 게이미피케이션으로 확장 가능.

---

## 5. 비용 · 권한 · 리스크 정리

### 5.1 비용
| 항목 | 비용 | 빈도 |
|---|---|---|
| ML Kit OCR | 무료(온디바이스) | 촬영 시 |
| GPT-4o 구조화 | 유료(토큰) | **촬영 1회/페이지만** |
| TTS / STT | 무료(OS 내장) | 무제한 |

→ 핵심: GPT 호출을 "촬영 시점 1회"로 가두면 운영 비용이 사용자당 수십 원 수준으로 제한됨.

### 5.2 주요 권한
`CAMERA`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`(34+), `POST_NOTIFICATIONS`, `RECORD_AUDIO`(STT), `SYSTEM_ALERT_WINDOW`(옵션), `RECEIVE_BOOT_COMPLETED`(서비스 재시작), `INTERNET`.

### 5.3 리스크 & 대응
| 리스크 | 영향 | 대응 |
|---|---|---|
| 잠금화면 OS 제약 | 핵심 기능 UX 변경 | 우회 방식(3장) 사전 합의, 두 UX 옵션 제공 |
| 제조사 백그라운드 종료(삼성/샤오미) | 서비스 죽음 | Foreground Service + 자동시작 허용 가이드 + 배터리 예외 |
| OCR+GPT 오인식 | 잘못된 단어 저장 | **저장 전 검수 화면 필수** |
| API 키 노출 | 비용 도용 | 키를 앱에 하드코딩 금지 → 자체 프록시 서버 경유 권장 |
| 중국어 TTS/STT 미지원 기기 | 음성 기능 불가 | 가용성 체크 + 데이터 설치 안내 + 텍스트 폴백 |

> **API 키 보안 (중요)**: OpenAI 키를 APK에 넣으면 추출당해 과금 폭탄 위험. 초기 MVP/개인용이면 BuildConfig + 사용량 한도로 시작하되, 배포 시에는 **얇은 백엔드 프록시**(키 보관·rate limit)를 두는 것을 강력 권장.

---

## 6. 개발 로드맵 (권장 순서)

| 단계 | 내용 | 산출물 |
|---|---|---|
| **0** | 프로젝트 스캐폴딩, Hilt/Room/Retrofit 셋업 | 빌드되는 빈 앱 |
| **1** | Room 스키마 + DAO + 단원/단어/회화 더미 시드 | DB 동작 |
| **2** | 단어장/회화/노트 조회·편집 UI (Compose) | 콘텐츠 CRUD |
| **3** | 카메라 → ML Kit OCR → GPT 구조화 → 검수 → 저장 | 기능1 완성 |
| **4** | TTS 통합(스피커 아이콘 전역) | 기능4 완성 |
| **5** | 잠금화면 서비스 + 전체화면 학습 액티비티 | 기능3 완성 |
| **6** | 미션 알림 + STT 카운팅 + 축하 팝업 | 기능5 완성 |
| **7** | 학습 상태(SRS) 가중치, 설정, 다듬기 | MVP 완료 |

> 기능1(OCR/GPT)이 가장 까다로워 보이지만, **잠금화면(기능3)이 OS 제약 때문에 실제 리스크가 가장 큽니다.** 그래서 2~4단계로 "앱의 본체(콘텐츠+학습+음성)"를 먼저 완성해 가치를 확보하고, 그다음 잠금화면을 붙이는 순서를 권장합니다.

---

## 부록 A — 미해결/확인 필요 항목
- [ ] 참조 이미지(image_73f94e/73f92e/73f246.png) 실제 표 레이아웃 확인 → 컬럼 미세 조정
- [ ] "한자 풀이"의 정의: 글자별 뜻인지, 단어 어원 설명인지 (프롬프트 영향)
- [ ] 회화 병음 표시 필요 여부
- [ ] OpenAI 직접 호출 vs 프록시 서버 구축 범위 (배포 계획에 따라)
- [ ] 타깃 기기에 기본 잠금(PIN) 사용 여부 → 잠금화면 UX 옵션 A/B 결정
