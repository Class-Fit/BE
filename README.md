# ClassFit 백엔드

공공체육 강좌 탐색 서비스를 위한 Spring Boot 백엔드입니다. 현재 카카오 로그인은 프론트엔드 없이 로컬 브라우저에서 확인할 수 있습니다.

## 실행 환경

- Java 21
- 저장소의 Gradle Wrapper (`./gradlew`)
- 로컬 기본 주소: `http://localhost:8080`

## 카카오 로그인 준비

1. 카카오 디벨로퍼스에서 애플리케이션의 카카오 로그인을 활성화합니다.
2. Redirect URI에 `http://localhost:8080/login/oauth2/code/kakao`를 정확히 등록합니다.
3. REST API 키와 클라이언트 시크릿을 발급합니다. 현재 설정은 시크릿을 사용하는 `client_secret_post` 방식입니다.
4. 실행 프로세스에 `KAKAO_CLIENT_ID`, `KAKAO_CLIENT_SECRET` 환경변수를 설정합니다. IntelliJ에서는 실행 구성의 환경변수에 입력할 수 있습니다.

키는 채팅·소스·커밋·로그에 넣지 않습니다. Spring Boot는 프로젝트 루트의 `.env`를 자동으로 읽지 않습니다. 현재 설정은 추가 scope를 요청하지 않습니다. 선택 프로필을 받고 싶다면 해당 앱에서 사용할 수 있는 동의항목을 확인해 별도로 설정해야 합니다. OpenID Connect를 사용하지 않는 OAuth2 로그인 기준입니다.

환경변수를 설정한 터미널에서 실행합니다.

```bash
./gradlew bootRun
```

브라우저에서 다음 주소를 엽니다.

```text
http://localhost:8080/oauth2/authorization/kakao
```

카카오 인증 후 `/login/oauth2/code/kakao` 콜백을 처리하고 `/api/members/me`로 이동합니다. 해당 페이지의 JSON에서 회원 ID를 확인할 수 있습니다. 같은 서버 실행 중 다시 로그인하면 같은 회원 ID를 사용합니다.

현재 기본 DB는 PostgreSQL이며 `DB_NAME`, `DB_USER`, `DB_PASSWORD` 환경변수로 접속 정보를 설정합니다. 개발·테스트용 가짜 OAuth 값으로는 실제 카카오에 로그인할 수 없습니다.

## 공공데이터 동기화 설정

시설·강좌 동기화에는 다음 환경변수가 필요합니다.

| 환경변수 | 역할 |
|---|---|
| `PUBLIC_DATA_FACILITY_URL` | 등록시설 API의 전체 요청 경로 |
| `PUBLIC_DATA_FACILITY_SERVICE_KEY` | 등록시설 API 인증키 |
| `PUBLIC_DATA_COURSE_URL` | 등록강좌 API의 전체 요청 경로 |
| `PUBLIC_DATA_COURSE_SERVICE_KEY` | 등록강좌 API 인증키 |
| `PUBLIC_DATA_SYNC_ENABLED` | `true`일 때만 정기 동기화 활성화 |
| `PUBLIC_DATA_SYNC_CRON` | 정기 동기화 cron, 기본값 `0 0 3 * * *` |
| `PUBLIC_DATA_SYNC_ZONE` | 정기 동기화 시간대, 기본값 `Asia/Seoul` |

정기 동기화는 기본적으로 비활성화되어 있습니다. API 주소·인증키와 전체 수동 동기화를 검증한 환경에서만 `PUBLIC_DATA_SYNC_ENABLED=true`를 설정합니다. 이 값이 `false`이거나 없더라도 관리자의 `POST /api/admin/courses/sync` 수동 동기화는 사용할 수 있습니다.

Spring Boot는 `.env` 파일을 자동으로 읽지 않습니다. 터미널에서 `.env`를 사용할 때는 값을 현재 프로세스의 환경변수로 내보낸 뒤 애플리케이션을 실행해야 합니다. API 인증키와 인증키가 포함된 전체 요청 URL은 커밋하거나 로그에 남기지 않습니다.

## 요청과 응답

