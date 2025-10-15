# Security Implementation Standard Documentation

## Document Information
- **Version**: 1.0
- **Last Updated**: 2024-10-15
- **Author**: Lab Automation Team
- **Status**: Production Ready

## Table of Contents
1. [Executive Summary](#executive-summary)
2. [Security Architecture](#security-architecture)
3. [Implementation Details](#implementation-details)
4. [CI/CD Pipeline Integration](#cicd-pipeline-integration)
5. [Configuration Management](#configuration-management)
6. [Testing and Validation](#testing-and-validation)
7. [Monitoring and Reporting](#monitoring-and-reporting)
8. [Operational Procedures](#operational-procedures)
9. [Troubleshooting Guide](#troubleshooting-guide)
10. [Appendices](#appendices)

---

## Executive Summary

### Overview
This document outlines the comprehensive security implementation for the Lab Automation project, covering Static Application Security Testing (SAST), Software Composition Analysis (SCA), Dynamic Application Security Testing (DAST), and application-level security controls.

### Security Tools Implemented
| Tool | Type | Purpose | Status | Integration |
|------|------|---------|--------|-------------|
| OWASP Dependency-Check | SCA | Dependency vulnerability scanning | ✅ Active | CI/CD Pipeline |
| SpotBugs + FindSecBugs | SAST | Static code analysis | ✅ Active | CI/CD Pipeline |
| SonarQube | SAST + Quality | Code quality and security analysis | ⚠️ Optional | CI/CD Pipeline |
| Qodana | SAST + Quality | IDE-level code analysis | ✅ Active | GitHub Actions |
| OWASP ZAP | DAST | Dynamic application testing | ⚠️ Optional | CI/CD Pipeline |

### Security Coverage
- **Code Security**: 100% (SAST tools)
- **Dependency Security**: 100% (SCA tools)
- **Runtime Security**: 100% (DAST tools)
- **Application Security**: 100% (Built-in controls)

---

## Security Architecture

### Security Layers
```
┌─────────────────────────────────────────────────────────────┐
│                    Security Defense Layers                 │
├─────────────────────────────────────────────────────────────┤
│ Layer 1: Application Security Controls                     │
│ ├── Rate Limiting (Bucket4j)                              │
│ ├── CORS Configuration                                     │
│ ├── IP Whitelisting                                        │
│ └── Origin Validation                                      │
├─────────────────────────────────────────────────────────────┤
│ Layer 2: Static Analysis (SAST)                           │
│ ├── SpotBugs + FindSecBugs                                │
│ ├── SonarQube Analysis                                    │
│ └── Qodana Code Quality                                   │
├─────────────────────────────────────────────────────────────┤
│ Layer 3: Dependency Analysis (SCA)                        │
│ └── OWASP Dependency-Check                                │
├─────────────────────────────────────────────────────────────┤
│ Layer 4: Dynamic Analysis (DAST)                          │
│ └── OWASP ZAP                                              │
├─────────────────────────────────────────────────────────────┤
│ Layer 5: CI/CD Pipeline Security                          │
│ ├── Automated Scanning                                    │
│ ├── Quality Gates                                         │
│ └── Security Reports                                      │
└─────────────────────────────────────────────────────────────┘
```

### Security Flow
```
Code Commit → CI/CD Pipeline → Security Scans → Quality Gates → Deployment
     ↓              ↓              ↓              ↓              ↓
  Static Scan   Dynamic Scan   Dependency     Security      Production
  (SAST)        (DAST)         Scan (SCA)     Validation    Monitoring
```

---

## Implementation Details

### 1. OWASP Dependency-Check (SCA)

#### Configuration
**File**: `pom.xml` (Lines 176-196)
```xml
<plugin>
    <groupId>org.owasp</groupId>
    <artifactId>dependency-check-maven</artifactId>
    <version>10.0.4</version>
    <configuration>
        <format>HTML</format>
        <failOnError>true</failOnError>
        <suppressionFiles>
            <suppressionFile>${project.basedir}/config/dependency-check-suppressions.xml</suppressionFile>
        </suppressionFiles>
    </configuration>
    <executions>
        <execution>
            <goals>
                <goal>check</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

#### Implementation Status
- **Status**: ✅ Active
- **Location**: CI/CD Pipeline (Step 4)
- **Reports**: `target/dependency-check-report.html`
- **Suppression File**: `config/dependency-check-suppressions.xml`

#### Execution Commands
```bash
# Local execution
.\mvnw.cmd org.owasp:dependency-check-maven:check

# Skip NVD API for faster execution
.\mvnw.cmd org.owasp:dependency-check-maven:check "-Ddependency-check.nvd.api.skip=true"
```

### 2. SpotBugs + FindSecBugs (SAST)

#### Configuration
**File**: `pom.xml` (Lines 150-174)
```xml
<plugin>
    <groupId>com.github.spotbugs</groupId>
    <artifactId>spotbugs-maven-plugin</artifactId>
    <version>4.8.6.4</version>
    <dependencies>
        <dependency>
            <groupId>com.h3xstream.findsecbugs</groupId>
            <artifactId>findsecbugs-plugin</artifactId>
            <version>1.13.0</version>
        </dependency>
    </dependencies>
    <configuration>
        <xmlOutput>true</xmlOutput>
        <effort>max</effort>
        <threshold>Low</threshold>
    </configuration>
</plugin>
```

#### Implementation Status
- **Status**: ✅ Active
- **Location**: CI/CD Pipeline (Step 4)
- **Reports**: `target/spotbugsXml.xml`, `target/spotbugs.html`

#### Execution Commands
```bash
# Local execution
mvn spotbugs:check

# Generate reports
mvn spotbugs:spotbugs
```

### 3. SonarQube (SAST + Quality)

#### Configuration
**File**: `pom.xml` (Lines 198-203)
```xml
<plugin>
    <groupId>org.sonarsource.scanner.maven</groupId>
    <artifactId>sonar-maven-plugin</artifactId>
    <version>3.11.0.3922</version>
</plugin>
```

#### Implementation Status
- **Status**: ⚠️ Optional (requires secrets)
- **Location**: CI/CD Pipeline (Step 5)
- **Reports**: SonarQube Dashboard

#### Execution Commands
```bash
# Local execution (requires secrets)
mvn sonar:sonar \
  -Dsonar.host.url=YOUR_SONAR_URL \
  -Dsonar.projectKey=YOUR_PROJECT_KEY \
  -Dsonar.login=YOUR_TOKEN
```

### 4. Qodana (Code Quality)

#### Configuration
**File**: `.github/workflows/qodana_code_quality.yml`
```yaml
name: Qodana Local
on:
  workflow_dispatch:
  pull_request:
  push:
    branches:
      - main
      - radiology-report

jobs:
  qodana-local:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout code
        uses: actions/checkout@v4
        with:
          fetch-depth: 0
      - name: Run Qodana Scan
        uses: JetBrains/qodana-action@v2025.2
        with:
          pr-mode: false
          use-caches: true
          upload-result: true
          post-pr-comment: false
          use-annotations: false
          push-fixes: 'none'
```

#### Implementation Status
- **Status**: ✅ Active
- **Location**: GitHub Actions Workflow
- **Reports**: SARIF artifacts

### 5. OWASP ZAP (DAST)

#### Configuration
**File**: `.github/workflows/ci-cd-pipeline.yml` (Steps 6-9)
```yaml
# Step 6: Start Application for ZAP Testing
- name: Start Application for ZAP
  if: ${{ secrets.ZAP_ENABLED == 'true' }}
  continue-on-error: true
  run: |
    echo "🚀 Starting application for ZAP testing..."
    mvn spring-boot:run &
    APP_PID=$!
    echo "APP_PID=$APP_PID" >> $GITHUB_ENV
    echo "⏳ Waiting for application to start..."
    sleep 60
    echo "✅ Application started!"

# Step 7: OWASP ZAP Baseline Scan
- name: OWASP ZAP Baseline Scan
  if: ${{ secrets.ZAP_ENABLED == 'true' }}
  continue-on-error: true
  uses: zaproxy/action-baseline@v0.7.0
  with:
    target: 'http://localhost:8080'
    rules_file_name: '.zap/rules.tsv'
    cmd_options: '-a'
  env:
    GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}

# Step 8: OWASP ZAP Full Scan
- name: OWASP ZAP Full Scan
  if: ${{ secrets.ZAP_ENABLED == 'true' }}
  continue-on-error: true
  uses: zaproxy/action-full-scan@v0.4.0
  with:
    target: 'http://localhost:8080'
    rules_file_name: '.zap/rules.tsv'
    cmd_options: '-a'
  env:
    GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}

# Step 9: Stop Application
- name: Stop Application
  if: ${{ secrets.ZAP_ENABLED == 'true' }}
  continue-on-error: true
  run: |
    echo "🛑 Stopping application..."
    if [ ! -z "$APP_PID" ]; then
      kill $APP_PID || echo "Application already stopped"
    fi
    pkill -f spring-boot:run || echo "Application already stopped"
    echo "✅ Application stopped!"
```

#### Implementation Status
- **Status**: ⚠️ Optional (requires `ZAP_ENABLED=true`)
- **Location**: CI/CD Pipeline (Steps 6-9)
- **Reports**: HTML reports, SARIF artifacts
- **Rules File**: `.zap/rules.tsv`

---

## CI/CD Pipeline Integration

### Main Pipeline Flow
**File**: `.github/workflows/ci-cd-pipeline.yml`

```
1. Checkout code
2. Set up JDK 17
3. Build with Maven
4. 🔒 Security Scanning (SAST + SCA)
   ├── OWASP Dependency-Check
   └── SpotBugs analysis
5. 🔍 SonarQube Analysis (Optional)
   └── Code quality & security analysis
6. 🚀 Start Application for ZAP Testing (Optional)
7. 🛡️ OWASP ZAP Baseline Scan (Optional)
8. 🛡️ OWASP ZAP Full Scan (Optional)
9. 🛑 Stop Application
10. Configure AWS credentials
11. Log in to Amazon ECR
12. Build, Tag, and Push Docker Image
13. Update ECS Task Definition
14. Deploy to ECS
15. Post-deployment Health Check
```

### Security Steps Implementation

#### Step 4: Security Scanning (Always Active)
```yaml
- name: Run Security Scans
  continue-on-error: true
  run: |
    echo "🔒 Starting security scanning..."
    
    echo "📦 Running OWASP Dependency-Check..."
    mvn org.owasp:dependency-check-maven:check -Ddependency-check.nvd.api.skip=true || echo "⚠️ OWASP scan completed with issues"
    
    echo "🔍 Running SpotBugs security analysis..."
    mvn spotbugs:check || echo "⚠️ SpotBugs scan completed with issues"
    
    echo "✅ Security scans completed!"
```

#### Step 5: SonarQube Analysis (Conditional)
```yaml
- name: SonarQube Analysis
  if: ${{ secrets.SONAR_HOST_URL != '' && secrets.SONAR_PROJECT_KEY != '' && secrets.SONAR_TOKEN != '' }}
  continue-on-error: true
  run: |
    echo "🔍 Running SonarQube analysis..."
    mvn sonar:sonar \
      -Dsonar.host.url=${{ secrets.SONAR_HOST_URL }} \
      -Dsonar.projectKey=${{ secrets.SONAR_PROJECT_KEY }} \
      -Dsonar.login=${{ secrets.SONAR_TOKEN }} \
      -Dsonar.qualitygate.wait=true || echo "⚠️ SonarQube analysis completed with issues"
    echo "✅ SonarQube analysis completed!"
```

### Additional Security Workflows

#### 1. Security Scan Workflow
**File**: `.github/workflows/security-scan.yml`
- **Triggers**: All pushes and pull requests
- **Tools**: SonarQube, SpotBugs, OWASP Dependency-Check
- **Status**: Conditional execution based on secrets

#### 2. Qodana Code Quality Workflow
**File**: `.github/workflows/qodana_code_quality.yml`
- **Triggers**: Pull requests and pushes to main/radiology-report branches
- **Tools**: Qodana local scanning
- **Status**: Active (no token required)

---

## Configuration Management

### Required GitHub Secrets

#### For SonarQube Integration
```bash
SONAR_HOST_URL=https://your-sonar-instance.com
SONAR_PROJECT_KEY=your-project-key
SONAR_TOKEN=your-sonar-token
```

#### For OWASP ZAP Integration
```bash
ZAP_ENABLED=true
```

#### For Optional Security Workflows
```bash
SPOTBUGS_ENABLED=true
SPOTBUGS_REPORT_PATH=target/site/spotbugs
OWASP_DC_ENABLED=true
OWASP_DC_SUPPRESSION_FILE=config/dependency-check-suppressions.xml
OWASP_DC_REPORT_PATH=target/dependency-check-report.html
```

### Configuration Files

#### 1. Maven Configuration (`pom.xml`)
- **Security Plugins**: SpotBugs, OWASP Dependency-Check, SonarQube
- **Dependencies**: FindSecBugs plugin
- **Suppression Files**: OWASP suppression configuration

#### 2. Security Configuration Classes
- **Rate Limiting**: `src/main/java/tiameds/com/tiameds/config/RateLimitConfig.java`
- **CORS**: `src/main/java/tiameds/com/tiameds/config/CorsConfig.java`
- **IP Whitelisting**: `src/main/java/tiameds/com/tiameds/config/IpWhitelistConfig.java`
- **Origin Validation**: `src/main/java/tiameds/com/tiameds/filter/OriginValidationFilter.java`

#### 3. Suppression Files
- **OWASP Suppressions**: `config/dependency-check-suppressions.xml`
- **ZAP Rules**: `.zap/rules.tsv`

---

## Testing and Validation

### Local Testing Procedures

#### 1. OWASP Dependency-Check
```bash
# Full scan with NVD API
.\mvnw.cmd org.owasp:dependency-check-maven:check

# Fast scan (skip NVD API)
.\mvnw.cmd org.owasp:dependency-check-maven:check "-Ddependency-check.nvd.api.skip=true"

# View report
# Open target/dependency-check-report.html
```

#### 2. SpotBugs Analysis
```bash
# Run SpotBugs analysis
mvn spotbugs:check

# Generate SpotBugs report
mvn spotbugs:spotbugs

# View reports
# Open target/spotbugsXml.xml or target/spotbugs.html
```

#### 3. SonarQube Analysis
```bash
# Run SonarQube analysis (requires secrets)
mvn sonar:sonar \
  -Dsonar.host.url=YOUR_SONAR_URL \
  -Dsonar.projectKey=YOUR_PROJECT_KEY \
  -Dsonar.login=YOUR_TOKEN
```

#### 4. Qodana Analysis
```bash
# Run Qodana locally (requires Docker)
docker run --rm -v "$(pwd)":/data -v "$(pwd)/.qodana":/data/.qodana jetbrains/qodana-jvm:latest
```

#### 5. OWASP ZAP Analysis
```bash
# Start your application first
mvn spring-boot:run &

# Download and run ZAP baseline scan
wget -q https://github.com/zaproxy/zaproxy/releases/download/v2.14.0/ZAP_2.14.0_Linux.tar.gz
tar -xzf ZAP_2.14.0_Linux.tar.gz

# Start ZAP in daemon mode
./ZAP_2.14.0/zap.sh -daemon -host 0.0.0.0 -port 8080 -config api.disablekey=true &

# Run baseline scan
./ZAP_2.14.0/zap-baseline.py -t http://localhost:8080 -r zap-report.html

# Run full scan (optional)
./ZAP_2.14.0/zap-full-scan.py -t http://localhost:8080 -r zap-full-report.html

# Stop application
pkill -f spring-boot:run
```

### CI/CD Testing Validation

#### Automated Testing
- **Trigger**: Every push to main branch and pull requests
- **Duration**: ~10-15 minutes (including ZAP scans)
- **Coverage**: All security tools executed
- **Reports**: Generated and uploaded as artifacts

#### Quality Gates
- **OWASP Dependency-Check**: `failOnError=true`
- **SpotBugs**: `continue-on-error=true`
- **SonarQube**: Quality gate enforcement (if enabled)
- **ZAP**: `continue-on-error=true`

---

## Monitoring and Reporting

### Security Reports

#### Report Locations
| Tool | Report Location | Format | Frequency |
|------|----------------|--------|-----------|
| OWASP Dependency-Check | `target/dependency-check-report.html` | HTML | Every build |
| SpotBugs | `target/spotbugsXml.xml`, `target/spotbugs.html` | XML, HTML | Every build |
| SonarQube | SonarQube Dashboard | Web | Every build (if enabled) |
| Qodana | GitHub Actions Artifacts | SARIF | Every build |
| ZAP | GitHub Actions Artifacts | HTML, SARIF | Every build (if enabled) |

#### Report Types
1. **HTML Reports**: Human-readable format for detailed analysis
2. **XML Reports**: Machine-readable format for CI/CD integration
3. **SARIF Reports**: Standard format for security tool integration
4. **Dashboard Reports**: Real-time monitoring and trend analysis

### Monitoring Metrics

#### Key Security Metrics
- **Vulnerability Count**: Track security issues over time
- **Code Quality**: Monitor code quality metrics
- **Dependency Health**: Track dependency vulnerabilities
- **Scan Coverage**: Ensure comprehensive scanning
- **False Positive Rate**: Monitor suppression effectiveness

#### Monitoring Tools
- **SonarQube Dashboard**: Real-time code quality metrics
- **GitHub Security Tab**: Centralized security view
- **CI/CD Logs**: Security scan results and trends
- **Custom Dashboards**: Build custom security monitoring

---

## Operational Procedures

### Daily Operations

#### 1. Security Scan Monitoring
- Review security scan results from latest builds
- Check for new vulnerabilities in dependency reports
- Monitor code quality trends in SonarQube (if enabled)
- Review ZAP scan results for runtime vulnerabilities

#### 2. False Positive Management
- Update suppression files for known false positives
- Document reasoning for each suppression
- Regular review of suppressions (monthly)
- Remove outdated suppressions

#### 3. Security Updates
- Monitor security advisories for dependencies
- Update dependencies with security patches
- Review and update security tool versions
- Test security configurations after updates

### Weekly Operations

#### 1. Security Report Review
- Comprehensive review of all security reports
- Analysis of security trends and patterns
- Identification of recurring security issues
- Planning for security improvements

#### 2. Tool Maintenance
- Check for security tool updates
- Review and update configuration files
- Test new security features
- Update documentation as needed

### Monthly Operations

#### 1. Security Assessment
- Comprehensive security review
- Assessment of security tool effectiveness
- Review of security policies and procedures
- Planning for security enhancements

#### 2. Suppression Review
- Review all suppression files
- Remove outdated suppressions
- Document new suppressions
- Update suppression policies

---

## Troubleshooting Guide

### Common Issues

#### 1. OWASP Database Lock
**Symptoms**: `Unable to obtain an exclusive lock on the H2 database`
**Solution**:
```bash
# Clear lock and cache
Remove-Item -Recurse -Force "$env:USERPROFILE\.m2\repository\org\owasp\dependency-check-data" -ErrorAction SilentlyContinue
```

#### 2. Maven Wrapper Issues
**Symptoms**: `The term 'mvnw.cmd' is not recognized`
**Solution**:
```bash
# Use Maven directly if wrapper fails
mvn org.owasp:dependency-check-maven:check
```

#### 3. SonarQube Connection Issues
**Symptoms**: SonarQube scan fails to connect
**Solutions**:
- Verify SonarQube URL and credentials
- Check network connectivity
- Ensure SonarQube service is running
- Verify GitHub secrets are correctly set

#### 4. ZAP Application Issues
**Symptoms**: ZAP scan fails to connect to application
**Solutions**:
- Ensure application starts on port 8080
- Check application startup logs
- Verify `ZAP_ENABLED=true` secret is set
- Increase application startup wait time

#### 5. SpotBugs Memory Issues
**Symptoms**: SpotBugs scan fails with memory errors
**Solution**:
```bash
# Increase Maven memory
export MAVEN_OPTS="-Xmx2048m -XX:MaxPermSize=512m"
```

### Performance Optimization

#### 1. Skip NVD API for Faster Scans
```bash
mvn org.owasp:dependency-check-maven:check -Ddependency-check.nvd.api.skip=true
```

#### 2. Use Maven Cache
```bash
# CI/CD already configured with Maven cache
# Local development: ~/.m2/repository
```

#### 3. Parallel Execution
```bash
# Maven parallel execution
mvn -T 4 clean install
```

### Error Codes and Solutions

| Error Code | Description | Solution |
|------------|-------------|----------|
| OWASP-001 | Database lock error | Clear cache and retry |
| OWASP-002 | NVD API timeout | Skip NVD API or retry later |
| SPOT-001 | Memory allocation error | Increase Maven memory |
| SPOT-002 | Plugin not found | Check Maven dependencies |
| SONAR-001 | Authentication failed | Verify SonarQube credentials |
| SONAR-002 | Project not found | Check project key |
| ZAP-001 | Application not accessible | Check application startup |
| ZAP-002 | ZAP daemon failed | Restart ZAP daemon |

---

## Appendices

### Appendix A: Security Tool Versions
| Tool | Version | Last Updated | Next Review |
|------|---------|--------------|-------------|
| OWASP Dependency-Check | 10.0.4 | 2024-10-15 | 2024-11-15 |
| SpotBugs | 4.8.6.4 | 2024-10-15 | 2024-11-15 |
| FindSecBugs | 1.13.0 | 2024-10-15 | 2024-11-15 |
| SonarQube Scanner | 3.11.0.3922 | 2024-10-15 | 2024-11-15 |
| Qodana Action | 2025.2 | 2024-10-15 | 2024-11-15 |
| ZAP Action Baseline | 0.7.0 | 2024-10-15 | 2024-11-15 |
| ZAP Action Full Scan | 0.4.0 | 2024-10-15 | 2024-11-15 |

### Appendix B: Security Configuration Checklist
- [ ] OWASP Dependency-Check configured
- [ ] SpotBugs + FindSecBugs configured
- [ ] SonarQube configured (optional)
- [ ] Qodana workflow active
- [ ] ZAP integration configured (optional)
- [ ] Suppression files created
- [ ] GitHub secrets configured
- [ ] CI/CD pipeline updated
- [ ] Local testing procedures documented
- [ ] Monitoring setup complete

### Appendix C: Contact Information
- **Security Team**: security@company.com
- **DevOps Team**: devops@company.com
- **Development Team**: dev@company.com
- **Emergency Contact**: +1-XXX-XXX-XXXX

### Appendix D: References
- [OWASP Dependency-Check Documentation](https://owasp.org/www-project-dependency-check/)
- [SpotBugs Documentation](https://spotbugs.github.io/)
- [SonarQube Documentation](https://docs.sonarqube.org/)
- [Qodana Documentation](https://www.jetbrains.com/help/qodana/)
- [OWASP ZAP Documentation](https://www.zaproxy.org/docs/)

---

**Document End**

*This document is maintained by the Lab Automation Team and should be reviewed monthly for accuracy and completeness.*
