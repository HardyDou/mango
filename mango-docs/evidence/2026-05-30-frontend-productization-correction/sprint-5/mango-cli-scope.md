# mango-cli Scope

## 1. Decision

`mango-cli` is required, but it should start after Sprint 5 proves Maven and npm materials can be consumed outside the Mango monorepo.

Reason:

- CLI generates long-lived project structure.
- If package artifacts are not independently verified first, CLI will freeze unstable dependencies into new projects.
- The correct sequence is package consumption first, project generation second.

## 2. Responsibilities

`mango-cli` owns:

- Project initialization for standard frontend/backend projects.
- Monolith and microservice empty project generation.
- Private Maven and npm registry configuration.
- Modular dependency management.
- Required system module injection.
- Optional module selection.
- Dynamic rendering of `pom.xml`, `package.json`, `.npmrc`, `application.yml`.
- Startup entry generation.
- Directory structure generation.
- AI development baseline document generation.
- Unified Mango framework version management.

## 3. Credential Rule

`mango-cli` may configure registry locations and scope mappings.

`mango-cli` must not write secrets into generated repositories.

Credentials must come from:

- User-level Maven settings.
- User-level npm config.
- Environment variables.
- CI Secrets.

## 4. Sprint Placement

Sprint 6:

- Implement `mango-cli init` full Mango preset.
- Generated project depends on released Maven and npm materials.
- Full preset must start as original Mango Admin.

Sprint 7:

- Implement optional module selection and business extension commands.
- System module group remains required.
- Optional modules are added through dependencies and configuration, not copied source.
