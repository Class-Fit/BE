# ClassFit AWS 백엔드 배포 설계

## 1. 목적

ClassFit Spring Boot 백엔드를 AWS에 배포해 Vercel 프론트엔드와 카카오 OAuth 로그인을 실제 HTTPS 환경에서 검증한다. 단순 공개보다 취업 포트폴리오에서 VPC, EC2, RDS, IAM, ECR, SSM, Docker, DNS, TLS, CI/CD의 역할과 보안 경계를 설명할 수 있는 구성을 목표로 한다.

신규 AWS Free Plan의 초기 100달러 크레딧 안에서 시작하고, 비용 알림을 먼저 구성한다. 고가의 관리형 구성은 제외하되 애플리케이션과 데이터베이스는 분리한다.

## 2. 범위

### 포함

- 서울 리전의 EC2 기반 Spring Boot 실행 환경
- private RDS PostgreSQL
- Docker 이미지 빌드와 ECR 저장
- GitHub Actions, OIDC, SSM을 이용한 자동 배포
- 구매 도메인, 무료 DNS, Caddy를 이용한 HTTPS
- AWS Systems Manager Parameter Store를 이용한 비밀값 관리
- 운영 프로필, CORS, 세션 쿠키, health check 설정
- 비용 알림, 로그 확인, 실패 시 롤백 절차
- 카카오 운영 Redirect URI와 Vercel 연동 검증

### 제외

- NAT Gateway
- Application Load Balancer
- Auto Scaling 및 다중 EC2
- Multi-AZ RDS
- ECS 및 EKS
- AWS Secrets Manager
- 무중단 블루/그린 배포
- 첫 배포와 동시에 진행하는 Flyway 전환
- 검증 전 공공데이터 자동 동기화 활성화

## 3. 전체 구조

```text
사용자
  |
  | HTTPS
  v
Vercel 프론트엔드
  |
  | HTTPS API 요청
  v
구매 도메인(api.CLASSFIT_DOMAIN)
  |
  | DNS
  v
EC2 public subnet
  |- Caddy :80/:443
  `- Spring Boot Docker container :8080
       |
       | PostgreSQL TLS :5432
       v
RDS PostgreSQL private subnet
```

EC2만 인터넷 요청을 받는다. RDS는 public access를 끄고, EC2에 연결된 보안 그룹에서 들어오는 PostgreSQL 연결만 허용한다. Caddy가 HTTP를 HTTPS로 전환하고 Spring Boot로 reverse proxy한다.

## 4. AWS 리소스

### 4.1 네트워크

- 리전은 `ap-northeast-2`를 사용한다.
- 하나의 VPC 안에 public subnet과 private DB subnet을 둔다.
- EC2는 public subnet에서 실행하고, DNS 주소가 바뀌지 않도록 실행 중인 인스턴스에 Elastic IP 하나를 연결한다.
- RDS DB subnet group은 두 개 이상의 가용 영역에 걸친 private subnet으로 구성하되 DB 인스턴스 자체는 비용을 위해 Single-AZ로 만든다.
- NAT Gateway는 만들지 않는다.

### 4.2 EC2

- Free Tier 대상인 x86 계열 소형 인스턴스로 시작한다.
- Amazon Linux와 Docker, Docker Compose plugin, SSM Agent를 사용한다.
- Spring Boot 컨테이너와 Caddy 컨테이너만 실행한다.
- 인스턴스 재부팅 후에도 Docker Compose가 서비스를 다시 시작한다.
- 작은 메모리에서 안정적으로 실행하도록 JVM heap 상한을 명시하고 필요하면 제한된 swap을 사용한다.

### 4.3 RDS

- PostgreSQL, Single-AZ, Free Tier 대상 `db.t4g.micro`급으로 시작한다.
- public access를 비활성화한다.
- 자동 백업 보존 기간과 삭제 보호는 크레딧 및 실습 종료 정책에 맞춰 명시적으로 설정한다.
- 애플리케이션은 SSL을 사용해 접속한다.
- 최초 배포에서는 현재 스키마 생성 방식과 호환되도록 `ddl-auto: update`를 제한적으로 유지한다. 이후 Flyway 기준 스키마를 확정하는 작업은 별도 변경으로 수행한다.

### 4.4 ECR과 SSM

- ECR repository에는 Git commit SHA 태그와 배포용 태그를 함께 사용한다.
- 보존 정책으로 최근 이미지만 남겨 저장 비용을 제한한다.
- GitHub Actions는 SSM Run Command로 EC2에 새 이미지 배포를 요청한다.
- SSH 22번 포트는 열지 않는다.

## 5. DNS와 HTTPS

- 연간 비용이 낮은 도메인을 한 개 구매한다.
- AWS 크레딧은 Route 53 도메인 등록비에 사용할 수 없으므로 등록 전 가격을 직접 확인한다.
- DNS는 무료 Cloudflare DNS를 기본 선택으로 한다.
- `CLASSFIT_DOMAIN`은 구매 후 확정할 도메인 이름을 뜻한다.
- `api.CLASSFIT_DOMAIN` A 레코드를 EC2의 Elastic IP에 연결한다.
- Caddy가 ACME를 통해 인증서를 발급하고 자동 갱신한다.
- 인증서 발급과 HTTPS 확인이 끝나기 전에는 배포를 완료로 처리하지 않는다.
- 카카오 Redirect URI는 `https://api.CLASSFIT_DOMAIN/login/oauth2/code/kakao`로 등록한다.

