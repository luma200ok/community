# 🔥 Toy Community (대용량 트래픽 및 UX 최적화 커뮤니티)

> 🎯 **"백엔드 성능 최적화부터 프론트엔드 UX까지, 1인 풀사이클(Full-Cycle)로 완성한 커뮤니티 플랫폼"**
>
> 단순히 API만 만드는 것을 넘어, **Vanilla JS 기반의 SPA(Single Page Application) 프론트엔드**를 직접 구현하여 클라이언트와의 연동을 완벽하게 검증했습니다.
> AWS S3 파일 스토리지, Redis를 활용한 성능 개선 및 보안, GitHub Actions를 통한 CI/CD 무중단 배포까지 실제 서비스와 동일한 인프라를 구축했습니다.

🔗 **실제 서비스 접속해 보기:** [http://rkqkdrnportfolio.shop:8082/](http://rkqkdrnportfolio.shop:8082/)

<br>

## 🛠️ Tech Stack

**Backend**
- Java 21, Spring Boot 3.x
- Spring Data JPA, Spring Security
- MySQL (Prod), H2 (Local), Redis
- JWT (JSON Web Token)

**Frontend**
- HTML5, CSS3, Vanilla JavaScript (ES6)
- Fetch API, FormData (비동기 통신)

**Infrastructure & DevOps**
- AWS EC2, AWS S3
- GitHub Actions, SCP (배포 자동화)

<br>

## ✨ Key Features & Trouble Shooting

### 1. ⚡ 조회수 어뷰징 방지 및 성능 최적화 (Redis & JPA)
- **Redis TTL 활용:** 동일 IP에서 단기간 내 여러 번 조회 시 조회수가 무한정 오르지 않도록 Redis를 활용해 24시간의 쿨타임(TTL)을 적용하여 어뷰징을 원천 차단했습니다.
- **N+1 문제 해결:** 메인 화면 게시글 목록 호출 시 발생하는 연관 엔티티 조회(N+1) 문제를 인지하고, `@EntityGraph`(Fetch Join)와 `default_batch_fetch_size` 설정을 통해 쿼리 수를 대폭 감소시켰습니다.

### 2. 🔐 JWT 기반 Stateless 인증 및 Refresh Token 자동 갱신
- Spring Security와 JWT를 활용해 서버 확장성에 유리한 Stateless 인증 시스템을 구축했습니다.
- **보안 및 UX 강화:** Access Token 만료 시(401 Unauthorized), 프론트엔드의 `fetchWithAuth` 인터셉터 로직이 백엔드의 `/reissue` API를 조용히 호출하여 **사용자 모르게 토큰을 갱신하고 실패했던 요청을 재시도(Retry)** 하도록 구현하여 끊김 없는 UX를 제공합니다.

### 3. 🖼️ AWS S3 다중 파일 업로드 및 클라이언트 미리보기 UX
- **안전한 스토리지:** 서버 하드디스크 용량 문제 및 배포 시 파일 유실 문제를 해결하기 위해 AWS S3 클라우드 스토리지를 도입했습니다.
- **프론트엔드 렌더링:** `FormData`와 `FileReader` API를 활용해 여러 장의 이미지 및 PDF 파일을 업로드하기 전에 사용자에게 썸네일과 개별 삭제 버튼을 제공하는 모던한 UI를 구현했습니다.

### 4. 🚀 Vanilla JS 기반 SPA 라우팅 & 동적 페이징
- React나 Vue 없이 순수 Vanilla JS만으로 화면 깜빡임 없는 단일 페이지(SPA) 전환 로직을 직접 구현했습니다.
- 카드형 UI 레이아웃(3x3)에 맞춰 서버에 동적으로 페이징 정보를 요청하고, 하단에 동적 페이지네이션 버튼을 렌더링합니다.

### 5. ♾️ GitHub Actions 기반 CI/CD 파이프라인
- `main` 브랜치에 코드가 Push 되면 GitHub Actions가 자동으로 빌드를 수행하고, SCP를 통해 AWS EC2로 전송 후 기존 서버를 내리고 새 서버를 백그라운드(`nohup`)로 띄우는 자동 배포 프로세스를 구축했습니다.

<br>

## 📌 주요 화면 (Screenshots)

<details>

<summary>  🌌 screen shots </summary>

### 1. 메인 및 게시글 목록
![메인 화면](docs/images/community_main.png)
- **카드형 레이아웃:** 3x3 그리드 방식의 트렌디한 목록 UI와 페이징 처리를 지원합니다.

---

### 2. 회원가입 및 보안 (Auth)
| 회원가입 |                     비밀번호 찾기                      |
| :---: |:------------------------------------------------:|
| ![회원가입](docs/images/signup.png) |![비밀번호 찾기](docs/images/findpassword.png) |
- **보안 강화:** 회원가입 시 비밀번호 찾기 힌트를 설정하며, 이메일을 통한 임시 비밀번호 발급 기능을 제공합니다.



---

### 3. 게시글 관리 (Post Management)
|                     게시글 작성                     |                게시글 수정                |
|:----------------------------------------------:|:------------------------------------:|
|![게시글 작성](docs/images/post_create.png) | ![게시글 수정](docs/images/post_edit.png) |
- **다중 파일 업로드:** 여러 장의 사진을 누적 추가할 수 있으며, 업로드 전 미리보기 및 개별 삭제가 가능합니다.

---

### 4. 마이페이지 및 정보 수정
|               마이페이지               |                  정보 수정                   |
|:---------------------------------:|:----------------------------------------:|
| ![마이 페이지](docs/images/mypage.png) | ![정보 수정](docs/images/edit_myprofile.png) |
- **활동 내역 확인:** 내가 쓴 글, 댓글, 좋아요 목록을 한눈에 확인하고 개인 정보를 안전하게 수정할 수 있습니다.

</details>