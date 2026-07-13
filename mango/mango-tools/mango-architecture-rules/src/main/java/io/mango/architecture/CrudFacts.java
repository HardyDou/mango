package io.mango.architecture;

record CrudFacts(
        boolean crudContract,
        boolean typedCrudContract,
        boolean crudBase,
        boolean mybatisCrudBase,
        String directSuperclass,
        boolean standardCrudSurface,
        boolean spoofedCrudContract,
        boolean spoofedCrudBase) {}
