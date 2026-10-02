# ClassFit AWS Backend Deployment Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** ClassFit Spring Boot 백엔드를 HTTPS 도메인, private RDS PostgreSQL, ECR 이미지, GitHub OIDC와 SSM 자동 배포를 사용하는 AWS EC2 운영 환경에 배포한다.

**Architecture:** 서울 리전 public subnet의 단일 EC2에서 Caddy와 Spring Boot Docker 컨테이너를 실행하고, private subnet의 Single-AZ RDS PostgreSQL에 TLS로 연결한다. GitHub Actions는 OIDC 임시 자격 증명으로 이미지를 ECR에 올린 뒤 SSM Run Command로 짧은 중단 배포와 실패 롤백을 수행한다.

**Tech Stack:** Java 21, Spring Boot 4.1.1, Spring Security, Spring Boot Actuator, Gradle, Docker Compose, Caddy, PostgreSQL 17, AWS EC2/RDS/ECR/IAM/SSM/CloudFormation/Budgets, GitHub Actions

**Spec:** `docs/superpowers/specs/2026-10-02-aws-backend-deployment-design.md`

## Global Constraints

- AWS 리전은 `ap-northeast-2`다.
- NAT Gateway, ALB, Auto Scaling, Multi-AZ RDS, ECS, EKS, Secrets Manager를 만들지 않는다.
- EC2는 Free Tier 대상 x86 소형 인스턴스, RDS는 PostgreSQL Single-AZ `db.t4g.micro`급으로 시작한다.
- 인터넷에는 EC2의 80/443만 공개하며 SSH 22는 열지 않는다.
- RDS public access는 `false`이고 5432 ingress source는 EC2 security group뿐이다.
- 운영 비밀값은 SSM Parameter Store `SecureString`으로 관리하며 Git, 이미지, Actions 로그에 기록하지 않는다.
- GitHub는 장기 AWS access key 대신 OIDC 임시 자격 증명을 사용한다.
- 배포 이미지 태그는 Git commit SHA이며 실패 시 직전 정상 SHA로 되돌린다.
- 운영 공공데이터 스케줄러는 `PUBLIC_DATA_SYNC_ENABLED=false`가 기본이다.
- 첫 배포에서는 `ddl-auto: update`를 유지하고 Flyway 전환은 후속 작업으로 분리한다.
- 실제 도메인을 `CLASSFIT_DOMAIN`, Vercel 운영 Origin을 `CLASSFIT_FRONTEND_ORIGIN`으로 표기하며 구매·생성 전에 실제 값을 확정한다.
- 비용 발생 작업 전에 대상과 가격을 사용자에게 보여주고 명시적 승인을 받는다.

## Review Focus

- 필수 운영 비밀값이 빠졌을 때 애플리케이션이 불완전한 기본값으로 실행되지 않고 시작에 실패해야 한다. Task 1에서 prod context 실패 테스트로 고정한다.
- 신뢰하지 않는 Origin에는 CORS 허용 헤더가 없어야 하고, 지정 Vercel Origin에는 credentials가 허용돼야 한다. Task 2의 MVC 테스트로 고정한다.
- 교차 사이트 OAuth 세션 쿠키는 `Secure`, `HttpOnly`, `SameSite=None`이어야 한다. Task 2의 설정 테스트로 고정한다.
- 새 컨테이너가 unhealthy이면 직전 정상 SHA를 재실행하고 새 SHA를 정상 상태로 기록하지 않아야 한다. Task 3의 배포 스크립트 테스트로 고정한다.
- health endpoint는 익명 요청에 `UP`만 제공하고 DB 주소나 component 상세를 공개하지 않아야 한다. Task 1의 MVC 테스트로 고정한다.

---

### Task 1: 운영 프로필과 최소 Health Endpoint

**Files:**
- Modify: `build.gradle`
- Create: `src/main/resources/application-prod.yaml`
- Modify: `src/main/java/com/example/classfit/security/SecurityConfig.java`
- Create: `src/test/java/com/example/classfit/config/ProductionConfigurationTest.java`
- Create: `src/test/java/com/example/classfit/security/HealthEndpointSecurityTest.java`

