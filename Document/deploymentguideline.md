# Lab Automation Multi-Environment Deployment Guideline

Comprehensive reference for deploying the Spring Boot backend to AWS across dev, test, and prod using ECS Fargate, ECR, RDS, Route53, and an Application Load Balancer. Follow the sections sequentially when building the infrastructure and CI/CD.

---

## 1. Architecture Overview

```
                                   ┌──────────────────────────────┐
                                   │          GitHub             │
                                   │ Branches: dev/test/main     │
                                   │  GitHub Actions + OIDC      │
                                   └──────────────┬──────────────┘
                                                  │
                                                  ▼
                              ┌────────────────────────────────────────┐
                              │              AWS Account               │
                              │                                        │
                              │  ┌──────────┐     ┌──────────────┐    │
                              │  │  ECR     │◄────┤  GitHub CI   │    │
                              │  └────┬─────┘     └─────┬────────┘    │
                              │       │                │             │
                              │       ▼                │             │
                              │  ┌──────────────┐      │             │
                              │  │ ECS Cluster  │      │             │
                              │  │ Services:    │      │             │
                              │  │ dev/test/prod│      │             │
                              │  └─────┬────────┘      │             │
                              │        │               │             │
                              │  ┌─────▼───────┐       │             │
                              │  │ Application │       │             │
                              │  │ Load Balancer       │             │
                              │  └──┬─────┬────┘       │             │
                              │     │     │            │             │
                              │ Route53  Target Groups │             │
                              │ (dev/test/prod)        │             │
                              │                        │             │
                              │  ┌───────────────┐     │             │
                              │  │ RDS Instances │◄────┘             │
                              │  │ dev/test/prod │                   │
                              │  └───────────────┘                   │
                              └────────────────────────────────────────┘
```

---

## 2. Spring Boot Profile Strategy

- Store shared defaults in `src/main/resources/application.yml` and keep three profile-specific overrides: `application-dev.yml`, `application-test.yml`, and `application-prod.yml`.
- Activate profile at runtime through environment variable `SPRING_PROFILES_ACTIVE`, set per ECS task definition.
- Example configuration:

```
# application.yml
spring:
  application:
    name: lab-automation
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}
```

```
# application-dev.yml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST_DEV}:5432/lab_automation_dev
    username: ${DB_USER_DEV}
    password: ${DB_PASS_DEV}
  jpa:
    hibernate:
      ddl-auto: update
logging:
  level:
    root: DEBUG
```

- Repeat for test/prod with environment-specific DB hosts, caching flags, log levels, email gateways, etc.

---

## 3. Dockerfile Example

```
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring
COPY target/lab-automation.jar app.jar
RUN chown spring:spring app.jar
USER spring
EXPOSE 8080
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","app.jar"]
```

**Key points**
- Build jar with `mvn clean package -DskipTests` before the Docker build.
- Non-root user enhances container security.
- `EXPOSE 8080` aligns with ALB target group port mapping.
- Keep image lean; Alpine + Temurin JRE is sufficient for runtime.

---

## 4. Amazon ECR Strategy

1. Create three repos: `lab-automation-dev`, `lab-automation-test`, `lab-automation-prod`.
2. Enforce immutable tags to prevent overwriting released images.
3. Tag convention: `<env>-<git-sha>` plus rolling tag `<env>-latest`.
   - Example: `123456789012.dkr.ecr.us-east-1.amazonaws.com/lab-automation-dev:dev-4b1c2f9`.
4. Enable lifecycle policies to expire older tags (e.g., keep latest 30 per repo).

---

## 5. ECS Fargate Cluster & Services

- Single ECS cluster `lab-automation`.
- Launch type: Fargate, platform version `LATEST`.
- Services:
  - `lab-automation-dev`: desired count 1, runs in private subnets, uses `task-def-dev`.
  - `lab-automation-test`: similar but can run in isolated VPC or same VPC with stricter scaling.
  - `lab-automation-prod`: desired count ≥2, uses deployment circuit breaker, min healthy percent 100, max 200.
- Service autoscaling: target tracking on CPU 60% for prod.
- Each service registers to its dedicated ALB target group.

---

## 6. Load Balancer, Target Groups, Route53

