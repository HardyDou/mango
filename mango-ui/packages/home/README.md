# @mango/home

`@mango/home` provides the frontend API contracts for Mango user home workbench pages.

It owns home page list, default home resolution, create, rename, duplicate, sort, delete, and layout persistence calls. It does not render the shell host and does not register widgets. Admin shell uses this package together with `@mango/grid-layout` and `@mango/grid-widgets`.

## API

```ts
import { homePageApi } from '@mango/home';

await homePageApi.resolve();
await homePageApi.create({ name: '项目工作台', layoutJson, setDefault: true });
await homePageApi.saveLayout(homeId, { layoutJson });
```

Backend dependency: `mango-home-starter`.
