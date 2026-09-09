# ZeroRisk (제로리스크)
> **모의투자 기반 주식 학습 및 대회 플랫폼**

## 🗓 프로젝트 개요
- **개발 기간:** 2026-06 ~ 2026-09
- **기획 배경:**
  1. 실제 자산을 위험에 노출시키지 않고 주식 투자를 경험하고 학습할 수 있는 환경 제공
  2. 오픈뱅킹 연동으로 실잔액에 근거한 현실감 있는 시드머니 지급, 동시에 실물 계좌가 없는 사용자(학생 등)도 소외되지 않도록 실잔액 무관 학습용 크레딧을 병행 제공
  3. 대회 형태의 경쟁을 통해 투자 학습에 대한 동기 부여

---

## 팀 구성 및 역할
| 이름 | 역할 | 담당 업무 |
|:---:|:---|:---|
| **박종권** | BE·FE | - 인증/계좌/오픈뱅킹 시스템 설계 및 보안(JWT, OAuth2, Rate Limiting)<br>- 대회 라이프사이클(모집~진행~집계~종료), 상금 지급 정합성 구축<br>- 전체/기간별 랭킹 시스템<br>- 관리자 대시보드, 감사 로그 아키텍처, 권한 통제 구조 설계 |
| **오상민** | BE·FE | - 주문 매칭 및 체결 로직, KIS Open API 연동 실시간 시세<br>- 예약(지정가) 주문 스케줄러, 일별 포트폴리오 자산 스냅샷 배치 |
| **오명석** | BE·FE | - 커뮤니티 게시판 및 댓글, 관리자 콘텐츠 모더레이션<br>- 실시간 채팅 및 SSE 알림, 신고 시스템,프로필·팔로우, 공지사항 및 긴급 알림 |

---

## 기술 스택

### Frontend
- **Framework & Libraries:** React 19, TypeScript, Vite, Tailwind CSS 4, TanStack Query(React Query), Axios

### Backend
- **Core & API:** Java 17, Spring Boot 3.x, Spring Data JPA, Spring Security 6, JWT + OAuth2(Google/Kakao)
- **Batch & Resilience:** Spring Batch(대용량 배치/성능 벤치마크), Spring Retry(지수 백오프+Jitter), Spring AOP
- **Real-time:** WebSocket(STOMP), SSE(Server-Sent Events)
- **Monitoring:** Micrometer, Spring Boot Actuator, Prometheus
- **External Services:** 한국투자증권(KIS) Open API(실시간 시세), Google reCAPTCHA, Gmail SMTP

### Database & Infra
- **Database:** Oracle XE(19c), Redis(캐시·세션·Rate Limiting)
- **Infra/DevOps:** AWS EC2, Docker, Docker Compose, GitHub Actions, Nginx
- **Co-working:** Git, GitHub, notion

---

## 시스템 아키텍처
1. **CI/CD 파이프라인:** GitHub Actions를 통해 EC2 서버의 Docker Compose로 자동 빌드 및 배포
2. **프론트엔드 (Nginx 기반):** React 앱이 정적 파일로 서빙되며, Nginx가 리버스 프록시 역할 수행
3. **백엔드 (Spring Boot):** REST API, WebSocket(채팅), SSE(실시간 알림) 처리 및 외부 API(KIS, OAuth2, reCAPTCHA) 연동
4. **인증:** JWT를 HttpOnly Cookie로 발급하는 완전 무상태(Stateless) 구조. OAuth2 인증 중간 상태도 세션 대신 쿠키 기반 저장소로 관리해 서버 확장 시에도 안전하도록 설계
5. **데이터베이스:** Oracle XE를 정본(Source of Truth)으로, Redis를 랭킹·인증 등 조회 성능이 중요한 영역의 캐시 계층으로 분리 운용
6. **감사/모니터링:** 관리자 조치와 유저 활동을 도메인 이벤트 기반으로 비동기 기록, AOP로 기록 누락을 감지하는 안전망 구축
