-- Test database schema for H2
-- Tables created in dependency order to avoid foreign key constraint issues

-- 1. Create agent_states table first (no dependencies)
CREATE TABLE IF NOT EXISTS agent_states (
    id UUID PRIMARY KEY,
    session_id VARCHAR(255) NOT NULL UNIQUE,
    title VARCHAR(500),
    status VARCHAR(50) NOT NULL,
    iteration INTEGER NOT NULL DEFAULT 0,
    current_task VARCHAR(500),
    last_error VARCHAR(2000),
    execution_context TEXT,
    metadata TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

-- 2. Create tables that depend on agent_states
CREATE TABLE IF NOT EXISTS messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    state_id UUID NOT NULL,
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    FOREIGN KEY (state_id) REFERENCES agent_states(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS events (
    id UUID PRIMARY KEY,
    agent_state_id UUID NOT NULL,
    type VARCHAR(50) NOT NULL,
    iteration INTEGER NOT NULL,
    sequence INTEGER NOT NULL,
    content TEXT,
    data TEXT,
    timestamp TIMESTAMP NOT NULL,
    duration_ms BIGINT,
    success BOOLEAN,
    error TEXT,
    FOREIGN KEY (agent_state_id) REFERENCES agent_states(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS tool_executions (
    id UUID PRIMARY KEY,
    agent_state_id UUID NOT NULL,
    tool_name VARCHAR(255) NOT NULL,
    iteration INTEGER NOT NULL,
    input_params TEXT,
    output_result TEXT,
    success BOOLEAN NOT NULL,
    error_message VARCHAR(2000),
    execution_time_ms BIGINT,
    created_at TIMESTAMP NOT NULL,
    FOREIGN KEY (agent_state_id) REFERENCES agent_states(id) ON DELETE CASCADE
);

-- 3. Create independent tables (no foreign keys)
CREATE TABLE IF NOT EXISTS documents (
    id UUID PRIMARY KEY,
    session_id VARCHAR(255) NOT NULL,
    filename VARCHAR(500) NOT NULL,
    content TEXT NOT NULL,
    mime_type VARCHAR(100),
    size_bytes BIGINT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS document_chunks (
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL,
    chunk_index INTEGER NOT NULL,
    content TEXT NOT NULL,
    embedding BINARY,
    metadata TEXT,
    created_at TIMESTAMP NOT NULL,
    FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS console_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(255) NOT NULL,
    log_level VARCHAR(20) NOT NULL,
    message TEXT NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    source VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS network_requests (
    id UUID PRIMARY KEY,
    session_id VARCHAR(255) NOT NULL,
    method VARCHAR(10) NOT NULL,
    url VARCHAR(2000) NOT NULL,
    status_code INTEGER,
    request_headers TEXT,
    response_headers TEXT,
    request_body TEXT,
    response_body TEXT,
    duration_ms BIGINT,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(255),
    session_id VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    browser_notification BOOLEAN NOT NULL DEFAULT TRUE,
    action_url VARCHAR(500),
    created_at TIMESTAMP NOT NULL,
    read_at TIMESTAMP
);

-- Create indexes for common queries
CREATE INDEX IF NOT EXISTS idx_messages_state_id ON messages(state_id);
CREATE INDEX IF NOT EXISTS idx_events_agent_state_id ON events(agent_state_id);
CREATE INDEX IF NOT EXISTS idx_events_iteration_sequence ON events(iteration, sequence);
CREATE INDEX IF NOT EXISTS idx_tool_executions_agent_state_id ON tool_executions(agent_state_id);
CREATE INDEX IF NOT EXISTS idx_tool_executions_iteration ON tool_executions(iteration);
CREATE INDEX IF NOT EXISTS idx_console_logs_session_id ON console_logs(session_id);
CREATE INDEX IF NOT EXISTS idx_network_requests_session_id ON network_requests(session_id);
CREATE INDEX IF NOT EXISTS idx_notifications_session_id ON notifications(session_id);
CREATE INDEX IF NOT EXISTS idx_documents_session_id ON documents(session_id);
CREATE INDEX IF NOT EXISTS idx_document_chunks_document_id ON document_chunks(document_id);