**Interfaces:**
- Consumes: 기존 `application.yaml`의 `DB_*`, `KAKAO_*`, `OPENAI_API_KEY`, `PUBLIC_DATA_*` 환경변수 계약
- Produces: `prod` profile, anonymous `GET /actuator/health`, Docker가 사용할 `/actuator/health` readiness contract

- [ ] **Step 1: 필수 운영 설정 실패 테스트 작성**

`ProductionConfigurationTest`에 `application-prod.yaml`을 `YamlPropertySourceLoader`로 읽어 다음을 단언한다: datasource URL은 `${DB_URL}`을 요구하고, `ddl-auto=update`, `show-sql=false`, `public-data.sync.enabled=false`, `server.forward-headers-strategy=framework`, health detail은 `never`다. 별도 `ApplicationContextRunner` 케이스는 필수 DB/Kakao/OpenAI 값이 없을 때 context startup failure를 단언한다.

- [ ] **Step 2: 실패 확인**

Run: `OPENAI_API_KEY=test-key ./gradlew test --tests '*ProductionConfigurationTest' --no-daemon`

Expected: FAIL because `application-prod.yaml` does not exist and required production properties are not defined.

- [ ] **Step 3: 운영 프로필과 Actuator 최소 구현**

`build.gradle`에 `spring-boot-starter-actuator`를 추가한다. `application-prod.yaml`은 `DB_URL`, `DB_USER`, `DB_PASSWORD`, `CLASSFIT_FRONTEND_ORIGIN`을 환경변수로 받고 SQL 출력 비활성화, forwarded headers, secure session cookie, `SameSite=None`, health detail 비공개, scheduler 비활성화를 정의한다. `SecurityConfig`는 GET `/actuator/health`만 `permitAll`한다.

- [ ] **Step 4: Health 정보 비노출 MVC 테스트 작성**

`HealthEndpointSecurityTest`는 익명 GET `/actuator/health`가 200이고 `$.status=UP`이며 `$.components`, JDBC URL, username이 응답에 없음을 단언한다. `/actuator/env`는 익명 요청에 403임을 단언한다.

- [ ] **Step 5: 관련 테스트 통과 확인**

Run: `OPENAI_API_KEY=test-key ./gradlew test --tests '*ProductionConfigurationTest' --tests '*HealthEndpointSecurityTest' --no-daemon`

Expected: PASS, two test classes green.

- [ ] **Step 6: 커밋**

```bash
git add build.gradle src/main/resources/application-prod.yaml src/main/java/com/example/classfit/security/SecurityConfig.java src/test/java/com/example/classfit/config/ProductionConfigurationTest.java src/test/java/com/example/classfit/security/HealthEndpointSecurityTest.java
git commit -m "feat: 운영 프로필과 헬스 체크 추가"
```

### Task 2: Vercel CORS와 운영 세션 쿠키

**Files:**
- Create: `src/main/java/com/example/classfit/security/WebSecurityProperties.java`
- Modify: `src/main/java/com/example/classfit/security/SecurityConfig.java`
- Create: `src/test/java/com/example/classfit/security/CorsConfigurationTest.java`
- Modify: `src/test/java/com/example/classfit/security/SecurityConfigurationTest.java`

**Interfaces:**
- Consumes: Task 1의 `CLASSFIT_FRONTEND_ORIGIN`, 기존 session OAuth2 login
- Produces: `WebSecurityProperties(String frontendOrigin)`, credentialed CORS policy, production cookie property contract

- [ ] **Step 1: CORS 실패 테스트 작성**

`CorsConfigurationTest`는 `classfit.web.frontend-origin=https://classfit.vercel.app`로 context를 시작한다. OPTIONS `/api/members/me` 요청에서 해당 Origin은 200, `Access-Control-Allow-Origin` 정확한 일치, `Access-Control-Allow-Credentials=true`를 단언하고 `https://evil.example`에는 allow-origin 헤더가 없고 403임을 단언한다.

- [ ] **Step 2: 실패 확인**

Run: `OPENAI_API_KEY=test-key ./gradlew test --tests '*CorsConfigurationTest' --no-daemon`

Expected: FAIL because no `CorsConfigurationSource` is registered.

- [ ] **Step 3: 타입이 있는 CORS 설정 구현**

