package com.cephx.def.service.cb;

import java.util.List;

public interface ChargeBeeService {
    void receiveEvent(final String eventString);

    List<String> cbBillingPlanNames() throws Exception;
}
