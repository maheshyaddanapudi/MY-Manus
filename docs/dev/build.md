# Build & Development Guide

## Prerequisites

| Tool | Required Version | Check Command |
|---|---|---|
| Java (JDK) | 17+ | `java --version` |
| Maven | 3.9+ | `mvn --version` |
| Node.js | 22.x | `node --version` |
| npm | 10.x | `npm --version` |
| Docker | 20+ | `docker --version` |
| PostgreSQL | 15 (or use H2 dev profile) | `psql --version` |

The `backend/mvnw` script is a thin wrapper (`exec mvn "$@"`) that delegates to system Maven. There is no embedded Maven Wrapper (no `.mvn/wrapper/` directory). You must have `mvn` on your PATH.

## Backend Commands

### Compile
```bash
cd backend
mvn compile
```

### Run (development — H2 in-memory, no Docker sandbox)
```bash
cd backend
mvn spring-boot:run -Dspring.profiles.active=dev
```
This uses H2 in-memory database and host-based Python execution (no Docker required). Server starts on port 8080.

### Run (default — PostgreSQL + Docker sandbox)
```bash
cd backend
mvn spring-boot:run
```
Requires PostgreSQL running on localhost:5432 and Docker available. Configure via environment variables or `backend/.env`.

### Run all tests
```bash
cd backend
mvn test
```
Tests use `@WebMvcTest` (controller layer) and `@SpringBootTest` (integration). Test security config disables auth. `IntegrationTestConfiguration` mocks `AnthropicService`.

### Run a single test class
```bash
cd backend
mvn test -Dtest=AgentControllerTest
```

### Run a single test method
```bash
cd backend
mvn test -Dtest=AgentControllerTest#testSendMessage
```

### Package JAR (skip tests)
```bash
cd backend
mvn package -DskipTests
```
Output: `backend/target/manus-backend-1.0.0.jar`

### Run packaged JAR
```bash
java -jar backend/target/manus-backend-1.0.0.jar --spring.profiles.active=dev
```

## Frontend Commands

### Install dependencies
```bash
cd frontend
npm install
```

### Run dev server
```bash
cd frontend
npm run dev
```
Starts Vite dev server (default port 5173). Proxies `/api` and `/ws` to `http://localhost:8080` (see `vite.config.ts`).

### Build for production
```bash
cd frontend
npm run build
```
Runs `tsc -b && vite build`. TypeScript type-checking must pass before Vite bundles.

### Run all tests
```bash
cd frontend
npx vitest run
```
Uses Vitest with jsdom environment. Setup file: `src/test/setup.ts` (imports `@testing-library/jest-dom`). Configuration in `vite.config.ts` under `test`.

### Run a single test file
```bash
cd frontend
npx vitest run src/components/__tests__/ChatPanel.test.tsx
```

### Run tests in watch mode
```bash
cd frontend
npm test
```

### Run tests with coverage
```bash
cd frontend
npm run test:coverage
```

### Lint (ESLint)
```bash
cd frontend
npm run lint
```
Uses ESLint 9 flat config (`eslint.config.js`) with `typescript-eslint`, `react-hooks`, and `react-refresh` plugins.

### Type check only
```bash
cd frontend
npx tsc -b
```

### Generate API client from OpenAPI
```bash
# Requires backend running on localhost:8080
cd frontend
npm run generate-api
```
Generates TypeScript Axios client from `/v3/api-docs` into `src/api/generated/`.

## Docker Commands

### Build sandbox image
```bash
./scripts/build-sandbox.sh
# or manually:
cd sandbox && docker build -t mymanus-sandbox:latest .
```

### Run full stack with Docker Compose
```bash
docker-compose up -d
```
Starts PostgreSQL (port 5432), backend (port 8080), frontend (port 3000).

### Stop full stack
```bash
docker-compose down
```

## Spring Profiles

| Profile | Database | Auth | Sandbox | Use Case |
|---|---|---|---|---|
| (none/default) | PostgreSQL | Configurable (`auth.enabled`) | Docker | Production |
| `dev` | H2 in-memory | Disabled | Host Python | Local development without Docker/Postgres |

Activate with: `-Dspring.profiles.active=dev` or `SPRING_PROFILES_ACTIVE=dev`.

The `dev` profile initializes H2 from `schema.sql` and enables the H2 console at `/h2-console`.

## Environment Variables

### Backend (required for default profile)
```bash
ANTHROPIC_API_KEY=sk-ant-...      # Required for LLM calls
DB_HOST=localhost                   # PostgreSQL host
DB_PORT=5432                        # PostgreSQL port
DB_NAME=mymanus                     # Database name
DB_USER=mymanus                     # Database user
DB_PASSWORD=mymanus                 # Database password
AUTH_ENABLED=false                  # Enable JWT auth (production)
JWT_SECRET=change-in-production     # JWT signing key
DOCKER_HOST=unix:///var/run/docker.sock  # Docker socket
SANDBOX_MODE=docker                 # "docker" or "host"
```

### Frontend
```bash
VITE_API_BASE_URL=http://localhost:8080   # Backend URL
VITE_WS_URL=ws://localhost:8080/ws        # WebSocket URL
```

Copy `.env.example` to `.env` in both `backend/` and `frontend/` directories.

## Known Gotchas

1. **Spring Boot 3.5.6 is a milestone release.** It resolves from `https://repo.spring.io/milestone`, not Maven Central. If your network blocks this repository, the build will fail with "Non-resolvable parent POM". The `<repositories>` section in `pom.xml` configures this.

2. **Dual application config.** Both `application.yml` and `application.properties` exist in `backend/src/main/resources/` with overlapping settings. Spring Boot loads both — properties from `.properties` and `.yml` are merged, with `.properties` taking precedence for duplicates.

3. **Frontend ESLint has pre-existing errors.** Running `npm run lint` reports ~137 errors (mostly `@typescript-eslint/no-explicit-any`). TypeScript compilation (`tsc -b`) passes clean.

4. **3 pre-existing test failures** in `frontend/src/components/__tests__/MessageItem.test.tsx` — tests query for CSS classes (`.justify-start`, `.justify-end`) that don't exist in the rendered output. These are not regressions.

5. **Lombok is required.** All backend model, service, and controller classes use Lombok annotations (`@Data`, `@Builder`, `@Slf4j`, `@RequiredArgsConstructor`). IDEs need the Lombok plugin installed, and annotation processing must be enabled.

6. **Docker socket access.** The backend needs access to `/var/run/docker.sock` to manage sandbox containers (default profile). In dev profile, set `sandbox.mode=host` to skip Docker.

7. **Port conflicts.** Backend: 8080, Frontend dev: 5173 (Vite default), Frontend Docker: 3000, PostgreSQL: 5432. The Vite dev server proxies `/api` and `/ws` to port 8080.

## IDE Setup

### IntelliJ IDEA
- Import as Maven project (open `backend/pom.xml`)
- Enable annotation processing: Settings > Build > Compiler > Annotation Processors > Enable
- Install Lombok plugin
- Set Project SDK to Java 17+
- For frontend: open `frontend/` as a separate project or use the JavaScript plugin

### VS Code
- Java Extension Pack (for backend)
- Lombok Annotations Support
- ESLint extension (for frontend)
- Vite extension (for frontend dev server)
