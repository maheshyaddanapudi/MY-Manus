# Deployment Guide

## Deployment Strategy

Three-phase rollout: Development → Staging → Production

## Environment Overview

### Development
- Local Docker Compose
- Auth disabled
- Debug logging
- Hot reload enabled
- Single instance

### Staging  
- Kubernetes cluster
- Auth enabled
- Production-like data
- Performance testing
- Multi-instance

### Production
- Multi-region Kubernetes
- Full auth + rate limiting
- High availability
- Auto-scaling
- Monitoring + alerts

## Infrastructure Architecture

### Core Services
- **Application**: Spring Boot pods
- **Frontend**: React static files on CDN
- **Database**: PostgreSQL (managed service)
- **Sandbox**: Docker-in-Docker nodes
- **Load Balancer**: Layer 7 with SSL

### Supporting Services
- **Monitoring**: Prometheus + Grafana
- **Logging**: ELK stack
- **Tracing**: Jaeger
- **Alerts**: PagerDuty

## Database Configuration

### PostgreSQL Setup
- Version 15+
- High availability replica
- Point-in-time recovery
- Connection pooling
- JSONB indexing

### Schema Management
- Flyway migrations
- Version control
- Rollback procedures
- Index optimization
- Vacuum scheduling

### Performance Tuning
```
max_connections = 200
shared_buffers = 256MB
effective_cache_size = 1GB
work_mem = 4MB
```

## Kubernetes Deployment

### Namespace Structure
```
agent-platform/
├── agent-platform-dev
├── agent-platform-staging  
└── agent-platform-prod
```

### Resource Allocation
- Backend: 2-10 pods
- Frontend: 2-5 pods
- Sandbox: Dedicated nodes
- Database: External managed

### Pod Configuration
```yaml
resources:
  requests:
    memory: 1Gi
    cpu: 500m
  limits:
    memory: 2Gi
    cpu: 2000m
```

### Autoscaling Rules
- CPU > 70%: Scale up
- CPU < 30%: Scale down
- Min replicas: 2
- Max replicas: 10

## Security Configuration

### Authentication Modes

**Development**:
```yaml
auth.enabled: false
cors.allowed: "*"
```

**Production**:
```yaml
auth.enabled: true
jwt.secret: ${JWT_SECRET}
cors.allowed: "https://yourdomain.com"
```

### Network Policies
- Ingress: HTTPS only
- Egress: Restricted to APIs
- Internal: Service mesh
- Sandbox: Fully isolated

### Secrets Management
- Kubernetes secrets
- Environment injection
- Rotation policy
- Audit logging

## CI/CD Pipeline

### Build Stage
1. Run tests
2. Security scan
3. Build Docker images
4. Push to registry
5. Tag with version

### Deploy Stage
1. Update Kubernetes manifests
2. Apply to cluster
3. Wait for health checks
4. Run smoke tests
5. Monitor metrics

### Rollback Strategy
- Keep 3 previous versions
- Instant rollback capability
- Database migration safety
- Feature flags

## Monitoring Setup

### Application Metrics
- Request rate
- Response time
- Error rate
- Agent success rate
- Tool usage

### System Metrics
- CPU utilization
- Memory usage
- Disk I/O
- Network traffic
- Container count

### Business Metrics
- Active users
- Tasks completed
- Token consumption
- Cost per task
- User satisfaction

### Alert Thresholds
- Error rate > 1%
- Response time > 2s
- CPU > 80%
- Memory > 90%
- Disk > 85%

## Performance Optimization

### Caching Strategy
- Static assets: CDN
- API responses: Application cache
- Database queries: Query cache
- Session data: In-memory

### Database Optimization
- Connection pooling
- Query optimization
- Index tuning
- Partitioning strategy
- Read replicas

### Frontend Optimization
- Code splitting
- Lazy loading
- Asset compression
- CDN distribution
- Service worker

## Scaling Patterns

### Horizontal Scaling
- Stateless backend
- Session affinity
- Distributed cache
- Queue-based tools

### Vertical Scaling
- Sandbox nodes: More CPU
- Database: More memory
- Cache: More storage

### Geographic Distribution
- Multi-region deployment
- Data replication
- CDN edge locations
- Latency-based routing

## Cost Optimization

### Resource Management
- Spot instances for sandbox
- Reserved instances for core
- Auto-scaling policies
- Idle resource cleanup

### Monitoring Costs
- Track per component
- Alert on anomalies
- Regular optimization
- Usage forecasting

## Disaster Recovery

### Backup Strategy
- Database: Daily snapshots
- Code: Git repository
- Configurations: Version controlled
- Secrets: Encrypted backup

### Recovery Procedures
- RTO: 1 hour
- RPO: 15 minutes
- Runbook documentation
- Regular drills

## Compliance & Security

### Data Protection
- Encryption at rest
- Encryption in transit
- PII handling
- Data retention policy

### Audit Requirements
- All actions logged
- User attribution
- Change tracking
- Regular reviews

## Deployment Checklist

### Pre-deployment
- [ ] Tests passing
- [ ] Security scan clean
- [ ] Performance baseline
- [ ] Rollback plan ready
- [ ] Team notified

### Deployment
- [ ] Blue-green deploy
- [ ] Health checks passing
- [ ] Smoke tests successful
- [ ] Metrics normal
- [ ] No critical alerts

### Post-deployment
- [ ] Monitor for 1 hour
- [ ] Check error rates
- [ ] Verify performance
- [ ] User feedback
- [ ] Document issues

## Environment Variables

### Required for Production
```bash
# Database
DB_HOST=
DB_NAME=
DB_USER=
DB_PASSWORD=

# Security
JWT_SECRET=
AUTH_ENABLED=true

# APIs
ANTHROPIC_API_KEY=

# Monitoring
SENTRY_DSN=
DATADOG_API_KEY=
```

## Common Issues

### Deployment Failures
- Image pull errors
- Health check timeouts
- Database migrations
- Secret misconfigurations

### Runtime Issues
- Memory leaks
- Connection pool exhaustion
- Sandbox resource limits
- WebSocket disconnections

### Performance Problems
- Slow database queries
- Large message handling
- Tool execution timeouts
- Frontend bundle size

## Maintenance Windows

### Regular Maintenance
- Weekly: Log rotation
- Monthly: Security patches
- Quarterly: Dependency updates
- Yearly: Major upgrades

### Emergency Maintenance
- Security vulnerabilities
- Critical bugs
- Performance degradation
- Data corruption

## Documentation

### Required Docs
- Architecture diagram
- API documentation
- Runbook procedures
- Troubleshooting guide
- Security policies

## Launch Phases

### Week 1-2: Internal Testing
- Team dogfooding
- Bug fixes
- Performance tuning

### Week 3-4: Beta Users
- Limited access
- Feedback collection
- Iterative improvements

### Week 5-6: General Availability
- Public launch
- Marketing push
- 24/7 monitoring
- Support ready

## Success Criteria

- 99.9% uptime
- <2s response time
- <0.1% error rate
- >90% task success
- <$0.10 per task cost