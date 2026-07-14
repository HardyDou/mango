pipeline {
  agent any

  options {
    skipDefaultCheckout(true)
    disableConcurrentBuilds()
    timestamps()
    timeout(time: 180, unit: 'MINUTES')
    buildDiscarder(logRotator(numToKeepStr: '30'))
  }

  parameters {
    string(name: 'GIT_SHA', defaultValue: '', description: 'GitHub main 上可达的完整 40 位 commit SHA。', trim: true)
    string(name: 'RELEASE_VERSION', defaultValue: '', description: '本次 Maven 版本；正式发布必须等于 release-versions.json 的锁定版本。', trim: true)
    string(name: 'REQUEST_ID', defaultValue: '', description: 'GitHub run id 与 attempt 组成的幂等追踪号。', trim: true)
    booleanParam(name: 'DRY_RUN', defaultValue: true, description: '默认仅验证参数、精确提交、发布计划和命令，不写 Nexus。')
    booleanParam(name: 'RUN_TESTS', defaultValue: false, description: '正式发布批次是否执行 Maven 测试。')
  }

  environment {
    MAVEN_VERSION = '3.9.9'
    MAVEN_HOME = "${JENKINS_HOME}/.tools/apache-maven-3.9.9"
    MAVEN_ARGS = "-s ${JENKINS_HOME}/.m2/settings.xml"
    LANG = 'C.UTF-8'
    LC_ALL = 'C.UTF-8'
    MANGO_RELEASE_FALLBACK_REPO_URL = 'https://github.com/HardyDou/mango.git'
  }

  stages {
    stage('Validate Request') {
      steps {
        script {
          if (!(params.GIT_SHA ==~ /[0-9a-fA-F]{40}/)) {
            error('GIT_SHA must be a full 40-character commit SHA.')
          }
          if (!(params.RELEASE_VERSION ==~ /[0-9]+(\.[0-9]+){2,}([-+][0-9A-Za-z][0-9A-Za-z.-]*)?/)) {
            error('RELEASE_VERSION must be a SemVer-like Maven version.')
          }
          if (!(params.REQUEST_ID ==~ /[A-Za-z0-9._-]{1,128}/)) {
            error('REQUEST_ID has an invalid format.')
          }
          if (!env.MANGO_RELEASE_REPO_URL?.trim()) {
            error('MANGO_RELEASE_REPO_URL must be configured on the Jenkins runtime.')
          }
          if (!env.MANGO_MAVEN_VERIFY_BASE_URL?.trim()) {
            error('MANGO_MAVEN_VERIFY_BASE_URL must be configured on the Jenkins runtime.')
          }
          currentBuild.displayName = "#${env.BUILD_NUMBER} ${params.RELEASE_VERSION} ${params.DRY_RUN ? 'dry-run' : 'release'}"
          currentBuild.description = "sha=${params.GIT_SHA.take(12)} request=${params.REQUEST_ID}"
        }
      }
    }

    stage('Checkout Exact Commit') {
      steps {
        withEnv([
          "GIT_SHA=${params.GIT_SHA}",
          "MANGO_RELEASE_REPO_URL=${env.MANGO_RELEASE_REPO_URL}"
        ]) {
          sh '''#!/usr/bin/env bash
            set -euo pipefail
            if [ -d .git ]; then
              git reset --hard
              git clean -ffdx
              git remote set-url origin "${MANGO_RELEASE_REPO_URL}"
            else
              git init .
              git remote add origin "${MANGO_RELEASE_REPO_URL}"
            fi
            fetched=false
            for attempt in $(seq 1 3); do
              if git fetch --force --no-tags origin "${GIT_SHA}"; then
                fetched=true
                break
              fi
              echo "Commit is not visible from the configured mirror yet; retry ${attempt}/3."
              sleep 5
            done
            if [ "${fetched}" != "true" ]; then
              echo "Mirror is delayed; fetching the exact public release commit without waiting for the next mirror cycle."
              git remote remove release-fallback 2>/dev/null || true
              git remote add release-fallback "${MANGO_RELEASE_FALLBACK_REPO_URL}"
              git fetch --force --no-tags release-fallback "${GIT_SHA}"
              fetched=true
            fi
            if [ "${fetched}" != "true" ]; then
              echo "Unable to fetch requested commit: ${GIT_SHA}" >&2
              exit 1
            fi
            git checkout --detach "${GIT_SHA}"
            test "$(git rev-parse HEAD)" = "${GIT_SHA}"
            main_ready=false
            if git fetch --force --no-tags origin main &&
              git merge-base --is-ancestor "${GIT_SHA}" FETCH_HEAD; then
              main_ready=true
            fi
            if [ "${main_ready}" != "true" ]; then
              git remote remove release-fallback 2>/dev/null || true
              git remote add release-fallback "${MANGO_RELEASE_FALLBACK_REPO_URL}"
              git fetch --force --no-tags release-fallback main
              git merge-base --is-ancestor "${GIT_SHA}" FETCH_HEAD
            fi
            git status --short --branch
          '''
        }
      }
    }

    stage('Prepare Maven') {
      steps {
        sh '''#!/usr/bin/env bash
          set -euo pipefail
          mkdir -p "${JENKINS_HOME}/.tools"
          if [ ! -x "${MAVEN_HOME}/bin/mvn" ]; then
            archive="${JENKINS_HOME}/.tools/apache-maven-${MAVEN_VERSION}-bin.tar.gz"
            mirror_url="https://repo.huaweicloud.com/apache/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz"
            official_url="https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz"
            rm -f "${archive}"
            downloaded=false
            for url in "${mirror_url}" "${official_url}"; do
              if curl --connect-timeout 20 --max-time 600 --retry 5 --retry-all-errors -fSL \
                "${url}" -o "${archive}"; then
                downloaded=true
                break
              fi
              rm -f "${archive}"
            done
            if [ "${downloaded}" != "true" ]; then
              echo "Unable to download Maven ${MAVEN_VERSION}." >&2
              exit 1
            fi
            expected_sha512="$(curl --connect-timeout 20 --max-time 120 --retry 5 --retry-all-errors -fsSL "${official_url}.sha512" | tr -d '[:space:]')"
            actual_sha512="$(sha512sum "${archive}" | cut -d ' ' -f 1)"
            test "${actual_sha512}" = "${expected_sha512}"
            tar -xzf "${archive}" -C "${JENKINS_HOME}/.tools"
            rm -f "${archive}"
          fi
          "${MAVEN_HOME}/bin/mvn" -version
        '''
      }
    }

    stage('Release Contract') {
      steps {
        withEnv([
          "RELEASE_VERSION=${params.RELEASE_VERSION}",
          "DRY_RUN=${params.DRY_RUN}"
        ]) {
          sh '''#!/usr/bin/env bash
            set -euo pipefail
            locked_version="$(awk -F'"' '/"mangoBackend"/ {print $4; exit}' mango-ui/packages/mango-cli/release-versions.json)"
            if ! printf '%s' "${locked_version}" | grep -Eq '^[0-9]+([.][0-9]+){2,}([-+][0-9A-Za-z][0-9A-Za-z.-]*)?$'; then
              echo "Unable to read a valid Mango backend lock from release-versions.json: ${locked_version}" >&2
              exit 1
            fi
            if [ "${DRY_RUN}" = "false" ] && [ "${RELEASE_VERSION}" != "${locked_version}" ]; then
              echo "Formal release version must equal the committed Mango backend lock. requested=${RELEASE_VERSION}, locked=${locked_version}" >&2
              exit 1
            fi
            if [ "${DRY_RUN}" = "false" ] && ! grep -Fq "maven-${RELEASE_VERSION}" CHANGELOG.md; then
              echo "CHANGELOG.md does not contain the formal Maven release version: ${RELEASE_VERSION}" >&2
              exit 1
            fi
            if [ "${DRY_RUN}" = "false" ] && [ ! -s "${HOME}/.m2/settings.xml" ]; then
              echo "Formal release requires Jenkins Maven credentials at ${HOME}/.m2/settings.xml." >&2
              exit 1
            fi
            echo "Release contract passed. version=${RELEASE_VERSION}, locked=${locked_version}, dry_run=${DRY_RUN}"
          '''
        }
      }
    }

    stage('Publish Non-App Maven Batch') {
      steps {
        withEnv([
          "PATH=${env.MAVEN_HOME}/bin:${env.PATH}",
          "RELEASE_VERSION=${params.RELEASE_VERSION}",
          "DRY_RUN=${params.DRY_RUN}",
          "RUN_TESTS=${params.RUN_TESTS}",
          "MANGO_MAVEN_VERIFY_BASE_URL=${env.MANGO_MAVEN_VERIFY_BASE_URL}"
        ]) {
          sh '''#!/usr/bin/env bash
            set -euo pipefail
            args=(
              --all-non-app
              --release-version "${RELEASE_VERSION}"
              --verify-base-url "${MANGO_MAVEN_VERIFY_BASE_URL}"
            )
            if [ "${RUN_TESTS}" = "true" ]; then
              args+=(--run-tests)
            fi
            if [ "${DRY_RUN}" = "true" ]; then
              args+=(--dry-run)
            fi
            scripts/publish-maven-batch.sh "${args[@]}"
          '''
        }
      }
    }
  }

  post {
    always {
      sh 'git status --short --branch || true'
      echo "Mango Maven pipeline finished. request=${params.REQUEST_ID}, dry_run=${params.DRY_RUN}"
    }
  }
}