**Application Load Balancer**
- Deployed in public subnets.
- HTTPS listener (443) with ACM cert covering `*.lab.example.com`.
- Optional HTTP listener (80) redirects to HTTPS.

**Target Groups**
- `tg-lab-dev`: health check `/actuator/health`, matcher 200-399.
- `tg-lab-test`.
- `tg-lab-prod` (tight thresholds, e.g., healthy threshold 5).

**Listener Rules**
- Host `dev.lab.example.com` → `tg-lab-dev`.
- Host `test.lab.example.com` → `tg-lab-test`.
- Default or host `app.lab.example.com` → `tg-lab-prod`.

**Route53**
- Hosted zone `lab.example.com`.
- A/AAAA alias records pointing each subdomain to ALB DNS.
- Enable health checks for prod record referencing ALB target.

---

## 7. Networking & Security Groups

- VPC with two public subnets (ALB) and two private subnets (ECS + RDS) across AZs.
- NAT Gateway in each AZ for private subnet egress.

**Security Groups**
- `sg-alb`: inbound 80/443 from `0.0.0.0/0`, outbound to `sg-ecs`.
- `sg-ecs`: inbound 8080 from `sg-alb`, outbound 5432 to `sg-rds`, general outbound allowed for updates.
- `sg-rds`: inbound 5432 only from `sg-ecs`.

- Enable AWS VPC endpoints (Interface) for Secrets Manager, CloudWatch Logs to avoid internet traversal.
- ECS tasks use `awsvpc` networking; assign private subnets and `sg-ecs`.

---

## 8. RDS per Environment

- Engine: PostgreSQL 15 (example).
- Instances:
  - Dev: `db.t4g.micro`, single AZ, smaller storage.
  - Test: `db.t4g.small`, automated backups retained 7 days.
  - Prod: `db.t4g.medium` or larger, Multi-AZ, 35-day backup retention, Performance Insights enabled.
- Use subnet group limited to private subnets.
- Parameter groups tuned per environment (prod with connection limits, SSL enforcement).
- Apply maintenance windows outside business hours; enable deletion protection for prod.

---

## 9. Credentials with AWS Secrets Manager

- Secrets per environment: `lab-automation/dev/db`, `lab-automation/test/db`, `lab-automation/prod/db`.
- JSON payload:
```
{
  "username": "lab_dev_user",
  "password": "REDACTED",
  "host": "lab-dev.abcdefg.us-east-1.rds.amazonaws.com"
}
```
- Additional secrets for SMTP, third-party APIs, etc.
- ECS task role policy needs:
```
{
  "Effect": "Allow",
  "Action": ["secretsmanager:GetSecretValue"],
  "Resource": "arn:aws:secretsmanager:us-east-1:123456789012:secret:lab-automation/*"
}
```
- Optionally enable rotation for prod DB credentials; coordinate with application connection pool refresh.

---

## 10. GitHub Actions CI/CD Workflow

`.github/workflows/deploy.yml`

