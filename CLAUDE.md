# Community Server — CLAUDE.md

## 프로젝트 개요

JWT 기반 인증을 사용하는 커뮤니티 게시판 백엔드 서버.
게시글/댓글/좋아요/마이페이지 기능을 제공하며, Redis로 조회수 캐싱 및 Refresh Token을 관리하고, S3로 다중 이미지 업로드를 처리한다.

## 기술 스택

- **언어/플랫폼**: Java 21, Spring Boot 3.5
- **데이터**: Spring Data JPA, Spring Data Redis, MySQL, H2(테스트)
- **쿼리**: QueryDSL 5.0 (Jakarta)
- **웹**: Spring MVC, Thymeleaf, Spring Security (JWT Stateless)
- **스토리지**: AWS S3 (spring-cloud-aws 3.1)
- **API 문서**: springdoc-openapi 2.8.x (`/swagger-ui/index.html`)
- **메일**: Spring Mail (Gmail SMTP)
- **빌드**: Gradle (Groovy DSL)
- **유틸**: Lombok, Bean Validation

## 데이터 흐름

```
클라이언트
  → POST /api/users/login → JWT Access/Refresh Token 발급 → Redis 저장
  → POST /api/posts (multipart) → S3 이미지 업로드 → MySQL 저장
  → GET /api/posts/{id} → Redis 조회수 증가 (IP 기반 중복 방지)
  → 스케줄러(새벽 3시) → Redis 조회수 DB 동기화 + 휴지통 게시글 영구 삭제
```

## 패키지 구조

```
com.community.community
├── comment/         # 댓글 CRUD, 대댓글, 고아 댓글 삭제
├── common/          # BaseTimeEntity, S3Service, HealthCheckController
├── config/          # DataCleanupScheduler, QuerydslConfig, SwaggerConfig, WebConfig
├── exception/       # GlobalExceptionHandler, CustomException, ErrorCode(enum), ErrorDto
├── like/            # 좋아요 토글
├── mypage/          # 내 정보 조회/수정, 내 글/댓글/좋아요 목록
├── post/            # 게시글 CRUD, 페이징/검색, 소프트 딜리트, S3 이미지
├── redis/           # RedisService, RedisTestRunner
├── security/        # SecurityConfig, JWT 필터/유틸/핸들러
└── user/            # 회원가입/로그인/로그아웃/토큰 재발급/비밀번호 찾기, Role(enum)
```

## 레이어 규칙

- **Controller → Service → Repository** 단방향 의존
- Controller는 DTO만 반환, 엔티티를 직접 노출하지 않음
- `@Transactional`은 Service 레이어에서만 관리 (Controller에 금지)
- 예외는 `CustomException(ErrorCode)` 를 throw, `GlobalExceptionHandler` 가 일괄 처리
- 새 에러 케이스는 `ErrorCode` enum에 추가 후 `CustomException` 으로 래핑

## 빌드 및 실행

```bash
./gradlew build          # 전체 빌드 (Q클래스 자동 생성 포함)
./gradlew test           # 테스트
./gradlew bootRun        # 로컬 실행 (application-local.yaml 활성화 필요)
```

QueryDSL Q클래스 생성 경로: `build/generated/querydsl`

## 환경 프로파일

| 프로파일 | 설정 파일 | 설명 |
|---------|-----------|------|
| `local` | `application-local.yaml` | 로컬 개발 (H2 또는 로컬 MySQL/Redis) |
| `prod`  | `application-prod.yaml`  | 운영 환경 (EC2 + MySQL + Redis) |

## 엔티티 요약

| 엔티티 | 저장소 | 설명 |
|--------|--------|------|
| `UserEntity` | MySQL | 회원 정보, Role Enum (USER/ADMIN) |
| `PostEntity` | MySQL | 게시글, 소프트 딜리트(`isDeleted`), 카테고리 |
| `PostImageEntity` | MySQL | 게시글 첨부 이미지 URL (S3) |
| `CommentEntity` | MySQL | 댓글/대댓글 (자기참조), 소프트 딜리트 |
| `LikeEntity` | MySQL | 게시글 좋아요 |
| RT:{userId} | Redis | Refresh Token (TTL 7일) |
| viewCount::{postId} | Redis | 게시글 조회수 캐시 (배치로 DB 동기화) |
| view:post:{id}:ip:{ip} | Redis | 조회수 중복 방지 (TTL 24시간) |

## 주요 API

| 메서드 | 경로 | 인증 | 설명 |
|--------|------|------|------|
| POST | `/api/users/signup` | 불필요 | 회원가입 |
| POST | `/api/users/login` | 불필요 | 로그인 (JWT 발급) |
| POST | `/api/users/reissue` | 불필요 | 토큰 재발급 |
| POST | `/api/users/logout` | 필요 | 로그아웃 |
| POST | `/api/users/password/find` | 불필요 | 임시 비밀번호 발급 |
| GET | `/api/posts` | 불필요 | 게시글 목록 (페이징/검색) |
| GET | `/api/posts/{id}` | 불필요 | 게시글 상세 |
| POST | `/api/posts` | 필요 | 게시글 작성 (multipart) |
| PUT | `/api/posts/{id}` | 필요 | 게시글 수정 |
| DELETE | `/api/posts/{id}` | 필요 | 게시글 삭제 (소프트) |
| POST | `/api/posts/{postId}/comments` | 필요 | 댓글 작성 |
| POST | `/api/posts/{postId}/comments/{parentId}/replies` | 필요 | 대댓글 작성 |
| POST | `/api/posts/{postId}/likes` | 필요 | 좋아요 토글 |
| GET | `/api/mypage/info` | 필요 | 내 정보 조회 |
| GET | `/swagger-ui/index.html` | 불필요 | Swagger API 문서 |

## 코딩 규칙

- Lombok(`@RequiredArgsConstructor`) 생성자 주입 사용, 필드 주입(`@Autowired`) 금지
- Role, 상태값 등 문자열 상수는 Enum으로 관리
- 복잡한 조회 쿼리는 `*RepositoryCustom` (QueryDSL) 에 작성
- 이미지 업로드/삭제는 `S3Service` 에서만 처리
- 스케줄러 로직은 `DataCleanupScheduler` 에서만 관리