`WebSecurityProperties`를 `@ConfigurationProperties(prefix = "classfit.web")` record로 만들고 `@EnableConfigurationProperties`로 등록한다. `SecurityConfig`의 `CorsConfigurationSource` bean은 단 하나의 정확한 frontend Origin, 필요한 GET/POST/DELETE/OPTIONS method, `Content-Type`와 CSRF header, credentials를 허용하며 wildcard Origin을 사용하지 않는다.

- [ ] **Step 4: 쿠키 정책 테스트 보강**

`SecurityConfigurationTest`에서 prod YAML을 읽어 `server.servlet.session.cookie.secure=true`, `http-only=true`, `same-site=none`을 단언한다. 누락되거나 빈 `CLASSFIT_FRONTEND_ORIGIN`으로 prod context가 뜨지 않는 테스트를 `ProductionConfigurationTest`에 추가한다.

- [ ] **Step 5: 관련 보안 테스트 통과 확인**

Run: `OPENAI_API_KEY=test-key ./gradlew test --tests '*CorsConfigurationTest' --tests '*SecurityConfigurationTest' --tests '*MemberSecurityTest' --no-daemon`

Expected: PASS; trusted Origin only, existing authorization behavior unchanged.

- [ ] **Step 6: 커밋**

```bash
git add src/main/java/com/example/classfit/security/WebSecurityProperties.java src/main/java/com/example/classfit/security/SecurityConfig.java src/test/java/com/example/classfit/security/CorsConfigurationTest.java src/test/java/com/example/classfit/security/SecurityConfigurationTest.java src/test/java/com/example/classfit/config/ProductionConfigurationTest.java
git commit -m "feat: 운영 CORS와 세션 쿠키 정책 구성"
```

### Task 3: 재현 가능한 컨테이너와 롤백 배포 스크립트

**Files:**
- Create: `Dockerfile`
- Create: `.dockerignore`
- Create: `deploy/compose.yaml`
- Create: `deploy/Caddyfile`
- Create: `deploy/scripts/deploy.sh`
- Create: `deploy/scripts/render-env.sh`
- Create: `src/test/java/com/example/classfit/deployment/DeploymentArtifactTest.java`

**Interfaces:**
- Consumes: Task 1의 `/actuator/health`, `prod` profile; SSM parameters under `/classfit/prod/`; `AWS_ACCOUNT_ID`, `AWS_REGION`, `ECR_REPOSITORY`, `IMAGE_TAG`, `CLASSFIT_DOMAIN`
- Produces: `linux/amd64` application image, `docker compose` runtime, `deploy.sh <new-sha> <previous-sha>` rollback contract

- [ ] **Step 1: 배포 산출물 계약 테스트 작성**

`DeploymentArtifactTest`는 Dockerfile이 Java 21 multi-stage, non-root runtime user, healthcheck를 포함하는지 확인한다. Compose는 `SPRING_PROFILES_ACTIVE=prod`, `restart: unless-stopped`, app의 8080을 host에 직접 공개하지 않는지 확인한다. Caddyfile은 `api.{$CLASSFIT_DOMAIN}`과 app reverse proxy만 포함해야 한다.

- [ ] **Step 2: 실패 확인**

Run: `OPENAI_API_KEY=test-key ./gradlew test --tests '*DeploymentArtifactTest' --no-daemon`

Expected: FAIL because deployment artifacts do not exist.

- [ ] **Step 3: Docker와 Caddy 산출물 구현**

Dockerfile은 Gradle dependency/build stage와 JRE 21 runtime stage를 분리하고 `bootJar` 하나만 복사한다. Compose는 Caddy의 80/443, 인증서 volume, 내부 app network만 정의한다. `.dockerignore`는 `.git`, `.env`, build output, IDE file, secret file을 제외한다.

- [ ] **Step 4: 환경파일 생성 스크립트 구현**

`render-env.sh`는 AWS CLI `ssm get-parameters-by-path --with-decryption` 결과를 `/opt/classfit/runtime/app.env`에 mode 600으로 원자적으로 기록한다. 값은 stdout에 출력하지 않고 누락된 필수 parameter 이름만 보고한 뒤 non-zero로 끝낸다.

