import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

const repoRoot = resolve(new URL('..', import.meta.url).pathname);

const checks = [
  {
    label: 'main dev-workspace',
    path: 'scripts/dev-workspace.sh',
    required: [
      'SPRING_BOOT_PLUGIN="org.springframework.boot:spring-boot-maven-plugin:3.5.14:run"',
      'mvn -pl :mango-monolith-app -am "${SPRING_BOOT_PLUGIN}"',
      'diagnose_backend_failure()',
      'run_frontend()',
      'start_all()',
      'VITE_ADMIN_PROXY_PATH="http://127.0.0.1:${MANGO_BACKEND_PORT}"',
    ],
    forbidden: [
      'mvn -pl :mango-monolith-app -am spring-boot:run',
    ],
  },
  {
    label: 'legacy dev-env',
    path: 'scripts/dev-env.sh',
    required: [
      'SPRING_BOOT_PLUGIN="org.springframework.boot:spring-boot-maven-plugin:3.5.14:run"',
      'exec mvn -pl :mango-monolith-app -am "${SPRING_BOOT_PLUGIN}"',
    ],
    forbidden: [
      'exec mvn -pl :mango-monolith-app -am spring-boot:run',
    ],
  },
  {
    label: 'generated full dev-workspace template',
    path: 'mango-ui/packages/mango-cli/templates/full/scripts/dev-workspace.sh',
    required: [
      'SPRING_BOOT_PLUGIN="org.springframework.boot:spring-boot-maven-plugin:{{springBootVersion}}:run"',
      'mvn -f backend/pom.xml -DskipTests install',
      '"${SPRING_BOOT_PLUGIN}"',
      'diagnose_backend_failure()',
      'run_frontend()',
      'start_all()',
      'VITE_ADMIN_PROXY_PATH="http://127.0.0.1:${MANGO_BACKEND_PORT}"',
      'npm run dev -- --host "${MANGO_FRONTEND_HOST}" --port "${MANGO_FRONTEND_PORT}"',
    ],
    forbidden: [
      'spring-boot:run',
    ],
  },
];

for (const check of checks) {
  const source = readFileSync(resolve(repoRoot, check.path), 'utf8');
  for (const expected of check.required) {
    if (!source.includes(expected)) {
      throw new Error(`${check.label} is missing required startup contract: ${expected}`);
    }
  }
  for (const forbidden of check.forbidden) {
    if (source.includes(forbidden)) {
      throw new Error(`${check.label} still contains forbidden startup contract: ${forbidden}`);
    }
  }
}

console.log('Development startup scripts keep explicit Maven plugin goals and unified frontend/backend entries.');
