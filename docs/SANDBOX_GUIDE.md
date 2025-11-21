# Sandbox Environment Guide

## Overview

Create an Ubuntu 22.04 environment matching Manus AI's setup for secure Python code execution.

## Base Container Structure

### Dockerfile Foundation
```
FROM ubuntu:22.04
- System packages (839 total in Manus)
- Python 3.11 with scientific stack
- Node.js 22.13 via NVM
- Java 11 for JVM tools
- Media processing utilities
```

### Key System Packages

**Essential Tools**
- git, curl, wget, vim, nano
- build-essential, gcc, make
- jq for JSON processing
- tar, gzip, zip, unzip

**Python Environment**
- Python 3.11.0 (not default 3.10)
- pip managed via uv
- Virtual environment support

**Media Processing**
- FFmpeg 4.4.2 with full codecs
- Poppler-utils for PDF
- ImageMagick alternatives
- GraphViz for diagrams

**Libraries**
- libssl3, libcurl4
- libxml2, libxmlsec1
- libjpeg-turbo8, libpng16
- libfreetype6 for fonts

## Python Package Stack

### Data Science Core
- pandas 2.3.3
- numpy 2.3.3
- matplotlib 3.10.7
- seaborn 0.13.2
- plotly 6.3.1

### Web Development
- fastapi 0.119.0
- flask 3.1.2
- uvicorn 0.37.0
- requests 2.32.5
- httpx 0.28.1

### Document Processing
- fpdf2 2.8.4
- reportlab 4.4.4
- pypdf 6.1.1
- pdf2image 1.17.0
- weasyprint 66.0
- openpyxl 3.1.5

### Web Scraping
- beautifulsoup4 4.14.2
- lxml 6.0.2
- playwright 1.55.0

### Cloud & APIs
- boto3 1.40.51
- openai 2.3.0

## Node.js Environment

### Installation via NVM
```bash
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.0/install.sh | bash
nvm install 22.13.0
nvm use 22.13.0
```

### Global Packages
- pnpm 10.23.0
- yarn 1.22.22
- Custom tool bridges

## Custom Utilities (Manus Replacements)

We build our own versions of Manus tools:

### render-diagram
Combines multiple diagram tools:
- D2 for modern diagrams
- Mermaid via CLI
- GraphViz dot
- PlantUML support

### markdown-to-pdf
HTML/CSS to PDF pipeline:
- Parse markdown
- Apply styling
- Generate PDF via WeasyPrint

### file-upload
S3 or local storage:
- Generate unique URLs
- Set proper permissions
- Return shareable links

### speech-to-text
Audio transcription:
- Use OpenAI Whisper
- Support multiple formats
- Return text output

## Security Configuration

### Resource Limits
```yaml
Memory: 512MB (hard limit)
CPU: 50% of one core
Processes: 100 max
File descriptors: 1024
Execution time: 30 seconds
Disk: Temporary only
```

### Network Isolation
- Internal network only
- No external internet
- Except for approved APIs
- Block all incoming

### User Permissions
- Run as non-root user
- No sudo in production
- Read-only root filesystem
- Write only to /tmp and /workspace

### Capability Restrictions
Drop all capabilities except:
- CHOWN
- SETUID
- SETGID

## Environment Variables

### Standard Setup
```bash
HOME=/home/ubuntu
USER=ubuntu
SHELL=/bin/bash
PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin
```

### Python Path
```bash
PYTHONPATH=/workspace
PYTHONUNBUFFERED=1
PYTHONDONTWRITEBYTECODE=1
```

### API Configuration  
```bash
ANTHROPIC_API_KEY=${PROVIDED_BY_BACKEND}
# Note: We use Anthropic, not Manus proxy
```

## Execution Lifecycle

### Container Creation
1. Spawn from base image
2. Mount workspace volume
3. Apply resource limits
4. Set environment variables

### Code Execution Flow
1. Restore previous state
2. Inject tool functions
3. Run user code
4. Capture all output
5. Save new state
6. Destroy container

### State Persistence
- Serialize Python variables
- Store in PostgreSQL
- Restore on next execution
- Handle special types

## File System Layout

```
/home/ubuntu/           # User home
  ├── workspace/       # Code execution
  ├── .local/         # User installations
  └── .cache/         # Package cache

/tmp/                  # Temporary files
/usr/local/bin/        # Custom tools
```

## Browser Automation

### Playwright Setup
- Chromium pre-installed
- Headless mode only
- No GPU acceleration
- Controlled via Python

### Usage Pattern
```python
from playwright.sync_api import sync_playwright
with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    # Automation code
```

## Package Installation

### Runtime Installation
Allow agents to install packages:

**Python**: `pip install` with restrictions
**Node**: `pnpm add` for project deps
**System**: Blocked in production

### Pre-approved Packages
Whitelist safe packages:
- Scientific computing
- Data processing  
- Web scraping
- Document generation

## Testing the Sandbox

### Security Tests
1. Try network escape
2. Attempt privilege escalation
3. Resource exhaustion
4. Path traversal
5. Code injection

### Functionality Tests
1. Python package imports
2. File operations
3. Media processing
4. Diagram rendering
5. State persistence

### Performance Tests
1. Execution speed
2. Memory usage
3. CPU utilization
4. Startup time
5. Cleanup efficiency

## Monitoring

### Metrics to Track
- Container count
- Resource usage
- Execution times
- Success rates
- Error patterns

### Logging
- All code executions
- Tool invocations
- Resource violations
- Security events
- Cleanup actions

## Best Practices

1. **Pre-build base images** for fast startup
2. **Cache package installations** when possible
3. **Clean up immediately** after execution
4. **Log everything** for debugging
5. **Test security** continuously
6. **Monitor resources** closely
7. **Update packages** regularly but carefully

## Common Issues

- Package conflicts
- Memory exhaustion
- Slow startup times
- Network timeout errors
- State serialization failures
- Permission denied errors

## Maintenance

### Regular Updates
- Security patches monthly
- Python packages quarterly
- Node.js versions carefully
- System packages as needed

### Image Optimization
- Remove unnecessary packages
- Clear package caches
- Minimize layer count
- Use multi-stage builds

## Implementation Steps

1. Create base Dockerfile
2. Install system packages
3. Setup Python environment
4. Configure Node.js
5. Add custom utilities
6. Apply security settings
7. Test thoroughly
8. Optimize performance
9. Document changes
10. Deploy to registry