- [ ] **Step 5: 실패 롤백 테스트 작성**

`DeploymentArtifactTest`에 임시 PATH의 fake `docker`/`aws` executable을 사용한 process test를 추가한다. 새 image health 실패 시 `deploy.sh` exit가 non-zero이고 previous SHA로 compose가 다시 실행되며 `current-image` 파일은 이전 SHA를 유지하는지 단언한다.

- [ ] **Step 6: 배포 스크립트 구현**

`deploy.sh <new-sha>`는 parameter 검증, ECR login, image pull, 현재 SHA 보존, 기존 app 중지, 새 app 실행, 제한 시간 health polling을 순서대로 수행한다. 실패하면 새 app을 중지하고 이전 SHA를 재실행하며 secret value를 출력하지 않는다.

- [ ] **Step 7: 산출물 및 shell 검증**

Run: `bash -n deploy/scripts/deploy.sh deploy/scripts/render-env.sh`

Run: `OPENAI_API_KEY=test-key ./gradlew test --tests '*DeploymentArtifactTest' --no-daemon`

Expected: shell syntax valid and all artifact/rollback tests PASS.

- [ ] **Step 8: 커밋**

```bash
git add Dockerfile .dockerignore deploy src/test/java/com/example/classfit/deployment/DeploymentArtifactTest.java
git commit -m "feat: AWS 실행용 컨테이너 배포 구성 추가"
```

### Task 4: 비용 제한형 AWS 인프라 코드

**Files:**
- Create: `infra/cloudformation/classfit-infrastructure.yaml`
- Create: `infra/cloudformation/classfit-github-oidc.yaml`
- Create: `infra/parameters/prod.example.json`
- Create: `infra/README.md`
- Create: `scripts/verify-infrastructure.sh`

**Interfaces:**
- Consumes: `GitHubOrg=Class-Fit`, `GitHubRepo=BE`, 배포 branch `main`, 실제 `AlertEmail`, `ClassfitDomain`
- Produces: VPC/subnets/routes, EC2/EIP, private RDS, security groups, ECR, IAM instance role, SSM parameters' names, GitHub OIDC deployment role, stack outputs

- [ ] **Step 1: 정적 인프라 검증 스크립트 작성**

`verify-infrastructure.sh`는 template에서 NAT Gateway/ALB/22 ingress가 없고, RDS `PubliclyAccessible: false`, DB security group source가 EC2 security group, ECR lifecycle policy, EC2 SSM managed policy, budget thresholds 10/30/70이 존재하는지 검사한다. parameter example에 실제 비밀값이 없음을 검사한다.

- [ ] **Step 2: 실패 확인**

Run: `bash scripts/verify-infrastructure.sh`

Expected: FAIL because CloudFormation templates do not exist.

- [ ] **Step 3: 기반 인프라 template 작성**

`classfit-infrastructure.yaml`은 VPC, public subnet 1개, 서로 다른 AZ의 private DB subnet 2개, internet gateway/route, EC2/RDS security groups, ECR lifecycle, EC2 role/profile, Elastic IP, Single-AZ PostgreSQL RDS, 비용 태그와 Budget을 정의한다. DB password는 NoEcho parameter로 template에 저장하지 않고 초기 생성 후 Parameter Store 경로와 일치시킨다.

- [ ] **Step 4: GitHub OIDC template 작성**

`classfit-github-oidc.yaml`은 GitHub OIDC provider와 `repo:Class-Fit/BE:ref:refs/heads/main` subject만 신뢰하는 role을 만든다. 권한은 지정 ECR push와 지정 EC2 instance에 대한 SSM command 실행 및 결과 조회로 제한한다.

- [ ] **Step 5: 인프라 문서와 parameter example 작성**

`infra/README.md`에 생성 순서, CloudFormation parameter 의미, 예상 과금 항목, 삭제 순서, RDS snapshot 선택을 기록한다. `prod.example.json`에는 비밀값 대신 `REPLACE_IN_CONSOLE` 문자열만 둔다.

- [ ] **Step 6: template 검증**

Run: `bash scripts/verify-infrastructure.sh`

Run after AWS CLI authentication: `aws cloudformation validate-template --region ap-northeast-2 --template-body file://infra/cloudformation/classfit-infrastructure.yaml`

