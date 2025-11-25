# MY-Manus Documentation

Welcome to the MY-Manus documentation! This directory contains comprehensive guides, architecture documentation, and project information.

## 📋 Table of Contents

### 🚀 Getting Started
Before diving into the detailed documentation, start here:
- **[QUICKSTART.md](../QUICKSTART.md)** - Get up and running in 5 minutes
- **[SETUP.md](../SETUP.md)** - Detailed installation and configuration guide
- **[CLAUDE.md](../CLAUDE.md)** - Development instructions for Claude Code users

---

## 📖 Documentation Structure

### 📚 Guides (`guides/`)
Step-by-step guides for different aspects of MY-Manus:

- **[AGENT_GUIDE.md](guides/AGENT_GUIDE.md)** - Deep dive into the CodeAct agent implementation
  - Agent loop architecture
  - Python code generation and execution
  - State management
  - Error handling and recovery

- **[UI_GUIDE.md](guides/UI_GUIDE.md)** - Frontend development patterns
  - Component architecture
  - State management with Zustand
  - WebSocket integration
  - Real-time UI updates

- **[SANDBOX_GUIDE.md](guides/SANDBOX_GUIDE.md)** - Sandbox environment setup
  - Docker configuration
  - Python and Node.js setup
  - Security and resource limits
  - Package management

- **[TOOLS_GUIDE.md](guides/TOOLS_GUIDE.md)** - Tool development and customization
  - Creating custom tools
  - Tool registration
  - Python bindings
  - Testing tools

- **[DEPLOYMENT.md](guides/DEPLOYMENT.md)** - Production deployment strategies
  - Docker Compose setup
  - Kubernetes deployment
  - Environment configuration
  - Scaling and performance

---

### 🏗️ Architecture (`architecture/`)
System design and architectural documentation:

- **[FRONTEND_ARCHITECTURE.md](architecture/FRONTEND_ARCHITECTURE.md)** - Complete UI architecture
  - **Full UI wireframe with Mermaid diagrams**
  - Eight-panel layout (Chat, Terminal, Editor, Browser, Events, Files, Replay, Knowledge, Plan)
  - Component hierarchy
  - WebSocket event flows
  - State management patterns
  - Multi-turn conversation flow
  - Tool system architecture
  - Technology stack details

- **[SANDBOX_ARCHITECTURE.md](architecture/SANDBOX_ARCHITECTURE.md)** - Docker sandbox design
  - Container isolation strategy
  - Resource limits
  - Network configuration
  - Security considerations

---

### 📊 Project Documentation (`project/`)
Project status, analysis, and planning documents:

- **[DIFFERENTIAL_ANALYSIS.md](project/DIFFERENTIAL_ANALYSIS.md)** - Comprehensive comparison with Manus AI
  - Feature-by-feature comparison
  - Architecture differences
  - Exclusive MY-Manus features
  - Technology stack comparison
  - 100% core feature parity analysis
  - 8 major exclusive enhancements

- **[IMPLEMENTATION_STATUS.md](project/IMPLEMENTATION_STATUS.md)** - Feature completion tracking
  - Backend implementation status (100% complete)
  - Frontend implementation status (100% complete)
  - Infrastructure setup (100% complete)
  - Testing coverage details

- **[FINAL_SUMMARY.md](project/FINAL_SUMMARY.md)** - Project completion summary
  - Implementation achievements
  - Performance metrics
  - Production readiness checklist

- **[GAP_ANALYSIS.md](project/GAP_ANALYSIS.md)** - Original gap analysis vs Manus AI
  - Identified gaps
  - Implemented solutions
  - Remaining opportunities

- **[IMPLEMENTATION_PLAN.md](project/IMPLEMENTATION_PLAN.md)** - Original implementation plan
  - Phase-by-phase development strategy
  - Timeline and milestones
  - Resource allocation

- **[TEST_PLAN.md](project/TEST_PLAN.md)** - Testing strategy and coverage
  - Unit testing approach
  - Integration testing
  - E2E testing
  - Security testing

---

### 🔬 Research (`research/`)
Research reports and analysis:

- **[MANUS_AI_RESEARCH_REPORT.md](research/MANUS_AI_RESEARCH_REPORT.md)** - Original Manus AI research
  - Platform analysis
  - Feature discovery
  - Architecture insights
  - Competitive analysis

