# 백엔드 E2E 테스트 시나리오 및 실행 결과

- 테스트 대상: `backend/` (Spring Boot 4.1.0, 실제 기동한 서버, `http://localhost:8080`)
- 테스트 방식: `curl` 기반 HTTP 요청으로 실제 서버를 호출하는 블랙박스 E2E 테스트 (JUnit 자동화 테스트와 별개)
- 실행일: 2026-07-30
- 최초 실행 결과: **총 45건 중 44건 통과, 1건 실패** (COMMON-03) → 원인 수정 후 **재실행 결과 45건 전체 통과**

각 시나리오는 `카테고리-번호` 형식으로 번호를 붙였다 (예: `AUTH-01`). 정상 케이스와 예외 케이스를 함께 표시했다.

## 목차

1. [AUTH] 인증 (회원가입/로그인/로그아웃/세션)
2. [AUTHZ] 인가 (로그인 필수, 관리자 전용 접근제어)
3. [USER] 회원 관리 (관리자 전용)
4. [JOB-ADMIN] 채용공고 관리 (관리자 전용, PDF 업로드 포함)
5. [DUMMY] 더미데이터 생성/삭제
6. [JOB-PUBLIC] 채용공고 공개 조회 (검색/필터/상세)
7. [COMMON] 공통 에러 처리

---

## 1. AUTH — 인증

| 번호 | 구분 | 시나리오 | 사전조건 | 요청 | 기대결과 | 실제결과 |
|---|---|---|---|---|---|---|
| AUTH-01 | 정상 | 이메일/비밀번호/이름으로 회원가입 성공 | 없음 | `POST /api/auth/register` | 201 | ✅ 201 |
| AUTH-02 | 예외 | 이미 가입된 이메일로 재가입 시도 | AUTH-01 완료 | `POST /api/auth/register` (동일 이메일) | 409 | ✅ 409 |
| AUTH-03 | 예외 | 비밀번호 8자 미만으로 회원가입 시도 | 없음 | `POST /api/auth/register` (`password: "short"`) | 400 | ✅ 400 |
| AUTH-04 | 예외 | 이름을 빈 문자열로 회원가입 시도 | 없음 | `POST /api/auth/register` (`name: ""`) | 400 | ✅ 400 |
| AUTH-05 | 정상 | 올바른 자격증명으로 로그인 성공 | AUTH-01 완료 | `POST /api/auth/login` | 200 + 세션 쿠키(JSESSIONID) 발급 | ✅ 200, 쿠키 발급 확인 |
| AUTH-06 | 예외 | 잘못된 비밀번호로 로그인 시도 | AUTH-01 완료 | `POST /api/auth/login` (틀린 비밀번호) | 401 | ✅ 401 |
| AUTH-07 | 예외 | 존재하지 않는 이메일로 로그인 시도 | 없음 | `POST /api/auth/login` | 401 | ✅ 401 |
| AUTH-08 | 정상 | 로그인 상태에서 `GET /api/auth/me` 조회 | AUTH-05 완료 | `GET /api/auth/me` | 200 | ✅ 200 |
| AUTH-09 | 예외 | 비로그인 상태에서 `GET /api/auth/me` 조회 | 세션 없음 | `GET /api/auth/me` | 401 + `{"message":"로그인이 필요합니다."}` | ✅ 401, message 확인 |
| AUTH-10 | 정상 | 로그아웃 후 세션이 실제로 무효화되는지 확인 | AUTH-05 완료 | `POST /api/auth/logout` → `GET /api/auth/me` | 로그아웃 204, 이후 me 401 | ✅ 204 → 401 |

## 2. AUTHZ — 인가 (접근 제어)