Run after AWS CLI authentication: `aws cloudformation validate-template --region ap-northeast-2 --template-body file://infra/cloudformation/classfit-github-oidc.yaml`

Expected: static checks pass and both commands return template descriptions without validation error.

- [ ] **Step 7: 커밋**

```bash
git add infra scripts/verify-infrastructure.sh
git commit -m "infra: ClassFit AWS 기반 리소스 정의"
```

### Task 5: CI 통과 후 ECR·SSM 자동 배포

**Files:**
- Create: `.github/workflows/deploy.yml`
- Modify: `.github/workflows/ci.yml`
- Create: `scripts/verify-deploy-workflow.sh`
- Modify: `README.md`

**Interfaces:**
- Consumes: Task 3의 Dockerfile/`deploy.sh`, Task 4 stack outputs `AWS_DEPLOY_ROLE_ARN`, `ECR_REPOSITORY`, `EC2_INSTANCE_ID`; GitHub Environment `production`
- Produces: main CI 성공 후 SHA-tagged ECR image와 SSM deployment command

- [ ] **Step 1: workflow 정책 실패 검증 작성**

`verify-deploy-workflow.sh`는 deploy workflow에 `permissions: id-token: write`, `contents: read`, production environment, reusable CI job에 대한 `needs: verify`, immutable `${{ github.sha }}` tag, ECR login, SSM send-command와 command status polling이 있는지 확인한다. 장기 access key 이름과 secret echo가 있으면 실패한다.

- [ ] **Step 2: 실패 확인**

Run: `bash scripts/verify-deploy-workflow.sh`

Expected: FAIL because `.github/workflows/deploy.yml` does not exist.

- [ ] **Step 3: CI와 배포 workflow 연결**

기존 `ci.yml`에 `workflow_call` trigger를 추가하고, 직접 실행 trigger는 PR과 `develop` push를 유지하되 `main` push는 `deploy.yml`이 호출하는 reusable verify job 한 번으로 처리한다. `deploy.yml`은 main push에서 `verify` job으로 `./.github/workflows/ci.yml`을 호출하고, `deploy` job에 `needs: verify`를 둔다. 배포 job은 OIDC role assumption, `linux/amd64` image build/push, SSM Run Command 실행, 제한 시간 status polling을 수행한다. PR과 `develop` push는 배포하지 않는다.

- [ ] **Step 4: 운영 문서 갱신**

README에 운영 URL 변수, 배포 흐름, GitHub Environment variable 이름, rollback 방법, scheduler 기본 비활성, secret 금지 규칙을 추가한다. 실제 ARN, 계정 ID, 도메인, secret은 커밋하지 않는다.

- [ ] **Step 5: workflow와 전체 빌드 검증**

Run: `bash scripts/verify-deploy-workflow.sh`

Run: `OPENAI_API_KEY=test-key ./gradlew clean build --no-daemon`

Expected: workflow policy checks pass and full Gradle build passes with the reported test count recorded in the handoff.

- [ ] **Step 6: 커밋**

```bash
git add .github/workflows/ci.yml .github/workflows/deploy.yml scripts/verify-deploy-workflow.sh README.md
git commit -m "ci: AWS 운영 자동 배포 워크플로 추가"
```

### Task 6: AWS 생성, 도메인 연결, 실제 운영 검증

**Files:**
- Modify: `infra/README.md` only if live execution reveals a reproducible operational correction
- Modify: `README.md` only for the final public API URL and verified limitations

**Interfaces:**
- Consumes: Tasks 1-5 artifacts, AWS Free Plan account, purchased `CLASSFIT_DOMAIN`, Cloudflare account, Kakao developer app, Vercel production URL, actual secret values entered by user
- Produces: reachable `https://api.CLASSFIT_DOMAIN`, verified Kakao session flow, recorded AWS cost and rollback evidence

- [ ] **Step 1: 비용 보호를 먼저 생성**

AWS Billing에서 Free Tier alert를 활성화하고 10/30/70달러 Budget 이메일을 확인한다. 사용자에게 생성 대상과 현재 예상 비용을 보여준 뒤 승인받기 전에는 EC2/RDS/domain을 생성하지 않는다.

