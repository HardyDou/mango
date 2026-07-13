package evil;

import org.springframework.stereotype.Service;

@Service
public final class SpoofedOrderService extends MangoCrudServiceImpl
        implements IOrderService, MangoCrudService, MangoTypedCrudService {
}
