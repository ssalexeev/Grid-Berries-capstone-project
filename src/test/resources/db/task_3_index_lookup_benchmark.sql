\timing on
-- Task:
-- Keeping your domain and relational models in mind, think about the cases when
-- you might need an index. Create a script to populate your tables with a big amount
-- of data (couple millions of rows). Check the query execution plan with and without
-- an index, compare the execution times as well.
CREATE TEMP TABLE benchmark_user (
    id integer PRIMARY KEY
) ON COMMIT PRESERVE ROWS;

CREATE TEMP TABLE benchmark_user_metric_event (
    id integer NOT NULL,
    user_id integer NOT NULL REFERENCES benchmark_user(id),
    event_type varchar(100) NOT NULL,
    created_at timestamp NOT NULL
) ON COMMIT PRESERVE ROWS;

INSERT INTO benchmark_user (id)
SELECT generate_series(1, 1000);

INSERT INTO benchmark_user_metric_event (id, user_id, event_type, created_at)
SELECT event_id,
       ((event_id - 1) % 1000) + 1,
       CASE event_id % 4
           WHEN 0 THEN 'VIEW'
           WHEN 1 THEN 'SEARCH'
           WHEN 2 THEN 'ADD_TO_CART'
           ELSE 'PURCHASE'
       END,
       now() - ((event_id - 1) % 100000) * interval '1 minute'
FROM generate_series(1, 2000000) AS generated(event_id);

ANALYZE benchmark_user_metric_event;

-- A user's recent activity is a representative lookup for the event model.
-- Each user's events are selective enough that a B-tree on (user_id, created_at)
-- should outperform scanning all two million rows.
\echo 'Warm-up query WITHOUT the lookup index'
SELECT count(*)
FROM benchmark_user_metric_event
WHERE user_id = 777
  AND created_at >= now() - interval '7 days';

\echo 'Query plan and timing WITHOUT the lookup index'
EXPLAIN (ANALYZE, BUFFERS)
SELECT count(*)
FROM benchmark_user_metric_event
WHERE user_id = 777
  AND created_at >= now() - interval '7 days';

CREATE INDEX benchmark_user_metric_event_user_created_idx
    ON benchmark_user_metric_event (user_id, created_at);

ANALYZE benchmark_user_metric_event;

\echo 'Warm-up query using BOTH indexed columns'
SELECT count(*)
FROM benchmark_user_metric_event
WHERE user_id = 777
  AND created_at >= now() - interval '7 days';

\echo 'Query plan using BOTH indexed columns (user_id, created_at)'
EXPLAIN (ANALYZE, BUFFERS)
SELECT count(*)
FROM benchmark_user_metric_event
WHERE user_id = 777
  AND created_at >= now() - interval '7 days';

\echo 'Warm-up query using only the LEADING indexed column'
SELECT count(*)
FROM benchmark_user_metric_event
WHERE user_id = 777;

\echo 'Query plan using only the LEADING indexed column (user_id)'
EXPLAIN (ANALYZE, BUFFERS)
SELECT count(*)
FROM benchmark_user_metric_event
WHERE user_id = 777;

\echo 'Warm-up query using only the SECOND indexed column'
SELECT count(*)
FROM benchmark_user_metric_event
WHERE created_at >= now() - interval '7 days';

\echo 'Query plan using only the SECOND indexed column (created_at)'
EXPLAIN (ANALYZE, BUFFERS, FORMAT json)
SELECT count(*)
FROM benchmark_user_metric_event
WHERE created_at >= now() - interval '7 days';