## 6. 애플리케이션 운영 설정

`application-prod.yaml`을 추가하고 다음 정책을 적용한다.

- DB 주소, 사용자명, 비밀번호는 환경변수로 받는다.
- `server.forward-headers-strategy`로 Caddy가 전달한 원래 HTTPS 요청 정보를 반영한다.
- 세션 쿠키는 `Secure`, `HttpOnly`를 사용한다.
- 프론트 `app.<소유 도메인>`과 API `api.<소유 도메인>`을 같은 사이트에 배치하고, `SameSite=Lax` 및 CORS credentials 정책을 함께 검증한다. 기본 `*.vercel.app` 주소는 운영 로그인에 사용하지 않는다.
- CORS 허용 Origin은 실제 Vercel 운영 주소로 제한한다.
- SQL 출력과 불필요한 운영 로그를 끈다.
- Spring Boot Actuator의 health endpoint만 공개한다.
- `PUBLIC_DATA_SYNC_ENABLED=false`를 유지한다.
- OpenAI, 카카오, 공공데이터 키는 기본값을 두지 않고 외부 비밀값으로 주입한다.

필요한 운영 환경변수는 다음 범주로 관리한다.

- 데이터베이스: JDBC URL, 사용자명, 비밀번호
- OAuth: Kakao client ID와 client secret
- AI: OpenAI API key
- 공공데이터: API URL과 service key
- 웹 보안: 허용할 프론트 Origin
- 스케줄: 동기화 활성화 여부, cron, zone

## 7. 비밀값과 권한

- 비밀값은 채팅, Git, Dockerfile, Docker image layer, GitHub Actions 로그에 넣지 않는다.
- SSM Parameter Store의 `SecureString`에 운영 비밀값을 저장한다.
- EC2 instance profile에는 해당 프로젝트 경로의 Parameter 조회와 ECR pull 권한만 부여한다.
- GitHub Actions는 저장된 장기 AWS Access Key 대신 GitHub OIDC로 짧은 수명의 AWS 자격 증명을 받는다.
- GitHub 배포 역할은 지정 ECR repository push와 지정 EC2 대상 SSM command 실행에 필요한 최소 권한만 가진다.
- RDS 보안 그룹은 EC2 보안 그룹을 source로 지정하며 인터넷 CIDR을 허용하지 않는다.

## 8. 빌드와 배포 흐름

```text
main push
  -> GitHub Actions build/test
  -> Docker image build
  -> Git commit SHA로 ECR push
  -> SSM Run Command 호출
  -> EC2가 새 이미지 pull
  -> 기존 컨테이너 중지
  -> 새 컨테이너 시작
  -> 내부 health check 성공 시 배포 확정
  -> 실패 시 이전 이미지 재실행
```

