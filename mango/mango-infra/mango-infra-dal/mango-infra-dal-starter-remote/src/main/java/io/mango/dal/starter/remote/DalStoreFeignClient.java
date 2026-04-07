package io.mango.dal.starter.remote;

import io.mango.dal.api.IKvStore;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * DAL Store Feign Client for microservice cross-process calls.
 * Use this when DAL store needs to be accessed remotely in microservice deployment.
 */
@FeignClient(name = "dal-service", path = "/dal")
public interface DalStoreFeignClient extends IKvStore {
}
