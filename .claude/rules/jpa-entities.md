---
paths:
- 'backend/src/main/java/ai/mymanus/model/**'
- 'backend/src/main/java/ai/mymanus/repository/**'
- 'backend/src/main/resources/schema.sql'
---

# JPA Entity and Persistence Rules

ALWAYS use `@Column(columnDefinition = "json")` with `@Convert(converter = JsonMapConverter.class)` for Map/JSON columns. NEVER use `columnDefinition = "jsonb"` — the H2 dev profile does not support JSONB, and this mismatch caused three separate bug-fix commits (`28b8bcf`, `7d20c84`, `4769723`).

Correct pattern for JSON columns:
```java
@Convert(converter = JsonMapConverter.class)
@Column(columnDefinition = "json")
private Map<String, Object> executionContext;
```

ALWAYS include all four Lombok annotations on entity classes: `@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`. Missing `@NoArgsConstructor` breaks JPA proxy instantiation. Missing `@Builder` breaks test setup code that uses `.builder()`.

ALWAYS add `@Builder.Default` to fields with default values (e.g., `private Status status = Status.IDLE`). Without it, `@Builder` ignores the default and sets null.

ALWAYS add `@PrePersist` and `@PreUpdate` lifecycle callbacks for `createdAt`/`updatedAt` fields, following the pattern in `AgentState`. Do not rely on database defaults — H2 dev profile doesn't support them the same way PostgreSQL does.

ALWAYS update `schema.sql` when adding a new entity. The dev and test profiles use `schema.sql` to initialize H2 — a missing table definition breaks `mvn spring-boot:run -Dspring.profiles.active=dev`.

ALWAYS create a corresponding `@Repository` interface in `repository/` extending `JpaRepository<EntityClass, UUID>`. Follow the naming convention: `{EntityName}Repository`.
