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
const jenkinsfile = path.join(root, 'jenkins', 'mango-maven-release.Jenkinsfile')
const jenkinsJob = path.join(root, 'jenkins', 'mango-maven-release-job.xml')
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
  assert.match(source, /scripts\/publish-maven-batch\.sh/)
  assert.match(source, /--all-non-app/)
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

test('release runner downloads only the approved bridge artifact instead of cloning Mango', async () => {
  const workflow = await readFile(releaseWorkflow, 'utf8')
  const runnerJob = workflow.split('  internal-jenkins-release:')[1]

  assert.match(workflow, /actions\/upload-artifact@v4/)
  assert.match(runnerJob, /actions\/download-artifact@v4/)
  assert.match(runnerJob, /\.release-bridge\/jenkins-release-bridge\.sh/)
  assert.doesNotMatch(runnerJob, /actions\/checkout@/)
})
