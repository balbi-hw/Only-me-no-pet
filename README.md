# Only Me No Puppy

자율적으로 생활하는 가상 반려동물 MVP입니다. 강아지/고양이를 입양하고 홈캠에서 현재 상태와 부재 중 기록을 확인하며 먹이·놀이·훈련을 할 수 있습니다.

## 실행

필요 환경: Java 21, Node.js 20+, Docker, Maven 3.9+ (또는 IDE의 Maven 실행 기능).

```powershell
docker compose up -d db
cd backend
mvn spring-boot:run
```

새 터미널에서:

```powershell
cd frontend
npm install
npm run dev
```

브라우저에서 `http://localhost:3000`을 엽니다. 기본 DB 접속 정보는 `compose.yaml`과 `backend/src/main/resources/application.yml`에 있습니다. 외부 배포 전 반드시 환경 변수로 교체하세요.

## 현재 구현 범위

- 반려동물 한 마리 입양, 상태 조회, 홈캠 장면, 활동 기록
- 먹이·놀이·훈련 및 멱등성 키 처리
- 접속 시 경과 시간 복원, 결정적 자율 행동, PostgreSQL 이벤트 기록

인증과 실제 집 사진 업로드, 공간 재구성, 3D/생성형 AI 렌더러는 다음 단계입니다. 현재 API는 로컬 개발용 단일 사용자(`demo-user`)로 동작합니다.
