import assert from 'node:assert/strict'
import { chmod, mkdtemp, readFile, writeFile } from 'node:fs/promises'
import { spawnSync } from 'node:child_process'
import os from 'node:os'
import path from 'node:path'
import test from 'node:test'
import { fileURLToPath } from 'node:url'

const testDir = path.dirname(fileURLToPath(import.meta.url))
const root = path.resolve(testDir, '..', '..')
const bridge = path.join(root, 'scripts', 'ci', 'jenkins-release-bridge.sh')
const poller = path.join(root, 'scripts', 'ci', 'github-release-jenkins-poller.sh')
const jenkinsfile = path.join(root, 'jenkins', 'mango-maven-release.Jenkinsfile')
const jenkinsJob = path.join(root, 'jenkins', 'mango-maven-release-job.xml')
const watcherJenkinsfile = path.join(root, 'jenkins', 'mango-github-release-watcher.Jenkinsfile')
const watcherJob = path.join(root, 'jenkins', 'mango-github-release-watcher-job.xml')
const releaseWorkflow = path.join(root, '.github', 'workflows', 'maven-release.yml')

const baseEnv = {
  ...process.env,
  JENKINS_URL: 'http://jenkins.internal:8081',
  JENKINS_USER: 'github-mango-release',
  JENKINS_API_TOKEN: 'test-token-must-not-be-printed',
  JENKINS_JOB: 'mango-maven-release',
  GIT_SHA: 'a'.repeat(40),
  RELEASE_VERSION: '1.2.3-bridge.1',
  REQUEST_ID: '12345-1',
  DRY_RUN: 'true',
  RUN_TESTS: 'false',
  JENKINS_TIMEOUT_SECONDS: '60',
  JENKINS_POLL_SECONDS: '1'
}

test('bridge rejects a short Git SHA before making a network request', () => {
  const result = spawnSync('bash', [bridge], {
    cwd: root,
    env: { ...baseEnv, GIT_SHA: 'abc123' },
    encoding: 'utf8'
  })
  assert.equal(result.status, 2)
  assert.match(result.stderr, /full 40-character Git commit SHA/)
})

