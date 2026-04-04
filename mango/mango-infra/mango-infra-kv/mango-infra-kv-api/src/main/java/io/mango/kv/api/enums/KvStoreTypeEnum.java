package io.mango.kv.api.enums;

public enum KvStoreTypeEnum {
    AUTO,   // Auto cascade: redis → db → memory
    REDIS,  // Redis only
    DB,     // DB only
    MEMORY  // Memory only (single instance)
}
