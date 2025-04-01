## 서비스별 개요

회원가입 시 선택한 사용자의 관심사를 기반으로 네이버 뉴스를 조회합니다.

AI를 사용하여 해당 뉴스의 본문을 요약하여 제공하는 서비스입니다.
<br><br>

##  빌드 및 실행 방법 (Jenkins + ECS + ECR 기반)
Jenkins를 활용한 자동화 빌드 및 배포 파이프라인과, AWS ECS 기반의 컨테이너 실행 및 관리 체계를 구성하였습니다. 빌드된 Docker 이미지는 latest 태그로 ECR에 푸시되며, ECS 서비스는 해당 태그를 기준으로 항상 최신 버전을 배포합니다. 일반적인 Jenkinsfile 파이프라인 예시는 다음과 같습니다.

### 📦 Jenkinsfile 예시

```bash
Pipeline {
  Tools: gradle 8.12.1

  Stages:
    1. Gradle Build
       - GitHub에서 코드 pull
       - gradle clean build -x test

    2. Docker Build & Deploy (조건: DOCKER_BUILD == true)
       - SSH로 Docker 서버 접속
       - Docker 로그인 (ECR)
       - 이미지 빌드 및 태깅
       - Docker 이미지 push (ECR)
       - ECS 태스크 정의 json 추출 → jq로 필터링
       - ECS 태스크 재등록
       - ECS 서비스에 새 태스크 배포
}
```
<br>

###  환경 변수 설정

ECS 태스크 정의 시 environment로 설정
- article-service의 ECS Task 정의 내 환경변수 예시

```json
"environment": [
    {
        "name": "GROQ_API_KEY",
        "value": "example-groq-api-key"
    },
    {
        "name": "ARTICLE_HOSTNAME",
        "value": "example-article-hostname"
    },
    {
        "name": "NAVER_CLIENT_ID",
        "value": "example-client-id"
    },
    {
        "name": "NAVER_CLIENT_SECRET",
        "value": "example-client-secret"
    }
]

```
