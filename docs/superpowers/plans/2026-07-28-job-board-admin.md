# 채용정보 게시판 (관리자 큐레이션형) 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 관리자가 채용공고 PDF를 업로드하면 OpenAI API로 정보를 자동 추출·검토 후 게시하고, 로그인한 일반 이용자는 검색/필터로 채용정보를 조회할 수 있는 웹 서비스를 만든다.

**Architecture:** React(SPA) 프론트엔드가 Vite 개발 서버의 `/api` 프록시를 통해 Spring Boot REST API 백엔드와 통신한다. 백엔드는 세션 기반 인증(커스텀 인터셉터, Spring Security 미사용)으로 로그인/권한을 관리하고, H2(파일 모드)에 데이터를 저장하며, PDF 업로드 시 Apache PDFBox로 텍스트를 추출한 뒤 OpenAI API를 동기 호출해 구조화 정보를 받아온다.

**Tech Stack:**
- Backend: Java 25 (LTS), Spring Boot 4.1.0 (Web, Data JPA, Validation), H2 2.4.240 (파일 모드), Apache PDFBox 3.0.8, spring-security-crypto(BCrypt), Lombok, Maven
- Frontend: Node.js 24 (Active LTS), React 19.2.8, react-router 8.3.0, Vite 8.1.5 + @vitejs/plugin-react 6.0.4, Vitest 4.1.10 + @testing-library/react 16.3.2

## Global Constraints

- Java 버전은 25 (LTS) 이상을 사용한다.
- Spring Boot는 4.1.0을 사용한다 (Spring Framework 7 기반).
- Node.js는 24 (Active LTS) 이상을 사용한다.
- 로그인이 필요한 API: `/api/auth/register`, `/api/auth/login` 을 제외한 모든 `/api/**` 엔드포인트는 세션 로그인이 되어 있어야 한다 (비로그인 시 401).
- 관리자 전용 API: `/api/admin/**` 는 세션의 `userRole`이 `ADMIN`이어야 한다 (아니면 403).
- URL 등록 기능은 이번 스펙 범위에서 제외한다 (PDF 업로드만 지원).
- 모든 커밋 메시지, 코드 내 사용자 노출 문자열(에러 메시지 등), 문서는 한글로 작성한다.
- 원본 PDF는 서버 로컬 파일시스템(`backend/uploads/`)에 저장한다.

---

## Task 1: 프로젝트 초기 설정 (백엔드/프론트엔드 스캐폴딩)

**Files:**
- Create: `.gitignore` (루트)
- Create: `backend/pom.xml`
- Create: `backend/src/main/java/com/jobboard/JobBoardApplication.java`
- Create: `backend/src/main/java/com/jobboard/common/HealthController.java`
- Create: `backend/src/test/java/com/jobboard/common/HealthControllerTest.java`
- Create: `backend/src/main/resources/application.properties`
- Create: `frontend/` (Vite React 스캐폴드 — `npm create vite@latest` 결과물)
- Create: `frontend/vite.config.js` (프록시 설정 추가)

**Interfaces:**
- Produces: `GET /api/health` → `{"status":"ok"}` (이후 태스크에서 백엔드가 정상 기동했는지 확인하는 용도로만 사용, 다른 태스크가 의존하지 않음)

- [ ] **Step 1: 저장소 초기화**

```bash
cd /Users/hun/Summer_lecture_vibe_coding
git init
```

- [ ] **Step 2: 루트 `.gitignore` 작성**

```gitignore
# Backend
backend/target/
backend/uploads/
backend/data/
backend/*.log

# Frontend
frontend/node_modules/
frontend/dist/

# IDE / OS
.DS_Store
.idea/
*.iml
```

- [ ] **Step 3: 백엔드 `pom.xml` 작성**

`backend/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>4.1.0</version>
        <relativePath/>
    </parent>

    <groupId>com.jobboard</groupId>
    <artifactId>job-board</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>job-board</name>

    <properties>
        <java.version>25</java.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <version>2.4.240</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-crypto</artifactId>
        </dependency>
        <dependency>
            <groupId>org.apache.pdfbox</groupId>
            <artifactId>pdfbox</artifactId>
            <version>3.0.8</version>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 4: `application.properties` 작성**

`backend/src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:h2:file:./data/jobboard;AUTO_SERVER=TRUE
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
server.port=8080

app.upload-dir=uploads

app.openai.api-key=${OPENAI_API_KEY:}
app.openai.model=gpt-4o-mini
app.openai.base-url=https://api.openai.com/v1

app.admin.seed-email=admin@jobboard.local
app.admin.seed-password=admin1234!
app.admin.seed-name=관리자
```

- [ ] **Step 5: 실패하는 헬스체크 테스트 작성**

`backend/src/test/java/com/jobboard/common/HealthControllerTest.java`:

```java
package com.jobboard.common;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void 헬스체크_엔드포인트는_ok를_반환한다() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }
}
```

- [ ] **Step 6: 테스트 실행하여 실패 확인**

Run: `cd backend && ./mvnw test -Dtest=HealthControllerTest` (mvnw가 없으므로 우선 `mvn -N io.takari:maven:wrapper` 또는 로컬 설치된 `mvn test -Dtest=HealthControllerTest` 사용)

Expected: FAIL (컴파일 에러 — `JobBoardApplication`, `HealthController` 없음)

- [ ] **Step 7: 최소 구현 작성**

`backend/src/main/java/com/jobboard/JobBoardApplication.java`:

```java
package com.jobboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class JobBoardApplication {
    public static void main(String[] args) {
        SpringApplication.run(JobBoardApplication.class, args);
    }
}
```

`backend/src/main/java/com/jobboard/common/HealthController.java`:

```java
package com.jobboard.common;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/api/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }
}
```

- [ ] **Step 8: 테스트 실행하여 통과 확인**

Run: `cd backend && mvn test -Dtest=HealthControllerTest`
Expected: PASS

- [ ] **Step 9: 프론트엔드 스캐폴딩**

```bash
cd /Users/hun/Summer_lecture_vibe_coding
npm create vite@latest frontend -- --template react
cd frontend
npm install
npm install react-router@8.3.0
npm install -D vitest@4.1.10 @testing-library/react@16.3.2 @testing-library/dom@10.4.0 @testing-library/jest-dom@6.6.3 jsdom@25.0.1
```

- [ ] **Step 10: Vite 프록시 설정**

`frontend/vite.config.js` (기존 생성 파일에 `server`, `test` 옵션 추가):

```js
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: './src/setupTests.js',
  },
})
```

`frontend/src/setupTests.js`:

```js
import '@testing-library/jest-dom'
```

- [ ] **Step 11: 프론트엔드 개발 서버 기동 확인 (수동 확인)**

Run: `cd frontend && npm run dev`
Expected: `http://localhost:5173` 에서 Vite 기본 React 페이지가 뜬다. 확인 후 서버 종료(Ctrl+C).

- [ ] **Step 12: 커밋**

```bash
cd /Users/hun/Summer_lecture_vibe_coding
git add .gitignore backend frontend
git commit -m "chore: 백엔드/프론트엔드 프로젝트 초기 스캐폴딩"
```

---

## Task 2: User 엔티티 + Repository

**Files:**
- Create: `backend/src/main/java/com/jobboard/user/UserRole.java`
- Create: `backend/src/main/java/com/jobboard/user/User.java`
- Create: `backend/src/main/java/com/jobboard/user/UserRepository.java`
- Test: `backend/src/test/java/com/jobboard/user/UserRepositoryTest.java`

**Interfaces:**
- Produces: `User` 엔티티 (`getId()`, `getEmail()`, `getPassword()`, `getName()`, `getRole()`, `getCreatedAt()`, setter 동일), `UserRole { USER, ADMIN }`, `UserRepository.findByEmail(String): Optional<User>`, `UserRepository.existsByEmail(String): boolean`

- [ ] **Step 1: 실패하는 리포지토리 테스트 작성**

`backend/src/test/java/com/jobboard/user/UserRepositoryTest.java`:

```java
package com.jobboard.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void 이메일로_회원을_조회한다() {
        User user = new User("test@jobboard.com", "encoded-pw", "홍길동", UserRole.USER);
        userRepository.save(user);

        assertThat(userRepository.findByEmail("test@jobboard.com")).isPresent();
        assertThat(userRepository.existsByEmail("test@jobboard.com")).isTrue();
        assertThat(userRepository.existsByEmail("none@jobboard.com")).isFalse();
    }
}
```

- [ ] **Step 2: 테스트 실행하여 실패 확인**

Run: `cd backend && mvn test -Dtest=UserRepositoryTest`
Expected: FAIL (컴파일 에러 — `User`, `UserRole`, `UserRepository` 없음)

- [ ] **Step 3: 구현 작성**

`backend/src/main/java/com/jobboard/user/UserRole.java`:

```java
package com.jobboard.user;

public enum UserRole {
    USER, ADMIN
}
```

`backend/src/main/java/com/jobboard/user/User.java`:

```java
package com.jobboard.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public User(String email, String password, String name, UserRole role) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.role = role;
    }
}
```

`backend/src/main/java/com/jobboard/user/UserRepository.java`:

```java
package com.jobboard.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
```

- [ ] **Step 4: 테스트 실행하여 통과 확인**

Run: `cd backend && mvn test -Dtest=UserRepositoryTest`
Expected: PASS

- [ ] **Step 5: 커밋**

```bash
git add backend/src/main/java/com/jobboard/user backend/src/test/java/com/jobboard/user
git commit -m "feat: User 엔티티와 Repository 추가"
```

---

## Task 3: 공통 예외 처리 (ApiException, GlobalExceptionHandler)

**Files:**
- Create: `backend/src/main/java/com/jobboard/common/ApiException.java`
- Create: `backend/src/main/java/com/jobboard/common/GlobalExceptionHandler.java`
- Test: `backend/src/test/java/com/jobboard/common/GlobalExceptionHandlerTest.java`

**Interfaces:**
- Produces: `ApiException(HttpStatus status, String message)` — 이후 서비스 레이어 전반에서 도메인 예외로 사용. `GlobalExceptionHandler`가 `ApiException`과 `MethodArgumentNotValidException`을 JSON `{"message": "..."}` 응답으로 변환.

- [ ] **Step 1: 실패하는 테스트 작성**

`backend/src/test/java/com/jobboard/common/GlobalExceptionHandlerTest.java`:

```java
package com.jobboard.common;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    @Test
    void ApiException을_상태코드와_메시지로_변환한다() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        ApiException exception = new ApiException(HttpStatus.CONFLICT, "이미 가입된 이메일입니다.");

        ResponseEntity<Map<String, String>> response = handler.handleApiException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).containsEntry("message", "이미 가입된 이메일입니다.");
    }
}
```

- [ ] **Step 2: 테스트 실행하여 실패 확인**

Run: `cd backend && mvn test -Dtest=GlobalExceptionHandlerTest`
Expected: FAIL (컴파일 에러)

- [ ] **Step 3: 구현 작성**

`backend/src/main/java/com/jobboard/common/ApiException.java`:

```java
package com.jobboard.common;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
```

`backend/src/main/java/com/jobboard/common/GlobalExceptionHandler.java`:

```java
package com.jobboard.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, String>> handleApiException(ApiException exception) {
        return ResponseEntity.status(exception.getStatus())
                .body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("입력값이 올바르지 않습니다.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", message));
    }
}
```

- [ ] **Step 4: 테스트 실행하여 통과 확인**

Run: `cd backend && mvn test -Dtest=GlobalExceptionHandlerTest`
Expected: PASS

- [ ] **Step 5: 커밋**

```bash
git add backend/src/main/java/com/jobboard/common backend/src/test/java/com/jobboard/common/GlobalExceptionHandlerTest.java
git commit -m "feat: 공통 예외 처리(ApiException, GlobalExceptionHandler) 추가"
```

---

## Task 4: UserService (회원가입/인증)

**Files:**
- Create: `backend/src/main/java/com/jobboard/common/SecurityConfig.java`
- Create: `backend/src/main/java/com/jobboard/user/UserService.java`
- Test: `backend/src/test/java/com/jobboard/user/UserServiceTest.java`

**Interfaces:**
- Consumes: `UserRepository` (Task 2), `ApiException` (Task 3)
- Produces: `PasswordEncoder` 빈, `UserService.register(String email, String rawPassword, String name): User`, `UserService.authenticate(String email, String rawPassword): User`, `UserService.findAll(): List<User>`, `UserService.updateRole(Long id, UserRole role): User`

- [ ] **Step 1: 실패하는 테스트 작성**

`backend/src/test/java/com/jobboard/user/UserServiceTest.java`:

```java
package com.jobboard.user;

import com.jobboard.common.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserServiceTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = new BCryptPasswordEncoder();
        userService = new UserService(userRepository, passwordEncoder);
    }

    @Test
    void 중복되지_않은_이메일로_회원가입에_성공한다() {
        when(userRepository.existsByEmail("new@jobboard.com")).thenReturn(false);
        when(userRepository.save(org.mockito.ArgumentMatchers.any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User user = userService.register("new@jobboard.com", "password123", "홍길동");

        assertThat(user.getEmail()).isEqualTo("new@jobboard.com");
        assertThat(user.getRole()).isEqualTo(UserRole.USER);
        assertThat(passwordEncoder.matches("password123", user.getPassword())).isTrue();
    }

    @Test
    void 이미_가입된_이메일이면_예외를_던진다() {
        when(userRepository.existsByEmail("dup@jobboard.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.register("dup@jobboard.com", "password123", "홍길동"))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void 올바른_비밀번호로_인증에_성공한다() {
        String encoded = passwordEncoder.encode("password123");
        User existing = new User("login@jobboard.com", encoded, "홍길동", UserRole.USER);
        when(userRepository.findByEmail("login@jobboard.com")).thenReturn(Optional.of(existing));

        User authenticated = userService.authenticate("login@jobboard.com", "password123");

        assertThat(authenticated.getEmail()).isEqualTo("login@jobboard.com");
    }

    @Test
    void 비밀번호가_틀리면_예외를_던진다() {
        String encoded = passwordEncoder.encode("password123");
        User existing = new User("login@jobboard.com", encoded, "홍길동", UserRole.USER);
        when(userRepository.findByEmail("login@jobboard.com")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> userService.authenticate("login@jobboard.com", "wrong-password"))
                .isInstanceOf(ApiException.class);
    }
}
```

