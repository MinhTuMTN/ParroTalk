CREATE TABLE IF NOT EXISTS public.ai_usage_events (
    id UUID PRIMARY KEY,
    user_id UUID NULL REFERENCES public.users(id) ON DELETE SET NULL,
    provider VARCHAR(60) NOT NULL,
    model VARCHAR(120) NOT NULL,
    feature VARCHAR(60) NOT NULL DEFAULT 'UNKNOWN',
    input_tokens INT NOT NULL DEFAULT 0,
    output_tokens INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'OK',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ai_usage_events_created_at
    ON public.ai_usage_events (created_at);
CREATE INDEX IF NOT EXISTS idx_ai_usage_events_user_created
    ON public.ai_usage_events (user_id, created_at);
CREATE INDEX IF NOT EXISTS idx_ai_usage_events_model_created
    ON public.ai_usage_events (model, created_at);