test('bridge maps a successful Jenkins build to success without printing the API token', async () => {
  const temp = await mkdtemp(path.join(os.tmpdir(), 'mango-jenkins-bridge-test-'))
  const outputFile = path.join(temp, 'github-output')
  const fakeCurl = path.join(temp, 'curl')
  await writeFile(fakeCurl, `#!/usr/bin/env bash
set -euo pipefail
args="$*"
if [[ "$args" == *"jenkins.ui.internal"* && "$args" == *"api/json"* ]]; then
  printf 'build API must use the configured internal Jenkins URL\n' >&2
  exit 8
fi
if [[ "$args" == *"buildWithParameters"* ]]; then
  headers=""
  while [[ $# -gt 0 ]]; do
    if [[ "$1" == "--dump-header" ]]; then headers="$2"; shift 2; continue; fi
    shift
  done
  printf 'HTTP/1.1 201 Created\\r\\nLocation: http://jenkins.internal:8081/queue/item/7/\\r\\n\\r\\n' > "$headers"
  printf '201'
elif [[ "$args" == *"/queue/item/7/api/json"* ]]; then
  printf '{"cancelled":false,"executable":{"url":"https://jenkins.ui.internal/job/mango-maven-release/42/"}}'
elif [[ "$args" == *"/job/mango-maven-release/42/api/json"* ]]; then
  printf '{"building":false,"result":"SUCCESS","number":42,"url":"http://jenkins.internal:8081/job/mango-maven-release/42/"}'
else
  printf 'unexpected curl invocation: %s\\n' "$args" >&2
  exit 9
fi
`)
  await chmod(fakeCurl, 0o755)

  const result = spawnSync('bash', [bridge], {
    cwd: root,
    env: {
      ...baseEnv,
      PATH: `${temp}:${process.env.PATH}`,
      RUNNER_TEMP: temp,
      GITHUB_OUTPUT: outputFile
    },
    encoding: 'utf8'
  })
  assert.equal(result.status, 0, `${result.stdout}\n${result.stderr}`)
  assert.doesNotMatch(`${result.stdout}\n${result.stderr}`, /test-token-must-not-be-printed/)
  const outputs = await readFile(outputFile, 'utf8')
  assert.match(outputs, /jenkins_build_number=42/)
  assert.match(outputs, /jenkins_result=SUCCESS/)
  assert.match(outputs, /jenkins_build_url=https:\/\/jenkins\.ui\.internal\/job\/mango-maven-release\/42\//)
})

test('tracked Jenkins pipeline publishes only the governed non-app batch at the exact SHA', async () => {
  const source = await readFile(jenkinsfile, 'utf8')
  assert.match(source, /git checkout --detach "\$\{GIT_SHA\}"/)
  assert.match(source, /git merge-base --is-ancestor "\$\{GIT_SHA\}" FETCH_HEAD/)
  assert.match(source, /main_ready=false/)
  assert.match(source, /git remote add release-fallback "\$\{MANGO_RELEASE_FALLBACK_REPO_URL\}"/)
  assert.match(source, /scripts\/publish-maven-batch\.sh/)
  assert.match(source, /--all-non-app/)
  assert.match(
    source,
    /MAVEN_ARGS = "-s \$\{JENKINS_HOME\}\/\.m2\/settings\.xml"/,
    'every Maven invocation must read the persisted Jenkins release credentials',
  )
  assert.equal(
    (source.match(/sh '''#!\/usr\/bin\/env bash/g) ?? []).length,
    4,
    'every multi-line Jenkins shell stage must opt into Bash',
  )
  assert.doesNotMatch(source, /mvn[^\n]*\sdeploy/)
  assert.match(
    source,
    /grep -Eq '\^\[0-9\]\+\(\[\.\]\[0-9\]\+\)\{2,\}/,
    'the release version regex must avoid Groovy-invalid backslash escapes',
  )

  const job = await readFile(jenkinsJob, 'utf8')
  assert.match(job, /<name>GIT_SHA<\/name>/)
  assert.match(job, /<name>RELEASE_VERSION<\/name>/)
  assert.match(job, /<name>REQUEST_ID<\/name>/)
  assert.match(job, /<defaultValue>true<\/defaultValue>/)
  const inlinePipeline = job.match(/<script><!\[CDATA\[\n([\s\S]*?)\n\]\]><\/script>/)?.[1]
  assert.equal(inlinePipeline, source.trimEnd(), 'tracked Job XML must embed the exact reviewed Jenkinsfile')
  assert.doesNotMatch(job, /CpsScmFlowDefinition/)
})

test('published GitHub Release creates a machine request without a private runner', async () => {
  const workflow = await readFile(releaseWorkflow, 'utf8')
  assert.match(workflow, /release:\s*\n\s+types: \[published\]/)
  assert.match(workflow, /mango-internal-release-request-v1\.json/)
  assert.match(workflow, /gh release upload/)
  assert.doesNotMatch(workflow, /self-hosted/)
  assert.doesNotMatch(workflow, /JENKINS_/)
  assert.doesNotMatch(workflow, /--clobber/, 'a published release request must be immutable')
})

test('internal watcher is scheduled, idempotent and delegates only an exact formal release', async () => {
  const source = await readFile(watcherJenkinsfile, 'utf8')
  const job = await readFile(watcherJob, 'utf8')

  assert.match(source, /cron\('H\/2 \* \* \* \*'\)/)
  assert.match(source, /github-release-jenkins-poller\.sh discover/)
  assert.match(source, /git cat-file -e "FETCH_HEAD:scripts\/ci\/github-release-jenkins-poller\.sh"/)
  assert.match(source, /github-release-jenkins-poller\.sh claim/)
  assert.match(source, /github-release-jenkins-poller\.sh success/)
  assert.match(source, /github-release-jenkins-poller\.sh failed/)
  assert.match(source, /build job: 'mango-maven-release'/)
  assert.match(source, /booleanParam\(name: 'DRY_RUN', value: false\)/)
  assert.match(source, /refs\/tags\/\$\{RELEASE_TAG\}/)
  assert.match(source, /git merge-base --is-ancestor/)
  assert.match(source, /loading the watcher from public GitHub main/)
  assert.match(source, /timeout\(time: 180, unit: 'MINUTES'\)/)
  assert.match(job, /<spec>H\/2 \* \* \* \*<\/spec>/)
  const inlinePipeline = job.match(/<script><!\[CDATA\[\n([\s\S]*?)\n\]\]><\/script>/)?.[1]
  assert.equal(inlinePipeline, source.trimEnd(), 'watcher Job XML must embed the exact reviewed Jenkinsfile')
})

test('poller bootstraps old releases and never discovers them as new work', async () => {
  const temp = await mkdtemp(path.join(os.tmpdir(), 'mango-release-poller-test-'))
  const fakeCurl = path.join(temp, 'curl')
  const state = path.join(temp, 'state')
  const output = path.join(temp, 'candidate.properties')
  await writeFile(fakeCurl, `#!/usr/bin/env bash
set -euo pipefail
printf '<feed><entry><link href="https://github.com/HardyDou/mango/releases/tag/v-old-release"/></entry></feed>'
`)
  await chmod(fakeCurl, 0o755)
  const env = {
    ...process.env,
    PATH: `${temp}:${process.env.PATH}`,
    MANGO_RELEASE_STATE_DIR: state,
    MANGO_RELEASE_POLL_OUTPUT: output,
  }
  const bootstrap = spawnSync('bash', [poller, 'bootstrap'], { cwd: root, env, encoding: 'utf8' })
  assert.equal(bootstrap.status, 0, `${bootstrap.stdout}\n${bootstrap.stderr}`)
  const discover = spawnSync('bash', [poller, 'discover'], { cwd: root, env, encoding: 'utf8' })
  assert.equal(discover.status, 0, `${discover.stdout}\n${discover.stderr}`)
  assert.equal(await readFile(output, 'utf8'), 'ACTION=none\n')
})

test('poller claims a request once and a failed immutable batch is never auto-retried', async () => {
  const temp = await mkdtemp(path.join(os.tmpdir(), 'mango-release-poller-claim-test-'))
  const fakeCurl = path.join(temp, 'curl')
  const state = path.join(temp, 'state')
  const output = path.join(temp, 'candidate.properties')
  const tag = 'v2026.08.01-maven-2.0.0-release'
  const sha = 'b'.repeat(40)
  await writeFile(fakeCurl, `#!/usr/bin/env bash
set -euo pipefail
output=''
url="\${!#}"
while [[ $# -gt 0 ]]; do
  if [[ "$1" == '-o' ]]; then output="$2"; shift 2; continue; fi
  shift
done
if [[ "$url" == *.atom ]]; then
  printf '<feed><entry><link href="https://github.com/HardyDou/mango/releases/tag/${tag}"/></entry></feed>'
else
  printf '%s' '{"schema":"mango.internal-release-request/v1","repository":"HardyDou/mango","releaseId":200,"tag":"${tag}","sourceSha":"${sha}","components":{"maven":{"version":"2.0.0","runTests":false}}}' > "$output"
  printf '200'
fi
`)
  await chmod(fakeCurl, 0o755)
  const env = {
    ...process.env,
    PATH: `${temp}:${process.env.PATH}`,
    MANGO_RELEASE_STATE_DIR: state,
    MANGO_RELEASE_POLL_OUTPUT: output,
  }

  const discover = spawnSync('bash', [poller, 'discover'], { cwd: root, env, encoding: 'utf8' })
  assert.equal(discover.status, 0, `${discover.stdout}\n${discover.stderr}`)
  assert.match(await readFile(output, 'utf8'), /ACTION=release/)

  const releaseEnv = {
    ...env,
    RELEASE_ID: '200',
    RELEASE_TAG: tag,
    RELEASE_VERSION: '2.0.0',
    GIT_SHA: sha,
  }
  const claim = spawnSync('bash', [poller, 'claim'], { cwd: root, env: releaseEnv, encoding: 'utf8' })
  assert.equal(claim.status, 0, `${claim.stdout}\n${claim.stderr}`)
  const failed = spawnSync('bash', [poller, 'failed'], { cwd: root, env: releaseEnv, encoding: 'utf8' })
  assert.equal(failed.status, 0, `${failed.stdout}\n${failed.stderr}`)

  const rediscover = spawnSync('bash', [poller, 'discover'], { cwd: root, env, encoding: 'utf8' })
  assert.equal(rediscover.status, 0, `${rediscover.stdout}\n${rediscover.stderr}`)
  assert.equal(await readFile(output, 'utf8'), 'ACTION=none\n')
})

test('poller blocks a Maven version older than the latest successful release', async () => {
  const temp = await mkdtemp(path.join(os.tmpdir(), 'mango-release-poller-rollback-test-'))
  const fakeCurl = path.join(temp, 'curl')
  const state = path.join(temp, 'state')
  const output = path.join(temp, 'candidate.properties')
  const tag = 'v2026.08.02-maven-1.9.9-release'
  const sha = 'c'.repeat(40)
  await writeFile(fakeCurl, `#!/usr/bin/env bash
set -euo pipefail
output=''
url="\${!#}"
while [[ $# -gt 0 ]]; do
  if [[ "$1" == '-o' ]]; then output="$2"; shift 2; continue; fi
  shift
done
if [[ "$url" == *.atom ]]; then
  printf '<feed><entry><link href="https://github.com/HardyDou/mango/releases/tag/${tag}"/></entry></feed>'
else
  printf '%s' '{"schema":"mango.internal-release-request/v1","repository":"HardyDou/mango","releaseId":201,"tag":"${tag}","sourceSha":"${sha}","components":{"maven":{"version":"1.9.9","runTests":false}}}' > "$output"
  printf '200'
fi
`)
  await chmod(fakeCurl, 0o755)
  await writeFile(
    path.join(temp, 'ledger.tsv'),
    `2026-08-01T00:00:00Z\tsuccess\t200\tv2026.08.01-maven-2.0.0-release\t2.0.0\t${'b'.repeat(40)}\n`,
  )
  const env = {
    ...process.env,
    PATH: `${temp}:${process.env.PATH}`,
    MANGO_RELEASE_STATE_DIR: temp,
    MANGO_RELEASE_POLL_OUTPUT: output,
  }

  const discover = spawnSync('bash', [poller, 'discover'], { cwd: root, env, encoding: 'utf8' })
  assert.equal(discover.status, 1, `${discover.stdout}\n${discover.stderr}`)
  assert.match(discover.stderr, /Blocked Maven release rollback/)
  assert.match(await readFile(path.join(temp, 'ledger.tsv'), 'utf8'), /blocked-rollback\t201/)

  const rediscover = spawnSync('bash', [poller, 'discover'], { cwd: root, env, encoding: 'utf8' })
  assert.equal(rediscover.status, 0, `${rediscover.stdout}\n${rediscover.stderr}`)
  assert.equal(await readFile(output, 'utf8'), 'ACTION=none\n')
})
