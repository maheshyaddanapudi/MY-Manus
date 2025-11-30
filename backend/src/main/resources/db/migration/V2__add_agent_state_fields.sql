-- Add new fields to agent_states table
-- Migration for AgentState: status, iteration, currentTask, lastError

-- Add status column with default IDLE
ALTER TABLE agent_states
ADD COLUMN IF NOT EXISTS status VARCHAR(50) NOT NULL DEFAULT 'IDLE';

-- Add iteration column with default 0
ALTER TABLE agent_states
ADD COLUMN IF NOT EXISTS iteration INTEGER NOT NULL DEFAULT 0;

-- Add currentTask column (nullable)
ALTER TABLE agent_states
ADD COLUMN IF NOT EXISTS current_task VARCHAR(500);

-- Add lastError column (nullable)
ALTER TABLE agent_states
ADD COLUMN IF NOT EXISTS last_error VARCHAR(2000);

-- Add check constraint for status enum values
ALTER TABLE agent_states
DROP CONSTRAINT IF EXISTS check_status;

ALTER TABLE agent_states
ADD CONSTRAINT check_status CHECK (status IN ('IDLE', 'RUNNING', 'WAITING_INPUT', 'COMPLETED', 'ERROR'));

-- Create index on status for faster queries
CREATE INDEX IF NOT EXISTS idx_agent_states_status ON agent_states(status);

-- Create index on session_id and status combination
CREATE INDEX IF NOT EXISTS idx_agent_states_session_status ON agent_states(session_id, status);
