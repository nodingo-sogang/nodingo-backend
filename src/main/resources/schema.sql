CREATE INDEX IF NOT EXISTS idx_news_embedding ON news USING hnsw (embedding vector_cosine_ops);
CREATE INDEX IF NOT EXISTS idx_keyword_embedding ON keywords USING hnsw (embedding vector_cosine_ops);
CREATE INDEX IF NOT EXISTS idx_user_embedding ON users USING hnsw (embedding vector_cosine_ops);