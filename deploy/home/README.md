# 홈서버 이전 파일럿 — 1회 세팅 체크리스트

이 문서는 **홈서버(x86, self-hosted runner)에서 메타(A)가 직접** 실행하는 1회 세팅 절차다.
이 PR은 도커화 산출물(Dockerfile·compose·배포 헬퍼·워크플로우)만 추가하며, 아래 절차의 실제
실행(runner 설치·공유 네트워크 조인 등)은 별도로 진행한다 — 이 문서는 그 실행 가이드다.

기존 OCI 배포(`.github/workflows/deploy.yml`, systemd 방식)는 그대로 유지·무수정 — 이 파일럿은
병행 검증 단계이며 트래픽 전환은 별도 결정 사항이다.

## 0. 전제

- 홈서버는 x86_64, Docker + docker compose plugin 설치돼 있음.
- `db-mysql`(MySQL 8, `community` 스키마 복원 완료), `db-redis`(Redis 7, requirepass 설정)
  컨테이너가 이미 홈서버에서 별도 compose로 떠 있다고 가정 — 이 스택은 신규로 만들지 않고
  `shared-net`으로만 연결한다.

## 1. 공유 네트워크 생성

```
docker network create shared-net
```

- [ ] `db-mysql`, `db-redis` 컨테이너가 `shared-net`에 조인돼 있는지 확인
      (`networks: [default, shared-net]` 형태 — 이 레포의 `deploy/home/compose.yml` app 서비스와 동일 패턴)
      — 미조인 시 해당 컨테이너의 compose에 추가 후 `docker compose up -d`로 재적용

## 2. `.env` 작성

```
mkdir -p ~/srv/community
cp deploy/home/.env.example ~/srv/community/.env
# ~/srv/community/.env 를 편집기로 열어 실제 값 채우기(DB_PASSWORD, JWT_SECRET_KEY, AWS 키 등)
chmod 700 ~/srv/community
chmod 600 ~/srv/community/.env
```

- [ ] **소유·접근 모델**: `.env`는 관리자 소유 600 + 디렉터리 700 — `runner` 계정은 접근 불가하고,
      배포 시엔 sudo 헬퍼(§4)가 root 권한으로 읽어 `--env-file`로 compose에 공급한다.
      runner(=워크플로우 코드)는 시크릿 값을 볼 수 없다.
- [ ] `JWT_SECRET_KEY`는 32바이트 이상 랜덤 값(`openssl rand -base64 48`)
- [ ] 절대 `.env`를 git에 커밋하지 말 것 — `.env.example`만 예외로 커밋됨

## 3. GitHub self-hosted runner 설치 (라벨 `home`)

- [ ] 레포 Settings → Actions → Runners → New self-hosted runner, 라벨에 `home` 추가
- [ ] **전용 `runner` 계정**으로 설치 — 관리자 계정이 아닌 별도 비루트 계정이며,
      **`docker` 그룹에 넣지 않는다**. runner(=워크플로우 코드)가 docker 소켓·시크릿에 직접
      접근하지 못하게 권한을 분리하는 것이 목적.
- [ ] **배포 입력용 레포 최초 1회 root clone** — 헬퍼는 러너 워크스페이스가 아닌 root 소유
      사본에서만 compose를 실행한다(runner가 쓸 수 있는 파일을 root가 실행하는 경로 차단):

      ```
      sudo mkdir -p /opt/deploy
      sudo git clone https://github.com/luma200ok/community.git /opt/deploy/community
      ```
- [ ] **헬퍼 설치**(레포 버전관리본 `deploy/home/community-deploy.sh` — 갱신 시에도 동일 명령 재실행):

      ```
      sudo install -o root -g root -m 755 deploy/home/community-deploy.sh /usr/local/bin/community-deploy
      ```
