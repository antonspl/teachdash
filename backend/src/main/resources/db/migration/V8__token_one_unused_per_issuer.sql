drop index one_time_tokens_one_unused_idx;
create unique index one_time_tokens_one_unused_per_issuer_idx on one_time_tokens (user_id, issued_by) where used_at is null;