---

## 🎯 Quick Navigation by Topic

### For Developers

**Building Custom Tools:**
1. Read [TOOLS_GUIDE.md](guides/TOOLS_GUIDE.md)
2. Check [AGENT_GUIDE.md](guides/AGENT_GUIDE.md) for integration

**Frontend Development:**
1. Start with [UI_GUIDE.md](guides/UI_GUIDE.md)
2. Review [FRONTEND_ARCHITECTURE.md](architecture/FRONTEND_ARCHITECTURE.md) for component structure
3. Check [IMPLEMENTATION_STATUS.md](project/IMPLEMENTATION_STATUS.md) for current state

**Backend Development:**
1. Begin with [AGENT_GUIDE.md](guides/AGENT_GUIDE.md)
2. Understand sandbox in [SANDBOX_GUIDE.md](guides/SANDBOX_GUIDE.md)
3. Review [SANDBOX_ARCHITECTURE.md](architecture/SANDBOX_ARCHITECTURE.md)

### For DevOps/Infrastructure

**Deployment:**
1. Follow [DEPLOYMENT.md](guides/DEPLOYMENT.md)
2. Configure sandbox per [SANDBOX_GUIDE.md](guides/SANDBOX_GUIDE.md)
3. Review [SETUP.md](../SETUP.md) for environment setup

### For Product Managers

**Feature Comparison:**
1. Read [DIFFERENTIAL_ANALYSIS.md](project/DIFFERENTIAL_ANALYSIS.md)
2. Check [IMPLEMENTATION_STATUS.md](project/IMPLEMENTATION_STATUS.md)
3. Review [FINAL_SUMMARY.md](project/FINAL_SUMMARY.md)

**Project Planning:**
1. See [IMPLEMENTATION_PLAN.md](project/IMPLEMENTATION_PLAN.md)
2. Review [GAP_ANALYSIS.md](project/GAP_ANALYSIS.md)
3. Check [TEST_PLAN.md](project/TEST_PLAN.md)

---

## 🌟 Key Features Documented

### Core Features
- ✅ CodeAct Architecture
- ✅ Eight-panel UI
- ✅ 20 built-in tools
- ✅ Docker sandboxing
- ✅ Real-time WebSocket updates

### Advanced Features (Recently Implemented)
- ✅ **Session Replay** - Time-travel debugging
- ✅ **RAG/Knowledge Base** - Document upload and semantic search
- ✅ **Enhanced Browser** - Console logs and network monitoring
- ✅ **Notifications** - Browser and in-app notifications
- ✅ **Live Plan Visualization** - Real-time todo.md tracking
- ✅ **Multi-Turn Conversations** - LLM-based message classification
- ✅ **Hybrid Tool System** - Core + MCP dynamic tool discovery
- ✅ **Observability** - Prometheus metrics integration

---

## 📝 Documentation Standards

All documentation in this directory follows these conventions:

1. **Markdown Format**: All docs use GitHub-flavored Markdown
2. **Code Examples**: Include syntax-highlighted code blocks
3. **Diagrams**: Use Mermaid for architecture diagrams
4. **Cross-References**: Link to related documentation
5. **Version Tags**: Include version numbers where applicable
6. **Last Updated**: Date stamps for tracking changes

---

## 🤝 Contributing to Documentation

Found an error or want to improve the docs?

1. Documentation lives in `/docs` directory
2. Follow existing formatting conventions
3. Include code examples where helpful
4. Add Mermaid diagrams for complex concepts
5. Update this README when adding new docs

---

## 📧 Support

- **GitHub Issues**: [Report bugs or request features](https://github.com/yourusername/my-manus/issues)
- **Discussions**: [Ask questions and share ideas](https://github.com/yourusername/my-manus/discussions)
- **Email**: support@mymanus.ai

---

## 🔗 External Resources

- [Manus AI Official Site](https://manus.im)
- [CodeAct Paper (ICML 2024)](https://arxiv.org/abs/2402.01030)
- [Spring AI Documentation](https://docs.spring.io/spring-ai/reference/)
- [React Documentation](https://react.dev/)
- [Playwright Documentation](https://playwright.dev/)

---

**Last Updated**: 2025-11-25
**Documentation Version**: 2.0
**Project Status**: 100% Complete ✅
