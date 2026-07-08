package com.xd.smartworksite.ai.infra;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xd.smartworksite.ai.repository.AiRepository;
import com.xd.smartworksite.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class AiPythonServiceClientTest {

    @Test
    void convertDataMapsProviderData() {
        AiPythonServiceProperties properties = new AiPythonServiceProperties();
        AiRepository repository = mock(AiRepository.class);
        AiPythonServiceClient client = new AiPythonServiceClient(properties, new ObjectMapper(), repository);
        AiProviderResponse response = new AiProviderResponse();
        response.getData().put("answer", "ok");

        TestResponse converted = client.convertData(response, TestResponse.class);

        assertEquals("ok", converted.getAnswer());
        verify(repository, never()).saveExternalCallLog(any());
    }

    @Test
    void postLogsPersistenceFailureWithoutMaskingOriginalError() {
        AiPythonServiceProperties properties = new AiPythonServiceProperties();
        properties.setBaseUrl("http://127.0.0.1:1");
        properties.setConnectTimeoutMs(50);
        properties.setReadTimeoutMs(50);
        properties.setRetryCount(0);
        AiRepository repository = mock(AiRepository.class);
        doThrow(new RuntimeException("log db down")).when(repository).saveExternalCallLog(any());
        AiPythonServiceClient client = new AiPythonServiceClient(properties, new ObjectMapper(), repository);

        assertThrows(BusinessException.class, () -> client.post("/v1/model/invoke", "MODEL_INVOKE", 1L, new Object()));
        verify(repository).saveExternalCallLog(any());
    }

    @Test
    void postFailsForUnavailableService() {
        AiPythonServiceProperties properties = new AiPythonServiceProperties();
        properties.setBaseUrl("http://127.0.0.1:1");
        properties.setConnectTimeoutMs(50);
        properties.setReadTimeoutMs(50);
        properties.setRetryCount(0);
        AiRepository repository = mock(AiRepository.class);
        AiPythonServiceClient client = new AiPythonServiceClient(properties, new ObjectMapper(), repository);

        assertThrows(BusinessException.class, () -> client.post("/v1/model/invoke", "MODEL_INVOKE", 1L, new Object()));
    }

    public static class TestResponse {
        private String answer;
        public String getAnswer() { return answer; }
        public void setAnswer(String answer) { this.answer = answer; }
    }
}
