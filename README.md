# Homework - Notification Dispatch Service

Click **Use this template → Create a new repository**. Create a **private** repository under your own account and invite orion-homework-dev as a collaborator. Complete the assignment and send us your repository link. Please do not publish your solution or open a pull request against this template.

## Objective

Refactor the legacy code to a production ready solution for the notification dispatch service that is maintainable, readable, and scalable using modern practices.

This exercise is intended to be completed in approximately 4 hours.

The application sends `EMAIL`, `SMS`, and `PUSH` notifications, stores the outcome of those attempts, and enforces the operational safeguards already present in the system. 
The goal is to improve the design so the code is easier to understand, easier to extend, and more robust without changing the behavior that matters.

This is a design and refactoring exercise. The focus is on making a small system feel thoughtful, clean, and ready for change. 
It also acknowledges that existing API boundaries and persistence design may not be ideal and should be improved.

## System summary

The service is responsible for:

- sending notifications to different channels
- tracking the result of each attempt
- rejecting unsupported notification types
- rejecting blocked or rate-limited recipients
- recording failures and applying a simple rejection path after repeated problems

## Core expectations

The solution should be clear and defensible.

The project should demonstrate sound engineering judgment through:

- clear, readable code
- sensible use of current best practices
- no unnecessary complexity

## Behavior to preserve

The system must continue to support the existing notification types and operational safeguards:

- `EMAIL`
- `SMS`
- `PUSH`
- blocked or rate-limited recipients are rejected
- unsupported notification types fail clearly
- failures are tracked
- repeated failures eventually trigger a simple rejection path

## What matters most

We care about:

- correctness
- maintainability
- readability
- clear design decisions
- practical modernization
- scalability

## Development process

Please keep a record of the changes you make and why you make them. Preferably, use small, meaningful Git commits with messages that explain both the change and its rationale. Brief notes in the README can supplement the commit history, especially for trade-offs or alternatives you considered.

## Constraints

The solution must use Java and Spring Boot. You may upgrade the Java version and dependencies.

Everything else may be redesigned or rewritten, including the API, database schema, persistence approach, and project structure, provided the behavior described above is preserved.

Keep the solution proportional to the problem. The goal is to transform a small but messy system into something clean, understandable, and fit for future change.


## Bonus points

These are not required for the core task, but they can earn additional credit if included well:

- adding a update endpoint for notifications
- introducing a proper blacklist functionality to enforcement flow for blocked recipients

## Design notes

### Structure
- **Spring Boot 3 on plain JDBC** (`JdbcTemplate`, H2 in memory). The data model is a single log table plus a blacklist, so an ORM would add more than it removes.
- **Package-by-layer** (`controller`, `service`, `repository`, `dto`, `model`, `handler`, `config`, `exception`, `util`), kept flat. With one feature, nesting layers inside a feature package would add depth without separating anything. If more features arrive, the layout can move to feature packages with the layers nested inside.
- **Channels are pluggable.** Each channel is a `NotificationHandler` component that declares its own type. The service receives all handlers as a list, so a new channel is one new class and no edits to the service.
- **Enums instead of codes.** `NotificationStatus` and `NotificationType` replace the one-letter status codes and string comparisons. The database still stores the same codes; conversion happens at the boundary. The visible API change is that JSON `status` is now `"SENT"` instead of `"S"`, and `blocked` is a boolean.
- **Row mapping lives in the repositories.** Each repository keeps a private static `RowMapper` next to the SQL whose columns it reads, so a column change touches one file, and a separate mapper class would have had a single user. The trade-off is that the repository returns the API response record directly, so an API type reaches the persistence layer. If a separate domain object is introduced, or several queries need the same mapping, the row mapping and an entity-to-DTO mapper should move into their own classes.

### API and errors
- Expected failures are typed exceptions under a `NotificationException` base and mapped in one `@RestControllerAdvice` to RFC 7807 problem responses: invalid request or unsupported type `400`, blacklisted `403`, unknown id `404`, duplicate blacklist entry or non-editable notification `409`, rate limited `429`, delivery failure `502`, circuit breaker open `503`. Anything unexpected is a generic `500` that does not leak internals.
- Request validation uses Bean Validation, with size limits that match the column sizes, so oversized input is a `400` rather than a database error.
- **Update endpoint:** `PATCH /api/notifications/{id}` edits the target and/or payload of a finished (`SENT` or `ERROR`) notification. Nothing is re-sent, and the status, counters and failure message are untouched, so history stays an honest record. The status check is part of the `UPDATE` statement, so it is atomic.

### Dispatch flow and concurrency
- Ids come from an identity column, and the counters are computed inside the single insert statement, so there is no read-then-write in application code and no `synchronized`.
- **Short transactions.** Admission (policy check, insert, breaker read) is one transaction. Delivery runs outside any transaction, so a slow channel never holds a connection or a lock. The outcome is one atomic `UPDATE`.
- Rejections are thrown only after the admission transaction commits. Throwing inside it would roll back the `REJECTED` and `UNKNOWN` rows that are meant to be kept.
- **Circuit breaker:** its state is derived from the last completed delivery (`SENT` or `ERROR` rows only), so it is shared across instances and rejected or unsupported attempts never influence it. After a configurable cooldown one trial dispatch is allowed: success closes it, failure reopens it. It stays global across channels, as in the original behavior. A `Clock` is injected so the cooldown is tested without sleeping.
- **Configuration** lives in `application.properties` and is bound to records (`notification.circuit-breaker.*`, `notification.rate-limit.*`).

### Blacklist and rate limit
- **Both are enforced by `RecipientPolicy`** before delivery. Refused attempts are stored as `REJECTED` rows with the blocked flag set (an audit trail), and are not counted towards the rate limit, so being refused never extends a lockout.
- **Blacklist API:** `POST`, `GET` and `DELETE /api/blacklist`. There is no update endpoint, since an entry is only a target and a reason.
- **Recipient matching is case-insensitive only for `EMAIL`** targets, in both the blacklist and the rate limit. SMS numbers and push tokens are matched exactly, since tokens can be case-sensitive. Blacklist entries are stored as entered (trimmed), so two entries differing only in case can coexist; an email request matches either.
- **Logs mask recipients** (`b***@example.com`) so personal data is not written to them. The payload is never logged.

### Known trade-offs
- **Rate limit under concurrency:** the check counts rows and then inserts, which is not atomic. A burst of concurrent requests for one recipient can slightly exceed the limit. Making it strict would need a per-recipient lock or a counter row; this was left out to keep the solution proportional.
- **Half-open breaker:** concurrent requests can all pass as trials before the first outcome is recorded.
- **Counters:** the failure counter is copied from the latest row at insert and the dispatch counter is a global `max + 1`, so concurrent requests can undercount or duplicate them. Treat them as soft counters.
- **Crash between admission and outcome:** a row can stay `NEW` with the message `START`. Delivery cannot sit inside the transaction without losing the failure record. A scheduled cleanup of stale `NEW` rows would handle it.
- **Schema:** `schema.sql` drops and recreates the tables on startup, which is fine for the in-memory demo but not for a real database, where a migration tool such as Flyway should replace it. Column names are kept from the original schema.
- **Log injection:** the user-supplied `type` is logged. It is limited to 32 characters, but it is not stripped of newlines.

### Possible next steps
Flyway migrations with readable column names, per-channel circuit breakers, a cleanup job for stale `NEW` rows, structured validation errors that name the field, and pagination for the history endpoint.