```
name: Deploy Lab Automation

on:
  push:
    branches: [dev, test, main]

env:
  AWS_REGION: us-east-1
  ECR_REGISTRY: 123456789012.dkr.ecr.us-east-1.amazonaws.com

permissions:
  id-token: write
  contents: read

jobs:
  deploy:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4

      - name: Map Environment
        id: envmap
        run: |
          BRANCH=${GITHUB_REF##*/}
          case $BRANCH in
            dev)
              echo "ENV=dev" >> $GITHUB_OUTPUT
              echo "ECR_REPO=lab-automation-dev" >> $GITHUB_OUTPUT
              echo "ECS_SERVICE=lab-automation-dev" >> $GITHUB_OUTPUT
              echo "TASK_NAME=lab-automation-dev" >> $GITHUB_OUTPUT
              ;;
            test)
              echo "ENV=test" >> $GITHUB_OUTPUT
              echo "ECR_REPO=lab-automation-test" >> $GITHUB_OUTPUT
              echo "ECS_SERVICE=lab-automation-test" >> $GITHUB_OUTPUT
              echo "TASK_NAME=lab-automation-test" >> $GITHUB_OUTPUT
              ;;
            *)
              echo "ENV=prod" >> $GITHUB_OUTPUT
              echo "ECR_REPO=lab-automation-prod" >> $GITHUB_OUTPUT
              echo "ECS_SERVICE=lab-automation-prod" >> $GITHUB_OUTPUT
              echo "TASK_NAME=lab-automation-prod" >> $GITHUB_OUTPUT
              ;;
          esac

      - name: Configure AWS Credentials
        uses: aws-actions/configure-aws-credentials@v4
        with:
          role-to-assume: arn:aws:iam::123456789012:role/github-oidc-lab-automation
          aws-region: ${{ env.AWS_REGION }}

      - uses: aws-actions/amazon-ecr-login@v2

      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: "21"

      - name: Build Jar
        run: mvn clean package -DskipTests

      - name: Build Container
        run: |
          IMAGE_TAG=${{ steps.envmap.outputs.ENV }}-${{ github.sha }}
          docker build -t $ECR_REGISTRY/${{ steps.envmap.outputs.ECR_REPO }}:$IMAGE_TAG \
                       -t $ECR_REGISTRY/${{ steps.envmap.outputs.ECR_REPO }}:${{ steps.envmap.outputs.ENV }}-latest .

      - name: Push Container
        run: |
          for tag in ${{ steps.envmap.outputs.ENV }}-${{ github.sha }} ${{ steps.envmap.outputs.ENV }}-latest; do
            docker push $ECR_REGISTRY/${{ steps.envmap.outputs.ECR_REPO }}:$tag
          done

      - name: Render Task Definition
        run: |
          IMAGE="$ECR_REGISTRY/${{ steps.envmap.outputs.ECR_REPO }}:${{ steps.envmap.outputs.ENV }}-${{ github.sha }}"
          sed "s|<IMAGE>|$IMAGE|g" deploy/ecs-task-${{ steps.envmap.outputs.ENV }}.json > task-def.json

      - name: Deploy to ECS
        uses: aws-actions/amazon-ecs-deploy-task-definition@v2
        with:
          task-definition: task-def.json
          service: ${{ steps.envmap.outputs.ECS_SERVICE }}
          cluster: lab-automation
          wait-for-service-stability: true
```

- Workflow uses AWS OIDC, avoiding long-lived credentials.
- `deploy/ecs-task-*.json` templates stored in repo.

---

## 11. ECS Task Definition Explained

`deploy/ecs-task-prod.json`

```
{
  "family": "lab-automation-prod",
  "requiresCompatibilities": ["FARGATE"],
  "networkMode": "awsvpc",
  "cpu": "1024",
  "memory": "2048",
  "executionRoleArn": "arn:aws:iam::123456789012:role/ecsTaskExecutionRole",
  "taskRoleArn": "arn:aws:iam::123456789012:role/lab-automation-app-role",
  "containerDefinitions": [
    {
      "name": "lab-automation",
      "image": "<IMAGE>",
      "portMappings": [
        { "containerPort": 8080, "hostPort": 8080, "protocol": "tcp" }
      ],
      "environment": [
        { "name": "SPRING_PROFILES_ACTIVE", "value": "prod" }
      ],
      "secrets": [
        { "name": "DB_USER_PROD", "valueFrom": "arn:aws:secretsmanager:...:prod/db:username::" },
        { "name": "DB_PASS_PROD", "valueFrom": "arn:aws:secretsmanager:...:prod/db:password::" },
        { "name": "DB_HOST_PROD", "valueFrom": "arn:aws:secretsmanager:...:prod/db:host::" }
      ],
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/lab-automation",
          "awslogs-region": "us-east-1",
          "awslogs-stream-prefix": "prod"
        }
      },
      "healthCheck": {
        "command": [
          "CMD-SHELL",
          "curl -f http://localhost:8080/actuator/health || exit 1"
        ],
        "interval": 30,
        "timeout": 5,
        "retries": 3
      },
      "essential": true
    }
  ]
}
```

**Section explanations**
- `family`: logical name; each update creates new revision.
- `requiresCompatibilities` + `networkMode`: required for Fargate.
- `cpu`/`memory`: Fargate task size (1 vCPU/2GB). Adjust for other environments.
- `executionRoleArn`: grants ECS agent permissions to pull images/logs.
- `taskRoleArn`: runtime IAM permissions for app (Secrets Manager, S3).
- `containerDefinitions`: each container config; specify image, port, env, secrets.
- `logConfiguration`: pushes stdout/stderr to CloudWatch log group, enabling aggregated logs per environment.
- `healthCheck`: container-level check complementing ALB health check.

