# 🔥 Toy Community (백엔드 중심 커뮤니티 API 서버)

> 🎯 **대용량 트래픽 처리와 성능 최적화에 집중한 백엔드 API 서버입니다.**
>
>💡 설계한 API의 완벽한 클라이언트 연동 검증을 위해 프론트엔드(Vanilla JS) 화면 구현부터 인프라 무중단 배포(CI/CD)까지 1인 풀사이클을 구축했습니다.
> 
>  **바닐라 JS 기반의 프론트엔드 UI**부터 **무중단 배포 파이프라인**까지 서비스 전체를 1인 구축했습니다.  
> 
> ⚡ **성능 최적화** : Batch Size 및 Fetch Join을 통한 JPA N+1 문제 해결  
> 🛡️ **어뷰징 방지** : Redis TTL을 활용한 단기간 무한 조회수 증가 방어  
> ☁️ **인프라 구축** : AWS S3 이미지 처리 및 GitHub Actions 기반 무중단 배포  
> 🔐 **보안 및 인증** : Spring Security와 JWT를 활용한 Stateless 인증 시스템

🔗 **실제 서비스 접속해 보기:** http://rkqkdrnportfolio.shop:8082/

<br>

## 🚀 프로젝트 개요
- **개발 인원**: 1인 (개인 프로젝트)
- **주요 목적**: RESTful API 설계 역량 강화, 클라우드 인프라 활용 및 SPA(Single Page Application) 기반의 부드러운 UI/UX 구현

<br>

## 🛠️ 기술 스택 (Tech Stack)

### Backend
- **Framework**: Spring Boot 3.x
- **Language**: Java 21
- **Database**: Spring Data JPA, MySQL (Prod), H2 (Local), Redis
- **Security**: Spring Security, JWT (JSON Web Token)

### Frontend
- **Languages**: HTML5, CSS3, Vanilla JavaScript (ES6)
- **Architecture**: SPA (단일 페이지 화면 전환 로직 직접 구현)

### Infrastructure & CI/CD
- **Cloud**: AWS EC2, AWS S3
- **CI/CD**: GitHub Actions, SCP (배포 자동화)

<br>

## ✨ 핵심 기능 (Key Features)

### 1. 🔐 사용자 인증/인가 및 관리자 권한
- Spring Security와 JWT를 활용한 Stateless 로그인 구현
- Local Storage 기반의 토큰 관리 및 위조 토큰(null) 전송 방어 로직 적용
- `ADMIN` 권한을 부여받은 사용자는 악성 게시글 강제 삭제 가능

### 2. 📝 게시글/댓글 CRUD 및 모던 UI
- **카드형 레이아웃**의 트렌디한 게시판 UI 제공 (화면 깜빡임 없는 SPA 동작)
- 게시글 작성/수정/삭제 및 실시간 댓글 작성 기능 (본인 글만 제어 가능)
- 반정규화(`commentCount`)를 통한 리스트 화면 댓글 수 표시 최적화

### 3. 🖼️ AWS S3 다중 이미지 및 썸네일
- 게시글 작성 시 여러 장의 이미지를 클라우드(S3)에 안전하게 저장
- 게시글 목록 조회 시 S3에서 첫 번째 이미지를 **썸네일**로 자동 추출하여 노출

### 4. 🚀 조회수/좋아요 동시성 및 어뷰징 방지 (Redis)
- 동일 IP에서 단기간 내 여러 번 조회 시 조회수가 무한정 오르지 않도록 Redis TTL 활용
- 좋아요 기능 제공 및 실시간 카운트 반영

### 5. ⚡ JPA 성능 최적화 (N+1 문제 해결)
- 메인 화면 게시글 목록(PostList) 호출 시 발생하는 연관 엔티티 조회의 N+1 문제 인지
- `default_batch_fetch_size` 설정과 Fetch Join을 통해 쿼리 수 대폭 최적화 달성

<br>

## 🌐 시스템 아키텍처 및 배포 파이프라인
1. 개발자가 GitHub `main` 브랜치에 코드를 **Push**
2. **GitHub Actions**가 트리거되어 코드 빌드 (`.jar` 생성)
3. 빌드된 파일을 **AWS EC2** 인스턴스로 전송