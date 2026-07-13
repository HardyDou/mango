package io.mango.architecture;

import com.tngtech.archunit.core.domain.JavaType;

record TypedCrudTypes(
        JavaType entity,
        JavaType create,
        JavaType update,
        JavaType query,
        JavaType view,
        JavaType identifier,
        JavaType mapper,
        JavaType baseEntity) {}
