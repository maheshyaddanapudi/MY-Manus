# API Reference

**Version:** 1.0
**Date:** November 2025
**Base URL:** `http://localhost:8080`

---

## Table of Contents

1. [Overview](#overview)
2. [Authentication](#authentication)
3. [REST API Endpoints](#rest-api-endpoints)
   - [Agent Operations](#agent-operations)
   - [Session Management](#session-management)
   - [Event Stream](#event-stream)
   - [Notifications](#notifications)
   - [Documents (RAG/Knowledge)](#documents-ragknowledge)
   - [Browser Monitoring](#browser-monitoring)
   - [Plan Management](#plan-management)
   - [Sandbox](#sandbox)
   - [Health & Metrics](#health--metrics)
4. [WebSocket API](#websocket-api)
5. [Error Handling](#error-handling)
6. [Rate Limiting](#rate-limiting)
7. [Examples](#examples)

---

## Overview

MY-Manus provides a **RESTful API** for agent interaction and a **WebSocket API** for real-time updates.

### API Characteristics

- **Format**: JSON
- **Protocol**: HTTP/HTTPS + WebSocket
- **Real-time**: WebSocket (STOMP) for live events
- **OpenAPI**: Swagger UI at `/swagger-ui.html`

### Base URLs

| Environment | REST API | WebSocket |
|-------------|----------|-----------|
| **Local** | `http://localhost:8080` | `ws://localhost:8080/ws` |
| **Production** | `https://api.my-manus.com` | `wss://api.my-manus.com/ws` |

---

## Authentication

### Development Mode (Default)

```properties
# application.properties
auth.enabled=false
```

**No authentication required** - all endpoints are open.

### Production Mode

```properties
# application.properties
auth.enabled=true
```

**JWT Bearer Token required:**

```http
Authorization: Bearer <jwt_token>
```

**Get Token:** (implement `/api/auth/login` endpoint)

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "user", "password": "pass"}'
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresIn": 3600
}
```

---

## REST API Endpoints

### Agent Operations

#### POST /api/agent/chat

**Send message to agent and get response.**

**Request:**
```http
POST /api/agent/chat HTTP/1.1
Content-Type: application/json

{
  "sessionId": "session-123",
  "message": "Analyze sales data in data.csv"
}
```

**Parameters:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `sessionId` | string | No | Session ID (auto-generated if omitted) |
| `message` | string | Yes | User's message/query |

**Response:**
```json
{
  "sessionId": "session-123",
  "message": "I'll analyze the sales data. Let me read the CSV first...",
  "status": "completed"
}
```

**Status Codes:**
- `200 OK` - Request processed successfully
- `400 Bad Request` - Invalid parameters
- `500 Internal Server Error` - Processing failed

**WebSocket Updates:**

Real-time updates sent to `/topic/agent/{sessionId}`:
```json
{
  "type": "status",
  "content": "thinking",
  "metadata": {"iteration": 1}
}
```

**cURL Example:**
```bash
curl -X POST http://localhost:8080/api/agent/chat \
  -H "Content-Type: application/json" \
  -d '{
    "sessionId": "test-session",
    "message": "What is 2+2?"
  }'
```

---

#### GET /api/agent/{sessionId}/messages

**Get conversation history for a session.**

**Request:**
```http
GET /api/agent/session-123/messages HTTP/1.1
```

**Response:**
```json
[
  {
    "id": 1,
    "role": "USER",
    "content": "Analyze sales data",
    "timestamp": "2025-11-25T10:00:00Z"
  },
  {
    "id": 2,
    "role": "ASSISTANT",
    "content": "I'll analyze the sales data...",
    "timestamp": "2025-11-25T10:00:05Z"
  }
]
```

---

#### POST /api/agent/{sessionId}/clear

**Clear session history and reset agent state.**

**Request:**
```http
POST /api/agent/session-123/clear HTTP/1.1
```

**Response:**
```json
{
  "message": "Session cleared successfully",
  "sessionId": "session-123"
}
```

---

#### GET /api/agent/{sessionId}/context

**Get execution context (Python variables, metadata).**

**Request:**
```http
GET /api/agent/session-123/context HTTP/1.1
```

**Response:**
```json
{
  "sessionId": "session-123",
  "executionContext": {
    "df": {"_type": "DataFrame", "_shape": [100, 5]},
    "total_sales": 150000,
    "api_key": "sk-..."
  },
  "metadata": {
    "model": "claude-3-5-sonnet",
    "iterations": 5
  }
}
```

---

### Session Management

#### GET /api/agent/sessions

**Get all sessions for the user.**

**Request:**
```http
GET /api/agent/sessions HTTP/1.1
```

**Response:**
```json
[
  {
    "id": "uuid-1",
    "sessionId": "session-123",
    "title": "Sales data analysis",
    "createdAt": "2025-11-25T10:00:00Z",
    "updatedAt": "2025-11-25T10:15:00Z"
  },
  {
    "id": "uuid-2",
    "sessionId": "session-456",
    "title": "Trip planning",
    "createdAt": "2025-11-25T11:00:00Z",
    "updatedAt": "2025-11-25T11:30:00Z"
  }
]
```

---

#### POST /api/agent/session

**Create new session.**

**Request:**
```http
POST /api/agent/session HTTP/1.1
Content-Type: application/json

{
  "title": "My new task"
}
```

**Response:**
```json
{
  "id": "uuid-3",
  "sessionId": "session-789",
  "title": "My new task",
  "createdAt": "2025-11-25T12:00:00Z"
}
```

---

#### PUT /api/agent/session/{sessionId}/title

**Update session title.**

**Request:**
```http
PUT /api/agent/session/session-123/title HTTP/1.1
Content-Type: application/json

{
  "title": "Updated title"
}
```

**Response:**
```json
{
  "message": "Title updated successfully",
  "sessionId": "session-123",
  "title": "Updated title"
}
```

---

#### DELETE /api/agent/session/{sessionId}

**Delete session and all associated data.**

**Request:**
```http
DELETE /api/agent/session/session-123 HTTP/1.1
```

**Response:**
```json
{
  "message": "Session deleted successfully",
  "sessionId": "session-123"
}
```

**Status:** `204 No Content`

---

### Event Stream

#### GET /api/agent/{sessionId}/events

**Get all events for a session (complete execution history).**

**Request:**
```http
GET /api/agent/session-123/events HTTP/1.1
```

**Response:**
```json
[
  {
    "id": "event-1",
    "type": "USER_MESSAGE",
    "iteration": 0,
    "sequence": 0,
    "content": "Analyze sales data",
    "timestamp": "2025-11-25T10:00:00Z",
    "success": true
  },
  {
    "id": "event-2",
    "type": "AGENT_THOUGHT",
    "iteration": 1,
    "sequence": 0,
    "content": "I'll read the CSV first...",
    "timestamp": "2025-11-25T10:00:02Z",
    "success": true
  },
  {
    "id": "event-3",
    "type": "AGENT_ACTION",
    "iteration": 1,
    "sequence": 1,
    "content": "df = pd.read_csv('data.csv')",
    "data": {"tool": "execute_code"},
    "timestamp": "2025-11-25T10:00:03Z"
  },
  {
    "id": "event-4",
    "type": "OBSERVATION",
    "iteration": 1,
    "sequence": 2,
    "content": "DataFrame loaded: 100 rows, 5 columns",
    "data": {
      "stdout": "...",
      "variables": {"df": {...}}
    },
    "duration_ms": 850,
    "success": true,
    "timestamp": "2025-11-25T10:00:03.850Z"
  }
]
```

---

#### GET /api/agent/{sessionId}/events/iteration/{n}

**Get events for specific iteration.**

**Request:**
```http
GET /api/agent/session-123/events/iteration/1 HTTP/1.1
```

**Response:**
```json
[
  {
    "type": "AGENT_THOUGHT",
    "iteration": 1,
    "content": "..."
  },
  {
    "type": "AGENT_ACTION",
    "iteration": 1,
    "content": "..."
  },
  {
    "type": "OBSERVATION",
    "iteration": 1,
    "content": "..."
  }
]
```

---

#### GET /api/agent/{sessionId}/events/stream

**Server-Sent Events (SSE) stream for real-time events.**

**Request:**
```http
GET /api/agent/session-123/events/stream HTTP/1.1
Accept: text/event-stream
```

**Response:** (SSE stream)
```
data: {"type":"AGENT_THOUGHT","content":"..."}

data: {"type":"AGENT_ACTION","content":"..."}

data: {"type":"OBSERVATION","content":"..."}
```

**JavaScript Example:**
```javascript
const eventSource = new EventSource('/api/agent/session-123/events/stream');

eventSource.onmessage = (event) => {
  const data = JSON.parse(event.data);
  console.log('Event:', data.type, data.content);
};
```

---

### Notifications

#### GET /api/notifications

**Get all notifications for user.**

**Request:**
```http
GET /api/notifications?userId=user-123 HTTP/1.1
```

**Response:**
```json
[
  {
    "id": 1,
    "userId": "user-123",
    "sessionId": "session-123",
    "type": "TASK_COMPLETED",
    "title": "Task completed",
    "message": "Your analysis is ready",
    "priority": "HIGH",
    "isRead": false,
    "browserNotification": true,
    "createdAt": "2025-11-25T10:15:00Z"
  }
]
```

---

#### GET /api/notifications/unread

**Get unread notifications.**

**Request:**
```http
GET /api/notifications/unread?userId=user-123 HTTP/1.1
```

**Response:**
```json
[
  {
    "id": 1,
    "type": "TASK_COMPLETED",
    "title": "Task completed",
    "message": "Your analysis is ready",
    "priority": "HIGH",
    "isRead": false,
    "createdAt": "2025-11-25T10:15:00Z"
  }
]
```

---

#### GET /api/notifications/unread/count

**Get count of unread notifications.**

**Request:**
```http
GET /api/notifications/unread/count?userId=user-123 HTTP/1.1
```

**Response:**
```json
{
  "count": 3
}
```

---

#### POST /api/notifications/{id}/read

**Mark notification as read.**

**Request:**
```http
POST /api/notifications/1/read HTTP/1.1
```

**Response:**
```json
{
  "message": "Notification marked as read",
  "id": 1
}
```

**Status:** `200 OK`

---

#### POST /api/notifications/read-all

**Mark all notifications as read for user.**

**Request:**
```http
POST /api/notifications/read-all?userId=user-123 HTTP/1.1
```

**Response:**
```json
{
  "markedCount": 5
}
```

---

### Documents (RAG/Knowledge)

#### POST /api/documents/upload

**Upload document for RAG/knowledge base.**

**Request:**
```http
POST /api/documents/upload HTTP/1.1
Content-Type: multipart/form-data

sessionId=session-123
file=@document.pdf
```

**Response:**
```json
{
  "id": 1,
  "sessionId": "session-123",
  "filename": "document.pdf",
  "type": "pdf",
  "fileSize": 1024000,
  "indexed": false,
  "uploadedAt": "2025-11-25T10:00:00Z"
}
```

---

#### GET /api/documents

**Get all documents for session.**

**Request:**
```http
GET /api/documents?sessionId=session-123 HTTP/1.1
```

**Response:**
```json
[
  {
    "id": 1,
    "filename": "document.pdf",
    "type": "pdf",
    "fileSize": 1024000,
    "indexed": true,
    "uploadedAt": "2025-11-25T10:00:00Z"
  }
]
```

---

#### POST /api/documents/search

**Semantic search across documents.**

**Request:**
```http
POST /api/documents/search HTTP/1.1
Content-Type: application/json

{
  "sessionId": "session-123",
  "query": "sales analysis methodology",
  "topK": 5
}
```

**Response:**
```json
{
  "results": [
    {
      "documentId": 1,
      "filename": "document.pdf",
      "chunkContent": "The sales analysis methodology involves...",
      "score": 0.95
    }
  ]
}
```

---

#### DELETE /api/documents/{id}

**Delete document.**

**Request:**
```http
DELETE /api/documents/1 HTTP/1.1
```

**Response:**
```json
{
  "message": "Document deleted successfully",
  "id": 1
}
```

---

### Browser Monitoring

#### GET /api/browser/{sessionId}/console-logs

**Get browser console logs.**

**Request:**
```http
GET /api/browser/session-123/console-logs HTTP/1.1
```

**Query Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `level` | string | Filter by level (LOG, INFO, WARN, ERROR) |
| `limit` | integer | Max results (default: 100) |

**Response:**
```json
[
  {
    "id": 1,
    "sessionId": "session-123",
    "level": "LOG",
    "message": "Page loaded successfully",
    "sourceFile": "app.js",
    "lineNumber": 42,
    "timestamp": "2025-11-25T10:00:00Z"
  },
  {
    "id": 2,
    "level": "ERROR",
    "message": "Failed to fetch data",
    "sourceFile": "api.js",
    "lineNumber": 15,
    "timestamp": "2025-11-25T10:00:05Z"
  }
]
```

---

#### GET /api/browser/{sessionId}/network-requests

**Get browser network requests.**

**Request:**
```http
GET /api/browser/session-123/network-requests HTTP/1.1
```

**Query Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `method` | string | Filter by HTTP method (GET, POST, etc.) |
| `status` | integer | Filter by status code |
| `limit` | integer | Max results |

**Response:**
```json
[
  {
    "id": 1,
    "sessionId": "session-123",
    "method": "GET",
    "url": "https://api.example.com/data",
    "statusCode": 200,
    "contentType": "application/json",
    "duration_ms": 250,
    "requestHeaders": {...},
    "responseHeaders": {...},
    "timestamp": "2025-11-25T10:00:00Z"
  }
]
```

---

### Plan Management

#### GET /api/plan/{sessionId}

**Get current plan (todo.md) for session.**

**Request:**
```http
GET /api/plan/session-123 HTTP/1.1
```

**Response:**
```json
{
  "sessionId": "session-123",
  "title": "Trip to Japan",
  "tasks": [
    {
      "id": 1,
      "description": "Research flights",
      "status": "COMPLETED",
      "completedAt": "2025-11-25T10:00:00Z"
    },
    {
      "id": 2,
      "description": "Book hotels",
      "status": "IN_PROGRESS"
    },
    {
      "id": 3,
      "description": "Plan itinerary",
      "status": "PENDING"
    }
  ],
  "progress": 33.3,
  "updatedAt": "2025-11-25T10:15:00Z"
}
```

---

#### POST /api/plan/{sessionId}/update

**Update plan (todo.md).**

**Request:**
```http
POST /api/plan/session-123/update HTTP/1.1
Content-Type: application/json

{
  "taskUpdates": [
    {
      "id": 2,
      "status": "COMPLETED"
    },
    {
      "id": 3,
      "status": "IN_PROGRESS"
    }
  ]
}
```

**Response:**
```json
{
  "message": "Plan updated successfully",
  "updatedTasks": 2
}
```

---

### Sandbox

#### GET /api/sandbox/stats

**Get Docker container statistics.**

**Request:**
```http
GET /api/sandbox/stats HTTP/1.1
```

**Response:**
```json
{
  "totalContainers": 5,
  "runningContainers": 3,
  "cachedContainers": 2,
  "containerStats": [
    {
      "sessionId": "session-123",
      "containerId": "abc123",
      "status": "running",
      "cpuUsage": 15.5,
      "memoryUsage": 256000000,
      "uptime": 3600
    }
  ]
}
```

---

#### POST /api/sandbox/{sessionId}/cleanup

**Clean up sandbox container for session.**

**Request:**
```http
POST /api/sandbox/session-123/cleanup HTTP/1.1
```

**Response:**
```json
{
  "message": "Container cleaned up successfully",
  "sessionId": "session-123",
  "containerId": "abc123"
}
```

---

### Health & Metrics

#### GET /actuator/health

**Health check endpoint.**

**Request:**
```http
GET /actuator/health HTTP/1.1
```

**Response:**
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "validationQuery": "isValid()"
      }
    },
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": 500000000000,
        "free": 250000000000,
        "threshold": 10485760
      }
    }
  }
}
```

---

#### GET /actuator/metrics

**Get list of available metrics.**

**Request:**
```http
GET /actuator/metrics HTTP/1.1
```

**Response:**
```json
{
  "names": [
    "jvm.memory.used",
    "http.server.requests",
    "notifications.sent",
    "agent.tasks.completed",
    "..."
  ]
}
```

---

#### GET /actuator/metrics/{name}

**Get specific metric value.**

**Request:**
```http
GET /actuator/metrics/jvm.memory.used HTTP/1.1
```

**Response:**
```json
{
  "name": "jvm.memory.used",
  "measurements": [
    {
      "statistic": "VALUE",
      "value": 536870912
    }
  ],
  "availableTags": [
    {"tag": "area", "values": ["heap", "nonheap"]},
    {"tag": "id", "values": ["G1 Eden Space", "G1 Old Gen"]}
  ]
}
```

---

#### GET /actuator/prometheus

**Prometheus metrics endpoint (for scraping).**

**Request:**
```http
GET /actuator/prometheus HTTP/1.1
```

**Response:** (Prometheus format)
```
# HELP jvm_memory_used_bytes The amount of used memory
# TYPE jvm_memory_used_bytes gauge
jvm_memory_used_bytes{area="heap",id="G1 Eden Space"} 2.1474836E8
jvm_memory_used_bytes{area="heap",id="G1 Old Gen"} 3.2212992E8

# HELP http_server_requests_seconds
# TYPE http_server_requests_seconds histogram
http_server_requests_seconds_count{method="POST",uri="/api/agent/chat",status="200"} 1234
http_server_requests_seconds_sum{method="POST",uri="/api/agent/chat",status="200"} 56.789

# HELP notifications_sent_total Total notifications sent
# TYPE notifications_sent_total counter
notifications_sent_total{type="TASK_COMPLETED"} 42
```

---

## WebSocket API

### Connection

**Endpoint:** `/ws`

**Protocol:** STOMP over WebSocket

**JavaScript Example:**
```javascript
import SockJS from 'sockjs-client';
import {Stomp} from '@stomp/stompjs';

// Create WebSocket connection
const socket = new SockJS('http://localhost:8080/ws');
const stompClient = Stomp.over(socket);

// Connect
stompClient.connect({}, (frame) => {
  console.log('Connected:', frame);

  // Subscribe to agent events
  stompClient.subscribe('/topic/agent/session-123', (message) => {
    const event = JSON.parse(message.body);
    console.log('Agent event:', event);
  });

  // Subscribe to notifications
  stompClient.subscribe('/topic/notifications/session-123', (message) => {
    const notification = JSON.parse(message.body);
    console.log('Notification:', notification);
  });
});
```

---

### Topics

#### /topic/agent/{sessionId}

**Agent execution events (thoughts, actions, observations).**

**Message Format:**
```json
{
  "type": "thought",
  "content": "I'll analyze the data...",
  "metadata": {
    "iteration": 1,
    "timestamp": "2025-11-25T10:00:00Z"
  }
}
```

**Event Types:**
- `status` - Agent status (thinking, executing, done, error)
- `thought` - Agent's reasoning
- `code` - Code to be executed
- `terminal` - Terminal output
- `observation` - Execution result

---

#### /topic/notifications/{sessionId}

**Real-time notifications.**

**Message Format:**
```json
{
  "id": 1,
  "type": "TASK_COMPLETED",
  "title": "Task completed",
  "message": "Your analysis is ready",
  "priority": "HIGH",
  "createdAt": "2025-11-25T10:15:00Z"
}
```

---

#### /topic/plan/{sessionId}

**Plan updates (when todo.md changes).**

**Message Format:**
```json
{
  "sessionId": "session-123",
  "title": "Trip to Japan",
  "tasks": [...],
  "progress": 66.7,
  "updatedAt": "2025-11-25T10:20:00Z"
}
```

---

#### /topic/browser/{sessionId}

**Browser console/network updates.**

**Message Format:**
```json
{
  "type": "console",
  "level": "ERROR",
  "message": "Failed to fetch data",
  "sourceFile": "app.js",
  "timestamp": "2025-11-25T10:00:00Z"
}
```

---

#### /topic/terminal/{sessionId}

**Terminal output stream.**

**Message Format:**
```json
{
  "output": "$ ls -la\ntotal 48\ndrwxr-xr-x...",
  "type": "stdout"
}
```

---

#### /user/{userId}/notifications

**User-specific notifications (requires authentication).**

**Message Format:**
```json
{
  "id": 1,
  "type": "TASK_COMPLETED",
  "sessionId": "session-123",
  "title": "Task completed",
  "message": "Your analysis is ready"
}
```

---

## Error Handling

### Error Response Format

All errors return:

```json
{
  "timestamp": "2025-11-25T10:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid session ID",
  "path": "/api/agent/chat"
}
```

### Common Error Codes

| Code | Meaning | Common Causes |
|------|---------|---------------|
| `400` | Bad Request | Invalid parameters, malformed JSON |
| `401` | Unauthorized | Missing or invalid auth token |
| `404` | Not Found | Session or resource doesn't exist |
| `429` | Too Many Requests | Rate limit exceeded |
| `500` | Internal Server Error | Unexpected server error |
| `503` | Service Unavailable | LLM API down, database down |

---

## Rate Limiting

### Limits (Development)

**No rate limits in development mode.**

### Limits (Production)

| Endpoint | Limit | Window |
|----------|-------|--------|
| `/api/agent/chat` | 60 requests | per minute |
| `/api/notifications/*` | 100 requests | per minute |
| `/api/documents/upload` | 10 uploads | per hour |
| `/actuator/*` | Unlimited | - |

**Rate Limit Headers:**
```http
X-RateLimit-Limit: 60
X-RateLimit-Remaining: 45
X-RateLimit-Reset: 1638360000
```

---

## Examples

### Example 1: Send Message & Get Response

```bash
curl -X POST http://localhost:8080/api/agent/chat \
  -H "Content-Type: application/json" \
  -d '{
    "sessionId": "test-123",
    "message": "Calculate 2+2"
  }'
```

**Response:**
```json
{
  "sessionId": "test-123",
  "message": "The result is 4.",
  "status": "completed"
}
```

---

### Example 2: Get Event Stream

```bash
curl http://localhost:8080/api/agent/test-123/events
```

**Response:**
```json
[
  {
    "type": "USER_MESSAGE",
    "iteration": 0,
    "content": "Calculate 2+2"
  },
  {
    "type": "AGENT_ACTION",
    "iteration": 1,
    "content": "result = 2 + 2\nprint(result)"
  },
  {
    "type": "OBSERVATION",
    "iteration": 1,
    "content": "4",
    "duration_ms": 120
  }
]
```

---

### Example 3: Subscribe to WebSocket Events

```javascript
// Connect to WebSocket
const socket = new SockJS('http://localhost:8080/ws');
const client = Stomp.over(socket);

client.connect({}, () => {
  // Subscribe to agent events
  client.subscribe('/topic/agent/test-123', (message) => {
    const event = JSON.parse(message.body);

    if (event.type === 'thought') {
      console.log('Agent is thinking:', event.content);
    } else if (event.type === 'code') {
      console.log('Agent generated code:', event.content);
    } else if (event.type === 'observation') {
      console.log('Execution result:', event.content);
    }
  });

  // Send message
  client.send('/app/chat', {}, JSON.stringify({
    sessionId: 'test-123',
    message: 'Calculate 2+2'
  }));
});
```

---

### Example 4: Upload Document

```bash
curl -X POST http://localhost:8080/api/documents/upload \
  -F "sessionId=test-123" \
  -F "file=@document.pdf"
```

**Response:**
```json
{
  "id": 1,
  "sessionId": "test-123",
  "filename": "document.pdf",
  "fileSize": 1024000,
  "uploadedAt": "2025-11-25T10:00:00Z"
}
```

---

### Example 5: Get Unread Notifications

```bash
curl "http://localhost:8080/api/notifications/unread?userId=user-123"
```

**Response:**
```json
[
  {
    "id": 1,
    "type": "TASK_COMPLETED",
    "title": "Analysis complete",
    "message": "Your sales analysis is ready",
    "priority": "HIGH",
    "isRead": false,
    "createdAt": "2025-11-25T10:15:00Z"
  }
]
```

---

## Summary

MY-Manus provides **comprehensive REST and WebSocket APIs** for:

✅ **Agent Interaction** - Chat, send messages, get responses
✅ **Session Management** - Create, list, update, delete sessions
✅ **Event Stream** - Complete execution history and real-time updates
✅ **Notifications** - In-app and browser notifications
✅ **Documents** - Upload, search, manage knowledge base
✅ **Browser Monitoring** - Console logs and network requests
✅ **Plan Management** - View and update agent plans
✅ **Sandbox Management** - Container stats and cleanup
✅ **Health & Metrics** - Monitoring and observability

**OpenAPI Documentation:** Visit `http://localhost:8080/swagger-ui.html` for interactive API documentation.

---

## Next Steps

**Explore More:**
- [Architecture Guide →](../architecture/ARCHITECTURE.md)
- [Development Guide →](DEVELOPMENT_GUIDE.md)
- [Multi-Turn Scenarios →](MULTI_TURN_SCENARIOS.md)

**Try It:**
```bash
# Start MY-Manus
docker-compose up

# Send a message
curl -X POST http://localhost:8080/api/agent/chat \
  -H "Content-Type: application/json" \
  -d '{"sessionId": "test", "message": "Hello!"}'

# View Swagger UI
open http://localhost:8080/swagger-ui.html
```

---

**Document Version:** 1.0
**Last Updated:** November 2025
**Next:** [Development Guide →](DEVELOPMENT_GUIDE.md)