- [ ] **Step 2: 도메인 구매와 DNS 위임**

사용자가 선택한 등록기관에서 실제 가격과 갱신 가격을 확인하고 명시적 결제 승인 후 도메인을 구매한다. Cloudflare zone을 만들고 registrar nameserver를 변경한다. 도메인과 카드 정보는 저장소나 로그에 기록하지 않는다.

- [ ] **Step 3: CloudFormation stack 생성**

AWS 콘솔 또는 CLI에서 infrastructure stack을 먼저 생성하고 outputs를 확인한다. OIDC stack에는 정확한 GitHub org/repo/main subject를 전달한다. 생성 직후 RDS public access, security group ingress, EC2 SSM managed 상태, EIP association을 읽기 전용 명령으로 재검증한다.

- [ ] **Step 4: Parameter Store와 GitHub Environment 설정**

사용자가 실제 `DB_URL`, `DB_USER`, `DB_PASSWORD`, Kakao/OpenAI/public-data values, `CLASSFIT_FRONTEND_ORIGIN`, `CLASSFIT_DOMAIN`을 Parameter Store에 직접 입력한다. GitHub `production` environment에는 비밀값이 아닌 role ARN, region, ECR repository, instance ID만 등록한다.

- [ ] **Step 5: DNS와 카카오 운영 URI 연결**

`api.CLASSFIT_DOMAIN` A record를 EIP에 연결하고 DNS 전파를 확인한다. Kakao developer console에 정확한 HTTPS redirect URI와 허용 Origin을 등록한다. Caddy certificate issuance가 성공할 때까지 OAuth 검증을 시작하지 않는다.

- [ ] **Step 6: 최초 배포와 외부 smoke test**

main 배포 workflow를 명시적으로 실행한다. `curl --fail https://api.CLASSFIT_DOMAIN/actuator/health`가 `UP`인지, `/actuator/env`가 403인지, HTTP가 HTTPS로 redirect되는지 확인한다. 외부 호스트에서 RDS 5432가 닫혀 있는지 확인한다.

- [ ] **Step 7: 브라우저 계약 검증**

Vercel 운영 Origin에서 credentialed API 요청의 CORS header와 cookie를 확인한다. Kakao 로그인 시작 → callback → `/api/members/me` 요청에서 동일 `JSESSIONID` 세션이 유지되는지 확인한다. 브라우저 개발자 도구에서 cookie가 Secure/HttpOnly/SameSite=None인지 확인한다.

- [ ] **Step 8: 롤백과 재부팅 검증**

의도적으로 존재하지 않는 image tag 또는 health 실패 image로 배포 probe를 실행해 이전 SHA가 복구되는지 확인한다. EC2를 재부팅하고 Caddy/app 자동 시작과 HTTPS health를 다시 확인한다. 테스트용 실패 image와 command 기록은 정리한다.

- [ ] **Step 9: 최종 비용과 상태 기록**

Cost Explorer/Billing에서 현재 누적 비용, forecast, 남은 credit을 확인한다. 실제 공개 URL과 무료 크레딧 종료 전 점검일을 README/운영 문서에 기록하되 account ID, instance ID, DB endpoint, secret은 공개하지 않는다.

- [ ] **Step 10: 운영 확인 커밋**

라이브 검증에서 문서 수정이 생긴 경우에만 커밋한다.

```bash
git add README.md infra/README.md
git commit -m "docs: AWS 운영 배포 검증 결과 기록"
```

## Final Verification

- [ ] Run: `bash -n deploy/scripts/deploy.sh deploy/scripts/render-env.sh`
- [ ] Run: `bash scripts/verify-infrastructure.sh`
- [ ] Run: `bash scripts/verify-deploy-workflow.sh`
- [ ] Run: `OPENAI_API_KEY=test-key ./gradlew clean build --no-daemon`
- [ ] Run: `docker build --platform linux/amd64 -t classfit:verify .`
- [ ] Verify: `git status --short` contains no accidental secret or uncommitted generated file.
- [ ] Verify: HTTPS health, OAuth session, trusted/untrusted CORS, private RDS, EC2 reboot, failed-deploy rollback, and AWS budget alerts with fresh evidence.
