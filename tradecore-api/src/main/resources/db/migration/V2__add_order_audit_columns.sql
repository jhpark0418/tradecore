alter table orders
    add column if not exists created_at timestamptz not null default now(),
    add column if not exists updated_at timestamptz not null default now();

create index if not exists idx_orders_account_id_created_at
    on orders (account_id, created_at desc);