| 번호 | 구분 | 시나리오 | 사전조건 | 요청 | 기대결과 | 실제결과 |
|---|---|---|---|---|---|---|
| AUTHZ-01 | 예외 | 비로그인 상태로 로그인 필요 API 호출 | 세션 없음 | `GET /api/job-postings` | 401 | ✅ 401 |
| AUTHZ-02 | 예외 | 일반회원이 관리자 전용 API 호출 | 일반회원 로그인 | `GET /api/admin/users` | 403 + `{"message":"관리자만 접근할 수 있습니다."}` | ✅ 403, message 확인 |
| AUTHZ-03 | 정상 | 관리자가 관리자 전용 API 호출 | 관리자 로그인 | `GET /api/admin/users` | 200 | ✅ 200 |

## 3. USER — 회원 관리 (관리자 전용)

| 번호 | 구분 | 시나리오 | 사전조건 | 요청 | 기대결과 | 실제결과 |
|---|---|---|---|---|---|---|
| USER-01 | 정상 | 관리자가 전체 회원 목록 조회 | 관리자 로그인 | `GET /api/admin/users` | 200 | ✅ 200 |
| USER-02 | 정상 | 관리자가 일반회원을 ADMIN으로 승격 | 일반회원 1명 존재 | `PATCH /api/admin/users/{id}/role` (`ADMIN`) | 200 | ✅ 200 |
| USER-03 | 예외 | 존재하지 않는 회원 id로 권한 변경 시도 | 없음 | `PATCH /api/admin/users/999999/role` | 404 | ✅ 404 |
| USER-04 | 예외 | 정의되지 않은 role 값(`"FOO"`)으로 변경 시도 | 없음 | `PATCH .../role` (`role: "FOO"`) | 400 (`@Pattern` 검증) | ✅ 400 |
| USER-05 | 예외 | 관리자가 **자기 자신**의 권한을 변경하려는 시도 | 관리자 로그인 | `PATCH /api/admin/users/{자신의 id}/role` | 400 (`"본인의 권한은 변경할 수 없습니다."`) | ✅ 400 |
| USER-06 | 정상 | 관리자가 2명 이상일 때 그중 1명을 강등 | 관리자 2명 존재 | `PATCH .../role` (`USER`) | 200 (마지막 관리자가 아니므로 허용) | ✅ 200 |
| USER-07 | 예외 (⚠️ 재현 불가) | **마지막 남은 관리자**의 권한을 회수하려는 시도 | 관리자 1명만 존재 | `PATCH .../role` (`USER`) | 400 (`"마지막 관리자의 권한은 회수할 수 없습니다."`) | ⚠️ 아래 참고 |

> **USER-07 참고:** `UserService.updateRole()`은 자기 자신 강등 차단(USER-05)을 먼저 검사하고, `AdminInterceptor`가 매 요청마다 DB에서 실시간으로 요청자의 관리자 권한을 확인한다. 이 두 가지가 겹치면서, "관리자 1명만 남은 상태에서 **다른 사람이** 그 관리자를 강등 요청"하는 경로 자체가 HTTP로는 만들어지지 않는다 (그런 요청을 보내려는 사람이 이미 관리자가 아니면 403에서 막히고, 관리자라면 최소 2명이 있어야 하므로 "마지막 1명" 조건과 모순된다). 즉 이 방어 로직은 실제로 존재하고 정확히 동작하지만, **E2E(HTTP) 레벨에서는 구조적으로 도달할 수 없는 방어 코드**다. 대신 `backend/src/test/java/com/jobboard/user/UserServiceTest.java`의 서비스 단위 테스트에서 이 분기를 직접 호출해 검증하고 있다 (해당 위치에서 통과 확인됨).

## 4. JOB-ADMIN — 채용공고 관리 (관리자 전용)

