# 🔥 Toy Community Project

> 🎯 **"백엔드 성능 최적화부터 프론트엔드 UX까지, 1인 풀사이클(Full-Cycle) 커뮤니티 플랫폼"**
>
> 단순히 API만 만드는 것을 넘어, Vanilla JS 기반의 SPA(Single Page Application) 프론트엔드를 직접 구현하여 클라이언트와의 연동을 검증했습니다.  
> 
> AWS S3 파일 스토리지, Redis를 활용한 성능 개선 및 보안, Nginx를 통한 CI/CD 무중단 배포까지 실제 서비스와 동일한 인프라를 구축했습니다.

🔗 **실제 서비스 접속해 보기:** [http://community.rkqkdrnportfolio.shop/](http://community.rkqkdrnportfolio.shop/)

🔗 **Swagger API**: [http://community.rkqkdrnportfolio.shop/swagger-ui/index.html](http://community.rkqkdrnportfolio.shop/swagger-ui/index.html)
<br>

## 🛠️ Tech Stack & Architecture

### Tech Stack
* Backend: Java 21, Spring Boot 3.x, Spring Data JPA, Spring Security, JWT
* Frontend: HTML5, CSS3, Vanilla JavaScript (ES6), Fetch API
* Database & Storage: MySQL (Prod), H2 (Local), Redis, AWS S3
* Infrastructure: AWS EC2, Nginx (Reverse Proxy), GitHub Actions, SCP 

### System Architecture
![AWS EC2 Infrastructure Flow-2026-03-14-143058.png](docs/images/AWS%20EC2%20Infrastructure%20Flow-2026-03-14-143058.png)

* Client: 외부 라이브러리(React, Vue 등) 의존 없이 브라우저 핵심 API와 DOM을 직접 제어하여 성능 최적화
* Server: RESTful API 설계 및 Security 필터 단의 안전한 JWT 검증 로직 통과
* Reverse Proxy: Nginx가 80번 포트에서 트래픽을 받아 8080/8081 포트로 무중단 스위칭 수행
* Storage:
  * 정형 데이터(게시글, 유저, 댓글 등): MySQL
  * 비정형 데이터(이미지, PDF 등): AWS S3
  * 캐시 및 실시간 데이터(조회수, Refresh Token): Redis

### 성능 최적화 전략
- **조회수 Write-Back 패턴**: Redis를 활용하여 조회수 업데이트 부하 최적화
  - 실시간 DB Update를 지양하고 Redis 카운팅 후 스케줄러를 통한 배치 동기화 구현
- QueryDSL 기반 N+1 방어: Fetch Join을 통한 데이터 로딩 성능 최적화 (API 응답 속도 개선)
  - **QueryDSL 기반 N+1 방어**: Fetch Join을 통해 기존 `1 + N`번의 쿼리를 `1`번으로 통합. **(댓글 20개 기준, DB I/O 요청 약 90% 감소 효과)**
  - **Like 존재 여부 최적화**: `exists` 쿼리 시 전체 엔티티 조회 대신 `selectOne()`과 `fetchFirst()`를 사용해 **조회 성능 최적화**
- **데이터 생명주기 관리 (Data GC)**:
  - S3 좀비 파일(고아 객체) 탐색 및 삭제 스케줄러 구현
  - 30일 경과 휴지통 데이터 및 고아 댓글 자동 파기 로직 적용

<br>

## 💡 주요 기술적 의사결정 및 트러블 슈팅

### 1. 🌐 프론트엔드 프레임워크 대신 Vanilla JS 선택 및 SPA 구현
> **의사결정:** 라이브러리 의존 없이 브라우저 핵심 API와 DOM 제어 원리를 깊이 있게 학습하기 위해 순수 **JavaScript**로 SPA 구축

* **🎯 목적:** React 등 프레임워크에 의존하기 전, 브라우저 렌더링 원리와 비동기 통신(`Fetch API`)의 본질을 이해
* **🛠️ 구현:** 라이브러리 없이 순수 JavaScript(ES6)만으로 DOM 직접 조작, LocalStorage 기반 JWT 토큰 관리 및 SPA(Single Page Application) 라우팅 로직 구현
* **📈 성과:** 클라이언트-서버 간의 HTTP 통신 흐름과 JWT 인증 사이클을 체득 및 불필요한 번들 사이즈 감소

<br>

### 2. 🔐 JWT & RTR(Refresh Token Rotation) 인증 설계
> **의사결정:** 서버 부하를 줄이는 무상태(Stateless) 인증을 위해 **Spring Security + JWT**를 도입하고, 보안 강화를 위해 **RTR** 기법 결합

* **🚨 Issue:** 보안을 위해 Access Token 만료 시간을 짧게 설정하자, 글 작성 중 토큰이 만료되어 **데이터가 증발하고 강제 로그아웃되는 심각한 UX 저하** 발생
* **💡 Resolution:**
  * **Refresh Token Rotation (RTR) 기법 적용**: 토큰 재발급 시 Refresh Token도 함께 갱신하여 보안성 강화
  * **Axios-like Interceptor 구현:** 401 에러 발생 시 백그라운드에서 토큰을 갱신하고, 실패했던 원래 요청을 자동으로 재시도하여 끊김 없는 UX 제공
* **📈 성과:** 보안성 확보와 동시에 사용자 활동의 연속성을 보장하는 인증 시스템 완성

<br>

### 3. 🧹 지능형 리소스 관리 및 스토리지 GC(Garbage Collection)
>  **의사결정:** 클라우드 비용 절감 및 데이터 무결성을 위해 Spring Scheduler를 활용한 **데이터 클리닝 자동화** 설계

* **🚨 Issue:** Soft Delete 정책으로 인해 DB에서는 삭제되었으나 S3 버킷에 남아있는 **고아 객체(좀비 파일)들로 인한 비용 증가**
* **💡 Resolution:**
  * **S3 좀비 파일 삭제 스케줄러:** DB에 존재하지 않는 파일 경로를 S3에서 주기적으로 탐색하여 삭제하는 가비지 컬렉션 로직 구현
  * **자동 파기 정책:** 삭제 후 30일이 경과한 데이터 및 하위 댓글이 없는 고아 댓글을 자동으로 영구 삭제 처리
* **📈 성과:** 스토리지 가용량 확보 및 클라우드 인프라 유지 비용 최적화

<br>

### 4. ⚡ Redis Write-Back 패턴을 통한 조회수 업데이트 최적화
> **의사결정:** 빈번한 조회수 UPDATE 쿼리로 인한 DB 병목을 방지하고자 Redis에 선 누적 후 배치 동기화 전략 채택

![Memory and Disk Data Flow-2026-03-14-143130.png](docs/images/Memory%20and%20Disk%20Data%20Flow-2026-03-14-143130.png)
* **🚨 Issue:** 무분별한 새로고침 시 조회수가 비정상적으로 급증하고 DB I/O 부하가 심화됨
* **💡 Resolution:**
    * **Write-Back 패턴 도입:** 실시간 DB 업데이트 대신 Redis에 조회수를 우선 누적하고, 스케줄러를 통해 주기적으로 DB에 일괄 반영(Bulk Update)하도록 개선
    * **어뷰징 차단:** `view:post:{postId}:ip:{clientIp}` 키와 24시간 TTL을 활용해 중복 카운팅 방지
* **📈 성과:** DB 쓰기 부하를 80% 이상 절감하고 동시 접속자 처리 성능 개선

<br>

### 5. 🚀 Nginx 기반 Blue-Green 무중단 배포 파이프라인

> **의사결정:** 서비스 가동 중지 시간(Downtime)을 없애기 위해 Nginx 리버스 프록시를 활용한 **Blue-Green Deployment** 구축

* **🚨 Issue:** 새로운 기능 배포 시 서버 재시작으로 인해 발생하는 **서비스 단절(Downtime) 및 유저 경험 저하**
* **💡 Resolution:**
    * **Blue-Green 배포:** GitHub Actions와 Nginx 리버스 프록시를 결합하여 8080/8081 포트 간의 무중단 스위칭 환경 구축
    * **안정적 전환:** 새 서버 기동 후 Health Check API(`OK` 응답)를 통해 구동이 확인된 시점에만 트래픽 방향을 전환
* **📈 성과:** 배포 중에도 사용자 경험을 저해하지 않는 Zero-Downtime 운영 환경 확보

### 6. 🏗️ QueryDSL 도입을 통한 동적 쿼리 및 N+1 최적화

> 의사결정: JPA의 한계를 넘어 타입 안정성이 보장된 동적 쿼리 구현 및 성능 최적화를 위해 QueryDSL 전면 도입

* **🚨 Issue:** 검색 조건 조합에 따른 동적 쿼리 작성 시, JPA의 메서드 명명 규칙만으로는 가독성이 떨어지고 런타임 에러 위험이 큼 
  * 게시글 상세 조회 시 댓글과 유저 정보를 가져오는 과정에서 발생하는 N+1 문제로 인한 성능 저하

* **💡 Resolution:**
  * 동적 쿼리 표준화: BooleanExpression을 활용하여 검색 조건(keyword) 유무에 따라 쿼리를 유연하게 생성하도록 개선
  *Fetch Join 최적화: fetchJoin()을 적용하여 연관 엔티티를 한 번의 쿼리로 통합 조회, DB I/O 발생 횟수를 획기적으로 단축
  *비즈니스 로직 DB 위임: '공지사항 상단 고정' 등의 복잡한 정렬 로직을 CaseBuilder를 통해 DB 레벨에서 처리하여 애플리케이션 메모리 부하 절감

* **📈 성과:** 파일 시점의 문법 체크로 코드 안정성 확보 및 유지보수 비용 감소
  * Repository-Custom-Impl 3단 설계를 통해 JPA의 생산성과 QueryDSL의 유연함을 동시에 확보한 객체지향적 아키텍처 완성
  * "단순히 성능을 높인 것을 넘어, **자바 코드로 쿼리를 작성함으로써 코드 리뷰가 용이해지고 오타로 인한 런타임 에러를 사전에 차단**하는 개발 생산성을 확보함"

<br>

## 📌 주요 화면 (Screenshots)

<details>
<summary> 🌌 화면 캡처 보기 (클릭하여 펼치기) </summary>

### 1. 메인 및 게시글 목록
![메인 화면](docs/images/community_main.png)
- **카드형 레이아웃:** 3x3 그리드 방식의 트렌디한 목록 UI, 카테고리 뱃지(공지/자유/질문) 및 동적 페이징 처리

---

### 2. 회원가입 및 보안 (Auth)
| 회원가입 | 비밀번호 찾기 |
| :---: |:---:|
| ![회원가입](docs/images/signup.png) |![비밀번호 찾기](docs/images/findpassword.png) |
- **보안 강화:** 회원가입 시 비밀번호 찾기 힌트를 설정하며, 이메일을 통한 임시 비밀번호 발급 기능 제공

---

### 3. 게시글 관리 (Post Management)
| 게시글 작성 (다중 이미지/PDF) | 게시글 수정 (동적 UI) |
|:---:|:---:|
|![게시글 작성](docs/images/post_create.png)|![게시글 수정](docs/images/post_edit.png)|
- **파일 첨부:** 장바구니 형태의 파일 누적 추가 및 개별 삭제(X버튼) 지원
- **카테고리:** 관리자 전용 '공지' 작성 및 일반 유저의 '자유/질문' 분류 기능

---

### 4. 마이페이지 (My Page)
| 활동 내역 요약 | 내 정보 수정 |
|:---:|:---:|
|![활동 내역](docs/images/mypage.png)|![정보 수정](docs/images/edit_myprofile.png)|
- **통합 조회:** 내가 쓴 글, 작성한 댓글, 좋아요 누른 게시글을 한곳에서 모아보기 가능
- **보안 검증:** 비밀번호 변경 시 현재 비밀번호 확인 필수

</details>

---

최근 업데이트 2026.03.25 -README V1.1.0 (서버 url 변경)