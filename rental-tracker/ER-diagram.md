# Rental Tracker — ER Diagram

GitHub renders this Mermaid diagram natively when you view this file in the repo.

```mermaid
erDiagram
    USERS ||--o{ LISTED_ITEMS : owns
    USERS ||--o{ RENTALS : "rents as"
    LISTED_ITEMS ||--o{ RENTALS : "rented in"

    USERS {
        INTEGER id PK
        TEXT username "NOT NULL, UNIQUE"
        TEXT password "nullable"
        TEXT created_at "NOT NULL, default now"
    }

    LISTED_ITEMS {
        INTEGER item_id PK
        INTEGER owner_id FK "-> users.id"
        TEXT item_name "NOT NULL"
        TEXT description "nullable"
        REAL cost_per_day "NOT NULL"
        TEXT status "NOT NULL, CHECK IN (available, rented, unlisted)"
        TEXT created_at "NOT NULL, default now"
    }

    RENTALS {
        INTEGER rental_id PK
        INTEGER item_id FK "-> listed_items.item_id"
        INTEGER renter_id FK "-> users.id"
        TEXT start_time "NOT NULL"
        TEXT end_time "NOT NULL"
        TEXT returned_at "nullable"
        TEXT status "NOT NULL, CHECK IN (active, closed)"
    }
```

If your reviewer wants a standalone image rather than a rendered markdown file,
paste the mermaid block above into https://mermaid.live and export as PNG/SVG.
