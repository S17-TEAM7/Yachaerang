# 🍅 야채랑 🥦

농수산물 가격 정보 제공과 농업 도우미 플랫폼 야채랑을 소개합니다.

---

## 🥬 프로젝트 소개 🥬

<img src=/exec/page1.png>

<br>

<img src=/exec/page2.png>

<br>

<img src=/exec/page3.png>

<br>

<img src=/exec/page4.png>

<br>

<img src=/exec/page5.png>

<br>

<img src=/exec/page6.png>

<br>

<img src=/exec/page7.png>

<br>

---

## 🎁 팀원 소개 🎁


| **전유연** | **김가은** |
|:--:|:--:|
| [<img src="https://avatars.githubusercontent.com/u/109857975?v=4" width=200><br/>@youyeon11](https://github.com/youyeon11) | [<img src="https://avatars.githubusercontent.com/u/151455492?v=4" width=200><br/>@gaeunji1](https://github.com/gaeunji1) |
| **BE** | **FE** |

---

## 🗂️ 프로젝트 구조 🗂️

- backend
```
com.yachaerang.backend
├── api                                   # API 도메인 및 비즈니스 로직
│   ├── article                           # 게시글 도메인
│   ├── bookmark                          # 북마크 도메인
│   ├── chat                              # 채팅 도메인 (WebClient 활용)
│   ├── common                            # 공통 코드 (BaseEntity, Enum 등)
│   ├── farm                              # 나의 농장 관리 도메인
│   ├── favorite                          # 관심 상품 도메인
│   ├── member                            # 회원 관리 도메인
│   ├── product                           # 농산물 가격 및 상품 도메인 (Daily, Weekly, Monthly, Yearly)
│   └── reaction                          # 반응(좋아요 등) 도메인
│
├── global                                # 전역 설정 및 유틸리티
│   ├── auth                              # 인증/인가 관련 (JWT, SecurityConfig, OAuth)
│   ├── config                            # 설정 파일 (Async, Redis, MyBatis, WebClient 등)
│   ├── exception                         # 전역 예외 처리 (GlobalExceptionHandler)
│   ├── filter                            # 필터 설정 (LoggingFilter 등)
│   ├── health                            # 헬스 체크
│   ├── response                          # 공통 응답 포맷 (ApiResponse)
│   └── util                              # 유틸리티 클래스 (TypeHandler 등)
│
├── infrastructure                        # 외부 인프라 연동
│   ├── s3                                # AWS S3 파일 업로드/다운로드
│   └── smtp                              # 이메일 전송 서비스
│
└── BackendApplication 
```

- batch
```
com.yachaerang.batch
├── configuration                         # 배치 및 시스템 설정
│   ├── job                               # Spring Batch Job 설정 (Daily, Weekly, Monthly, Yearly 등)
│   ├── parameter                         # Job 파라미터 관리
│   ├── BatchConfiguration                # 배치 공통 설정
│   └── SchedulerConfig                   # 스케줄러 설정
│
├── controller                            # 배치 Job 실행 컨트롤러
│
├── domain                                # 도메인 비즈니스 로직
│   ├── common                            # 공통 엔티티
│   ├── dailyPrice                        # 일별 가격 처리 (Processor, Reader, Writer)
│   ├── dto                               # 데이터 전송 객체 (Kamis API 응답 등)
│   ├── entity                            # 배치 엔티티 (Price, Product 등)
│   └── processor                         # 데이터 가공 프로세서
│
├── exception                             # 예외 처리
├── listener                              # Job/Step 실행 리스너
├── repository                            # 데이터 접근 계층 (MyBatis Mapper, JPA Repository)
├── scheduler                             # 스케줄러 (Daily, Monthly, Weekly 등)
├── service                               # 비즈니스 서비스 (API 호출, 데이터 집계 등)
├── util                                  # 유틸리티 (날짜, 파싱 등)
└── BatchApplication                     
```

- frontend
```
frontend/src
├── api                                   # API 호출 모듈 (Axios 인터셉터 및 도메인별 API)
│   ├── article.js, auth.js, chat.js ...
│
├── assets                                # 정적 자원 (이미지, CSS, 로고)
│
├── components                            # 재사용 가능한 UI 컴포넌트
│   ├── brand                             # 브랜드 로고 등
│   ├── common                            # 공통 컴포넌트 (Pagination 등)
│   ├── icons                             # 아이콘 컴포넌트 모음
│   ├── layout                            # 레이아웃 컴포넌트 (Header, Sidebar, Footer)
│   ├── modal                             # 모달 컴포넌트
│   └── spinner                           # 로딩 스피너
│
├── router                                # 라우터 설정 (Vue Router)
├── stores                                # 상태 관리 (Pinia)
│   ├── auth.js, navigation.js, toast.js ...
│
├── utils                                 # 유틸리티 함수 (Storage 등)
│
├── views                                 # 페이지 뷰
│   ├── ai                                # AI 챗봇 페이지 (Chatbot)
│   ├── article                           # 게시글 페이지 (목록, 상세, 작성)
│   ├── auth                              # 인증 페이지 (로그인, 회원가입, 비밀번호 찾기)
│   ├── dashboard                         # 대시보드 (차트, 통계)
│   ├── main                              # 메인 페이지 (소개, 뉴스, 랭킹 등 섹션)
│   ├── myfarm                            # 내 텃밭 관리 페이지
│   ├── mypage                            # 마이 페이지 (정보 수정, 관심 목록)
│   ├── PriceSearchMain                   # 가격 검색 메인 페이지
│   └── rank                              # 랭킹 페이지
│
├── App.vue                               # 루트 컴포넌트
└── main.js                               # 앱 진입점
```

<br>

---

## ✨ 아키텍처 ✨

<img src=/exec/architecture.png>

<br>

---

## ✨ ERD ✨

<img src=/exec/ERD.png>

<br>

---


## ✨ 기술 스택 ✨

| Category     | Stack                                                       |
|:-------------|:------------------------------------------------------------|
| **Frontend** | Vue 3, JavaScript, Tailwind CSS, Axios                      |
| **Backend**  | Spring Boot 3.5.7, Spring AI, Spring Batch, MyBatis, Gradle |
| **Database** | MySQL 8.0.33, Redis                                         |
| **CI/CD**    | Github Actions                                              |
| **API Docs** | Notion, REST Docs                                           |
| **Infra**    | AWS EC2, AWS RDS, AWS S3, Docker, Nginx                     |

<br>

---

## ✨ 코드 커버리지 ✨

> 백엔드 코드 커버리지 결과입니다.

[![codecov](https://codecov.io/gh/S17-TEAM7/Yachaerang/branch/main/graph/badge.svg)](https://codecov.io/gh/yachaerang/backend)
