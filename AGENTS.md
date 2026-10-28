# VetHub: Architecture Guide

VetHub is a veterinary clinic management app (owners, pets, pet types, vets, specialties, visits). It is a monorepo with a Spring Boot REST API (`server/`) and a SvelteKit frontend (`client/`). The project is a training workshop, so some intentional gaps exist (see [Known gaps](#known-gaps)).

## Tech stack

| Part | Stack |
|---|---|
| Backend (`server/`) | Java 25, Spring Boot (Web, Data JPA, Validation, Security, Actuator), Gradle (Kotlin DSL) |
| Persistence | H2 in-memory database, Spring Data JPA, Liquibase |
| Backend libraries | MapStruct (mapping), Lombok, springdoc-openapi 3.0.1, jframe `starter-jpa`, Logstash Logback encoder |
| Backend quality | Checkstyle, PMD, SpotBugs (`ru.vyarus.quality`), Spotless |
| Backend tests | JUnit 5, Mockito, Hamcrest, Spring `MockMvc` |
| Frontend (`client/`) | SvelteKit 2, Svelte 5, TypeScript, Vite 7, Bun |
| Frontend UI | Tailwind CSS 4, shadcn-svelte, bits-ui, lucide icons, svelte-sonner (toasts) |
| HTTP and types | `openapi-fetch` (client), `openapi-typescript` (type generation) |
| Tooling | `mise.toml` pins Java (Temurin 25), Bun 1.3.0, Node 22.20.0 |

## Project structure

```
vethub/
├── mise.toml                  tool versions, SERVER_PORT=8080, SPRING_PROFILES_ACTIVE=dev
├── scripts/
│   ├── openapi-sync.sh        regenerate frontend API types from the backend
│   └── common.sh              helpers (start/stop backend, logging)
├── server/
│   └── src/
│       ├── main/java/dev/ilionx/workshop/
│       │   ├── Application.java
│       │   ├── api/Paths.java          all route constants
│       │   ├── api/<feature>/          owner | pet | vet | visit
│       │   │   ├── controller/
│       │   │   ├── service/
│       │   │   ├── repository/
│       │   │   └── model/  (entity, request/, response/, mapper/, validator/)
│       │   └── common/  config/ (+ properties/), exception/ (ApiErrorCode), security/
│       ├── main/resources/
│       │   ├── application.yml, application-dev.yml
│       │   └── db/changelog/  db.changelog-master.yaml, db.changelog-plugin.yaml, changesets/
│       └── test/  support/ (base classes) + tests mirroring main packages
└── client/
    └── src/
        ├── routes/            owners/[id]/pets/[petId]/visits/new, vets/, visits/
        └── lib/
            ├── api/           client.ts, models.ts, <feature>/<Feature>Controller.ts
            ├── components/    ui/ (shadcn), layout/, owners/, pets/, vets/
            ├── config/        constants.ts (base URL, credentials)
            └── types/         api.d.ts, openapi.json (GENERATED, do not edit)
```

Pet types are served by their own controller and service inside the `pet` package. Specialties are in the `vet` package.

## How the layers connect

```
Browser (SvelteKit, :5173)
   │  Svelte page/component
   ▼
lib/api/<feature>/*Controller.ts      thin wrappers; throw on error
   ▼
openapi-fetch `client`                typed by generated api.d.ts, adds Basic Auth header
   │  HTTP + JSON  →  http://localhost:8080/api/v1/...
   ▼
Spring MVC Controller                 routing, status codes, OpenAPI annotations
   ├─ Validator                        (where present) hand-written request validation
   ├─ Service  (@Transactional)       business rules, loads related entities, maps request → entity
   │     └─ Repository (Spring Data JPA)  → H2 (schema from Liquibase)
   └─ Mapper (MapStruct)              entity → response DTO
```

**Example flow, creating a pet:** `POST /api/v1/owners/{ownerId}/pets` goes to `PetController.createPet`, then `PetService.create`. The service loads the `Owner` and `PetType` (throwing `DataNotFoundException` → 404 if either is missing), builds a `Pet`, and calls `petRepository.save`. A row is inserted into `pets`. `PetMapper` converts the entity into a `PetResponse`, returned as `201 Created`.

## Backend conventions

### Layering rules
- **Package by feature, then by layer.** New features go in `api/<feature>/{controller,service,repository,model}`. Use `owner` as the reference implementation.
- **Controllers are thin.** Validate (if a validator exists), call the service, map the result with the mapper, return a `ResponseEntity` with an explicit status. Annotate each method with `@ResponseStatus`. No business logic in controllers.
- **Services own business logic and transactions.** Use `@Transactional(readOnly = true)` for reads and `@Transactional` for writes. Inject with Lombok `@RequiredArgsConstructor` and `private final` fields.
- **Repositories** are interfaces extending `JpaRepository<Entity, Integer>`. Use derived queries (`findByLastName`, `findByOwnerId`, `findByPetId`) rather than `@Query` unless needed.
- **Routes** live in `api/Paths.java` as constants (`BASE_PATH = "/v1"`, `OWNERS`, `OWNER_BY_ID`, `OWNER_PETS`, `OWNER_PET_BY_ID`, `PET_VISITS`, ...). The servlet context path `/api` is added separately, in `application.yml`. Use the constants in controllers and tests, never string literals.

### Naming patterns
| Thing | Pattern | Example |
|---|---|---|
| Entity | `<Feature>` | `Owner`, `Pet` |
| Table / columns | plural snake_case / snake_case | `owners`, `first_name` |
| Controller | `<Feature>Controller`; cross-owner variants `<Feature>GlobalController` | `PetController`, `PetGlobalController` |
| Service / Repository | `<Feature>Service`, `<Feature>Repository` | `VisitService` |
| Request DTOs | `Create<Feature>Request`, `Update<Feature>Request` | `CreatePetRequest` |
| Response DTOs | `<Feature>Response`, `<Feature>SummaryResponse` for nested views | `OwnerResponse`, `PetSummaryResponse` |
| Mapper / Validator | `<Feature>Mapper`, `<Feature>Validator` | `OwnerMapper` |
| Error codes | `<ENTITY>_NOT_FOUND` constants in `ApiErrorCode` | `PET_NOT_FOUND` |
| Controller methods | `getAll<X>s`, `get<X>ById`, `create<X>`, `update<X>`, `delete<X>` | `getOwnerById` |
| Service methods | `findAll`, `findById`, `findBy<Field>`, `create`, `update`, `delete` | `findByOwnerId` |

### DTO pattern: how to apply it
Entities (`model/<Feature>.java`) are never accepted from or returned to clients. For each feature:

1. **Request DTOs** in `model/request/`: `Create<X>Request` and `Update<X>Request`, kept as separate classes even when fields match. Lombok `@Data @NoArgsConstructor @Accessors(chain = true)`. Annotate every field with `@Schema(description, example, requiredMode)`.
2. **Response DTOs** in `model/response/`: `<X>Response` (Lombok `@Getter @Setter @NoArgsConstructor @Accessors(chain = true)`) with `@Schema` on each field. Nest related data as `<Y>SummaryResponse` records, not full child responses, to avoid deep or cyclic payloads.
3. **Entity → response with MapStruct:** `model/mapper/<X>Mapper` is an abstract class annotated `@Mapper(config = SharedMapperConfig.class)`. It declares `toResponse(...)` and `toResponseList(...)`. Use `@Mapping(source = "owner.id", target = "ownerId")` for flattened fields. Add abstract methods for each nested type it needs (e.g. `toPetSummaryResponse`). Controllers inject the mapper and call it after the service returns.
4. **Request → entity by hand, in the service.** The service loads related entities by id (owner, pet type, pet), copies fields with setters onto a new or existing entity, then calls `repository.save`. MapStruct is not used for requests because they need lookups.
5. **Validation of requests** goes in `model/validator/<X>Validator`, called from the controller on writes.

Adding a field means touching, in order: the changeset, the entity, the request and response DTOs, the service copy code, the validator, and the mapper (if the name differs). Then re-run `sync:api` for the frontend.

### Error handling style
- **Missing resource:** `throw new DataNotFoundException(<X>_NOT_FOUND)` from the service, usually inside `.orElseThrow(...)` on a repository `findById`. The enum constant comes from `ApiErrorCode`.
- **Adding a new error:** add a constant to `ApiErrorCode` with the next sequential code (`"ERR-0008"`) and a sentence-style message ending with a period ("The requested X does not exist."). Statically import the constant in the service.
- **Verify parent ownership in the service.** For nested routes, check the child belongs to the parent and throw the child's `_NOT_FOUND` (see `PetService.findByIdAndOwnerId`).
- **Validation errors:** validators collect field errors in a `ValidationResult` and throw `ValidationException(result)`. Use constants for messages (`FIRST_NAME_REQUIRED`) and field names (`FIRST_NAME`), and reject null/empty/blank separately.
- **No try/catch in controllers or services.** `DataNotFoundException` and `ValidationException` come from the jframe library, and there is no `@ControllerAdvice` in this repo. HTTP error mapping is handled by the library. Check how it maps a new exception type before adding one.
- **Frontend side:** API modules throw (`if (error) throw error;`) and the page catches with `toast.error(...)` plus `console.error`.

### Liquibase migrations: where they go
- **Location:** `server/src/main/resources/db/changelog/changesets/`. `db.changelog-master.yaml` uses `includeAll` for that folder, so new files are picked up automatically and run in filename order. Do not edit the master file. `db.changelog-plugin.yaml` points at the same folder for the Gradle Liquibase plugin.
- **Filename:** `<yyyyMMddHHmm>-<PRD|TST>-<description>.xml`, e.g. `202507101200-PRD-initial-schema.xml`, `202507101201-TST-seed-data.xml`. The timestamp must be later than existing files so ordering is preserved.
- **Schema changes (`PRD`):** no `context` attribute, so they run everywhere. Use raw `<sql>` inside a `<changeSet id="<filename-without-ext>-<n>" author="...">` with a `<comment>`. Existing style is `CREATE TABLE IF NOT EXISTS`, `INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY`, explicit `FOREIGN KEY`s.
- **Seed or test data (`TST`):** set `context="tst"` on each changeset. After inserting explicit ids, restart the identity (`ALTER TABLE pets ALTER COLUMN id RESTART WITH 14`).
- **Never edit an applied changeset.** Add a new file instead. The schema is not generated from entities (`ddl-auto: none`), so every entity change needs a matching changeset.
- **Contexts:** the base config runs `prd`, and the test config (`application.yml` `drop-first: true`, `application-test.yml`) runs `tst`. Integration tests rely on the seed ids (vets/pet types 1–6, specialties 1–3), so adding seed rows may need changes in `IntegrationTest.cleanDatabase()`.

### API documentation
Annotate controllers with `@Tag` and methods with `@Operation(summary, description)`. DTO fields get `@Schema`. This feeds springdoc and therefore the frontend types, so write them carefully. Docs are at `/api/v1/public/docs` (JSON) and `/api/v1/public/docs/openapi.html` (Swagger UI).

### Style
Spotless and the quality plugin (Checkstyle, PMD, SpotBugs) enforce formatting. Use `final` on parameters and locals, Javadoc on public service methods, and `// ===== SECTION =====` dividers in longer classes.

## Frontend: openapi-fetch with generated types

The frontend never hand-writes API types. They are generated from the backend's OpenAPI spec.

**1. Generate types** (from `client/`, after any backend API change):
```bash
bun run sync:api    # runs scripts/openapi-sync.sh
```
This starts the backend, downloads the spec to `server/openapi.json`, and runs `openapi-typescript` to write `src/lib/types/api.d.ts` (`paths` and `components` types). `download:api` and `generate:api` are the manual halves of the same process. Commit `api.d.ts` and `openapi.json` along with the backend change. Never edit them by hand.

**2. One typed client** (`lib/api/client.ts`):
```ts
export const client = createClient<paths>({
  baseUrl: SERVER_BASE_URL,   // VITE_SERVER_BASE_URL or http://localhost:8080, plus /api
  headers: { 'Content-Type': 'application/json', Authorization: createAuthHeader() }
});
```
Because the client is generic over `paths`, the URL string, path/query params, body and response are all type-checked. Paths in calls omit `/api` and start at `/v1/...`.

**3. Friendly type names** (`lib/api/models.ts`): re-export schemas as `export type OwnerResponse = components['schemas']['OwnerResponse']`. Application code imports from `$lib/api/models`, and never from `$lib/types/api`. Add a line here for each new DTO.

**4. One thin API module per feature** (`lib/api/<feature>/<Feature>Controller.ts`). It contains only HTTP calls:
```ts
export async function getOwnerById(id: number): Promise<OwnerResponse> {
  const { data, error } = await client.GET('/v1/owners/{id}', { params: { path: { id } } });
  if (error) throw error;
  if (!data) throw new Error('Owner not found');
  return data;
}
export async function createOwner(request: CreateOwnerRequest) {
  const { data, error } = await client.POST('/v1/owners', { body: request });
  ...
}
```
Conventions: functions are named `get<X>s`, `get<X>ById`, `create<X>`, `update<X>`, `delete<X>`. For nested routes, use `<verb><X>For<Parent>`, e.g. `createPetForOwner(ownerId, Omit<CreatePetRequest, 'ownerId'>)`. They return `data` (or `data ?? []` for lists), and throw on `error`. They contain no UI or business logic.

**5. Pages call the modules** inside async functions in `src/routes/`, with Svelte 5 runes (`$state`, `$derived`), and report failures with `toast.error` from svelte-sonner. There are no SvelteKit `load` functions. Data is fetched client-side.

**6. Other:** configuration comes from `VITE_SERVER_BASE_URL`, `VITE_API_USERNAME` and `VITE_API_PASSWORD`. A response middleware in `client.ts` logs non-2xx responses. Types are compile-time only, with no runtime response validation. If the spec is stale, TypeScript passes but the app can break, so re-run `sync:api` whenever the backend contract changes. Verify with `bun run check`.

## Testing (backend only; the frontend has no tests)

Tests live in `server/src/test/java/...`, mirroring the main packages. Name them `<Class>Test`. Use `@DisplayName("Unit Test - <X>")` or `@DisplayName("Integration Test - <X>")` on the class, a `@DisplayName` on every test, `// Given: / When: / Then:` comments, Hamcrest assertions, and `should<Result>When<Condition>` method names.

### When to use which base class

| Testing... | Extend | Why |
|---|---|---|
| A service (`*ServiceTest`) | `UnitTest` | Mockito mocks the repositories and the service is built by hand (`new OwnerService(mock)`). Fast, no Spring. |
| A validator (`*ValidatorTest`) | `UnitTest` | Pure logic. Instantiate with `new`. |
| A mapper or other plain class | `UnitTest` | No dependencies on Spring. |
| A controller (`*ControllerTest`) | `IntegrationTest` | Exercises routing, JSON, validation, service, repository and the real H2 database through `MockMvc`. |
| A new query method on a repository, or a Liquibase change | `IntegrationTest` | Needs the real schema and seed data. |

- **`UnitTest`** (`support/UnitTest.java`): enables `MockitoExtension` and provides `aValidOwner()`, `aValidPet()`, `aValidPetType()`, `aValidVisit()`, `aValidVet()`, `aValidSpecialty()` (ids set, ready to use in mocks). Use `given(repo.method()).willReturn(...)`. Don't start Spring.
- **`IntegrationTest`** (`support/IntegrationTest.java`, extending `WebMvcConfigurator` → `TestContextInitializer`):
  - `TestContextInitializer` is `@SpringBootTest(RANDOM_PORT)` with the `test` profile and autowires all repositories (`ownerRepository`, `petRepository`, ...).
  - `WebMvcConfigurator` creates `mockMvc` before each test.
  - `IntegrationTest` cleans the database before and after each test, keeping seed rows (vet/pet type id ≤ 6, specialty id ≤ 3).
  - Use `aSavedOwner()`, `aSavedPet(owner)`, `aSavedVisit(pet)` to arrange data, and `aCreate<X>Request()` / `anUpdate<X>Request()` to build bodies.
  - Call endpoints with `mockMvc.perform(get(OWNERS))` using `Paths` constants. The test profile uses context path `""`, so there is no `/api` prefix.
- **When you add an entity or DTO,** add its factories to the base classes (`aValid<X>()` in `UnitTest`; `aSaved<X>()` and request factories in `IntegrationTest`), and extend `cleanDatabase()` so new tables are emptied in foreign-key order.
- Integration tests rely on Liquibase `tst` seed data (`application-test.yml`), so don't assume empty tables for pet types, vets and specialties.

## Commands

```bash
# Backend (from server/)
./gradlew bootRun            # API on :8080, base path /api
./gradlew test               # unit + integration tests
./gradlew spotlessApply      # format code

# Frontend (from client/)
bun install
bun run dev                  # Vite dev server on :5173
bun run check                # svelte-check / type check
bun run build
bun run sync:api             # regenerate API types (see above)
```

## Workflow for common changes

- **Add or change an endpoint or DTO field:**
  1. Edit the controller, service, DTOs (with `@Schema`), mapper and validator.
  2. Add a Liquibase changeset if the schema changes.
  3. Add or update tests (unit for service/validator, integration for the controller).
  4. Run `bun run sync:api` in `client/`.
  5. Update `models.ts`, the API module and the pages.
- **Add a new feature:** copy the `owner` package. Add routes to `Paths.java`, error codes to `ApiErrorCode`, a changeset, test factories and tests, and a frontend API module with its `models.ts` exports and routes.

## Security and CORS

- HTTP Basic is configured (demo user `user` / `password`), but `WebSecurityConfig` has `anyRequest().permitAll()` and CSRF disabled, so authentication is not enforced.
- CORS allows `http://localhost:*` (`security.cors.allowed-origin`). The frontend calls the backend directly, with no dev proxy.

## Known gaps

This is a workshop project, so some gaps are deliberate. Don't assume they are bugs in your own change.
- `Owner.java` has no `@NotBlank` on the name fields, and `Pet.java` has no `@Past` on `birthDate` (marked with `// BUG` comments).
- `PetController` has no validator, unlike `OwnerController`. Missing fields in pet requests fall through to database constraints, and future birth dates are accepted.
- Generated frontend types can drift if `sync:api` isn't re-run, and nothing validates API responses at runtime.
- Data lives in an in-memory H2 database and is reset on every restart.
