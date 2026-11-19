DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'vector') THEN
        CREATE EXTENSION vector;
    END IF;
END $$;

-- Drop legacy PoC table created outside Flyway (lesson_id was varchar(100), not uuid).
-- Safe when re-run by Flyway later: skips DROP once lesson_id is uuid.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'lesson_embeddings'
          AND column_name = 'lesson_id'
          AND data_type <> 'uuid'
    ) THEN
        DROP TABLE public.lesson_embeddings;
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS public.lesson_embeddings (
    lesson_id UUID PRIMARY KEY REFERENCES public.lessons(id) ON DELETE CASCADE,
    search_content TEXT NOT NULL,
    content_hash VARCHAR(64) NOT NULL,
    embedding vector(1024) NOT NULL,
    embedding_model VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_lesson_embeddings_embedding_hnsw
    ON public.lesson_embeddings
    USING hnsw (embedding vector_cosine_ops);