| 요청 | 동작 |
|---|---|
| `GET /oauth2/authorization/kakao` | 카카오 로그인 화면으로 이동 |
| `GET /login/oauth2/code/kakao` | Security가 인가 코드·state를 검증하고 인증 처리 |
| `GET /api/members/me` | 로그인한 본인의 최신 DB 정보를 반환 |
| `POST /api/auth/logout` | 유효한 CSRF 토큰으로 세션 종료, 204 응답 |

미인증 API 요청은 401 `UNAUTHORIZED`, 권한 부족·CSRF 오류는 403 `FORBIDDEN`, 인증 사용자에 해당하는 회원이 없으면 404 `MEMBER_NOT_FOUND`입니다. 카카오 인증·회원 연결 실패는 내부 상세를 숨긴 401 `OAUTH_LOGIN_FAILED`를 반환합니다.

현재 공개 경로는 OAuth 콜백·시작 경로, 오류 처리 경로, Swagger 문서 GET 경로입니다. 나머지는 명시적으로 허용한 내 정보 GET 외에 기본 거부합니다. 향후 강좌 조회 API를 추가할 때 공개 조회 경로도 함께 등록해야 합니다.

로그아웃은 카카오 계정 로그아웃이나 연결 해제가 아닌 ClassFit 세션 종료입니다. GET 요청으로 로그아웃하지 않으며, CSRF 없는 POST는 403입니다. 프론트가 없는 현재 단계에서는 유효·누락·잘못된 CSRF와 세션 만료를 자동 테스트로 검증합니다. 브라우저에서 로그아웃을 호출할 CSRF 토큰 전달 UI/API는 프론트 연동 시 추가합니다.

## 코드의 역할

| 클래스 | 주요 역할 |
|---|---|
| `SecurityConfig` | 경로별 접근 정책, 세션 로그인, 성공·실패 응답, 로그아웃 설정 |
| `CustomOAuth2UserService` | 카카오 사용자 정보 요청을 위임하고 내부 회원과 연결 |
| `OAuthProviderUserInfo` | 제공자별 사용자 응답을 회원 연결에 필요한 공통 형식으로 정의 |
| `OAuthProviderUserInfoFactory` | OAuth 등록 이름에 맞는 제공자 응답 변환기를 선택 |
| `KakaoOAuth2UserInfo` | 카카오 응답의 ID·선택 프로필을 정규화 |
| `LoginMember` | 세션 principal에 내부 회원 ID와 DB 역할을 보관 |
| `MemberService` | `findOrCreate`로 신규·기존 계정 처리, `findById`로 내 정보 조회 |
| `MemberRepository` | `(provider, providerId)` 조회와 회원 영속화 |
| `Member` | 회원 필드와 신규 회원 생성 규칙 |
| `MemberController` / `MemberMeResponse` | 본인 조회 API와 외부에 반환할 정보 정의 |
| `MemberErrorCode` | 회원 도메인의 오류 코드 정의 |
| `ApiAuthenticationEntryPoint` / `ApiAccessDeniedHandler` | 필터 단계의 401·403 처리 |
| `SecurityErrorResponseWriter` | 기존 `ApiResponse` 형식으로 보안 오류 JSON 작성 |

클래스와 핵심 메서드에는 한글 설명이 있습니다. 원본 카카오 프로필과 토큰은 `LoginMember`에 복사하지 않습니다. OAuth 클라이언트가 토큰 교환 과정에서 관리하는 토큰과 서비스 세션의 로그인 사용자 정보는 별개입니다.

## 회원 정책과 현재 한계

- 회원은 이메일이 아니라 `(provider, providerId)`로 식별합니다.
- 신규 회원 역할은 `USER`입니다. 기존 회원의 프로필·역할은 재로그인 시 덮어쓰지 않습니다.
- 이름·이메일·성별·키·몸무게는 없을 수 있으며 JSON의 null을 미입력으로 해석합니다.
- DB 유니크 제약으로 중복 회원 생성을 막습니다. 동시에 최초 가입하면 한 요청은 안전한 로그인 실패를 받을 수 있습니다. 자동 재조회·재시도는 아직 구현하지 않았습니다.
- 운영 CORS는 `CLASSFIT_FRONTEND_ORIGIN`의 정확한 Origin 하나만 credentials와 함께 허용합니다. 운영 세션 쿠키는 `Secure`, `HttpOnly`, `SameSite=None`이며 현재 단일 서버의 메모리 세션을 사용합니다.

