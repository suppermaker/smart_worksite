package com.xd.smartworksite.intelligence.infra;

import com.xd.smartworksite.intelligence.dto.ModelCallRequest;
import com.xd.smartworksite.intelligence.dto.ModelCallResponse;

public interface ModelProviderClient {

    ModelCallResponse call(ModelCallRequest request);
}
