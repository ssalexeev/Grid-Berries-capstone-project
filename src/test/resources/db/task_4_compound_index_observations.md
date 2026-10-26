# Compound index experiment

## Domain use case

The `user_metric_event` table records activity for users over time. A common
query is retrieving one user's recent activity, for example for an activity
feed or user-level analytics:

```sql
SELECT count(*)
FROM benchmark_user_metric_event
WHERE user_id = 777
  AND created_at >= now() - interval '7 days';
```

The compound B-tree index is ordered by `(user_id, created_at)`. `user_id` is
the leading column, so the index can first locate one user's events and then
restrict that user's entries to a time range. The same index should also help
queries filtering by `user_id` alone. A query filtering only by `created_at`
does not constrain the leading column and is less efficient.

## Reproduce

Run the psql script against a disposable PostgreSQL database:

```sh
psql "$DATABASE_URL" -v ON_ERROR_STOP=1 \
  -f src/test/resources/db/task_3_index_lookup_benchmark.sql
```

The script creates temporary benchmark tables, generates 1,000 users and
2,000,000 events, and uses `EXPLAIN (ANALYZE, BUFFERS)` for:

1. Both indexed columns: `user_id` equality and a `created_at` range.
2. Only the leading column: `user_id`.
3. Only the second column: a `created_at` range.

It reports the first query before index creation as a baseline. The benchmark
tables are temporary and are discarded when the psql session ends.

## Observations

Measured on PostgreSQL 16 with the script's 2-million-row data set. Queries
were warmed before their `EXPLAIN ANALYZE` run.

| Filter columns | Plan observed | Rows returned | Execution time |
|---|---|---:|---:|
| `user_id`, `created_at` (before index) | Sequential scan; 1,999,800 rows removed by filter | 200 | 90.181 ms |
| `user_id`, `created_at` | Bitmap index scan + bitmap heap scan; 3 index buffers and 200 heap blocks | 200 | 0.117 ms |
| `user_id` only (leading column) | Bitmap index scan + bitmap heap scan; 4 index buffers and 2,000 heap blocks | 2,000 | 6.936 ms |
| `created_at` only (second column) | Bitmap index scan + bitmap heap scan; 2,002 index buffers and 1,405 heap blocks | 201,600 | 36.604 ms |

With both columns present, the compound index reduced the observed query time
from about 90 ms to 0.12 ms (roughly 770x in this run). Filtering by the
leading `user_id` alone also used the index. With only `created_at`, PostgreSQL
still chose a bitmap scan of the index for this data distribution, but had to
read far more index and table buffers. Thus, a compound index is most useful
when queries constrain its leading column; a plan that mentions the index does
not necessarily mean it can narrow the scan as effectively.

These are single-run measurements and depend on storage, caching, PostgreSQL
version, and data distribution. `EXPLAIN` plans and buffer counts are useful
alongside elapsed time when deciding whether an index fits production queries.
Indexes also add disk usage and write overhead.
