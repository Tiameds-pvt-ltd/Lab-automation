## Security Scanning Setup (GitHub Actions)

This guide shows how to run SAST/SCA on every push using GitHub Actions, aligned with the `security-scanning` block added to `src/main/resources/application-prod.yml`.

### 1) Configure environment variables (Repository → Settings → Secrets and variables → Actions)
- SONAR_HOST_URL: SonarQube server URL (e.g., `https://sonarcloud.io` or your self-hosted URL)
- SONAR_PROJECT_KEY: Your Sonar project key
- SONAR_TOKEN: Sonar token with analysis permissions
- SPOTBUGS_ENABLED: `true` to enable SpotBugs/FindSecBugs (default false)
- SPOTBUGS_REPORT_PATH: report file path (default `build/reports/spotbugs/spotbugs.xml`)
- OWASP_DC_ENABLED: `true` to enable Dependency-Check (default false)
- OWASP_DC_SUPPRESSION_FILE: path to suppression file if used
- OWASP_DC_REPORT_PATH: report file path (default `./reports/dependency-check-report.html`)

### 2) Add GitHub Actions workflow: `.github/workflows/security-scan.yml`
```yaml
name: Security Scan

on:
  push:
    branches: [ "**" ]
  pull_request:
    branches: [ "**" ]

jobs:
  build-and-scan:
    runs-on: ubuntu-latest
    permissions:
      contents: read
      pull-requests: write
    env:
      SONAR_HOST_URL: ${{ secrets.SONAR_HOST_URL }}
      SONAR_PROJECT_KEY: ${{ secrets.SONAR_PROJECT_KEY }}
      SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
      SPOTBUGS_ENABLED: ${{ secrets.SPOTBUGS_ENABLED }}
      SPOTBUGS_REPORT_PATH: ${{ secrets.SPOTBUGS_REPORT_PATH }}
      OWASP_DC_ENABLED: ${{ secrets.OWASP_DC_ENABLED }}
      OWASP_DC_SUPPRESSION_FILE: ${{ secrets.OWASP_DC_SUPPRESSION_FILE }}
      OWASP_DC_REPORT_PATH: ${{ secrets.OWASP_DC_REPORT_PATH }}
    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'

      - name: Cache Maven
        uses: actions/cache@v4
        with:
          path: ~/.m2/repository
          key: ${{ runner.os }}-maven-${{ hashFiles('**/pom.xml') }}
          restore-keys: |
            ${{ runner.os }}-maven-

      - name: Build (fail if tests fail)
        run: mvn -B -DskipTests=false clean verify

      # SonarQube SAST
      - name: Sonar Scan
        if: ${{ env.SONAR_HOST_URL != '' && env.SONAR_PROJECT_KEY != '' && env.SONAR_TOKEN != '' }}
        env:
          SONAR_HOST_URL: ${{ env.SONAR_HOST_URL }}
          SONAR_TOKEN: ${{ env.SONAR_TOKEN }}
        run: |
          mvn -B sonar:sonar \
            -Dsonar.host.url=${SONAR_HOST_URL} \
            -Dsonar.projectKey=${SONAR_PROJECT_KEY} \
            -Dsonar.login=${SONAR_TOKEN}

      # SpotBugs + FindSecBugs
      - name: SpotBugs Scan
        if: ${{ env.SPOTBUGS_ENABLED == 'true' }}
        run: |
          mvn -B com.github.spotbugs:spotbugs-maven-plugin:4.8.6.4:spotbugs
          echo "SpotBugs report at ${SPOTBUGS_REPORT_PATH}"

      # OWASP Dependency-Check (SCA)
      - name: Dependency Check
        if: ${{ env.OWASP_DC_ENABLED == 'true' }}
        run: |
          mvn -B org.owasp:dependency-check-maven:10.0.3:check \
            -Dformat=HTML \
            -DfailOnError=true \
            -DsuppressionFile="${OWASP_DC_SUPPRESSION_FILE}"
          echo "Dependency-Check report at ${OWASP_DC_REPORT_PATH}"

      - name: Upload reports (artifacts)
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: security-reports
          path: |
            **/target/site/spotbugs/*.xml
            ${OWASP_DC_REPORT_PATH}
          if-no-files-found: ignore
```

### 3) Maven plugins (pom.xml)
Ensure the following plugins are present/compatible in your `pom.xml`:

```xml
<!-- SpotBugs + FindSecBugs -->
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
  <executions>
    <execution>
      <goals>
        <goal>spotbugs</goal>
      </goals>
    </execution>
  </executions>
  <inherited>false</inherited>
</plugin>

<!-- OWASP Dependency Check -->
<plugin>
  <groupId>org.owasp</groupId>
  <artifactId>dependency-check-maven</artifactId>
  <version>10.0.3</version>
  <configuration>
    <format>HTML</format>
    <failOnError>true</failOnError>
    <suppressionFiles>
      <suppressionFile>${env.OWASP_DC_SUPPRESSION_FILE}</suppressionFile>
    </suppressionFiles>
  </configuration>
  <executions>
    <execution>
      <goals>
        <goal>check</goal>
      </goals>
    </execution>
  </executions>
  <inherited>false</inherited>
  </plugin>

<!-- Sonar is invoked via mvn sonar:sonar; explicit plugin not required here -->
```

### 4) How it fails the CI
- Build fails if unit tests fail (`mvn verify`).
- Sonar: you can enforce Quality Gate via SonarCloud/GitHub Checks; failing gates mark PRs accordingly.
- SpotBugs: configure `spotbugs.failOnError=true` or parse report to fail; alternatively keep as advisory.
- Dependency-Check: by default `failOnError=true` breaks the build on high severity CVEs unless suppressed.

### 5) Local runs (optional)
```bash
mvn clean verify
mvn sonar:sonar -Dsonar.host.url=$SONAR_HOST_URL -Dsonar.projectKey=$SONAR_PROJECT_KEY -Dsonar.login=$SONAR_TOKEN
mvn com.github.spotbugs:spotbugs-maven-plugin:4.8.6.4:spotbugs
mvn org.owasp:dependency-check-maven:10.0.3:check -Dformat=HTML -DsuppressionFile="$OWASP_DC_SUPPRESSION_FILE"
```

That’s it — pushing to any branch will trigger the workflow and fail if configured scanners report issues per the above settings.