- [ ] **Step 2: 테스트 실행하여 실패 확인**

Run: `cd backend && mvn test -Dtest=UserServiceTest`
Expected: FAIL (컴파일 에러 — `UserService` 없음)

- [ ] **Step 3: 구현 작성**

`backend/src/main/java/com/jobboard/common/SecurityConfig.java`:

```java
package com.jobboard.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

`backend/src/main/java/com/jobboard/user/UserService.java`:

```java
package com.jobboard.user;

import com.jobboard.common.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User register(String email, String rawPassword, String name) {
        if (userRepository.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "이미 가입된 이메일입니다.");
        }
        User user = new User(email, passwordEncoder.encode(rawPassword), name, UserRole.USER);
        return userRepository.save(user);
    }

    public User authenticate(String email, String rawPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."));
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다.");
        }
        return user;
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public User updateRole(Long id, UserRole role) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다."));
        user.setRole(role);
        return userRepository.save(user);
    }
}
```

- [ ] **Step 4: 테스트 실행하여 통과 확인**

Run: `cd backend && mvn test -Dtest=UserServiceTest`
Expected: PASS

- [ ] **Step 5: 커밋**

```bash
git add backend/src/main/java/com/jobboard/common/SecurityConfig.java backend/src/main/java/com/jobboard/user/UserService.java backend/src/test/java/com/jobboard/user/UserServiceTest.java
git commit -m "feat: UserService 회원가입/인증/권한변경 로직 추가"
```

---

## Task 5: 세션 인증 API + 인터셉터 (AuthController, AuthInterceptor, AdminInterceptor)

**Files:**
- Create: `backend/src/main/java/com/jobboard/common/SessionKeys.java`
- Create: `backend/src/main/java/com/jobboard/common/auth/AuthInterceptor.java`
- Create: `backend/src/main/java/com/jobboard/common/auth/AdminInterceptor.java`
- Create: `backend/src/main/java/com/jobboard/common/WebConfig.java`
- Create: `backend/src/main/java/com/jobboard/user/dto/RegisterRequest.java`
- Create: `backend/src/main/java/com/jobboard/user/dto/LoginRequest.java`
- Create: `backend/src/main/java/com/jobboard/user/dto/UserResponse.java`
- Create: `backend/src/main/java/com/jobboard/user/AuthController.java`
- Test: `backend/src/test/java/com/jobboard/user/AuthControllerTest.java`

**Interfaces:**
- Consumes: `UserService` (Task 4)
- Produces: `SessionKeys.{USER_ID,USER_ROLE,USER_NAME,USER_EMAIL}` (String 상수), `POST /api/auth/register`, `POST /api/auth/login`, `POST /api/auth/logout`, `GET /api/auth/me` — 이후 모든 `/api/**` 요청은 `AuthInterceptor`(로그인 필요) + `/api/admin/**`는 `AdminInterceptor`(ADMIN 필요)를 통과해야 함

- [ ] **Step 1: 실패하는 컨트롤러 테스트 작성**

`backend/src/test/java/com/jobboard/user/AuthControllerTest.java`:

```java
package com.jobboard.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void 회원가입_로그인_me_로그아웃_흐름이_동작한다() throws Exception {
        Map<String, String> registerBody = Map.of(
                "email", "flow@jobboard.com",
                "password", "password123",
                "name", "홍길동");
        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(registerBody)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("flow@jobboard.com"));

        Map<String, String> loginBody = Map.of("email", "flow@jobboard.com", "password", "password123");
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(loginBody)))
                .andExpect(status().isOk())
                .andReturn();

        var session = loginResult.getRequest().getSession(false);
        assertThat(session).isNotNull();

        mockMvc.perform(get("/api/auth/me").session((jakarta.servlet.http.MockHttpSession) session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("flow@jobboard.com"))
                .andExpect(jsonPath("$.role").value("USER"));

        mockMvc.perform(post("/api/auth/logout").session((jakarta.servlet.http.MockHttpSession) session))
                .andExpect(status().isNoContent());
    }

    @Test
    void 로그인하지_않고_me를_호출하면_401을_반환한다() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }
}
```

- [ ] **Step 2: 테스트 실행하여 실패 확인**

Run: `cd backend && mvn test -Dtest=AuthControllerTest`
Expected: FAIL (컴파일 에러 — DTO/컨트롤러 없음)

- [ ] **Step 3: 구현 작성**

`backend/src/main/java/com/jobboard/common/SessionKeys.java`:

```java
package com.jobboard.common;

public final class SessionKeys {
    public static final String USER_ID = "userId";
    public static final String USER_ROLE = "userRole";
    public static final String USER_NAME = "userName";
    public static final String USER_EMAIL = "userEmail";

    private SessionKeys() {
    }
}
```

`backend/src/main/java/com/jobboard/common/auth/AuthInterceptor.java`:

```java
package com.jobboard.common.auth;

import com.jobboard.common.SessionKeys;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.HandlerInterceptor;

public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute(SessionKeys.USER_ID) == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "로그인이 필요합니다.");
            return false;
        }
        return true;
    }
}
```

`backend/src/main/java/com/jobboard/common/auth/AdminInterceptor.java`:

```java
package com.jobboard.common.auth;

import com.jobboard.common.SessionKeys;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.HandlerInterceptor;

public class AdminInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession(false);
        Object role = session == null ? null : session.getAttribute(SessionKeys.USER_ROLE);
        if (!"ADMIN".equals(role)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "관리자만 접근할 수 있습니다.");
            return false;
        }
        return true;
    }
}
```

`backend/src/main/java/com/jobboard/common/WebConfig.java`:

```java
package com.jobboard.common;

import com.jobboard.common.auth.AdminInterceptor;
import com.jobboard.common.auth.AuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AuthInterceptor())
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/register", "/api/auth/login", "/api/health");
        registry.addInterceptor(new AdminInterceptor())
                .addPathPatterns("/api/admin/**");
    }
}
```

`backend/src/main/java/com/jobboard/user/dto/RegisterRequest.java`:

```java
package com.jobboard.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.") String password,
        @NotBlank String name
) {
}
```

`backend/src/main/java/com/jobboard/user/dto/LoginRequest.java`:

```java
package com.jobboard.user.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank String email,
        @NotBlank String password
) {
}
```

`backend/src/main/java/com/jobboard/user/dto/UserResponse.java`:

```java
package com.jobboard.user.dto;

import com.jobboard.user.User;

public record UserResponse(Long id, String email, String name, String role) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getName(), user.getRole().name());
    }
}
```

`backend/src/main/java/com/jobboard/user/AuthController.java`:

```java
package com.jobboard.user;

import com.jobboard.common.SessionKeys;
import com.jobboard.user.dto.LoginRequest;
import com.jobboard.user.dto.RegisterRequest;
import com.jobboard.user.dto.UserResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.register(request.email(), request.password(), request.name());
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user));
    }

    @PostMapping("/login")
    public UserResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        User user = userService.authenticate(request.email(), request.password());
        HttpSession session = httpRequest.getSession(true);
        session.setAttribute(SessionKeys.USER_ID, user.getId());
        session.setAttribute(SessionKeys.USER_ROLE, user.getRole().name());
        session.setAttribute(SessionKeys.USER_NAME, user.getName());
        session.setAttribute(SessionKeys.USER_EMAIL, user.getEmail());
        return UserResponse.from(user);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public UserResponse me(HttpServletRequest request) {
        HttpSession session = request.getSession();
        return new UserResponse(
                (Long) session.getAttribute(SessionKeys.USER_ID),
                (String) session.getAttribute(SessionKeys.USER_EMAIL),
                (String) session.getAttribute(SessionKeys.USER_NAME),
                (String) session.getAttribute(SessionKeys.USER_ROLE)
        );
    }
}
```

- [ ] **Step 4: 테스트 실행하여 통과 확인**

Run: `cd backend && mvn test -Dtest=AuthControllerTest`
Expected: PASS

- [ ] **Step 5: 커밋**

```bash
git add backend/src/main/java/com/jobboard/common backend/src/main/java/com/jobboard/user backend/src/test/java/com/jobboard/user/AuthControllerTest.java
git commit -m "feat: 세션 기반 회원가입/로그인/로그아웃 API와 인증 인터셉터 추가"
```

---

## Task 6: 관리자 시드 + 회원 관리 API (UserController)

**Files:**
- Create: `backend/src/main/java/com/jobboard/user/AdminSeeder.java`
- Create: `backend/src/main/java/com/jobboard/user/dto/RoleUpdateRequest.java`
- Create: `backend/src/main/java/com/jobboard/user/UserController.java`
- Test: `backend/src/test/java/com/jobboard/user/AdminSeederTest.java`
- Test: `backend/src/test/java/com/jobboard/user/UserControllerTest.java`

**Interfaces:**
- Consumes: `UserService` (Task 4), `SessionKeys`, `AdminInterceptor` (Task 5)
- Produces: `GET /api/admin/users`, `PATCH /api/admin/users/{id}/role`

- [ ] **Step 1: 실패하는 시더 테스트 작성**

`backend/src/test/java/com/jobboard/user/AdminSeederTest.java`:

```java
package com.jobboard.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AdminSeederTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void 애플리케이션_기동시_초기_관리자가_생성된다() {
        assertThat(userRepository.findByEmail("admin@jobboard.local"))
                .isPresent()
                .get()
                .satisfies(admin -> assertThat(admin.getRole()).isEqualTo(UserRole.ADMIN));
    }
}
```

- [ ] **Step 2: 테스트 실행하여 실패 확인**

Run: `cd backend && mvn test -Dtest=AdminSeederTest`
Expected: FAIL (관리자 계정이 존재하지 않음)

- [ ] **Step 3: AdminSeeder 구현**

`backend/src/main/java/com/jobboard/user/AdminSeeder.java`:

```java
package com.jobboard.user;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String seedEmail;
    private final String seedPassword;
    private final String seedName;

    public AdminSeeder(UserRepository userRepository,
                        PasswordEncoder passwordEncoder,
                        @Value("${app.admin.seed-email}") String seedEmail,
                        @Value("${app.admin.seed-password}") String seedPassword,
                        @Value("${app.admin.seed-name}") String seedName) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.seedEmail = seedEmail;
        this.seedPassword = seedPassword;
        this.seedName = seedName;
    }

    @Override
    public void run(String... args) {
        if (userRepository.existsByEmail(seedEmail)) {
            return;
        }
        User admin = new User(seedEmail, passwordEncoder.encode(seedPassword), seedName, UserRole.ADMIN);
        userRepository.save(admin);
    }
}
```

- [ ] **Step 4: 테스트 실행하여 통과 확인**

Run: `cd backend && mvn test -Dtest=AdminSeederTest`
Expected: PASS

- [ ] **Step 5: 실패하는 UserController 테스트 작성**

`backend/src/test/java/com/jobboard/user/UserControllerTest.java`:

```java
package com.jobboard.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private HttpSession loginAsAdmin() throws Exception {
        Map<String, String> loginBody = Map.of("email", "admin@jobboard.local", "password", "admin1234!");
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(loginBody)))
                .andExpect(status().isOk())
                .andReturn();
        return result.getRequest().getSession(false);
    }

    @Test
    void 관리자는_회원_목록을_조회하고_권한을_변경할_수_있다() throws Exception {
        HttpSession adminSession = loginAsAdmin();

        Map<String, String> registerBody = Map.of(
                "email", "member@jobboard.com", "password", "password123", "name", "회원1");
        mockMvc.perform(post("/api/auth/register")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(registerBody)));

        MvcResult listResult = mockMvc.perform(get("/api/admin/users")
                        .session((org.springframework.mock.web.MockHttpSession) adminSession))
                .andExpect(status().isOk())
                .andReturn();

        String body = listResult.getResponse().getContentAsString();
        com.fasterxml.jackson.databind.JsonNode users = objectMapper.readTree(body);
        long memberId = -1;
        for (var node : users) {
            if (node.get("email").asText().equals("member@jobboard.com")) {
                memberId = node.get("id").asLong();
            }
        }

        mockMvc.perform(patch("/api/admin/users/" + memberId + "/role")
                        .session((org.springframework.mock.web.MockHttpSession) adminSession)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("role", "ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void 일반회원은_회원_목록_조회시_403을_받는다() throws Exception {
        Map<String, String> registerBody = Map.of(
                "email", "plain@jobboard.com", "password", "password123", "name", "일반회원");
        mockMvc.perform(post("/api/auth/register")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(registerBody)));

        Map<String, String> loginBody = Map.of("email", "plain@jobboard.com", "password", "password123");
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(loginBody)))
                .andReturn();
        HttpSession session = loginResult.getRequest().getSession(false);

        mockMvc.perform(get("/api/admin/users")
                        .session((org.springframework.mock.web.MockHttpSession) session))
                .andExpect(status().isForbidden());
    }
}
```

- [ ] **Step 6: 테스트 실행하여 실패 확인**

Run: `cd backend && mvn test -Dtest=UserControllerTest`
Expected: FAIL (컴파일 에러 — `UserController`, `RoleUpdateRequest` 없음)

- [ ] **Step 7: 구현 작성**

`backend/src/main/java/com/jobboard/user/dto/RoleUpdateRequest.java`:

```java
package com.jobboard.user.dto;

import jakarta.validation.constraints.Pattern;

public record RoleUpdateRequest(
        @Pattern(regexp = "USER|ADMIN", message = "role은 USER 또는 ADMIN 이어야 합니다.") String role
) {
}
```

`backend/src/main/java/com/jobboard/user/UserController.java`:

```java
package com.jobboard.user;

import com.jobboard.user.dto.RoleUpdateRequest;
import com.jobboard.user.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<UserResponse> list() {
        return userService.findAll().stream().map(UserResponse::from).toList();
    }

    @PatchMapping("/{id}/role")
    public UserResponse updateRole(@PathVariable Long id, @Valid @RequestBody RoleUpdateRequest request) {
        User user = userService.updateRole(id, UserRole.valueOf(request.role()));
        return UserResponse.from(user);
    }
}
```

- [ ] **Step 8: 테스트 실행하여 통과 확인**

Run: `cd backend && mvn test -Dtest=UserControllerTest`
Expected: PASS

- [ ] **Step 9: 커밋**

```bash
git add backend/src/main/java/com/jobboard/user backend/src/test/java/com/jobboard/user/AdminSeederTest.java backend/src/test/java/com/jobboard/user/UserControllerTest.java
git commit -m "feat: 관리자 시드 및 회원 목록/권한변경 API 추가"
```

---

## Task 7: JobPosting 엔티티 + enum + Repository

**Files:**
- Create: `backend/src/main/java/com/jobboard/jobposting/CareerLevel.java`
- Create: `backend/src/main/java/com/jobboard/jobposting/EducationLevel.java`
- Create: `backend/src/main/java/com/jobboard/jobposting/EmploymentType.java`
- Create: `backend/src/main/java/com/jobboard/jobposting/JobPostingStatus.java`
- Create: `backend/src/main/java/com/jobboard/jobposting/JobPosting.java`
- Create: `backend/src/main/java/com/jobboard/jobposting/JobPostingRepository.java`
- Test: `backend/src/test/java/com/jobboard/jobposting/JobPostingRepositoryTest.java`

**Interfaces:**
- Consumes: `User` (Task 2)
- Produces: `JobPosting` 엔티티 (전체 필드 getter/setter), `CareerLevel{NEW,EXPERIENCED,ANY}`, `EducationLevel{NONE,HIGH_SCHOOL,ASSOCIATE,BACHELOR,MASTER}`, `EmploymentType{FULL_TIME,CONTRACT,INTERN,PART_TIME}`, `JobPostingStatus{ACTIVE,CLOSED}`, `JobPostingRepository` (JpaRepository + JpaSpecificationExecutor), `JobPostingRepository.findByStatusAndApplyEndDateBefore(JobPostingStatus, LocalDate): List<JobPosting>`

- [ ] **Step 1: 실패하는 리포지토리 테스트 작성**

`backend/src/test/java/com/jobboard/jobposting/JobPostingRepositoryTest.java`:

```java
package com.jobboard.jobposting;

import com.jobboard.user.User;
import com.jobboard.user.UserRepository;
import com.jobboard.user.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class JobPostingRepositoryTest {

    @Autowired
    private JobPostingRepository jobPostingRepository;

    @Autowired
    private UserRepository userRepository;

    private JobPosting newPosting(LocalDate endDate, JobPostingStatus status) {
        User admin = userRepository.save(new User("admin" + Math.random() + "@jobboard.com", "pw", "관리자", UserRole.ADMIN));
        JobPosting posting = new JobPosting();
        posting.setCompanyName("테스트회사");
        posting.setLocation("서울");
        posting.setCareerLevel(CareerLevel.NEW);
        posting.setEducation(EducationLevel.BACHELOR);
        posting.setEmploymentType(EmploymentType.FULL_TIME);
        posting.setConditionNote("우대사항 없음");
        posting.setApplyStartDate(LocalDate.now().minusDays(10));
        posting.setApplyEndDate(endDate);
        posting.setApplyMethod("이메일 접수");
        posting.setSalaryMin(3000);
        posting.setSalaryMax(3500);
        posting.setSalaryNote("협의가능");
        posting.setPdfFileName("sample.pdf");
        posting.setStatus(status);
        posting.setCreatedBy(admin);
        return posting;
    }

    @Test
    void 마감일이_지난_활성_공고를_조회한다() {
        jobPostingRepository.save(newPosting(LocalDate.now().minusDays(1), JobPostingStatus.ACTIVE));
        jobPostingRepository.save(newPosting(LocalDate.now().plusDays(10), JobPostingStatus.ACTIVE));
        jobPostingRepository.save(newPosting(LocalDate.now().minusDays(1), JobPostingStatus.CLOSED));

        List<JobPosting> expired = jobPostingRepository
                .findByStatusAndApplyEndDateBefore(JobPostingStatus.ACTIVE, LocalDate.now());

        assertThat(expired).hasSize(1);
    }
}
```

- [ ] **Step 2: 테스트 실행하여 실패 확인**

Run: `cd backend && mvn test -Dtest=JobPostingRepositoryTest`
Expected: FAIL (컴파일 에러)

- [ ] **Step 3: 구현 작성**

`backend/src/main/java/com/jobboard/jobposting/CareerLevel.java`:

```java
package com.jobboard.jobposting;

public enum CareerLevel {
    NEW, EXPERIENCED, ANY
}
```

`backend/src/main/java/com/jobboard/jobposting/EducationLevel.java`:

```java
package com.jobboard.jobposting;

public enum EducationLevel {
    NONE, HIGH_SCHOOL, ASSOCIATE, BACHELOR, MASTER
}
```

`backend/src/main/java/com/jobboard/jobposting/EmploymentType.java`:

```java
package com.jobboard.jobposting;

public enum EmploymentType {
    FULL_TIME, CONTRACT, INTERN, PART_TIME
}
```

`backend/src/main/java/com/jobboard/jobposting/JobPostingStatus.java`:

```java
package com.jobboard.jobposting;

public enum JobPostingStatus {
    ACTIVE, CLOSED
}
```

`backend/src/main/java/com/jobboard/jobposting/JobPosting.java`:

```java
package com.jobboard.jobposting;

import com.jobboard.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "job_postings")
@Getter
@Setter
@NoArgsConstructor
public class JobPosting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String companyName;

    @Column(nullable = false)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CareerLevel careerLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EducationLevel education;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmploymentType employmentType;

    @Column(columnDefinition = "TEXT")
    private String conditionNote;

    @Column(nullable = false)
    private LocalDate applyStartDate;

    @Column(nullable = false)
    private LocalDate applyEndDate;

    @Column(columnDefinition = "TEXT")
    private String applyMethod;

    private Integer salaryMin;

    private Integer salaryMax;

    private String salaryNote;

    @Column(nullable = false)
    private String pdfFileName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobPostingStatus status = JobPostingStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
}
```

`backend/src/main/java/com/jobboard/jobposting/JobPostingRepository.java`:

```java
package com.jobboard.jobposting;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.List;

public interface JobPostingRepository extends JpaRepository<JobPosting, Long>, JpaSpecificationExecutor<JobPosting> {
    List<JobPosting> findByStatusAndApplyEndDateBefore(JobPostingStatus status, LocalDate date);
}
```

- [ ] **Step 4: 테스트 실행하여 통과 확인**

Run: `cd backend && mvn test -Dtest=JobPostingRepositoryTest`
Expected: PASS

- [ ] **Step 5: 커밋**

```bash
git add backend/src/main/java/com/jobboard/jobposting backend/src/test/java/com/jobboard/jobposting
git commit -m "feat: JobPosting 엔티티, enum, Repository 추가"
```

---

## Task 8: FileStorageService (PDF 로컬 저장)

**Files:**
- Create: `backend/src/main/java/com/jobboard/jobposting/FileStorageService.java`
- Test: `backend/src/test/java/com/jobboard/jobposting/FileStorageServiceTest.java`

**Interfaces:**
- Consumes: `ApiException` (Task 3)
- Produces: `FileStorageService.store(MultipartFile): String` (저장된 파일명 반환), `FileStorageService.resolve(String): Path`

- [ ] **Step 1: 실패하는 테스트 작성**

`backend/src/test/java/com/jobboard/jobposting/FileStorageServiceTest.java`:

```java
package com.jobboard.jobposting;

import com.jobboard.common.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileStorageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void PDF_파일을_저장하고_경로를_반환한다() throws Exception {
        FileStorageService service = new FileStorageService(tempDir.toString());
        MockMultipartFile file = new MockMultipartFile("file", "posting.pdf", "application/pdf", "dummy-content".getBytes());

        String storedName = service.store(file);

        assertThat(storedName).endsWith(".pdf");
        assertThat(Files.exists(service.resolve(storedName))).isTrue();
    }

    @Test
    void PDF가_아닌_파일이면_예외를_던진다() {
        FileStorageService service = new FileStorageService(tempDir.toString());
        MockMultipartFile file = new MockMultipartFile("file", "posting.txt", "text/plain", "dummy".getBytes());

        assertThatThrownBy(() -> service.store(file)).isInstanceOf(ApiException.class);
    }
}
```

- [ ] **Step 2: 테스트 실행하여 실패 확인**

Run: `cd backend && mvn test -Dtest=FileStorageServiceTest`
Expected: FAIL (컴파일 에러)

- [ ] **Step 3: 구현 작성**

`backend/src/main/java/com/jobboard/jobposting/FileStorageService.java`:

```java
package com.jobboard.jobposting;

import com.jobboard.common.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;

@Component
public class FileStorageService {

    private final Path uploadDir;

    public FileStorageService(@Value("${app.upload-dir}") String uploadDir) {
        this.uploadDir = Path.of(uploadDir);
        try {
            Files.createDirectories(this.uploadDir);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public String store(MultipartFile file) {
        String original = file.getOriginalFilename();
        if (original == null || !original.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PDF 파일만 업로드할 수 있습니다.");
        }
        String storedName = UUID.randomUUID() + ".pdf";
        Path target = uploadDir.resolve(storedName);
        try {
            file.transferTo(target);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return storedName;
    }

    public Path resolve(String storedName) {
        return uploadDir.resolve(storedName);
    }
}
```

- [ ] **Step 4: 테스트 실행하여 통과 확인**

Run: `cd backend && mvn test -Dtest=FileStorageServiceTest`
Expected: PASS

- [ ] **Step 5: 커밋**

```bash
git add backend/src/main/java/com/jobboard/jobposting/FileStorageService.java backend/src/test/java/com/jobboard/jobposting/FileStorageServiceTest.java
git commit -m "feat: PDF 로컬 파일 저장 서비스 추가"
```

---

## Task 9: PdfTextExtractor (PDFBox)

**Files:**
- Create: `backend/src/main/java/com/jobboard/jobposting/PdfTextExtractor.java`
- Test: `backend/src/test/java/com/jobboard/jobposting/PdfTextExtractorTest.java`

**Interfaces:**
- Consumes: `ApiException` (Task 3)
- Produces: `PdfTextExtractor.extractText(Path): String`

- [ ] **Step 1: 실패하는 테스트 작성**

`backend/src/test/java/com/jobboard/jobposting/PdfTextExtractorTest.java`:

```java
package com.jobboard.jobposting;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class PdfTextExtractorTest {

    @TempDir
    Path tempDir;

    @Test
    void PDF에서_텍스트를_추출한다() throws Exception {
        Path pdfPath = tempDir.resolve("sample.pdf");
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                stream.beginText();
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                stream.newLineAtOffset(50, 700);
                stream.showText("테스트회사 채용공고");
                stream.endText();
            }
            document.save(pdfPath.toFile());
        }

        PdfTextExtractor extractor = new PdfTextExtractor();
        String text = extractor.extractText(pdfPath);

        assertThat(text).contains("테스트회사");
    }
}
```

- [ ] **Step 2: 테스트 실행하여 실패 확인**

Run: `cd backend && mvn test -Dtest=PdfTextExtractorTest`
Expected: FAIL (컴파일 에러)

- [ ] **Step 3: 구현 작성**

`backend/src/main/java/com/jobboard/jobposting/PdfTextExtractor.java`:

```java
package com.jobboard.jobposting;

import com.jobboard.common.ApiException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;

@Component
public class PdfTextExtractor {

    public String extractText(Path pdfPath) {
        try (PDDocument document = Loader.loadPDF(pdfPath.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        } catch (IOException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PDF 텍스트를 추출할 수 없습니다.");
        }
    }
}
```

- [ ] **Step 4: 테스트 실행하여 통과 확인**

Run: `cd backend && mvn test -Dtest=PdfTextExtractorTest`
Expected: PASS

- [ ] **Step 5: 커밋**

```bash
git add backend/src/main/java/com/jobboard/jobposting/PdfTextExtractor.java backend/src/test/java/com/jobboard/jobposting/PdfTextExtractorTest.java
git commit -m "feat: PDFBox 기반 PDF 텍스트 추출기 추가"
```

---

## Task 10: OpenAiJobExtractionClient (OpenAI 연동 구조화 추출)

**Files:**
- Create: `backend/src/main/java/com/jobboard/jobposting/OpenAiProperties.java`
- Create: `backend/src/main/java/com/jobboard/jobposting/dto/PdfExtractionResult.java`
- Create: `backend/src/main/java/com/jobboard/jobposting/OpenAiJobExtractionClient.java`
- Test: `backend/src/test/java/com/jobboard/jobposting/OpenAiJobExtractionClientTest.java`

**Interfaces:**
- Consumes: `ApiException` (Task 3)
- Produces: `PdfExtractionResult` record(`pdfFileName,companyName,location,careerLevel,education,employmentType,conditionNote,applyStartDate,applyEndDate,applyMethod,salaryMin,salaryMax,salaryNote` 모두 String, salaryMin/Max는 Integer), `OpenAiJobExtractionClient.extract(String pdfText): PdfExtractionResult` (pdfFileName은 항상 null로 반환 — 호출자가 채워 넣음)

- [ ] **Step 1: 실패하는 테스트 작성**

`backend/src/test/java/com/jobboard/jobposting/OpenAiJobExtractionClientTest.java`:

```java
package com.jobboard.jobposting;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobboard.jobposting.dto.PdfExtractionResult;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OpenAiJobExtractionClientTest {

    private static final String AI_RESPONSE_JSON = """
            {
              "choices": [
                {
                  "message": {
                    "content": "{\\"companyName\\":\\"테스트회사\\",\\"location\\":\\"서울\\",\\"careerLevel\\":\\"NEW\\",\\"education\\":\\"BACHELOR\\",\\"employmentType\\":\\"FULL_TIME\\",\\"conditionNote\\":\\"우대사항 없음\\",\\"applyStartDate\\":\\"2026-08-01\\",\\"applyEndDate\\":\\"2026-08-31\\",\\"applyMethod\\":\\"이메일 접수\\",\\"salaryMin\\":3000,\\"salaryMax\\":3500,\\"salaryNote\\":\\"협의가능\\"}"
                  }
                }
              ]
            }
            """;

    @Test
    void PDF_텍스트로부터_구조화된_정보를_추출한다() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        OpenAiProperties properties = new OpenAiProperties();
        properties.setApiKey("test-key");
        properties.setModel("gpt-4o-mini");
        properties.setBaseUrl("https://api.openai.com/v1");

        server.expect(requestTo("https://api.openai.com/v1/chat/completions"))
                .andRespond(withSuccess(AI_RESPONSE_JSON, MediaType.APPLICATION_JSON));

        OpenAiJobExtractionClient client = new OpenAiJobExtractionClient(builder, properties, new ObjectMapper());

        PdfExtractionResult result = client.extract("테스트회사 채용공고 텍스트");

        assertThat(result.companyName()).isEqualTo("테스트회사");
        assertThat(result.salaryMin()).isEqualTo(3000);
        assertThat(result.applyEndDate()).isEqualTo("2026-08-31");
    }
}
```

- [ ] **Step 2: 테스트 실행하여 실패 확인**

Run: `cd backend && mvn test -Dtest=OpenAiJobExtractionClientTest`
Expected: FAIL (컴파일 에러)

- [ ] **Step 3: 구현 작성**

`backend/src/main/java/com/jobboard/jobposting/OpenAiProperties.java`:

```java
package com.jobboard.jobposting;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.openai")
@Getter
@Setter
public class OpenAiProperties {
    private String apiKey;
    private String model;
    private String baseUrl;
}
```

`backend/src/main/java/com/jobboard/jobposting/dto/PdfExtractionResult.java`:

```java
package com.jobboard.jobposting.dto;

public record PdfExtractionResult(
        String pdfFileName,
        String companyName,
        String location,
        String careerLevel,
        String education,
        String employmentType,
        String conditionNote,
        String applyStartDate,
        String applyEndDate,
        String applyMethod,
        Integer salaryMin,
        Integer salaryMax,
        String salaryNote
) {
}
```

`backend/src/main/java/com/jobboard/jobposting/OpenAiJobExtractionClient.java`:

```java
package com.jobboard.jobposting;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobboard.common.ApiException;
import com.jobboard.jobposting.dto.PdfExtractionResult;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class OpenAiJobExtractionClient {

    private static final String SYSTEM_PROMPT = """
            당신은 채용공고 PDF 텍스트에서 정보를 추출하는 도우미입니다.
            반드시 아래 JSON 형식으로만 응답하세요. 알 수 없는 값은 빈 문자열 또는 0을 사용하세요.
            {
              "companyName": "string",
              "location": "string",
              "careerLevel": "NEW|EXPERIENCED|ANY",
              "education": "NONE|HIGH_SCHOOL|ASSOCIATE|BACHELOR|MASTER",
              "employmentType": "FULL_TIME|CONTRACT|INTERN|PART_TIME",
              "conditionNote": "string",
              "applyStartDate": "yyyy-MM-dd",
              "applyEndDate": "yyyy-MM-dd",
              "applyMethod": "string",
              "salaryMin": 0,
              "salaryMax": 0,
              "salaryNote": "string"
            }
            """;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String model;

    public OpenAiJobExtractionClient(RestClient.Builder builder, OpenAiProperties properties, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.model = properties.getModel();
        this.restClient = builder
                .baseUrl(properties.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + properties.getApiKey())
                .build();
    }

    public PdfExtractionResult extract(String pdfText) {
        Map<String, Object> requestBody = Map.of(
                "model", model,
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        Map.of("role", "system", "content", SYSTEM_PROMPT),
                        Map.of("role", "user", "content", pdfText)
                )
        );

        Map<String, Object> response = restClient.post()
                .uri("/chat/completions")
                .body(requestBody)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        String content = extractContent(response);
        try {
            return objectMapper.readValue(content, PdfExtractionResult.class);
        } catch (JsonProcessingException e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "채용정보 추출 결과를 해석할 수 없습니다.");
        }
    }

    @SuppressWarnings("unchecked")
    private String extractContent(Map<String, Object> response) {
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        return (String) message.get("content");
    }
}
```

- [ ] **Step 4: 테스트 실행하여 통과 확인**

Run: `cd backend && mvn test -Dtest=OpenAiJobExtractionClientTest`
Expected: PASS

- [ ] **Step 5: 커밋**

```bash
git add backend/src/main/java/com/jobboard/jobposting/OpenAiProperties.java backend/src/main/java/com/jobboard/jobposting/dto/PdfExtractionResult.java backend/src/main/java/com/jobboard/jobposting/OpenAiJobExtractionClient.java backend/src/test/java/com/jobboard/jobposting/OpenAiJobExtractionClientTest.java
git commit -m "feat: OpenAI API 연동 채용정보 구조화 추출 클라이언트 추가"
```

---

## Task 11: JobPostingService + 관리자 PDF 업로드/등록 API

**Files:**
- Create: `backend/src/main/java/com/jobboard/jobposting/dto/JobPostingCreateRequest.java`
- Create: `backend/src/main/java/com/jobboard/jobposting/dto/JobPostingResponse.java`
- Create: `backend/src/main/java/com/jobboard/jobposting/dto/PdfExtractionResponse.java`
- Create: `backend/src/main/java/com/jobboard/jobposting/JobPostingService.java`
- Create: `backend/src/main/java/com/jobboard/jobposting/AdminJobPostingController.java`
- Test: `backend/src/test/java/com/jobboard/jobposting/JobPostingServiceTest.java`
- Test: `backend/src/test/java/com/jobboard/jobposting/AdminJobPostingControllerTest.java`

**Interfaces:**
- Consumes: `FileStorageService`(Task 8), `PdfTextExtractor`(Task 9), `OpenAiJobExtractionClient`(Task 10), `UserRepository`(Task 2), `JobPostingRepository`(Task 7)
- Produces: `JobPostingService.extractFromPdf(MultipartFile): PdfExtractionResult`, `JobPostingService.create(JobPostingCreateRequest, Long adminId): JobPosting`, `POST /api/admin/job-postings/extract`(multipart), `POST /api/admin/job-postings`

- [ ] **Step 1: 실패하는 서비스 테스트 작성**

`backend/src/test/java/com/jobboard/jobposting/JobPostingServiceTest.java`:

```java
package com.jobboard.jobposting;

import com.jobboard.jobposting.dto.JobPostingCreateRequest;
import com.jobboard.jobposting.dto.PdfExtractionResult;
import com.jobboard.user.User;
import com.jobboard.user.UserRepository;
import com.jobboard.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JobPostingServiceTest {

    private FileStorageService fileStorageService;
    private PdfTextExtractor pdfTextExtractor;
    private OpenAiJobExtractionClient extractionClient;
    private JobPostingRepository jobPostingRepository;
    private UserRepository userRepository;
    private JobPostingService jobPostingService;

    @BeforeEach
    void setUp() {
        fileStorageService = mock(FileStorageService.class);
        pdfTextExtractor = mock(PdfTextExtractor.class);
        extractionClient = mock(OpenAiJobExtractionClient.class);
        jobPostingRepository = mock(JobPostingRepository.class);
        userRepository = mock(UserRepository.class);
        jobPostingService = new JobPostingService(
                jobPostingRepository, userRepository, fileStorageService, pdfTextExtractor, extractionClient);
    }

    @Test
    void PDF를_업로드하면_저장후_추출결과에_파일명을_채워_반환한다() {
        MockMultipartFile file = new MockMultipartFile("file", "posting.pdf", "application/pdf", "content".getBytes());
        when(fileStorageService.store(file)).thenReturn("stored-uuid.pdf");
        when(fileStorageService.resolve("stored-uuid.pdf")).thenReturn(Path.of("stored-uuid.pdf"));
        when(pdfTextExtractor.extractText(any())).thenReturn("추출된 텍스트");
        when(extractionClient.extract("추출된 텍스트")).thenReturn(new PdfExtractionResult(
                null, "테스트회사", "서울", "NEW", "BACHELOR", "FULL_TIME", "비고",
                "2026-08-01", "2026-08-31", "이메일 접수", 3000, 3500, "협의가능"));

        PdfExtractionResult result = jobPostingService.extractFromPdf(file);

        assertThat(result.pdfFileName()).isEqualTo("stored-uuid.pdf");
        assertThat(result.companyName()).isEqualTo("테스트회사");
    }

    @Test
    void 검토된_정보로_채용공고를_등록한다() {
        User admin = new User("admin@jobboard.com", "pw", "관리자", UserRole.ADMIN);
        when(userRepository.getReferenceById(1L)).thenReturn(admin);
        when(jobPostingRepository.save(any(JobPosting.class))).thenAnswer(invocation -> invocation.getArgument(0));

        JobPostingCreateRequest request = new JobPostingCreateRequest(
                "stored-uuid.pdf", "테스트회사", "서울", CareerLevel.NEW, EducationLevel.BACHELOR,
                EmploymentType.FULL_TIME, "비고", LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31),
                "이메일 접수", 3000, 3500, "협의가능");

        JobPosting posting = jobPostingService.create(request, 1L);

        assertThat(posting.getCompanyName()).isEqualTo("테스트회사");
        assertThat(posting.getStatus()).isEqualTo(JobPostingStatus.ACTIVE);
        assertThat(posting.getCreatedBy()).isEqualTo(admin);
    }
}
```

- [ ] **Step 2: 테스트 실행하여 실패 확인**

Run: `cd backend && mvn test -Dtest=JobPostingServiceTest`
Expected: FAIL (컴파일 에러 — DTO/서비스 없음)

- [ ] **Step 3: DTO 및 서비스 구현**

`backend/src/main/java/com/jobboard/jobposting/dto/JobPostingCreateRequest.java`:

```java
package com.jobboard.jobposting.dto;

import com.jobboard.jobposting.CareerLevel;
import com.jobboard.jobposting.EducationLevel;
import com.jobboard.jobposting.EmploymentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record JobPostingCreateRequest(
        @NotBlank String pdfFileName,
        @NotBlank String companyName,
        @NotBlank String location,
        @NotNull CareerLevel careerLevel,
        @NotNull EducationLevel education,
        @NotNull EmploymentType employmentType,
        String conditionNote,
        @NotNull LocalDate applyStartDate,
        @NotNull LocalDate applyEndDate,
        String applyMethod,
        Integer salaryMin,
        Integer salaryMax,
        String salaryNote
) {
}
```

`backend/src/main/java/com/jobboard/jobposting/dto/JobPostingResponse.java`:

```java
package com.jobboard.jobposting.dto;

import com.jobboard.jobposting.CareerLevel;
import com.jobboard.jobposting.EducationLevel;
import com.jobboard.jobposting.EmploymentType;
import com.jobboard.jobposting.JobPosting;
import com.jobboard.jobposting.JobPostingStatus;

import java.time.LocalDate;

public record JobPostingResponse(
        Long id,
        String companyName,
        String location,
        CareerLevel careerLevel,
        EducationLevel education,
        EmploymentType employmentType,
        String conditionNote,
        LocalDate applyStartDate,
        LocalDate applyEndDate,
        String applyMethod,
        Integer salaryMin,
        Integer salaryMax,
        String salaryNote,
        JobPostingStatus status
) {
    public static JobPostingResponse from(JobPosting posting) {
        return new JobPostingResponse(
                posting.getId(), posting.getCompanyName(), posting.getLocation(), posting.getCareerLevel(),
                posting.getEducation(), posting.getEmploymentType(), posting.getConditionNote(),
                posting.getApplyStartDate(), posting.getApplyEndDate(), posting.getApplyMethod(),
                posting.getSalaryMin(), posting.getSalaryMax(), posting.getSalaryNote(), posting.getStatus());
    }
}
```

`backend/src/main/java/com/jobboard/jobposting/dto/PdfExtractionResponse.java`:

```java
package com.jobboard.jobposting.dto;

public record PdfExtractionResponse(
        String pdfFileName,
        String companyName,
        String location,
        String careerLevel,
        String education,
        String employmentType,
        String conditionNote,
        String applyStartDate,
        String applyEndDate,
        String applyMethod,
        Integer salaryMin,
        Integer salaryMax,
        String salaryNote
) {
    public static PdfExtractionResponse from(PdfExtractionResult result) {
        return new PdfExtractionResponse(
                result.pdfFileName(), result.companyName(), result.location(), result.careerLevel(),
                result.education(), result.employmentType(), result.conditionNote(), result.applyStartDate(),
                result.applyEndDate(), result.applyMethod(), result.salaryMin(), result.salaryMax(), result.salaryNote());
    }
}
```

`backend/src/main/java/com/jobboard/jobposting/JobPostingService.java`:

```java
package com.jobboard.jobposting;

import com.jobboard.common.ApiException;
import com.jobboard.jobposting.dto.JobPostingCreateRequest;
import com.jobboard.jobposting.dto.PdfExtractionResult;
import com.jobboard.user.User;
import com.jobboard.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class JobPostingService {

    private final JobPostingRepository jobPostingRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final PdfTextExtractor pdfTextExtractor;
    private final OpenAiJobExtractionClient extractionClient;

    public JobPostingService(JobPostingRepository jobPostingRepository,
                              UserRepository userRepository,
                              FileStorageService fileStorageService,
                              PdfTextExtractor pdfTextExtractor,
                              OpenAiJobExtractionClient extractionClient) {
        this.jobPostingRepository = jobPostingRepository;
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
        this.pdfTextExtractor = pdfTextExtractor;
        this.extractionClient = extractionClient;
    }

    public PdfExtractionResult extractFromPdf(MultipartFile file) {
        String storedName = fileStorageService.store(file);
        String text = pdfTextExtractor.extractText(fileStorageService.resolve(storedName));
        PdfExtractionResult aiResult = extractionClient.extract(text);
        return new PdfExtractionResult(
                storedName, aiResult.companyName(), aiResult.location(), aiResult.careerLevel(),
                aiResult.education(), aiResult.employmentType(), aiResult.conditionNote(),
                aiResult.applyStartDate(), aiResult.applyEndDate(), aiResult.applyMethod(),
                aiResult.salaryMin(), aiResult.salaryMax(), aiResult.salaryNote());
    }

    public JobPosting create(JobPostingCreateRequest request, Long adminId) {
        User admin = userRepository.getReferenceById(adminId);

        JobPosting posting = new JobPosting();
        posting.setCompanyName(request.companyName());
        posting.setLocation(request.location());
        posting.setCareerLevel(request.careerLevel());
        posting.setEducation(request.education());
        posting.setEmploymentType(request.employmentType());
        posting.setConditionNote(request.conditionNote());
        posting.setApplyStartDate(request.applyStartDate());
        posting.setApplyEndDate(request.applyEndDate());
        posting.setApplyMethod(request.applyMethod());
        posting.setSalaryMin(request.salaryMin());
        posting.setSalaryMax(request.salaryMax());
        posting.setSalaryNote(request.salaryNote());
        posting.setPdfFileName(request.pdfFileName());
        posting.setCreatedBy(admin);

        return jobPostingRepository.save(posting);
    }

    public JobPosting update(Long id, JobPostingCreateRequest request) {
        JobPosting posting = jobPostingRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "채용공고를 찾을 수 없습니다."));
        posting.setCompanyName(request.companyName());
        posting.setLocation(request.location());
        posting.setCareerLevel(request.careerLevel());
        posting.setEducation(request.education());
        posting.setEmploymentType(request.employmentType());
        posting.setConditionNote(request.conditionNote());
        posting.setApplyStartDate(request.applyStartDate());
        posting.setApplyEndDate(request.applyEndDate());
        posting.setApplyMethod(request.applyMethod());
        posting.setSalaryMin(request.salaryMin());
        posting.setSalaryMax(request.salaryMax());
        posting.setSalaryNote(request.salaryNote());
        posting.setUpdatedAt(LocalDateTime.now());
        return jobPostingRepository.save(posting);
    }

    public void delete(Long id) {
        if (!jobPostingRepository.existsById(id)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "채용공고를 찾을 수 없습니다.");
        }
        jobPostingRepository.deleteById(id);
    }

    public List<JobPosting> findAllForAdmin() {
        return jobPostingRepository.findAll();
    }

    public JobPosting getById(Long id) {
        return jobPostingRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "채용공고를 찾을 수 없습니다."));
    }

    public Page<JobPosting> search(String keyword, String location, EmploymentType employmentType, Pageable pageable) {
        Specification<JobPosting> spec = Specification
                .where(JobPostingSpecifications.status(JobPostingStatus.ACTIVE))
                .and(JobPostingSpecifications.keyword(keyword))
                .and(JobPostingSpecifications.location(location))
                .and(JobPostingSpecifications.employmentType(employmentType));
        return jobPostingRepository.findAll(spec, pageable);
    }

    public void closeExpired() {
        LocalDate today = LocalDate.now();
        List<JobPosting> expired = jobPostingRepository
                .findByStatusAndApplyEndDateBefore(JobPostingStatus.ACTIVE, today);
        expired.forEach(posting -> posting.setStatus(JobPostingStatus.CLOSED));
        jobPostingRepository.saveAll(expired);
    }
}
```

`backend/src/main/java/com/jobboard/jobposting/JobPostingSpecifications.java` (검색/필터 조합용, Task 12에서 실제로 사용):

```java
package com.jobboard.jobposting;

import org.springframework.data.jpa.domain.Specification;

public final class JobPostingSpecifications {

    private JobPostingSpecifications() {
    }

    public static Specification<JobPosting> keyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        String pattern = "%" + keyword.toLowerCase() + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("companyName")), pattern);
    }

    public static Specification<JobPosting> location(String location) {
        if (location == null || location.isBlank()) {
            return null;
        }
        String pattern = "%" + location.toLowerCase() + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("location")), pattern);
    }

    public static Specification<JobPosting> employmentType(EmploymentType employmentType) {
        if (employmentType == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("employmentType"), employmentType);
    }

    public static Specification<JobPosting> status(JobPostingStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }
}
```

`backend/src/main/java/com/jobboard/jobposting/AdminJobPostingController.java`:

```java
package com.jobboard.jobposting;

import com.jobboard.common.SessionKeys;
import com.jobboard.jobposting.dto.JobPostingCreateRequest;
import com.jobboard.jobposting.dto.JobPostingResponse;
import com.jobboard.jobposting.dto.PdfExtractionResponse;
import com.jobboard.jobposting.dto.PdfExtractionResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/admin/job-postings")
public class AdminJobPostingController {

    private final JobPostingService jobPostingService;

    public AdminJobPostingController(JobPostingService jobPostingService) {
        this.jobPostingService = jobPostingService;
    }

    @PostMapping(value = "/extract", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PdfExtractionResponse extract(@RequestParam("file") MultipartFile file) {
        PdfExtractionResult result = jobPostingService.extractFromPdf(file);
        return PdfExtractionResponse.from(result);
    }

    @PostMapping
    public JobPostingResponse create(@Valid @RequestBody JobPostingCreateRequest request, HttpServletRequest httpRequest) {
        Long adminId = (Long) httpRequest.getSession().getAttribute(SessionKeys.USER_ID);
        JobPosting posting = jobPostingService.create(request, adminId);
        return JobPostingResponse.from(posting);
    }

    @GetMapping
    public List<JobPostingResponse> list() {
        return jobPostingService.findAllForAdmin().stream().map(JobPostingResponse::from).toList();
    }

    @PutMapping("/{id}")
    public JobPostingResponse update(@PathVariable Long id, @Valid @RequestBody JobPostingCreateRequest request) {
        return JobPostingResponse.from(jobPostingService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        jobPostingService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

`PdfExtractionResponse.from`이 참조하는 `PdfExtractionResult`는 `com.jobboard.jobposting.dto` 패키지에 있으므로 `PdfExtractionResponse.java` 상단에 `import com.jobboard.jobposting.dto.PdfExtractionResult;` 를 추가로 확인한다 (Step 3에서 이미 같은 패키지이므로 import 불필요 — 실제로는 같은 패키지이니 그대로 둔다).

- [ ] **Step 4: 테스트 실행하여 통과 확인 (서비스 단위테스트)**

Run: `cd backend && mvn test -Dtest=JobPostingServiceTest`
Expected: PASS

- [ ] **Step 5: 실패하는 컨트롤러 통합 테스트 작성**

`backend/src/test/java/com/jobboard/jobposting/AdminJobPostingControllerTest.java`:

```java
package com.jobboard.jobposting;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminJobPostingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OpenAiJobExtractionClient extractionClient;

    private MockHttpSession loginAsAdmin() throws Exception {
        Map<String, String> loginBody = Map.of("email", "admin@jobboard.local", "password", "admin1234!");
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(loginBody)))
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    @Test
    void 관리자가_PDF를_업로드하면_추출결과를_받는다() throws Exception {
        MockHttpSession session = loginAsAdmin();
        when(extractionClient.extract(any())).thenReturn(new com.jobboard.jobposting.dto.PdfExtractionResult(
                null, "테스트회사", "서울", "NEW", "BACHELOR", "FULL_TIME", "비고",
                "2026-08-01", "2026-08-31", "이메일 접수", 3000, 3500, "협의가능"));

        MockMultipartFile file = new MockMultipartFile("file", "posting.pdf", "application/pdf", "dummy".getBytes());

        mockMvc.perform(multipart("/api/admin/job-postings/extract").file(file).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyName").value("테스트회사"))
                .andExpect(jsonPath("$.pdfFileName").isNotEmpty());
    }
}
```

- [ ] **Step 6: 테스트 실행하여 통과 확인**

Run: `cd backend && mvn test -Dtest=AdminJobPostingControllerTest`
Expected: PASS (필요 시 `OpenAiJobExtractionClient`가 실제 스프링 빈으로 등록되어 있는지 확인 — Task 10에서 `@Component`로 이미 등록됨)

- [ ] **Step 7: 커밋**

```bash
git add backend/src/main/java/com/jobboard/jobposting backend/src/test/java/com/jobboard/jobposting/JobPostingServiceTest.java backend/src/test/java/com/jobboard/jobposting/AdminJobPostingControllerTest.java
git commit -m "feat: PDF 업로드/추출 및 관리자 채용공고 등록 API 추가"
```

---

## Task 12: 관리자 채용공고 수정/삭제 (기존 API 보강 + 테스트)

**Files:**
- Modify: `backend/src/test/java/com/jobboard/jobposting/AdminJobPostingControllerTest.java`

**Interfaces:**
- Consumes: `AdminJobPostingController.update/delete` (Task 11에서 이미 구현됨)

Task 11에서 `AdminJobPostingController`의 update/delete 엔드포인트 구현까지 이미 완료했으므로, 이 태스크는 수정/삭제 흐름에 대한 통합 테스트만 추가한다.

- [ ] **Step 1: 실패하는 테스트 추가**

`backend/src/test/java/com/jobboard/jobposting/AdminJobPostingControllerTest.java`에 아래 테스트 메서드 추가:

```java
    @Test
    void 관리자가_채용공고를_등록_수정_삭제한다() throws Exception {
        MockHttpSession session = loginAsAdmin();
        when(extractionClient.extract(any())).thenReturn(new com.jobboard.jobposting.dto.PdfExtractionResult(
                null, "테스트회사", "서울", "NEW", "BACHELOR", "FULL_TIME", "비고",
                "2026-08-01", "2026-08-31", "이메일 접수", 3000, 3500, "협의가능"));

        Map<String, Object> createBody = Map.of(
                "pdfFileName", "sample.pdf", "companyName", "테스트회사", "location", "서울",
                "careerLevel", "NEW", "education", "BACHELOR", "employmentType", "FULL_TIME",
                "conditionNote", "비고", "applyStartDate", "2026-08-01", "applyEndDate", "2026-08-31",
                "applyMethod", "이메일 접수", "salaryMin", 3000, "salaryMax", 3500, "salaryNote", "협의가능");

        MvcResult createResult = mockMvc.perform(post("/api/admin/job-postings")
                        .session(session)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(createBody)))
                .andExpect(status().isOk())
                .andReturn();

        long id = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        Map<String, Object> updateBody = Map.of(
                "pdfFileName", "sample.pdf", "companyName", "수정된회사", "location", "부산",
                "careerLevel", "EXPERIENCED", "education", "MASTER", "employmentType", "CONTRACT",
                "conditionNote", "수정된 비고", "applyStartDate", "2026-09-01", "applyEndDate", "2026-09-30",
                "applyMethod", "우편 접수", "salaryMin", 4000, "salaryMax", 4500, "salaryNote", "면접후 결정");

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
                        "/api/admin/job-postings/" + id)
                        .session(session)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(updateBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyName").value("수정된회사"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(
                        "/api/admin/job-postings/" + id)
                        .session(session))
                .andExpect(status().isNoContent());
    }
```

- [ ] **Step 2: 테스트 실행하여 실패 확인 (있다면)**

Run: `cd backend && mvn test -Dtest=AdminJobPostingControllerTest`
Expected: Task 11 구현이 맞다면 이미 PASS일 수 있음 — 만약 실패한다면 `AdminJobPostingController`/`JobPostingService`의 update/delete 로직을 점검한다.

- [ ] **Step 3: 통과 확인**

Run: `cd backend && mvn test -Dtest=AdminJobPostingControllerTest`
Expected: PASS

- [ ] **Step 4: 커밋**

```bash
git add backend/src/test/java/com/jobboard/jobposting/AdminJobPostingControllerTest.java
git commit -m "test: 관리자 채용공고 수정/삭제 통합 테스트 추가"
```

---

## Task 13: 공개 채용공고 목록(검색/필터)/상세 조회 API

**Files:**
- Create: `backend/src/main/java/com/jobboard/jobposting/JobPostingController.java`
- Test: `backend/src/test/java/com/jobboard/jobposting/JobPostingControllerTest.java`

**Interfaces:**
- Consumes: `JobPostingService.search/getById` (Task 11)
- Produces: `GET /api/job-postings?keyword=&location=&employmentType=&page=&size=`, `GET /api/job-postings/{id}`

- [ ] **Step 1: 실패하는 테스트 작성**

`backend/src/test/java/com/jobboard/jobposting/JobPostingControllerTest.java`:

```java
package com.jobboard.jobposting;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class JobPostingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private MockHttpSession loginAsUser(String email) throws Exception {
        Map<String, String> registerBody = Map.of("email", email, "password", "password123", "name", "회원");
        mockMvc.perform(post("/api/auth/register")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(registerBody)));

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", "password123"))))
                .andReturn();
        return (MockHttpSession) loginResult.getRequest().getSession(false);
    }

    @Test
    void 로그인한_회원은_채용공고_목록을_조회할_수_있다() throws Exception {
        MockHttpSession session = loginAsUser("list-view@jobboard.com");

        mockMvc.perform(get("/api/job-postings").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void 존재하지_않는_공고_조회시_404를_반환한다() throws Exception {
        MockHttpSession session = loginAsUser("detail-view@jobboard.com");

        mockMvc.perform(get("/api/job-postings/999999").session(session))
                .andExpect(status().isNotFound());
    }

    @Test
    void 비로그인_사용자는_목록_조회시_401을_받는다() throws Exception {
        mockMvc.perform(get("/api/job-postings"))
                .andExpect(status().isUnauthorized());
    }
}
```

- [ ] **Step 2: 테스트 실행하여 실패 확인**

Run: `cd backend && mvn test -Dtest=JobPostingControllerTest`
Expected: FAIL (컴파일 에러 — `JobPostingController` 없음)

- [ ] **Step 3: 구현 작성**

`backend/src/main/java/com/jobboard/jobposting/JobPostingController.java`:

```java
package com.jobboard.jobposting;

import com.jobboard.jobposting.dto.JobPostingResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/job-postings")
public class JobPostingController {

    private final JobPostingService jobPostingService;

    public JobPostingController(JobPostingService jobPostingService) {
        this.jobPostingService = jobPostingService;
    }

    @GetMapping
    public Page<JobPostingResponse> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) EmploymentType employmentType,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return jobPostingService.search(keyword, location, employmentType, pageable).map(JobPostingResponse::from);
    }

    @GetMapping("/{id}")
    public JobPostingResponse detail(@PathVariable Long id) {
        return JobPostingResponse.from(jobPostingService.getById(id));
    }
}
```

- [ ] **Step 4: 테스트 실행하여 통과 확인**

Run: `cd backend && mvn test -Dtest=JobPostingControllerTest`
Expected: PASS

- [ ] **Step 5: 커밋**

```bash
git add backend/src/main/java/com/jobboard/jobposting/JobPostingController.java backend/src/test/java/com/jobboard/jobposting/JobPostingControllerTest.java
git commit -m "feat: 공개 채용공고 검색/필터/상세 조회 API 추가"
```

---

## Task 14: 만료 공고 자동 마감 스케줄러

**Files:**
- Create: `backend/src/main/java/com/jobboard/jobposting/JobPostingExpirationScheduler.java`
- Test: `backend/src/test/java/com/jobboard/jobposting/JobPostingExpirationSchedulerTest.java`

**Interfaces:**
- Consumes: `JobPostingService.closeExpired()` (Task 11)

- [ ] **Step 1: 실패하는 테스트 작성**

`backend/src/test/java/com/jobboard/jobposting/JobPostingExpirationSchedulerTest.java`:

```java
package com.jobboard.jobposting;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class JobPostingExpirationSchedulerTest {

    @Test
    void 스케줄러_실행시_서비스의_마감처리를_호출한다() {
        JobPostingService jobPostingService = mock(JobPostingService.class);
        JobPostingExpirationScheduler scheduler = new JobPostingExpirationScheduler(jobPostingService);

        scheduler.closeExpiredPostings();

        verify(jobPostingService).closeExpired();
    }
}
```

- [ ] **Step 2: 테스트 실행하여 실패 확인**

Run: `cd backend && mvn test -Dtest=JobPostingExpirationSchedulerTest`
Expected: FAIL (컴파일 에러)

- [ ] **Step 3: 구현 작성**

`backend/src/main/java/com/jobboard/jobposting/JobPostingExpirationScheduler.java`:

```java
package com.jobboard.jobposting;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class JobPostingExpirationScheduler {

    private final JobPostingService jobPostingService;

    public JobPostingExpirationScheduler(JobPostingService jobPostingService) {
        this.jobPostingService = jobPostingService;
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void closeExpiredPostings() {
        jobPostingService.closeExpired();
    }
}
```

- [ ] **Step 4: 테스트 실행하여 통과 확인**

Run: `cd backend && mvn test -Dtest=JobPostingExpirationSchedulerTest`
Expected: PASS

- [ ] **Step 5: 전체 백엔드 테스트 실행 확인**

Run: `cd backend && mvn test`
Expected: 모든 테스트 PASS

- [ ] **Step 6: 커밋**

```bash
git add backend/src/main/java/com/jobboard/jobposting/JobPostingExpirationScheduler.java backend/src/test/java/com/jobboard/jobposting/JobPostingExpirationSchedulerTest.java
git commit -m "feat: 만료 채용공고 자동 마감 스케줄러 추가"
```

---

## Task 15: 프론트엔드 API 클라이언트 + AuthContext + ProtectedRoute

**Files:**
- Create: `frontend/src/api/client.js`
- Create: `frontend/src/api/authApi.js`
- Create: `frontend/src/api/jobApi.js`
- Create: `frontend/src/api/adminApi.js`
- Create: `frontend/src/context/AuthContext.jsx`
- Create: `frontend/src/components/ProtectedRoute.jsx`
- Test: `frontend/src/components/ProtectedRoute.test.jsx`

이 태스크는 `App.jsx`를 만들지 않는다 (아직 존재하지 않는 페이지 컴포넌트를 import하면 빌드가 깨지므로). 기존 Vite 기본 `App.jsx`/`App.css`는 Task 20에서 실제 라우팅으로 교체될 때까지 그대로 둔다.

**Interfaces:**
- Produces: `authApi.{register,login,logout,me}`, `jobApi.{list,detail}`, `adminApi.{extractPdf,createJobPosting,listJobPostings,updateJobPosting,deleteJobPosting,listUsers,updateUserRole}`, `useAuth()` 훅 (`{user, loading, login, logout}`), `<ProtectedRoute/>`, `<AdminRoute/>`

- [ ] **Step 1: API 클라이언트 작성**

`frontend/src/api/client.js`:

```js
const BASE_URL = '/api'

async function request(path, options = {}) {
  const response = await fetch(`${BASE_URL}${path}`, {
    ...options,
    headers: { 'Content-Type': 'application/json', ...options.headers },
  })
  if (!response.ok) {
    const body = await response.json().catch(() => ({}))
    throw new Error(body.message || `요청 실패 (${response.status})`)
  }
  if (response.status === 204) return null
  return response.json()
}

export function get(path) {
  return request(path)
}

export function post(path, body) {
  return request(path, { method: 'POST', body: body ? JSON.stringify(body) : undefined })
}

export function put(path, body) {
  return request(path, { method: 'PUT', body: JSON.stringify(body) })
}

export function patch(path, body) {
  return request(path, { method: 'PATCH', body: JSON.stringify(body) })
}

export function del(path) {
  return request(path, { method: 'DELETE' })
}

export async function postForm(path, formData) {
  const response = await fetch(`${BASE_URL}${path}`, { method: 'POST', body: formData })
  if (!response.ok) {
    const body = await response.json().catch(() => ({}))
    throw new Error(body.message || `요청 실패 (${response.status})`)
  }
  return response.json()
}
```

`frontend/src/api/authApi.js`:

```js
import { get, post } from './client'

export const authApi = {
  register: (data) => post('/auth/register', data),
  login: (data) => post('/auth/login', data),
  logout: () => post('/auth/logout'),
  me: () => get('/auth/me'),
}
```

`frontend/src/api/jobApi.js`:

```js
import { get } from './client'

export const jobApi = {
  list: (params) => {
    const query = new URLSearchParams(
      Object.fromEntries(Object.entries(params || {}).filter(([, v]) => v))
    ).toString()
    return get(`/job-postings?${query}`)
  },
  detail: (id) => get(`/job-postings/${id}`),
}
```

`frontend/src/api/adminApi.js`:

```js
import { get, post, put, del, postForm, patch } from './client'

export const adminApi = {
  extractPdf: (file) => {
    const formData = new FormData()
    formData.append('file', file)
    return postForm('/admin/job-postings/extract', formData)
  },
  createJobPosting: (data) => post('/admin/job-postings', data),
  listJobPostings: () => get('/admin/job-postings'),
  updateJobPosting: (id, data) => put(`/admin/job-postings/${id}`, data),
  deleteJobPosting: (id) => del(`/admin/job-postings/${id}`),
  listUsers: () => get('/admin/users'),
  updateUserRole: (id, role) => patch(`/admin/users/${id}/role`, { role }),
}
```

- [ ] **Step 2: AuthContext 작성**

`frontend/src/context/AuthContext.jsx`:

```jsx
import { createContext, useContext, useEffect, useState } from 'react'
import { authApi } from '../api/authApi'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    authApi.me()
      .then(setUser)
      .catch(() => setUser(null))
      .finally(() => setLoading(false))
  }, [])

  async function login(credentials) {
    const loggedInUser = await authApi.login(credentials)
    setUser(loggedInUser)
    return loggedInUser
  }

  async function logout() {
    await authApi.logout()
    setUser(null)
  }

  return (
    <AuthContext.Provider value={{ user, loading, login, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  return useContext(AuthContext)
}
```

- [ ] **Step 3: 실패하는 ProtectedRoute 테스트 작성**

`frontend/src/components/ProtectedRoute.test.jsx`:

```jsx
import { describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router'
import { AuthContextTestProvider } from '../test-utils/AuthContextTestProvider'
import { ProtectedRoute } from './ProtectedRoute'

describe('ProtectedRoute', () => {
  it('로그인하지 않았으면 로그인 페이지로 리다이렉트한다', () => {
    render(
      <AuthContextTestProvider value={{ user: null, loading: false }}>
        <MemoryRouter initialEntries={['/']}>
          <Routes>
            <Route path="/login" element={<div>로그인 페이지</div>} />
            <Route element={<ProtectedRoute />}>
              <Route path="/" element={<div>보호된 페이지</div>} />
            </Route>
          </Routes>
        </MemoryRouter>
      </AuthContextTestProvider>
    )

    expect(screen.getByText('로그인 페이지')).toBeInTheDocument()
  })

  it('로그인했으면 보호된 페이지를 보여준다', () => {
    render(
      <AuthContextTestProvider value={{ user: { id: 1, role: 'USER' }, loading: false }}>
        <MemoryRouter initialEntries={['/']}>
          <Routes>
            <Route path="/login" element={<div>로그인 페이지</div>} />
            <Route element={<ProtectedRoute />}>
              <Route path="/" element={<div>보호된 페이지</div>} />
            </Route>
          </Routes>
        </MemoryRouter>
      </AuthContextTestProvider>
    )

    expect(screen.getByText('보호된 페이지')).toBeInTheDocument()
  })
})
```

`frontend/src/test-utils/AuthContextTestProvider.jsx` (테스트 전용 헬퍼):

```jsx
import { AuthContext } from '../context/AuthContextInstance'

export function AuthContextTestProvider({ value, children }) {
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
```

이 헬퍼가 `AuthContext` 인스턴스를 직접 가져다 쓸 수 있도록 `AuthContext.jsx`에서 컨텍스트 객체를 별도 파일로 분리한다.

`frontend/src/context/AuthContextInstance.js`:

```js
import { createContext } from 'react'

export const AuthContext = createContext(null)
```

`frontend/src/context/AuthContext.jsx`를 다음과 같이 수정 (컨텍스트 정의를 `AuthContextInstance.js`에서 import):

```jsx
import { useEffect, useState } from 'react'
import { authApi } from '../api/authApi'
import { AuthContext } from './AuthContextInstance'
import { useContext } from 'react'

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    authApi.me()
      .then(setUser)
      .catch(() => setUser(null))
      .finally(() => setLoading(false))
  }, [])

  async function login(credentials) {
    const loggedInUser = await authApi.login(credentials)
    setUser(loggedInUser)
    return loggedInUser
  }

  async function logout() {
    await authApi.logout()
    setUser(null)
  }

  return (
    <AuthContext.Provider value={{ user, loading, login, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  return useContext(AuthContext)
}
```

- [ ] **Step 4: 테스트 실행하여 실패 확인**

Run: `cd frontend && npx vitest run src/components/ProtectedRoute.test.jsx`
Expected: FAIL (`ProtectedRoute` 모듈 없음)

- [ ] **Step 5: ProtectedRoute 구현**

`frontend/src/components/ProtectedRoute.jsx`:

```jsx
import { Navigate, Outlet } from 'react-router'
import { useAuth } from '../context/AuthContext'

export function ProtectedRoute() {
  const { user, loading } = useAuth()
  if (loading) return <p>로딩 중...</p>
  if (!user) return <Navigate to="/login" replace />
  return <Outlet />
}

export function AdminRoute() {
  const { user, loading } = useAuth()
  if (loading) return <p>로딩 중...</p>
  if (!user || user.role !== 'ADMIN') return <Navigate to="/" replace />
  return <Outlet />
}
```

- [ ] **Step 6: 테스트 실행하여 통과 확인**

Run: `cd frontend && npx vitest run src/components/ProtectedRoute.test.jsx`
Expected: PASS

- [ ] **Step 7: 커밋**

```bash
cd /Users/hun/Summer_lecture_vibe_coding
git add frontend/src/api frontend/src/context frontend/src/components/ProtectedRoute.jsx frontend/src/components/ProtectedRoute.test.jsx frontend/src/test-utils
git commit -m "feat: 프론트엔드 API 클라이언트, AuthContext, ProtectedRoute 추가"
```

---

## Task 16: 로그인/회원가입 페이지

**Files:**
- Create: `frontend/src/pages/LoginPage.jsx`
- Create: `frontend/src/pages/RegisterPage.jsx`
- Test: `frontend/src/pages/LoginPage.test.jsx`

**Interfaces:**
- Consumes: `useAuth()` (Task 15), `authApi` (Task 15)
- Produces: `<LoginPage/>`, `<RegisterPage/>`

- [ ] **Step 1: 실패하는 테스트 작성**

`frontend/src/pages/LoginPage.test.jsx`:

```jsx
import { describe, expect, it, vi } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { AuthContext } from '../context/AuthContextInstance'
import { LoginPage } from './LoginPage'

describe('LoginPage', () => {
  it('이메일과 비밀번호를 입력하고 제출하면 login이 호출된다', async () => {
    const login = vi.fn().mockResolvedValue({ id: 1, role: 'USER' })

    render(
      <AuthContext.Provider value={{ user: null, loading: false, login }}>
        <MemoryRouter>
          <LoginPage />
        </MemoryRouter>
      </AuthContext.Provider>
    )

    fireEvent.change(screen.getByLabelText('이메일'), { target: { value: 'user@jobboard.com' } })
    fireEvent.change(screen.getByLabelText('비밀번호'), { target: { value: 'password123' } })
    fireEvent.click(screen.getByRole('button', { name: '로그인' }))

    await waitFor(() => expect(login).toHaveBeenCalledWith({ email: 'user@jobboard.com', password: 'password123' }))
  })
})
```

- [ ] **Step 2: 테스트 실행하여 실패 확인**

Run: `cd frontend && npx vitest run src/pages/LoginPage.test.jsx`
Expected: FAIL (`LoginPage` 모듈 없음)

- [ ] **Step 3: 구현 작성**

`frontend/src/pages/LoginPage.jsx`:

```jsx
import { useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { useAuth } from '../context/AuthContext'

export function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState(null)

  async function handleSubmit(event) {
    event.preventDefault()
    setError(null)
    try {
      await login({ email, password })
      navigate('/')
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <form onSubmit={handleSubmit}>
      <h1>로그인</h1>
      {error && <p role="alert">{error}</p>}
      <label htmlFor="login-email">이메일</label>
      <input id="login-email" aria-label="이메일" type="email" value={email}
             onChange={(e) => setEmail(e.target.value)} required />
      <label htmlFor="login-password">비밀번호</label>
      <input id="login-password" aria-label="비밀번호" type="password" value={password}
             onChange={(e) => setPassword(e.target.value)} required />
      <button type="submit">로그인</button>
      <p><Link to="/register">회원가입</Link></p>
    </form>
  )
}
```

`frontend/src/pages/RegisterPage.jsx`:

```jsx
import { useState } from 'react'
import { useNavigate } from 'react-router'
import { authApi } from '../api/authApi'

export function RegisterPage() {
  const navigate = useNavigate()
  const [form, setForm] = useState({ email: '', password: '', name: '' })
  const [error, setError] = useState(null)

  function handleChange(event) {
    setForm({ ...form, [event.target.name]: event.target.value })
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setError(null)
    try {
      await authApi.register(form)
      navigate('/login')
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <form onSubmit={handleSubmit}>
      <h1>회원가입</h1>
      {error && <p role="alert">{error}</p>}
      <label htmlFor="register-email">이메일</label>
      <input id="register-email" aria-label="이메일" name="email" type="email" value={form.email}
             onChange={handleChange} required />
      <label htmlFor="register-password">비밀번호</label>
      <input id="register-password" aria-label="비밀번호" name="password" type="password" value={form.password}
             onChange={handleChange} required />
      <label htmlFor="register-name">이름</label>
      <input id="register-name" aria-label="이름" name="name" value={form.name}
             onChange={handleChange} required />
      <button type="submit">회원가입</button>
    </form>
  )
}
```

- [ ] **Step 4: 테스트 실행하여 통과 확인**

Run: `cd frontend && npx vitest run src/pages/LoginPage.test.jsx`
Expected: PASS

- [ ] **Step 5: 커밋**

```bash
git add frontend/src/pages/LoginPage.jsx frontend/src/pages/RegisterPage.jsx frontend/src/pages/LoginPage.test.jsx
git commit -m "feat: 로그인/회원가입 페이지 추가"
```

---

## Task 17: 채용공고 목록/검색/필터 페이지

**Files:**
- Create: `frontend/src/components/JobCard.jsx`
- Create: `frontend/src/pages/JobListPage.jsx`
- Test: `frontend/src/pages/JobListPage.test.jsx`

**Interfaces:**
- Consumes: `jobApi.list` (Task 15)
- Produces: `<JobCard/>`, `<JobListPage/>`

- [ ] **Step 1: 실패하는 테스트 작성**

`frontend/src/pages/JobListPage.test.jsx`:

```jsx
import { describe, expect, it, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { jobApi } from '../api/jobApi'
import { JobListPage } from './JobListPage'

vi.mock('../api/jobApi')

describe('JobListPage', () => {
  beforeEach(() => {
    jobApi.list.mockResolvedValue({
      content: [
        { id: 1, companyName: '테스트회사', location: '서울', employmentType: 'FULL_TIME' },
      ],
      totalElements: 1,
    })
  })

  it('채용공고 목록을 불러와 보여준다', async () => {
    render(
      <MemoryRouter>
        <JobListPage />
      </MemoryRouter>
    )

    await waitFor(() => expect(screen.getByText('테스트회사')).toBeInTheDocument())
  })
})
```

- [ ] **Step 2: 테스트 실행하여 실패 확인**

Run: `cd frontend && npx vitest run src/pages/JobListPage.test.jsx`
Expected: FAIL (`JobListPage` 모듈 없음)

- [ ] **Step 3: 구현 작성**

`frontend/src/components/JobCard.jsx`:

```jsx
import { Link } from 'react-router'

const EMPLOYMENT_TYPE_LABEL = {
  FULL_TIME: '정규직',
  CONTRACT: '계약직',
  INTERN: '인턴',
  PART_TIME: '파트타임',
}

export function JobCard({ posting }) {
  return (
    <li>
      <Link to={`/jobs/${posting.id}`}>
        <h2>{posting.companyName}</h2>
        <p>{posting.location}</p>
        <p>{EMPLOYMENT_TYPE_LABEL[posting.employmentType] ?? posting.employmentType}</p>
      </Link>
    </li>
  )
}
```

`frontend/src/pages/JobListPage.jsx`:

```jsx
import { useEffect, useState } from 'react'
import { jobApi } from '../api/jobApi'
import { JobCard } from '../components/JobCard'

const EMPLOYMENT_TYPE_OPTIONS = [
  { value: '', label: '전체' },
  { value: 'FULL_TIME', label: '정규직' },
  { value: 'CONTRACT', label: '계약직' },
  { value: 'INTERN', label: '인턴' },
  { value: 'PART_TIME', label: '파트타임' },
]

export function JobListPage() {
  const [postings, setPostings] = useState([])
  const [keyword, setKeyword] = useState('')
  const [location, setLocation] = useState('')
  const [employmentType, setEmploymentType] = useState('')
  const [error, setError] = useState(null)

  useEffect(() => {
    jobApi.list({ keyword, location, employmentType })
      .then((page) => setPostings(page.content))
      .catch((err) => setError(err.message))
  }, [keyword, location, employmentType])

  return (
    <div>
      <h1>채용공고</h1>
      {error && <p role="alert">{error}</p>}
      <div>
        <label htmlFor="keyword">검색어</label>
        <input id="keyword" value={keyword} onChange={(e) => setKeyword(e.target.value)} placeholder="회사명 검색" />
        <label htmlFor="location">지역</label>
        <input id="location" value={location} onChange={(e) => setLocation(e.target.value)} placeholder="지역" />
        <label htmlFor="employmentType">고용형태</label>
        <select id="employmentType" value={employmentType} onChange={(e) => setEmploymentType(e.target.value)}>
          {EMPLOYMENT_TYPE_OPTIONS.map((option) => (
            <option key={option.value} value={option.value}>{option.label}</option>
          ))}
        </select>
      </div>
      <ul>
        {postings.map((posting) => (
          <JobCard key={posting.id} posting={posting} />
        ))}
      </ul>
    </div>
  )
}
```

- [ ] **Step 4: 테스트 실행하여 통과 확인**

Run: `cd frontend && npx vitest run src/pages/JobListPage.test.jsx`
Expected: PASS

- [ ] **Step 5: 커밋**

```bash
git add frontend/src/components/JobCard.jsx frontend/src/pages/JobListPage.jsx frontend/src/pages/JobListPage.test.jsx
git commit -m "feat: 채용공고 목록/검색/필터 페이지 추가"
```

---

## Task 18: 채용공고 상세 페이지

**Files:**
- Create: `frontend/src/pages/JobDetailPage.jsx`
- Test: `frontend/src/pages/JobDetailPage.test.jsx`

**Interfaces:**
- Consumes: `jobApi.detail` (Task 15)
- Produces: `<JobDetailPage/>`

- [ ] **Step 1: 실패하는 테스트 작성**

`frontend/src/pages/JobDetailPage.test.jsx`:

```jsx
import { describe, expect, it, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router'
import { jobApi } from '../api/jobApi'
import { JobDetailPage } from './JobDetailPage'

vi.mock('../api/jobApi')

describe('JobDetailPage', () => {
  beforeEach(() => {
    jobApi.detail.mockResolvedValue({
      id: 1,
      companyName: '테스트회사',
      location: '서울',
      conditionNote: '경력 무관',
      applyStartDate: '2026-08-01',
      applyEndDate: '2026-08-31',
      applyMethod: '이메일 접수',
      salaryMin: 3000,
      salaryMax: 3500,
      salaryNote: '협의가능',
      status: 'ACTIVE',
    })
  })

  it('채용공고 상세 정보를 보여준다', async () => {
    render(
      <MemoryRouter initialEntries={['/jobs/1']}>
        <Routes>
          <Route path="/jobs/:id" element={<JobDetailPage />} />
        </Routes>
      </MemoryRouter>
    )

    await waitFor(() => expect(screen.getByText('테스트회사')).toBeInTheDocument())
    expect(screen.getByText('이메일 접수')).toBeInTheDocument()
  })
})
```

- [ ] **Step 2: 테스트 실행하여 실패 확인**

Run: `cd frontend && npx vitest run src/pages/JobDetailPage.test.jsx`
Expected: FAIL (`JobDetailPage` 모듈 없음)

- [ ] **Step 3: 구현 작성**

`frontend/src/pages/JobDetailPage.jsx`:

```jsx
import { useEffect, useState } from 'react'
import { useParams } from 'react-router'
import { jobApi } from '../api/jobApi'

export function JobDetailPage() {
  const { id } = useParams()
  const [posting, setPosting] = useState(null)
  const [error, setError] = useState(null)

  useEffect(() => {
    jobApi.detail(id).then(setPosting).catch((err) => setError(err.message))
  }, [id])

  if (error) return <p role="alert">{error}</p>
  if (!posting) return <p>불러오는 중...</p>

  return (
    <article>
      <h1>{posting.companyName}</h1>
      {posting.status === 'CLOSED' && <p>마감된 공고입니다.</p>}
      <p>위치: {posting.location}</p>
      <p>채용조건: {posting.conditionNote}</p>
      <p>지원기간: {posting.applyStartDate} ~ {posting.applyEndDate}</p>
      <p>지원방법: {posting.applyMethod}</p>
      <p>예상급여: {posting.salaryMin}만원 ~ {posting.salaryMax}만원 ({posting.salaryNote})</p>
    </article>
  )
}
```

- [ ] **Step 4: 테스트 실행하여 통과 확인**

Run: `cd frontend && npx vitest run src/pages/JobDetailPage.test.jsx`
Expected: PASS

- [ ] **Step 5: 커밋**

```bash
git add frontend/src/pages/JobDetailPage.jsx frontend/src/pages/JobDetailPage.test.jsx
git commit -m "feat: 채용공고 상세 페이지 추가"
```

---

## Task 19: 관리자 PDF 업로드 + 검토 폼 페이지

**Files:**
- Create: `frontend/src/pages/admin/AdminUploadPage.jsx`
- Test: `frontend/src/pages/admin/AdminUploadPage.test.jsx`

**Interfaces:**
- Consumes: `adminApi.extractPdf`, `adminApi.createJobPosting` (Task 15)
- Produces: `<AdminUploadPage/>`

- [ ] **Step 1: 실패하는 테스트 작성**

`frontend/src/pages/admin/AdminUploadPage.test.jsx`:

```jsx
import { describe, expect, it, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { adminApi } from '../../api/adminApi'
import { AdminUploadPage } from './AdminUploadPage'

vi.mock('../../api/adminApi')

describe('AdminUploadPage', () => {
  beforeEach(() => {
    adminApi.extractPdf.mockResolvedValue({
      pdfFileName: 'stored-uuid.pdf',
      companyName: '테스트회사',
      location: '서울',
      careerLevel: 'NEW',
      education: 'BACHELOR',
      employmentType: 'FULL_TIME',
      conditionNote: '비고',
      applyStartDate: '2026-08-01',
      applyEndDate: '2026-08-31',
      applyMethod: '이메일 접수',
      salaryMin: 3000,
      salaryMax: 3500,
      salaryNote: '협의가능',
    })
    adminApi.createJobPosting.mockResolvedValue({ id: 1 })
  })

  it('PDF를 업로드하면 추출결과가 폼에 채워지고 저장할 수 있다', async () => {
    render(
      <MemoryRouter>
        <AdminUploadPage />
      </MemoryRouter>
    )

    const file = new File(['dummy'], 'posting.pdf', { type: 'application/pdf' })
    fireEvent.change(screen.getByLabelText('PDF 파일'), { target: { files: [file] } })
    fireEvent.click(screen.getByRole('button', { name: 'PDF에서 추출' }))

    await waitFor(() => expect(screen.getByDisplayValue('테스트회사')).toBeInTheDocument())

    fireEvent.click(screen.getByRole('button', { name: '등록' }))

    await waitFor(() => expect(adminApi.createJobPosting).toHaveBeenCalled())
  })
})
```

- [ ] **Step 2: 테스트 실행하여 실패 확인**

Run: `cd frontend && npx vitest run src/pages/admin/AdminUploadPage.test.jsx`
Expected: FAIL (`AdminUploadPage` 모듈 없음)

- [ ] **Step 3: 구현 작성**

`frontend/src/pages/admin/AdminUploadPage.jsx`:

```jsx
import { useState } from 'react'
import { useNavigate } from 'react-router'
import { adminApi } from '../../api/adminApi'

const EMPTY_FORM = {
  pdfFileName: '', companyName: '', location: '', careerLevel: 'NEW', education: 'BACHELOR',
  employmentType: 'FULL_TIME', conditionNote: '', applyStartDate: '', applyEndDate: '',
  applyMethod: '', salaryMin: '', salaryMax: '', salaryNote: '',
}

export function AdminUploadPage() {
  const navigate = useNavigate()
  const [file, setFile] = useState(null)
  const [form, setForm] = useState(EMPTY_FORM)
  const [error, setError] = useState(null)

  async function handleExtract() {
    setError(null)
    try {
      const result = await adminApi.extractPdf(file)
      setForm({
        pdfFileName: result.pdfFileName ?? '',
        companyName: result.companyName ?? '',
        location: result.location ?? '',
        careerLevel: result.careerLevel || 'NEW',
        education: result.education || 'BACHELOR',
        employmentType: result.employmentType || 'FULL_TIME',
        conditionNote: result.conditionNote ?? '',
        applyStartDate: result.applyStartDate ?? '',
        applyEndDate: result.applyEndDate ?? '',
        applyMethod: result.applyMethod ?? '',
        salaryMin: result.salaryMin ?? '',
        salaryMax: result.salaryMax ?? '',
        salaryNote: result.salaryNote ?? '',
      })
    } catch (err) {
      setError(err.message)
    }
  }

  function handleChange(event) {
    setForm({ ...form, [event.target.name]: event.target.value })
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setError(null)
    try {
      await adminApi.createJobPosting({
        ...form,
        salaryMin: Number(form.salaryMin) || 0,
        salaryMax: Number(form.salaryMax) || 0,
      })
      navigate('/admin/jobs')
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <div>
      <h1>채용공고 PDF 업로드</h1>
      {error && <p role="alert">{error}</p>}
      <label htmlFor="pdf-file">PDF 파일</label>
      <input id="pdf-file" aria-label="PDF 파일" type="file" accept="application/pdf"
             onChange={(e) => setFile(e.target.files[0])} />
      <button type="button" onClick={handleExtract} disabled={!file}>PDF에서 추출</button>

      <form onSubmit={handleSubmit}>
        <label htmlFor="companyName">회사명</label>
        <input id="companyName" name="companyName" value={form.companyName} onChange={handleChange} required />
        <label htmlFor="location">위치</label>
        <input id="location" name="location" value={form.location} onChange={handleChange} required />
        <label htmlFor="careerLevel">경력</label>
        <select id="careerLevel" name="careerLevel" value={form.careerLevel} onChange={handleChange}>
          <option value="NEW">신입</option>
          <option value="EXPERIENCED">경력</option>
          <option value="ANY">무관</option>
        </select>
        <label htmlFor="education">학력</label>
        <select id="education" name="education" value={form.education} onChange={handleChange}>
          <option value="NONE">무관</option>
          <option value="HIGH_SCHOOL">고졸</option>
          <option value="ASSOCIATE">전문학사</option>
          <option value="BACHELOR">학사</option>
          <option value="MASTER">석사</option>
        </select>
        <label htmlFor="employmentType">고용형태</label>
        <select id="employmentType" name="employmentType" value={form.employmentType} onChange={handleChange}>
          <option value="FULL_TIME">정규직</option>
          <option value="CONTRACT">계약직</option>
          <option value="INTERN">인턴</option>
          <option value="PART_TIME">파트타임</option>
        </select>
        <label htmlFor="conditionNote">채용조건 비고</label>
        <textarea id="conditionNote" name="conditionNote" value={form.conditionNote} onChange={handleChange} />
        <label htmlFor="applyStartDate">지원 시작일</label>
        <input id="applyStartDate" name="applyStartDate" type="date" value={form.applyStartDate} onChange={handleChange} required />
        <label htmlFor="applyEndDate">지원 종료일</label>
        <input id="applyEndDate" name="applyEndDate" type="date" value={form.applyEndDate} onChange={handleChange} required />
        <label htmlFor="applyMethod">지원방법</label>
        <textarea id="applyMethod" name="applyMethod" value={form.applyMethod} onChange={handleChange} />
        <label htmlFor="salaryMin">예상급여 최소(만원)</label>
        <input id="salaryMin" name="salaryMin" type="number" value={form.salaryMin} onChange={handleChange} />
        <label htmlFor="salaryMax">예상급여 최대(만원)</label>
        <input id="salaryMax" name="salaryMax" type="number" value={form.salaryMax} onChange={handleChange} />
        <label htmlFor="salaryNote">급여 비고</label>
        <input id="salaryNote" name="salaryNote" value={form.salaryNote} onChange={handleChange} />
        <button type="submit">등록</button>
      </form>
    </div>
  )
}
```

- [ ] **Step 4: 테스트 실행하여 통과 확인**

Run: `cd frontend && npx vitest run src/pages/admin/AdminUploadPage.test.jsx`
Expected: PASS

- [ ] **Step 5: 커밋**

```bash
git add frontend/src/pages/admin/AdminUploadPage.jsx frontend/src/pages/admin/AdminUploadPage.test.jsx
git commit -m "feat: 관리자 PDF 업로드/검토 등록 페이지 추가"
```

---

## Task 20: 관리자 채용공고 관리 페이지 + 회원 관리 페이지 + 라우팅 마무리

**Files:**
- Create: `frontend/src/pages/admin/AdminJobListPage.jsx`
- Create: `frontend/src/pages/admin/AdminUserListPage.jsx`
- Test: `frontend/src/pages/admin/AdminJobListPage.test.jsx`
- Test: `frontend/src/pages/admin/AdminUserListPage.test.jsx`
- Create: `frontend/src/App.jsx` (기존 Vite 기본 파일 대체 — 이 태스크에서 처음 만들어짐)
- Modify: `frontend/src/main.jsx` (Vite 기본 엔트리에서 `App` import 확인)

**Interfaces:**
- Consumes: `adminApi.{listJobPostings,deleteJobPosting,listUsers,updateUserRole}` (Task 15), `useAuth()`/`ProtectedRoute`/`AdminRoute` (Task 15), `LoginPage`/`RegisterPage` (Task 16), `JobListPage` (Task 17), `JobDetailPage` (Task 18), `AdminUploadPage` (Task 19)
- Produces: `<AdminJobListPage/>`, `<AdminUserListPage/>`, `frontend/src/App.jsx` (전체 라우팅)

- [ ] **Step 1: 실패하는 AdminJobListPage 테스트 작성**

`frontend/src/pages/admin/AdminJobListPage.test.jsx`:

```jsx
import { describe, expect, it, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { adminApi } from '../../api/adminApi'
import { AdminJobListPage } from './AdminJobListPage'

vi.mock('../../api/adminApi')

describe('AdminJobListPage', () => {
  beforeEach(() => {
    adminApi.listJobPostings.mockResolvedValue([
      { id: 1, companyName: '테스트회사', status: 'ACTIVE' },
    ])
    adminApi.deleteJobPosting.mockResolvedValue(null)
  })

  it('채용공고 목록을 보여주고 삭제할 수 있다', async () => {
    render(
      <MemoryRouter>
        <AdminJobListPage />
      </MemoryRouter>
    )

    await waitFor(() => expect(screen.getByText('테스트회사')).toBeInTheDocument())

    fireEvent.click(screen.getByRole('button', { name: '삭제' }))

    await waitFor(() => expect(adminApi.deleteJobPosting).toHaveBeenCalledWith(1))
  })
})
```

- [ ] **Step 2: 테스트 실행하여 실패 확인**

Run: `cd frontend && npx vitest run src/pages/admin/AdminJobListPage.test.jsx`
Expected: FAIL

- [ ] **Step 3: AdminJobListPage 구현**

`frontend/src/pages/admin/AdminJobListPage.jsx`:

```jsx
import { useEffect, useState } from 'react'
import { adminApi } from '../../api/adminApi'

export function AdminJobListPage() {
  const [postings, setPostings] = useState([])
  const [error, setError] = useState(null)

  function reload() {
    adminApi.listJobPostings().then(setPostings).catch((err) => setError(err.message))
  }

  useEffect(() => {
    reload()
  }, [])

  async function handleDelete(id) {
    await adminApi.deleteJobPosting(id)
    reload()
  }

  return (
    <div>
      <h1>채용공고 관리</h1>
      {error && <p role="alert">{error}</p>}
      <ul>
        {postings.map((posting) => (
          <li key={posting.id}>
            {posting.companyName} ({posting.status})
            <button type="button" onClick={() => handleDelete(posting.id)}>삭제</button>
          </li>
        ))}
      </ul>
    </div>
  )
}
```

- [ ] **Step 4: 테스트 실행하여 통과 확인**

Run: `cd frontend && npx vitest run src/pages/admin/AdminJobListPage.test.jsx`
Expected: PASS

- [ ] **Step 5: 실패하는 AdminUserListPage 테스트 작성**

`frontend/src/pages/admin/AdminUserListPage.test.jsx`:

```jsx
import { describe, expect, it, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { adminApi } from '../../api/adminApi'
import { AdminUserListPage } from './AdminUserListPage'

vi.mock('../../api/adminApi')

describe('AdminUserListPage', () => {
  beforeEach(() => {
    adminApi.listUsers.mockResolvedValue([
      { id: 2, email: 'member@jobboard.com', name: '회원1', role: 'USER' },
    ])
    adminApi.updateUserRole.mockResolvedValue({ id: 2, role: 'ADMIN' })
  })

  it('회원 목록을 보여주고 권한을 변경할 수 있다', async () => {
    render(
      <MemoryRouter>
        <AdminUserListPage />
      </MemoryRouter>
    )

    await waitFor(() => expect(screen.getByText('member@jobboard.com')).toBeInTheDocument())

    fireEvent.click(screen.getByRole('button', { name: '관리자로 변경' }))

    await waitFor(() => expect(adminApi.updateUserRole).toHaveBeenCalledWith(2, 'ADMIN'))
  })
})
```

- [ ] **Step 6: 테스트 실행하여 실패 확인**

Run: `cd frontend && npx vitest run src/pages/admin/AdminUserListPage.test.jsx`
Expected: FAIL

- [ ] **Step 7: AdminUserListPage 구현**

`frontend/src/pages/admin/AdminUserListPage.jsx`:

```jsx
import { useEffect, useState } from 'react'
import { adminApi } from '../../api/adminApi'

export function AdminUserListPage() {
  const [users, setUsers] = useState([])
  const [error, setError] = useState(null)

  function reload() {
    adminApi.listUsers().then(setUsers).catch((err) => setError(err.message))
  }

  useEffect(() => {
    reload()
  }, [])

  async function handleToggleRole(user) {
    const nextRole = user.role === 'ADMIN' ? 'USER' : 'ADMIN'
    await adminApi.updateUserRole(user.id, nextRole)
    reload()
  }

  return (
    <div>
      <h1>회원 관리</h1>
      {error && <p role="alert">{error}</p>}
      <ul>
        {users.map((user) => (
          <li key={user.id}>
            {user.email} ({user.name}) - {user.role}
            <button type="button" onClick={() => handleToggleRole(user)}>
              {user.role === 'ADMIN' ? '일반회원으로 변경' : '관리자로 변경'}
            </button>
          </li>
        ))}
      </ul>
    </div>
  )
}
```

- [ ] **Step 8: 테스트 실행하여 통과 확인**

Run: `cd frontend && npx vitest run src/pages/admin/AdminUserListPage.test.jsx`
Expected: PASS

- [ ] **Step 9: App.jsx 라우팅 작성**

`frontend/src/App.jsx` (기존 Vite 기본 파일을 대체):

```jsx
import { BrowserRouter, Routes, Route } from 'react-router'
import { AuthProvider } from './context/AuthContext'
import { ProtectedRoute, AdminRoute } from './components/ProtectedRoute'
import { LoginPage } from './pages/LoginPage'
import { RegisterPage } from './pages/RegisterPage'
import { JobListPage } from './pages/JobListPage'
import { JobDetailPage } from './pages/JobDetailPage'
import { AdminUploadPage } from './pages/admin/AdminUploadPage'
import { AdminJobListPage } from './pages/admin/AdminJobListPage'
import { AdminUserListPage } from './pages/admin/AdminUserListPage'

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route element={<ProtectedRoute />}>
            <Route path="/" element={<JobListPage />} />
            <Route path="/jobs/:id" element={<JobDetailPage />} />
            <Route element={<AdminRoute />}>
              <Route path="/admin/upload" element={<AdminUploadPage />} />
              <Route path="/admin/jobs" element={<AdminJobListPage />} />
              <Route path="/admin/users" element={<AdminUserListPage />} />
            </Route>
          </Route>
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  )
}
```

- [ ] **Step 10: `main.jsx`가 `App`을 렌더링하는지 확인 (Vite 기본 스캐폴딩 그대로 사용)**

`frontend/src/main.jsx` 내용 확인 (Vite 템플릿이 기본으로 생성한 아래 형태여야 함, 다르면 아래 내용으로 맞춘다):

```jsx
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import App from './App.jsx'

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
```

- [ ] **Step 11: 프론트엔드 전체 테스트 실행**

Run: `cd frontend && npx vitest run`
Expected: 모든 테스트 PASS (`App.jsx`가 참조하는 모든 페이지 컴포넌트가 이제 존재하므로 빌드도 통과해야 함)

- [ ] **Step 12: 빌드 확인**

Run: `cd frontend && npm run build`
Expected: 에러 없이 빌드 성공

- [ ] **Step 13: 커밋 (App.jsx 포함 최종 반영)**

```bash
cd /Users/hun/Summer_lecture_vibe_coding
git add frontend/src/pages/admin frontend/src/App.jsx frontend/src/main.jsx
git commit -m "feat: 관리자 채용공고/회원 관리 페이지 추가 및 라우팅 완성"
```

- [ ] **Step 14: 수동 통합 확인 (OpenAI 실제 연동 제외)**

Run:
```bash
cd backend && mvn spring-boot:run
```
(다른 터미널에서)
```bash
cd frontend && npm run dev
```
Expected: `http://localhost:5173` 접속 → 회원가입 → 로그인 → 목록 조회. `admin@jobboard.local` / `admin1234!` 로 로그인 후 `/admin/jobs`, `/admin/users` 페이지 동작 확인.

`OPENAI_API_KEY`를 아직 설정하지 않았다면 `/admin/upload`에서 "PDF에서 추출" 버튼을 눌렀을 때 OpenAI API가 401을 반환하며 실패하는 것이 정상이다 (Task 3의 `GlobalExceptionHandler`가 `ApiException`으로 감싸 에러 메시지를 반환). 이 계획의 자동화 테스트(Task 9, 11, 19)는 전부 `MockRestServiceServer`/목(mock) 객체로 대체되어 있어 실제 키 없이도 통과하므로, 지금 단계에서는 이 실패를 무시하고 넘어가도 된다. 실제 키는 준비되는 대로 아래 "참고: 환경변수"에 따라 등록하면 별도 코드 변경 없이 바로 연동된다.

이 Step은 확인만 하는 단계이므로 코드 변경이 없다 — 별도 커밋 없음.

---

## 참고: 환경변수 (OpenAI API 키는 추후 연동)

이번 구현 단계에서는 `OPENAI_API_KEY`를 설정하지 않고 진행한다. 모든 자동화 테스트는 실제 API를 호출하지 않으므로 키가 없어도 `mvn test`는 전부 통과한다.

나중에 실제 키가 준비되면 아래처럼 환경변수만 설정하면 되며, 코드 변경은 필요 없다.

```bash
export OPENAI_API_KEY="sk-..."
```

키가 없는 상태에서 관리자가 실제로 PDF 업로드 화면에서 "PDF에서 추출"을 누르면 OpenAI API가 401을 반환하며 실패한다 (등록 자체는 막히지 않고, 관리자가 수동으로 폼을 채워 저장할 수 있음).