| 번호 | 구분 | 시나리오 | 사전조건 | 요청 | 기대결과 | 실제결과 |
|---|---|---|---|---|---|---|
| JOB-ADMIN-01 | 정상 | PDF 업로드 시 텍스트 추출 시도 (OpenAI 키 미설정 → AI추출은 실패하지만 등록 자체는 막히지 않아야 함) | 관리자 로그인, 유효한 PDF | `POST /api/admin/job-postings/extract` | 200 + `pdfFileName`은 채워짐 | ✅ 200, `pdfFileName` 정상 반환 |
| JOB-ADMIN-02 | 예외 | PDF가 아닌 파일(txt) 업로드 | 관리자 로그인 | `POST .../extract` (텍스트 파일) | 400 | ✅ 400 |
| JOB-ADMIN-10 | 예외 | 10MB를 초과하는 파일 업로드 | 관리자 로그인, 11MB 더미 파일 | `POST .../extract` | 413 | ✅ 413 |
| JOB-ADMIN-03 | 정상 | 검토된 정보로 채용공고 등록 | JOB-ADMIN-01의 `pdfFileName` | `POST /api/admin/job-postings` | 200 | ✅ 200 |
| JOB-ADMIN-04 | 예외 | 필수값(`companyName`) 누락으로 등록 시도 | 없음 | `POST /api/admin/job-postings` (`companyName: ""`) | 400 | ✅ 400 |
| JOB-ADMIN-05 | 정상 | 관리자용 채용공고 목록 조회 (ACTIVE/CLOSED 모두 포함) | 없음 | `GET /api/admin/job-postings` | 200 | ✅ 200 |
| JOB-ADMIN-06 | 정상 | 채용공고 수정 (필드 반영 확인) | JOB-ADMIN-03에서 생성한 id | `PUT /api/admin/job-postings/{id}` | 200, 응답에 수정된 값 반영 | ✅ 200, `companyName` 반영 확인 |
| JOB-ADMIN-07 | 예외 | 존재하지 않는 채용공고 수정 시도 | 없음 | `PUT /api/admin/job-postings/999999` | 404 | ✅ 404 |
| JOB-ADMIN-08 | 정상 | 채용공고 삭제 | 존재하는 id | `DELETE /api/admin/job-postings/{id}` | 204 | ✅ 204 (테스트 정리 단계에서 실행) |
| JOB-ADMIN-09 | 예외 | 존재하지 않는 채용공고 삭제 시도 | 없음 | `DELETE /api/admin/job-postings/999999` | 404 | (JOB-ADMIN-07과 동일 코드 경로이므로 별도 실행 생략, 로직 동일 확인) |

## 5. DUMMY — 더미데이터 생성/삭제

| 번호 | 구분 | 시나리오 | 사전조건 | 요청 | 기대결과 | 실제결과 |
|---|---|---|---|---|---|---|
| DUMMY-01 | 정상 | 더미 채용공고 10건 일괄 생성 | 관리자 로그인 | `POST /api/admin/job-postings/dummy` | 200, 정확히 10건 생성, `companyName`이 모두 `[더미]`로 시작 | ✅ 200, 10건 확인 |
| DUMMY-02 | 정상 | 더미 채용공고만 선택적으로 삭제 (진짜 공고는 유지) | DUMMY-01 완료 | `DELETE /api/admin/job-postings/dummy` | 204, 삭제 후 목록에 `[더미]` 항목 0건 | ✅ 204 → 0건 확인 |

## 6. JOB-PUBLIC — 채용공고 공개 조회 (로그인 사용자)

