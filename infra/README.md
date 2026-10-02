# ClassFit AWS 운영 인프라

이 디렉터리는 서울 리전(`ap-northeast-2`)의 단일 EC2, private Single-AZ RDS PostgreSQL, ECR, SSM, GitHub OIDC 배포 역할을 정의한다. NAT Gateway, ALB, SSH 포트, 장기 AWS access key는 사용하지 않는다.

## 생성 전 확인

- AWS 결제 콘솔에서 100 USD 크레딧의 만료일과 적용 서비스를 확인한다.
- AWS Pricing Calculator에서 `t3.micro`, `db.t4g.micro`, 20 GiB RDS·EBS, public IPv4 비용을 현재 가격으로 다시 계산한다.
- `production` GitHub Environment의 deployment branch를 `main`으로 제한한다. Environment를 사용하는 OIDC 토큰은 브랜치 대신 `repo:Class-Fit/BE:environment:production` subject를 가진다.
- 계정에 `token.actions.githubusercontent.com` OIDC provider가 이미 있다면 그 ARN을 두 번째 stack에 전달한다.
- 2026-07-15 이후 생성되었거나 immutable OIDC subject를 선택한 저장소는 GitHub에 표시되는 owner/repository ID 형식으로 `GitHubOidcSubject`를 바꾼다.

## 생성 순서

1. Parameter Store에 `/classfit/prod/DB_PASSWORD`를 강한 무작위 값의 `SecureString`으로 먼저 만든다. 실제 값은 파일이나 shell history에 남기지 않는다.
2. `prod.example.json`을 복사하고 콘솔에서 `AlertEmail`과 apex domain을 입력한다. 실제 값 파일은 커밋하지 않는다.
3. `classfit-infrastructure.yaml` stack을 `CAPABILITY_NAMED_IAM`과 함께 생성한다.
4. 출력의 RDS endpoint로 `DB_URL=jdbc:postgresql://ENDPOINT:5432/classfit?sslmode=require`를 만든다.
5. `/classfit/prod/DB_URL`, `DB_USER`, `KAKAO_CLIENT_ID`, `KAKAO_CLIENT_SECRET`, `OPENAI_API_KEY`, `CLASSFIT_FRONTEND_ORIGIN`도 Parameter Store `SecureString`으로 등록한다. 값은 터미널 출력이나 저장소 파일에 남기지 않는다.
6. 첫 stack 출력의 ECR ARN과 EC2 instance ID를 사용해 `classfit-github-oidc.yaml` stack을 생성한다.
7. GitHub `production` Environment variables에 `AWS_DEPLOY_ROLE_ARN`, `ECR_REPOSITORY`, `EC2_INSTANCE_ID`, `CLASSFIT_DOMAIN`을 설정한다. `CLASSFIT_DOMAIN`은 실제 소유한 apex domain이며 프론트는 `app.<domain>`, API는 `api.<domain>`으로 같은 사이트에 배치한다.
8. DNS에 `api.<domain>` A record를 Elastic IP로 연결하고, Vercel 프론트에는 `app.<domain>`을 연결한다. 두 주소가 같은 사이트여야 운영 세션 쿠키(`SameSite=Lax`)가 브라우저에서 전달된다. 기본 `*.vercel.app` 주소를 운영 프론트로 사용하지 않는다. 프록시가 뜬 뒤 Caddy가 인증서를 자동 발급한다.

로컬 AWS CLI가 준비된 경우 생성 전에 다음을 실행한다.

```bash
aws cloudformation validate-template --region ap-northeast-2 --template-body file://infra/cloudformation/classfit-infrastructure.yaml
aws cloudformation validate-template --region ap-northeast-2 --template-body file://infra/cloudformation/classfit-github-oidc.yaml
```

## 비용과 알림

월 예산은 100 USD이며 실제 사용액 10%, 30%, 70%에서 이메일을 보낸다. Budget 알림 자체는 비용 차단 장치가 아니므로 RDS, EC2, EBS, ECR 이미지, Elastic IP/public IPv4를 함께 확인한다. 무료 크레딧이 있더라도 크레딧 만료 또는 제외 서비스에는 정상 과금될 수 있다.

## 삭제 순서

1. 배포 workflow를 중지한다.
2. OIDC stack을 삭제한다.
3. 기반 stack을 삭제한다. RDS는 `DeletionPolicy: Snapshot`이므로 최종 snapshot이 남는다.
4. snapshot과 ECR 이미지가 더 필요 없는지 확인한 뒤 별도로 삭제한다. 이 단계는 복구 불가능할 수 있으므로 자동화하지 않는다.
