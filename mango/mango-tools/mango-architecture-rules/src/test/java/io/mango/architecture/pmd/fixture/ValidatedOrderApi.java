package io.mango.architecture.pmd.fixture;

import io.mango.common.result.R;
import jakarta.validation.Valid;

public interface ValidatedOrderApi {

    R<String> create(@Valid CreateOrderCommand command);
}