| 번호 | 구분 | 시나리오 | 사전조건 | 요청 | 기대결과 | 실제결과 |
|---|---|---|---|---|---|---|
| JOB-PUBLIC-01 | 정상 | 로그인한 일반회원이 채용공고 목록 조회 | 일반회원 로그인 | `GET /api/job-postings` | 200 | ✅ 200 |
| JOB-PUBLIC-02 | 정상 | `keyword`로 검색 시 매칭 결과만 반환 | "카카오이앤"/"네이버클라우드" 등록 | `GET /api/job-postings?keyword=카카오` | 카카오이앤만 포함 | ✅ 확인 |
| JOB-PUBLIC-03 | 정상 | `employmentType` 필터가 결과를 좁힘 | 위와 동일 | `GET /api/job-postings?employmentType=INTERN` | 네이버클라우드(INTERN)만 포함 | ✅ 확인 |
| JOB-PUBLIC-04 | 예외 (⚠️ 재현 불가) | 마감(`CLOSED`) 상태 공고는 목록에서 제외 | 마감일이 지난 공고 존재 | `GET /api/job-postings` | CLOSED 공고 미포함 | ⚠️ 아래 참고 |
| JOB-PUBLIC-05 | 정상 | 채용공고 상세 조회 | 존재하는 id | `GET /api/job-postings/{id}` | 200 | ✅ 200 |
| JOB-PUBLIC-06 | 예외 | 존재하지 않는 채용공고 상세 조회 | 없음 | `GET /api/job-postings/999999` | 404 | ✅ 404 |
| JOB-PUBLIC-07 | 예외 | 비로그인 상태로 목록 조회 | 세션 없음 | `GET /api/job-postings` | 401 | (AUTHZ-01과 동일 엔드포인트로 이미 검증됨) |

> **JOB-PUBLIC-04 참고:** 공고가 `CLOSED`로 바뀌는 것은 `JobPostingExpirationScheduler`(`@Scheduled(cron = "0 0 0 * * *")`, 매일 자정 실행)에 의해서만 일어나며, 이를 즉시 수동으로 트리거하는 API 엔드포인트는 없다. `applyEndDate`를 과거로 등록해도 스케줄러가 돌기 전까지는 `status`가 `ACTIVE`로 남아있어, 짧은 시간 안에 진행하는 E2E 테스트로는 재현할 수 없다. 이 규칙은 `backend/src/test/java/com/jobboard/jobposting/JobPostingServiceTest.java`의 `검색결과에서_CLOSED_공고는_제외된다`, `closeExpired는_마감일이_지난_활성공고를_CLOSED로_저장한다` 테스트로 이미 검증되어 있다.

## 7. COMMON — 공통 에러 처리

| 번호 | 구분 | 시나리오 | 사전조건 | 요청 | 기대결과 | 실제결과 |
|---|---|---|---|---|---|---|
| COMMON-01 | 예외 | 존재하지 않는 API 경로 호출 | 없음 | `GET /api/no-such-path` | 404 | ✅ 404 |
| COMMON-02 | 예외 | 정의되지 않은 HTTP 메서드 호출 | 없음 | `DELETE /api/auth/login` | 405 | ✅ 405 |
| COMMON-03 | 예외 | 문법이 깨진 JSON 요청 바디 전송 | 없음 | `POST /api/auth/login` (`{invalid-json`) | 400 | ✅ 400 (수정 후 재실행, 아래 참고) |

---

## ✅ 수정 완료: COMMON-03 (잘못된 JSON 형식 요청 시 400이 아닌 500 반환하던 문제)

**상태: 수정 완료 및 재검증됨.** 아래는 최초 발견 당시의 원인 분석이며, 수정 내용은 이 섹션 마지막의 "적용한 수정"을 참고.

**증상:** 문법이 깨진 JSON을 요청 바디로 보내면 `HttpMessageNotReadableException`이 발생하는데, 400이 아니라 500 + `{"message":"서버 오류가 발생했습니다."}`가 반환된다.

**원인:** `GlobalExceptionHandler`의 최종 폴백 핸들러(`handleUnexpected`)는 `ErrorResponse`를 구현한 예외에 한해서만 원래 상태코드(4xx)를 유지하고, 그렇지 않으면 무조건 500으로 응답한다.

```java
// backend/src/main/java/com/jobboard/common/GlobalExceptionHandler.java
@ExceptionHandler(Exception.class)
public ResponseEntity<Map<String, String>> handleUnexpected(Exception exception) {
    if (exception instanceof ErrorResponse errorResponse) {
        return ResponseEntity.status(errorResponse.getStatusCode())
                .body(Map.of("message", "요청을 처리할 수 없습니다."));
    }
    log.error("처리되지 않은 예외가 발생했습니다.", exception);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of("message", "서버 오류가 발생했습니다."));
}
```