- 현재 CI의 전체 테스트를 배포 전 필수 조건으로 유지한다.
- Docker image는 Java 21 multi-stage build로 만든다.
- 단일 EC2 배포이므로 컨테이너 교체 중 짧은 중단을 허용한다.
- 기존 컨테이너는 중지하되 롤백이 끝날 때까지 기존 이미지 태그를 삭제하지 않는다.
- 새 컨테이너가 정해진 시간 안에 healthy 상태가 되지 않으면 실패로 처리한다.
- 이전 정상 commit SHA를 보존해 수동 또는 자동 롤백할 수 있게 한다.

## 9. 상태 확인과 장애 처리

### 상태 확인

- `GET /actuator/health`가 인증 없이 최소 상태만 반환한다.
- 외부 HTTPS health check와 컨테이너 내부 health check를 구분한다.
- 민감한 component 세부 정보는 공개 응답에서 숨긴다.

### 장애 처리

- 애플리케이션 시작 실패: 실패 컨테이너를 제거하고 이전 이미지로 복구한다.
- RDS 연결 실패: 자격 증명을 로그에 출력하지 않고 배포 실패로 처리한다.
- health check 실패: 새 버전을 정상 배포로 표시하지 않는다.
- 인증서 발급 실패: DNS와 80/443 보안 그룹을 확인하고 HTTP 상태로 완료 처리하지 않는다.
- GitHub Actions 실패: 현재 EC2 컨테이너를 변경하지 않는다.
- 비용 임계치 도달: 알림을 확인하고 RDS 중지 또는 저비용 DB 전환을 결정한다.

## 10. 비용 보호

AWS 리소스 생성 전에 다음을 구성한다.

- Free Tier 사용 알림
- 월 실제 비용과 예상 비용을 확인할 AWS Budget
- 10달러, 30달러, 70달러 알림
- `Project=ClassFit`, `Environment=prod` 비용 태그

NAT Gateway, ALB, 추가 또는 유휴 Elastic IP, 불필요한 스냅샷은 만들지 않는다. EC2용 Elastic IP 하나도 인스턴스에서 분리한 채 방치하지 않는다. ECR lifecycle policy와 CloudWatch 로그 보존 기간을 설정한다. 신규 Free Plan은 6개월 또는 크레딧 소진 시 종료되므로 종료 전에 유지, 이전, 삭제 중 하나를 결정한다.

## 11. 검증 기준

다음을 모두 확인해야 배포 완료로 판단한다.

1. 전체 Gradle 테스트와 Docker build가 성공한다.
2. EC2 재부팅 후 Caddy와 Spring Boot가 자동 실행된다.
3. `https://api.CLASSFIT_DOMAIN/actuator/health`가 `UP`을 반환한다.
4. RDS public access가 꺼져 있고 외부에서는 5432에 접속할 수 없다.
5. EC2의 애플리케이션만 RDS에 접속할 수 있다.
6. Vercel 운영 Origin 요청에는 필요한 CORS 헤더가 있고 다른 Origin은 허용되지 않는다.
7. 카카오 로그인 콜백 후 세션을 사용한 인증 API가 성공한다.
8. 로그와 Docker image에 비밀값이 포함되지 않는다.
9. 실패 이미지를 배포했을 때 기존 정상 버전으로 복구된다.
10. 공공데이터 자동 동기화 Bean은 운영에서 기본 비활성 상태다.

## 12. 사용자 작업과 안전 경계

사용자가 직접 확인하거나 입력해야 하는 항목은 다음과 같다.

- 도메인 이름 선택과 결제 승인
- AWS 결제 및 Free Tier 상태 확인
- Parameter Store에 실제 비밀값 입력
- 카카오 개발자 콘솔의 운영 Redirect URI 등록
- Vercel 운영 URL 제공

에이전트는 비용이 발생하는 도메인 구매와 AWS 리소스 생성 전에 정확한 대상과 예상 비용을 보여준다. 비밀값을 채팅으로 요청하지 않는다.

## 13. 후속 개선

첫 배포 검증 이후 별도 작업으로 다음을 검토한다.

- Flyway 기반 스키마 변경 관리와 `ddl-auto: validate` 전환
- 운영 관측성과 알림 강화
- 공공데이터 동기화 중복 실행 방지 및 실행 이력 개선
- 세션을 여러 인스턴스에서 공유해야 할 때 Redis 또는 외부 세션 저장소 도입
- 크레딧 종료 전 운영 비용 재평가
