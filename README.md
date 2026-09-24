# Rental Tracker

A CLI app for tracking your own rentable items — listing, renting out, and
confirming returns — built on a five-layer architecture over SQLite.

## Architecture

```
transport/        Menu, prompts, pagination — talks to the person, not the DB
service/           Business rules and the item/rental state machines
repository/        The only code that touches SQL
domain/            Plain data classes + status enums
infrastructure/    Connection management (DatabaseManager, OwnerConfig)
exception/         Typed exceptions the repository layer throws
```

Each layer only calls downward: `transport` calls `service`, `service` calls
`repository`, `repository` calls `infrastructure`. Nothing above `repository`
ever writes SQL directly, and nothing in `repository` knows about menus.

### State machines

`ItemStatus` and `RentalStatus` each carry their own `canTransitionTo(...)`
method — the service layer must call it before changing a status, so the
legal-transition rules live in one place, in code, not just as a database
CHECK constraint.

- `AVAILABLE ↔ RENTED`, `AVAILABLE → UNLISTED`, `RENTED → UNLISTED`, `UNLISTED → AVAILABLE`
- Relisting also checks `RentalRepository.hasActiveRentalForItem(...)` — an
  item can be `UNLISTED` while still out on an active rental (you delisted it
  mid-rental), and that combination blocks relisting until the rental closes.
- On confirm-return: if the item's current status is `UNLISTED` (delisted
  while it was out), it stays `UNLISTED`; otherwise it goes back to `AVAILABLE`.

### Exception handling

Every repository method wraps its SQL in try/catch. `SqlExceptionTranslator`
inspects the SQLite error code and converts it into one of:

- `DatabaseConnectionException` — can't open the file
- `ConstraintViolationException` (with a `ConstraintType`: UNIQUE, NOT_NULL,
  CHECK, FOREIGN_KEY) — a write broke a table rule
- `NotFoundException` — a query expected a row and got none
- `MappingException` — a row wouldn't map into its domain object

`CliApp` catches these (plus the service layer's `BusinessRuleException`) at
the menu loop level, prints the message, and returns to the menu instead of
crashing.

### The "owner" account

The `users` table has no owner flag — it holds the single owner and every
renter in one table, exactly as specified. So `OwnerConfig` persists which
user id is "you" in a small `owner.cfg` file next to the database: written
once on first launch, read on every launch after.

## Setup

```bash
mvn dependency:resolve   # pulls sqlite-jdbc, JUnit, JaCoCo, shade
```

## Running the app

```bash
mvn compile exec:java -Dexec.mainClass="com.tracker.rentaltracker.Main"
```

To start with the seeded sample data instead of an empty inventory:

```bash
mvn compile exec:java -Dexec.mainClass="com.tracker.rentaltracker.Main" -Dexec.args="--seed"
```

This loads `sample-data.sql` (14 items, 6 active rentals, 4 users) and points
your account at user id 1 (`liisa`), the sample data's owner, so the seeded
items show up as yours instead of under a brand-new account. Running the seed
command again keeps existing rows and skips sample rows whose IDs are already
present.

Or build a runnable JAR:
```bash
mvn package
java -jar target/rental-tracker-1.0-SNAPSHOT.jar --seed
```

## Running tests + coverage

```bash
mvn test
```

JaCoCo runs automatically and writes `target/site/jacoco/index.html` — open
that in a browser for the coverage report. Every test opens its own temp
SQLite file (via `TestDatabaseFactory`, using JUnit 5's `@TempDir`) and runs
against real SQLite, never a mock, per the project's testing requirements.

Test groups (matching the brief):
- `repository/*Test.java` — data layer CRUD, plus one test per constraint type
  (UNIQUE, NOT_NULL, CHECK, FOREIGN_KEY), each asserting the *specific*
  exception thrown, not just that the write failed.
- `service/*Test.java` — behavior tests: state transitions and business rules,
  through the service layer, not the repository directly.
- `domain/ItemStatusTest.java` — pure unit tests of the state machine, no
  database involved.