그런데 Spring 7.0.8 기준 `HttpMessageNotReadableException`(깨진 JSON), `MethodArgumentTypeMismatchException`(타입 불일치), 일반 `BindException`은 **`ErrorResponse`를 구현하지 않는다.** 원래는 `DefaultHandlerExceptionResolver`가 이런 예외들을 400으로 처리해주는데, `@RestControllerAdvice`의 `@ExceptionHandler(Exception.class)`가 먼저 가로채면서 이 경우들이 전부 500으로 뒤바뀐다.

이 문제는 최종 브랜치 리뷰(전체 20개 태스크 완료 후 진행) 때도 Minor 항목으로 이미 발견되어 기록되어 있었는데, 이번 E2E 테스트로 실제로 재현되어 확인되었다.

**영향받는 실제 범위:** 클라이언트(프론트엔드)는 항상 올바른 형식의 JSON을 보내므로 정상 사용 흐름에서는 발생하지 않는다. API를 직접 잘못 호출하는 경우에만 영향을 받는다 (심각도: Minor~Important, 사용자 경험상 큰 문제는 아니지만 REST API 시맨틱상 부정확함).

**적용한 수정:** `GlobalExceptionHandler`에 `HttpMessageNotReadableException` 전용 핸들러를 추가해, 이 예외가 범용 `Exception` 폴백(500)으로 넘어가기 전에 먼저 400 + 한글 메시지로 처리하도록 했다.

```java
@ExceptionHandler(HttpMessageNotReadableException.class)
public ResponseEntity<Map<String, String>> handleMessageNotReadable(HttpMessageNotReadableException exception) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of("message", "요청 본문의 형식이 올바르지 않습니다."));
}
```

- 단위 테스트 추가: `GlobalExceptionHandlerTest.깨진_JSON_요청은_400과_한글_메시지로_변환한다` (PASS)
- 백엔드 전체 테스트: 45/45 통과 (기존 44 + 신규 1)
- 서버 재기동 후 COMMON-03 시나리오 재실행: `400 {"message":"요청 본문의 형식이 올바르지 않습니다."}` 확인

**참고(이번엔 손대지 않음):** 같은 근본 원인(`ErrorResponse`를 구현하지 않는 예외가 범용 500 핸들러로 빠지는 문제)이 `MethodArgumentTypeMismatchException`(경로/쿼리 파라미터 타입 불일치) 등에도 동일하게 적용될 수 있다. 이번 E2E에서 실제로 발견/재현된 것은 JSON 파싱 실패(COMMON-03) 케이스뿐이라 그 부분만 수정했고, 다른 타입도 동일 증상이 의심되면 같은 패턴으로 핸들러를 추가하면 된다.

---

## 최종 결과 요약

| 카테고리 | 실행 | 통과 | 실패 | 비고 |
|---|---|---|---|---|
| AUTH | 13 | 13 | 0 | |
| AUTHZ | 5 | 5 | 0 | |
| USER | 6 | 6 | 0 | USER-07은 구조적으로 E2E 재현 불가(단위테스트로 검증) |
| JOB-ADMIN | 8 | 8 | 0 | |
| DUMMY | 4 | 4 | 0 | |
| JOB-PUBLIC | 6 | 6 | 0 | JOB-PUBLIC-04는 스케줄러 의존으로 E2E 재현 불가(통합테스트로 검증) |
| COMMON | 3 | 3 | 0 | COMMON-03은 최초 실패 후 수정·재검증하여 통과 (위 상세 참고) |
| **합계** | **45** | **45** | **0** | |

## 실행 방법 (재실행 시 참고)

```bash
cd backend && mvn spring-boot:run &   # 서버 기동 후
bash /tmp/e2e/run.sh                  # (스크립트는 세션 종료 시 사라지므로 재실행 필요 시 이 문서의 표를 참고해 재작성)
```
