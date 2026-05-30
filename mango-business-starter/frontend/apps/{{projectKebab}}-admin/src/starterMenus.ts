import type { ShellMenu } from '@mango/admin/menu';

export const starterMenus: ShellMenu[] = [
  {
    appCode: 'internal-admin',
    moduleCode: '{{moduleKebab}}',
    menuId: '{{moduleKebab}}',
    menuName: '{{moduleName}}',
    menuCode: '{{moduleKebab}}',
    parentId: 0,
    menuType: 1,
    path: '/{{moduleKebab}}',
    icon: 'Menu',
    sort: 100,
    status: 1,
    visible: 1,
    children: [
      {
        appCode: 'internal-admin',
        moduleCode: '{{moduleKebab}}',
        menuId: '{{moduleKebab}}:{{aggregateKebab}}',
        menuName: '{{aggregatePascal}}管理',
        menuCode: '{{moduleKebab}}:{{aggregateKebab}}:list',
        parentId: '{{moduleKebab}}',
        menuType: 2,
        path: '/{{moduleKebab}}/{{aggregateKebab}}s',
        component: '{{moduleKebab}}/{{aggregateKebab}}/index',
        sort: 10,
        status: 1,
        visible: 1,
      },
    ],
  },
];
