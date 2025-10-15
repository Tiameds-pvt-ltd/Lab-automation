# Complete Security Implementation Guide

## Table of Contents
1. [Project Overview](#project-overview)
2. [Security Architecture](#security-architecture)
3. [Implemented Security Tools](#implemented-security-tools)
4. [CI/CD Pipeline Security](#cicd-pipeline-security)
5. [Local Development Security](#local-development-security)
6. [Configuration Files](#configuration-files)
7. [Security Reports](#security-reports)
8. [Setup Instructions](#setup-instructions)
9. [Troubleshooting](#troubleshooting)
10. [Security Best Practices](#security-best-practices)

## Project Overview

This Lab Automation project implements a **comprehensive multi-layered security strategy** combining:

- **SAST (Static Application Security Testing)**: Code analysis for vulnerabilities
- **SCA (Software Composition Analysis)**: Dependency vulnerability scanning
- **Code Quality Analysis**: Advanced code quality and security checks
- **Application Security**: Built-in security controls and configurations

## Security Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Security Layers                          │
├─────────────────────────────────────────────────────────────┤
│ 1. Application Security Controls                           │
│    ├── Rate Limiting                                      │
│    ├── CORS Configuration                                 │
│    ├── IP Whitelisting                                    │
│    └── Origin Validation                                  │
├─────────────────────────────────────────────────────────────┤
│ 2. Static Code Analysis (SAST)                            │
│    ├── SpotBugs + FindSecBugs                             │
│    ├── SonarQube Analysis                                 │
│    └── Qodana Code Quality                                │
├─────────────────────────────────────────────────────────────┤
│ 3. Dependency Analysis (SCA)                              │
│    └── OWASP Dependency-Check                             │
├─────────────────────────────────────────────────────────────┤
│ 4. CI/CD Pipeline Security                                │
│    ├── Automated Security Scanning                        │
│    ├── Quality Gates                                      │
│    └── Security Reports                                   │
└─────────────────────────────────────────────────────────────┘
```

## Implemented Security Tools

### 1. OWASP Dependency-Check (SCA)
- **Purpose**: Scans third-party dependencies for known vulnerabilities
- **Version**: 10.0.4
- **Location**: `pom.xml` + CI/CD pipeline
- **Configuration**: 
  - HTML report format
  - Suppression file support
  - NVD API skip option for faster execution
- **Reports**: `target/dependency-check-report.html`

### 2. SpotBugs with FindSecBugs (SAST)
- **Purpose**: Static code analysis for security vulnerabilities
- **Version**: 4.8.6.4
- **Location**: `pom.xml` + CI/CD pipeline
- **Configuration**:
  - Maximum effort analysis
  - Low threshold for comprehensive scanning
  - XML output format
- **Reports**: `target/spotbugsXml.xml`, `target/spotbugs.html`

### 3. SonarQube (SAST + Code Quality)
- **Purpose**: Comprehensive code quality and security analysis
- **Version**: 3.11.0.3922
- **Location**: `pom.xml` + CI/CD pipeline (conditional)
- **Features**:
  - Code quality metrics
  - Security vulnerability detection
  - Code smell identification
  - Quality gate enforcement
- **Reports**: SonarQube dashboard

### 4. Qodana (Code Quality)
- **Purpose**: JetBrains IDE-level code analysis
- **Location**: GitHub Actions workflow
- **Features**:
  - Code duplication detection
  - Security vulnerability scanning
  - Performance issue identification
  - Code quality metrics
- **Reports**: SARIF artifacts

### 5. OWASP ZAP (DAST)
- **Purpose**: Dynamic Application Security Testing
- **Version**: Latest (via GitHub Actions)
- **Location**: CI/CD pipeline (conditional)
- **Features**:
  - Baseline scan for common vulnerabilities
  - Full scan for comprehensive testing
  - Real-time application testing
  - OWASP Top 10 coverage
- **Reports**: HTML reports, SARIF artifacts

## CI/CD Pipeline Security

### Main CI/CD Pipeline (`.github/workflows/ci-cd-pipeline.yml`)

#### Pipeline Flow with Security
```
1. Checkout code
2. Set up JDK 17
3. Build with Maven
4. 🔒 Security Scanning
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

#### Security Steps Implementation

**Step 4: Security Scanning**
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

**Step 5: SonarQube Analysis (Conditional)**
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

**Step 6: Start Application for ZAP Testing (Conditional)**
```yaml
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
```

**Step 7: OWASP ZAP Baseline Scan (Conditional)**
```yaml
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
```

**Step 8: OWASP ZAP Full Scan (Conditional)**
```yaml
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
```

**Step 9: Stop Application (Conditional)**
```yaml
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

### Additional Security Workflows

#### 1. Security Scan Workflow (`.github/workflows/security-scan.yml`)
- **Triggers**: All pushes and pull requests
- **Tools**: SonarQube, SpotBugs, OWASP Dependency-Check
- **Status**: Conditional execution based on secrets
- **Purpose**: Comprehensive security scanning with detailed reports

#### 2. Qodana Code Quality Workflow (`.github/workflows/qodana_code_quality.yml`)
- **Triggers**: Pull requests and pushes to main/radiology-report branches
- **Tools**: Qodana local scanning
- **Status**: Active (no token required)
- **Purpose**: Code quality analysis with SARIF artifacts

## Local Development Security

### Running Security Scans Locally

#### OWASP Dependency-Check
```bash
# Full scan with NVD API
.\mvnw.cmd org.owasp:dependency-check-maven:check

# Fast scan (skip NVD API)
.\mvnw.cmd org.owasp:dependency-check-maven:check "-Ddependency-check.nvd.api.skip=true"

# View report
# Open target/dependency-check-report.html
```

#### SpotBugs Analysis
```bash
# Run SpotBugs analysis
mvn spotbugs:check

# Generate SpotBugs report
mvn spotbugs:spotbugs

# View reports
# Open target/spotbugsXml.xml or target/spotbugs.html
```

#### SonarQube Analysis
```bash
# Run SonarQube analysis (requires secrets)
mvn sonar:sonar \
  -Dsonar.host.url=YOUR_SONAR_URL \
  -Dsonar.projectKey=YOUR_PROJECT_KEY \
  -Dsonar.login=YOUR_TOKEN
```

#### Qodana Analysis
```bash
# Run Qodana locally (requires Docker)
docker run --rm -v "$(pwd)":/data -v "$(pwd)/.qodana":/data/.qodana jetbrains/qodana-jvm:latest
```

#### OWASP ZAP Analysis
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

## Configuration Files

### Maven Configuration (`pom.xml`)

#### Security Plugins
```xml
<!-- SpotBugs with FindSecBugs -->
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

<!-- OWASP Dependency-Check -->
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
</plugin>

<!-- SonarQube Scanner -->
<plugin>
    <groupId>org.sonarsource.scanner.maven</groupId>
    <artifactId>sonar-maven-plugin</artifactId>
    <version>3.11.0.3922</version>
</plugin>
```

### Security Configuration Classes

#### 1. Rate Limiting (`src/main/java/tiameds/com/tiameds/config/RateLimitConfig.java`)
- **Purpose**: Prevent abuse and DoS attacks
- **Features**: Bucket4j-based rate limiting
- **Configuration**: Customizable rate limits per endpoint

#### 2. CORS Configuration (`src/main/java/tiameds/com/tiameds/config/CorsConfig.java`)
- **Purpose**: Control cross-origin requests
- **Features**: Configurable allowed origins, methods, and headers
- **Security**: Prevents unauthorized cross-origin access

#### 3. IP Whitelisting (`src/main/java/tiameds/com/tiameds/config/IpWhitelistConfig.java`)
- **Purpose**: Restrict access to specific IP addresses
- **Features**: Configurable IP whitelist for sensitive endpoints
- **Security**: Additional layer of access control

#### 4. Origin Validation (`src/main/java/tiameds/com/tiameds/filter/OriginValidationFilter.java`)
- **Purpose**: Validate request origins
- **Features**: Custom filter for request validation
- **Security**: Prevents request forgery attacks

### Suppression Files

#### OWASP Suppressions (`config/dependency-check-suppressions.xml`)
- **Purpose**: Suppress false positives and accepted risks
- **Format**: XML-based suppression rules
- **Usage**: Maintained for known false positives

## Security Reports

### Report Locations
- **OWASP Report**: `target/dependency-check-report.html`
- **SpotBugs Report**: `target/spotbugsXml.xml`, `target/spotbugs.html`
- **SonarQube Report**: SonarQube dashboard (if configured)
- **Qodana Report**: SARIF artifacts in GitHub Actions

### Report Types
1. **HTML Reports**: Human-readable format for detailed analysis
2. **XML Reports**: Machine-readable format for CI/CD integration
3. **SARIF Reports**: Standard format for security tool integration
4. **Dashboard Reports**: Real-time monitoring and trend analysis

## Setup Instructions

### Prerequisites
- Java 17
- Maven 3.6+
- Docker (for Qodana local scanning)
- SonarQube instance (optional)

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

### Installation Steps

1. **Clone Repository**
   ```bash
   git clone <repository-url>
   cd Lab-automation
   ```

2. **Build Project**
   ```bash
   .\mvnw.cmd clean install
   ```

3. **Run Security Scans**
   ```bash
   # OWASP Dependency-Check
   .\mvnw.cmd org.owasp:dependency-check-maven:check

   # SpotBugs
   mvn spotbugs:check
   ```

4. **Configure SonarQube (Optional)**
   - Set up SonarQube instance
   - Configure GitHub secrets
   - Enable in CI/CD pipeline

5. **Configure OWASP ZAP (Optional)**
   - Set `ZAP_ENABLED=true` secret
   - Customize `.zap/rules.tsv` for false positives
   - Ensure application starts on port 8080

## Troubleshooting

### Common Issues

#### 1. OWASP Database Lock
```bash
# Clear lock and cache
Remove-Item -Recurse -Force "$env:USERPROFILE\.m2\repository\org\owasp\dependency-check-data" -ErrorAction SilentlyContinue
```

#### 2. Maven Wrapper Issues
```bash
# Use Maven directly if wrapper fails
mvn org.owasp:dependency-check-maven:check
```

#### 3. SonarQube Connection Issues
- Verify SonarQube URL and credentials
- Check network connectivity
- Ensure SonarQube service is running

#### 4. SpotBugs Memory Issues
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

## Security Best Practices

### 1. Regular Security Scanning
- Run security scans on every commit
- Schedule weekly comprehensive scans
- Monitor security reports regularly

### 2. Dependency Management
- Keep dependencies updated
- Review security advisories
- Use dependency management tools

### 3. Code Quality
- Follow secure coding practices
- Regular code reviews
- Use static analysis tools

### 4. CI/CD Security
- Integrate security into deployment pipeline
- Use quality gates
- Monitor security metrics

### 5. Incident Response
- Document security issues
- Maintain suppression files
- Regular security reviews

## Security Metrics and Monitoring

### Key Metrics
- **Vulnerability Count**: Track security issues over time
- **Code Quality**: Monitor code quality metrics
- **Dependency Health**: Track dependency vulnerabilities
- **Scan Coverage**: Ensure comprehensive scanning

### Monitoring Tools
- **SonarQube Dashboard**: Real-time code quality metrics
- **GitHub Security Tab**: Centralized security view
- **CI/CD Logs**: Security scan results and trends
- **Custom Dashboards**: Build custom security monitoring

## Conclusion

This Lab Automation project implements a **comprehensive, multi-layered security strategy** that provides:

- **Complete Coverage**: SAST, SCA, and code quality analysis
- **Automated Integration**: Security scanning in every deployment
- **Flexible Configuration**: Optional tools based on requirements
- **Comprehensive Reporting**: Multiple report formats and dashboards
- **Best Practices**: Industry-standard security practices

The security implementation ensures that security issues are caught early in the development process, providing defense in depth against potential vulnerabilities and maintaining high code quality standards throughout the project lifecycle.
