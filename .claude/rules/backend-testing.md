---
paths:
- 'backend/src/test/**'
---

# Backend Testing Rules

Three distinct test patterns exist. ALWAYS use the correct one:

**Controller tests**: `@WebMvcTest(XController.class)` + `@Import(TestSecurityConfig.class)`. `TestSecurityConfig` disables auth. ALWAYS `@MockBean` every service the controller injects — `@WebMvcTest` does not create them. Use `@Autowired MockMvc` and `ObjectMapper`.

**Service unit tests**: `@ExtendWith(MockitoExtension.class)` with `@Mock`/`@InjectMocks`. ALWAYS use `ReflectionTestUtils.setField()` in `@BeforeEach` to inject `@Value` fields (like `maxIterations`) — `@InjectMocks` does not resolve `@Value`.

**Integration tests**: `@SpringBootTest(classes = MyManusApplication.class)` + `@ActiveProfiles("test")` + `@Import(IntegrationTestConfiguration.class)` + `@AutoConfigureTestDatabase(replace = ANY)`. `IntegrationTestConfiguration` mocks `AnthropicService` to avoid real API calls. `replace = ANY` forces H2.

ALWAYS name test classes `{ClassName}Test` for unit tests and `{Feature}IntegrationTest` for integration tests.

ALWAYS use `lenient()` for mock stubs in `@BeforeEach` that not every test method uses — strict Mockito will fail otherwise.

NEVER call real `AnthropicService` in tests. It requires a live API key. Always mock it or use `IntegrationTestConfiguration`.
