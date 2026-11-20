# Spring Web implementation

This branch (`web-spring-crud-impl`) adds Spring MVC REST endpoints and related
web-layer functionality for Grid Berries. The work follows the CRUD, validation
and exception-handling, and search-and-pagination tasks.

## What was implemented

### REST endpoints and DTOs

Controllers accept request DTOs and return response DTOs rather than exposing
JPA entities as the API contract. MapStruct mappers translate between the
request/response models and domain objects.

| Area | Implemented operations |
| --- | --- |
| Authentication | Register, log in, refresh tokens, and log out |
| Products | Create, list/search, get by ID, update, patch, delete, and moderate status |
| Categories | List root categories and children, get by ID, create, patch, and delete |
| Cart | Get the current cart, add an item, change item quantity, remove an item, and clear the cart |
| Orders | Place an order, list the current buyer's orders, get an order, list seller orders, and update order status |
| Seller profile | Get, create, and update the current seller profile |
| Seller products | List products belonging to the current seller |
| Admin | Look up and moderate users, and moderate products |

Endpoints use conventional response statuses where implemented: for example,
product creation returns `201 Created` with a `Location` header, and delete or
logout operations return `204 No Content`.

### Persistence and authentication

The branch uses Spring Data JPA repositories and Liquibase migrations for the
application tables, including the refresh-token table. Product, category, cart,
order, seller profile, and user workflows are wired through services and
mappers.

JWT support includes token generation and validation configuration, user and
seller identity lookup from the authentication context, and refresh-token
rotation/revocation. The authentication API provides register, login, refresh,
and logout endpoints.

### Validation and error responses

Request models define Bean Validation constraints for required values, lengths,
email/password formats, numeric ranges, URLs, and nested order items.
Controllers trigger request-body validation with `@Valid`. The custom
`@PasswordMatch` constraint validates matching registration passwords and
reports the mismatch against `passwordConfirm`.

The global exception handler returns Spring `ProblemDetail` responses for
validation failures, constraint violations, and unhandled errors. Validation
problems include an `errors` map keyed by the invalid field or parameter, so
clients can distinguish multiple errors in one response. The handler avoids
returning the exception message for unexpected server errors.

### Product search and pagination

`GET /api/v1/products` accepts product search criteria and Spring `Pageable`
parameters. Search criteria currently support:

- `query`: case-insensitive partial match against title or description;
- `categoryIds`: exact category ID match, accepting multiple IDs;
- `minPrice` and `maxPrice`: inclusive price bounds.

Spring's pageable resolver supplies page, size, and sorting. The default page
size is 15, with products sorted by `createdAt` ascending unless the request
specifies another sort. The response wraps product content and `Slice` metadata
(`currentPage`, `pageSize`, `hasNext`, `hasPrevious`, `isFirst`, and `isLast`).

## Tests

Controller tests use `@WebMvcTest` and MockMvc to exercise endpoint routing,
request/response JSON, status codes, validation, and service delegation. The
global exception handler has MockMvc tests for field and parameter validation,
method-not-allowed responses, and unexpected errors. Service unit tests and a
user-service integration test are also present.

Most controller slice tests disable servlet filters, so they check MVC behavior
but do not verify JWT authentication or role authorization. The current tests
also do not establish that the project meets the task's 80% coverage target;
coverage has to be measured separately.

## Remaining work and limitations

- `UserController.getUserProfile()` is still a placeholder that returns `null`;
  the `/api/v1/users/me` endpoint is not implemented.
- Product pagination returns a `Slice`, not a `Page`. It does not report a
  total item count or maximum page count, so it only partially meets the task's
  request for total pagination metadata.
- CRUD coverage is not uniform across all resources. For example, categories
  have no dedicated endpoint to list every category independently of root or
  child queries, and seller products are read-only in this controller.
- Authorization behavior and the 80% coverage threshold need dedicated
  verification before considering those checklist items complete.

## Key code locations

- Controllers: `src/main/java/org/capstone/controller/`
- Request and response models: `src/main/java/org/capstone/dto/`
- Authentication and JWT: `src/main/java/org/capstone/security/` and `src/main/java/org/capstone/config/security/`
- Product filtering: `src/main/java/org/capstone/service/search/`
- Liquibase migrations: `src/main/resources/db/changelog/`
- Tests: `src/test/java/org/capstone/`