- [ ] 배포 실행 권한은 sudo 헬퍼 **1개만, 인자까지 고정**해 허용 — sudoers(`visudo -f /etc/sudoers.d/community-deploy`):

      ```
      runner ALL=(root) NOPASSWD: /usr/local/bin/community-deploy up, /usr/local/bin/community-deploy ps, /usr/local/bin/community-deploy prune, /usr/local/bin/community-deploy diagnose
      ```

      헬퍼는 `/opt/deploy/community`에 origin/main을 fetch/checkout한 뒤 그 안의 compose를 root로
      실행하고 `.env`(§2, runner 접근 불가)를 `--env-file`로 공급한다 — 워크플로우는
      `sudo -n /usr/local/bin/community-deploy <서브커맨드>`만 호출하며 임의 docker 명령·시크릿
      열람이 불가능하다.
- [ ] 이 파일럿의 배포 워크플로우는 **`pull_request` 트리거를 절대 사용하지 않는다** — self-hosted
      runner에서 fork PR의 워크플로우가 실행되면 PR 작성자가 임의 코드를 runner(홈 네트워크 접근
      가능)에서 실행시킬 수 있어 원격 코드 실행(RCE)/내부망 피벗 위험이 있다(GitHub Actions
      공식 보안 권고사항). 현행 `.github/workflows/deploy-home.yml`은 `workflow_dispatch`만
      사용하고 main ref 가드를 건다.
- [ ] runner 서비스로 등록(`svc.sh install && svc.sh start`)해 재부팅 후에도 유지

## 4. 스모크 절차

배포는 항상 헬퍼 경유로 실행한다(§3의 배포 경로와 동일 — origin/main 기준 빌드·기동):

```
sudo /usr/local/bin/community-deploy up
```

- [ ] `sudo /usr/local/bin/community-deploy ps` — app healthy 확인
- [ ] `curl -s http://127.0.0.1:8082/api/health` → 200 (로컬에서만 접근 가능)
- [ ] 회원가입 → 로그인 → 게시글 작성까지 1회 수동 스모크(파일럿이므로 운영 데이터 아님)

## 5. 롤백 (수동, 관리자)

`prune`은 `--filter until=72h`라 **직전 배포의 dangling 이미지가 3일간 보존**된다 — 그 안에서는
재빌드 없이 이미지 되돌리기가 가능하다. (정석은 revert 커밋을 main에 머지한 뒤 워크플로우 재실행 —
아래는 응급용.)

```
sudo docker images --filter dangling=true          # 직전 app 이미지 ID 확인(CREATED 시각으로 식별)
sudo docker tag <직전 app 이미지ID> community-home-app:latest
sudo docker compose -f /opt/deploy/community/deploy/home/compose.yml --env-file ~/srv/community/.env up -d --no-build
# --no-build: 되돌린 태그 그대로 컨테이너 재생성(빌드 생략)
```

- 이미지 이름은 compose 프로젝트명(`name: community-home`) 기반 `community-home-{서비스}`로 고정된다.
- 롤백 후에도 `curl -s http://127.0.0.1:8082/api/health` 스모크(§4)를 반복해 확인한다.

## 6. 알려진 제약 / 후속

- 외부 노출(리버스 프록시/터널)은 이 PR 범위 밖 — 8082는 로컬(127.0.0.1)에만 publish된다.
- 배포 워크플로우는 `workflow_dispatch` 전용이며, push(main) 자동 트리거는 파일럿 안정화 후
  별도 PR에서 추가 예정.
- `docker compose logs`의 앱 로그는 stdout이라 systemd/journalctl 기반이 아님 — 로그 보존 정책은
  후속 검토 필요(예: `logging: driver: json-file, options: max-size`).
- OCI arm1과 홈서버 두 곳에 동시에 서비스가 뜨는 동안 DB는 서로 다른 인스턴스(arm1=native MySQL,
  홈서버=`db-mysql` 컨테이너)이므로 데이터가 동기화되지 않는다 — 트래픽 전환 전 이관 계획 필요.