## AWS 운영 배포

운영 구조는 Vercel 프론트엔드 → `https://api.<CLASSFIT_DOMAIN>`의 Caddy → Spring Boot 컨테이너 → private RDS PostgreSQL입니다. `main` push 또는 수동 실행 시 CI 전체 검증을 먼저 통과한 뒤, commit SHA 태그의 `linux/amd64` 이미지만 ECR에 push하고 SSM Run Command로 EC2에 배포합니다. 장기 AWS access key와 SSH 포트는 사용하지 않습니다.

운영에서 카카오 로그인에 성공하면 `CLASSFIT_FRONTEND_ORIGIN`의 `/courses`로 이동합니다. 로컬 실행은 기존 `/api/members/me` 이동을 유지합니다.

GitHub `production` Environment에는 다음 비밀이 아닌 variable을 설정합니다.

| variable | 역할 |
|---|---|
| `AWS_DEPLOY_ROLE_ARN` | GitHub OIDC가 맡을 최소 권한 IAM role ARN |
| `ECR_REPOSITORY` | CloudFormation이 만든 ECR repository 이름 |
| `EC2_INSTANCE_ID` | SSM 명령을 받을 운영 EC2 instance ID |

운영 비밀값은 `/classfit/prod/` 아래 SSM Parameter Store `SecureString`으로만 보관합니다. `DB_URL`, `DB_USER`, `DB_PASSWORD`, `KAKAO_CLIENT_ID`, `KAKAO_CLIENT_SECRET`, `OPENAI_API_KEY`, `CLASSFIT_FRONTEND_ORIGIN`이 필요합니다. 실제 값, AWS account ID, DB endpoint는 저장소·Actions 로그·이미지에 넣지 않습니다. 자세한 생성 순서는 [`infra/README.md`](infra/README.md)를 따릅니다.

배포 스크립트는 새 컨테이너의 `/actuator/health`가 정상일 때만 `current-image`를 갱신합니다. 실패하면 직전 정상 SHA를 다시 실행하고 Actions job을 실패시킵니다. 긴급 수동 롤백도 EC2에 SSH로 접속하는 대신 SSM Run Command에서 `/opt/classfit/scripts/deploy.sh <정상-SHA>`를 실행합니다. 운영 공공데이터 scheduler는 기본적으로 비활성화되어 있습니다.

## 테스트

```bash
./gradlew clean build --no-daemon
```

Spring 컨텍스트 테스트는 `test` 프로필과 Testcontainers가 실행한 PostgreSQL 17을 사용합니다. 로컬 전체 테스트에는 Docker 호환 실행 환경이 필요하며 카카오 키를 CI에 등록할 필요는 없습니다.

- 응답·예외 테스트: 기존 성공/실패 DTO와 MVC 400·404·500 유지
- 회원 테스트: 신규·기존 회원, 선택 정보 누락, DB 유니크 제약
- 응답 변환 테스트: 숫자 ID·잘못된 ID·선택 정보 누락·관리자 권한·저장 실패
- 보안 테스트: 내 정보, 401·403·404, 비밀정보 비노출
- 로그아웃 테스트: 세션 무효화·쿠키 만료, CSRF 없거나 잘못된 요청 차단
- 콜백 통합 테스트: 로컬 HTTP 서버가 외부 토큰·사용자 정보 응답만 대체하며 실제 로그인 필터·회원 저장·다음 요청의 세션 인증을 확인

`oauth2Login()`으로 인증 사용자를 넣는 테스트는 실제 카카오 로그인 검증이 아닙니다. 콜백 통합 테스트도 카카오 콘솔·실제 계정 동의·브라우저 쿠키 동작을 대체하지 않으므로 실제 키가 준비되면 위 브라우저 절차로 별도 확인해야 합니다.