---

## 12. Deployment Flow

1. Developer pushes code to `dev`, `test`, or `main`.
2. GitHub Actions workflow triggers on branch.
3. Workflow maps branch → environment metadata (repo, ECS service, task family).
4. AWS OIDC role assumed; temporary credentials issued.
5. Maven builds Spring Boot jar; Docker image built with environment tags.
6. Image pushed to the correct ECR repo with SHA + latest tag.
7. Task definition template rendered with new image URI.
8. ECS deploy action registers new task definition and updates service.
9. ECS launches replacement task(s); ALB health check ensures `/actuator/health` passes before draining old tasks.
10. CloudWatch logs capture deployment logs; CloudWatch/ECS events confirm stability.

---

## 13. Troubleshooting & Debugging

- **GitHub Actions fails**: ensure IAM role trust policy includes GitHub OIDC provider and repo branch conditions; verify `aws-actions/configure-aws-credentials` version.
- **ECS task stops immediately**: inspect `lastStatus` via `aws ecs describe-tasks`; check CloudWatch logs for stack traces, ensure secrets are accessible and DB host resolves.
- **ALB shows unhealthy targets**: confirm target group health check path matches actuator endpoint, security groups allow ALB → ECS traffic, Spring Boot is listening on 0.0.0.0:8080.
- **Database connectivity errors**: verify RDS SG inbound rules, Secrets Manager values, and that ECS tasks run in same VPC/subnet with routing to RDS.
- **Image not updated**: check that `sed` replaced `<IMAGE>` placeholder; ensure ECS service uses the latest task definition revision (force deployment if needed).
- **Secrets rotation**: if credentials rotate, restart ECS tasks to pull new values or integrate runtime secrets caching logic.
- **Stuck deployments**: use ECS console to stop unhealthy tasks, review ALB metrics (5xx), reduce desired count temporarily.

---

## 14. Production Best Practices

- **Autoscaling**: configure ECS Service Auto Scaling (CPU/Mem), plus scheduled actions for peak traffic; consider target-tracking on ALB request count per target.
- **Monitoring & Alarms**: CloudWatch dashboards for ECS service metrics, ALB latency, RDS CPU/Connections; set alarms to notify via SNS/Slack.
- **Logging & Tracing**: send CloudWatch logs to centralized store (OpenSearch/Datadog); integrate AWS X-Ray or OpenTelemetry for distributed tracing.
- **Rollbacks**: keep previous task definition revisions and Docker tags; run `aws ecs update-service --force-new-deployment --task-definition <old>` for quick rollback.
- **Tagging**: apply AWS resource tags (`Application=LabAutomation`, `Environment`, `Owner`); enforce via Service Catalog or Control Tower guardrails.
- **Health Checks**: enable both container health checks and ALB target checks; implement `/actuator/health` readiness indicators (DB, cache).
- **Backups & DR**: automated backups, manual snapshots before schema changes, cross-region read replica or snapshot copy for prod.
- **Security**: enforce least-privilege IAM, enable VPC flow logs, restrict outbound traffic with security group egress rules, rotate secrets regularly.
- **Cost Optimization**: right-size Fargate tasks, schedule dev/test services to scale to zero off-hours, use Savings Plans for prod capacity.

---

## 15. Quick Reference Checklist

- [ ] Spring profiles configured with env-specific overrides.
- [ ] Docker image built via Maven + Dockerfile above.
- [ ] ECR repos created with lifecycle policies.
- [ ] ECS cluster + services mapped to target groups.
- [ ] ALB listener rules and Route53 subdomains deployed.
- [ ] Security groups enforce least privilege between ALB → ECS → RDS.
- [ ] RDS instances provisioned per environment with correct sizing.
- [ ] Secrets Manager stores DB/app credentials; task role permissions set.
- [ ] GitHub Actions workflow committed; IAM role trust set for OIDC.
- [ ] Task definition templates stored under `deploy/`.
- [ ] Monitoring, autoscaling, and rollback strategies documented.

Use this document as the canonical reference while implementing and operating the Lab Automation deployment pipeline across all environments.

