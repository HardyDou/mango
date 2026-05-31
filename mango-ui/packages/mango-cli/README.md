# mango-cli

Mango project CLI.

```bash
mango init mango-admin-demo --preset full --topology monolith
```

`full` preset generates a standalone Mango Admin consumer project. The generated frontend consumes published Mango npm packages through `@mango/admin/full` and `@mango/admin/style-full.css`; the generated backend consumes the Maven `mango-admin-starter`.

Credentials are not written into generated files. Maven credentials stay in the user's Maven `settings.xml`; npm credentials stay in user-level npm config or CI secrets.

## Scope

Sprint 6 implements only the full Mango baseline preset:

- full stack frontend and backend project generation
- monolith and microservice topology skeletons
- private Maven and npm registry configuration without credentials
- unified Mango framework versions rendered into generated files
- Mango PMO baseline documents in generated projects

Optional business module selection belongs to Sprint 7. This package intentionally does not generate business logic code.
