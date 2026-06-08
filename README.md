# 틈새 중국어 (ChineseLockApp)

직장인 맞춤형, 잠금화면/백그라운드 활용 중국어 학습 앱.
Kotlin + Jetpack Compose + Room + Hilt + ML Kit(OCR) + OpenAI GPT-4o(구조화).

> 전체 설계: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)

---

## 0. 현재 상태 — 1차 구현 완료 (시안 → 실제 Compose 앱)
하단 탭(홈/단어장/회화/즐겨찾기) + 단어찾기·추가 화면까지 동작하도록 구현했습니다.
첫 실행 시 샘플 데이터(3-1 단어/회화)가 자동 시드됩니다.

- **홈**: 오늘의 단어(날짜별 랜덤) + 단어·회화 추가 / 단어찾기 진입
- **단어장**: 단원 드롭다운, 전체/한자만/뜻만 가리기 토글, ★ 즐겨찾기, 발음 듣기, 연필=수정(추가/삭제)
- **회화**: A/B 화자(본문형은 화자 없음), 한 줄 듣기·전체 듣기, 중국어/번역 토글, 수정(추가/삭제)
- **단어찾기**: 한자·병음·한글 검색 + 품사별 필터(데이터에 있는 품사만)
- **즐겨찾기**: ★ 단어만 모아 가리기 연습
- **추가**: 제목(3-1) + 직접 입력(뜻/병음/한자 붙여넣기) 미리보기·등록

> 제거됨: 잠금화면·말하기 미션(제품 범위에서 제외).
> 미연결: 카메라 OCR→GPT 자동 구조화(직접 입력은 동작). 단어 사진 자동 매칭.
> 폰트: 시안의 Cafe24/맑은고딕은 ttf 번들 시 적용(현재 시스템 기본).

빌드하려면 아래 1번(Android Studio 설치) 후 프로젝트를 열면 됩니다.

## 1. 빌드하려면 먼저 설치 (이 PC엔 아직 없음)
1. **Android Studio** 최신 버전 설치 → 최초 실행 시 Android SDK(API 35), 빌드 도구, JDK 17이 함께 설치됩니다.
2. Android Studio에서 **이 폴더(`ChineseLockApp`)를 Open** → Gradle 동기화 시
   `gradle/wrapper/gradle-wrapper.jar` 와 `gradlew`가 자동 생성됩니다.
   (CLI로 만들려면 Gradle 설치 후 프로젝트 루트에서 `gradle wrapper` 실행)

## 2. OpenAI 키 설정
1. `secrets.properties.example` → `secrets.properties` 로 복사 (이 파일은 .gitignore 처리됨)
2. `OPENAI_API_KEY=sk-...` 입력
3. ⚠️ **배포 시 주의**: APK에 박힌 키는 추출당합니다. 실제 배포는 키를 앱에 넣지 말고
   **자체 백엔드 프록시**를 경유하도록 바꾸세요. (`NetworkModule` 주석 참고)

## 3. 프로젝트 구조
```
app/src/main/java/com/example/chineselock/
├── MainApplication.kt        # Hilt 진입점
├── MainActivity.kt           # 홈(Compose)
├── core/
│   ├── db/                   # Room: Entities, Daos, AppDatabase
│   ├── network/              # OpenAI service + OCR 구조화(OcrStructurer)
│   ├── tts/TtsManager.kt     # zh-CN TTS
│   └── di/                   # Hilt 모듈(Database/Network)
└── feature/
    ├── lockscreen/           # 기능3: Service / BootReceiver / LockLearnActivity
    └── mission/              # 기능5: MissionActivity
```

## 4. 다음 단계 (로드맵)
ARCHITECTURE.md 6장 참고. 권장 순서:
DB 시드 → 단어장/회화/노트 UI → 카메라 OCR→GPT 파이프라인 → TTS 연결 → 잠금화면 카드 UI → 미션 STT.

## 5. 주의사항
- `fallbackToDestructiveMigration()` 은 개발용. 출시 전 실제 마이그레이션으로 교체.
- 잠금화면은 OS 보안 잠금을 대체하지 않습니다(설계 문서 3장). 화면 켜짐 트리거 방식입니다.
- 일부 제조사(삼성/샤오미)는 백그라운드 서비스를 종료합니다 → 자동시작/배터리 예외 안내 필요.
