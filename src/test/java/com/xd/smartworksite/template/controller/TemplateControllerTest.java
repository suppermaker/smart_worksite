package com.xd.smartworksite.template.controller;

import com.xd.smartworksite.template.application.TemplateApplicationService;
import com.xd.smartworksite.template.application.TemplatePreviewApplicationService;
import com.xd.smartworksite.template.application.TemplateVariableApplicationService;
import com.xd.smartworksite.template.dto.TemplatePreviewFile;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TemplateControllerTest {

    @Test
    void previewReturnsInlineFileResponseWithOriginalName() {
        TemplateApplicationService templateService = mock(TemplateApplicationService.class);
        TemplatePreviewApplicationService previewService = mock(TemplatePreviewApplicationService.class);
        TemplateVariableApplicationService variableService = mock(TemplateVariableApplicationService.class);
        when(previewService.openPreview(10L)).thenReturn(new TemplatePreviewFile(
                "周报模板.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                4L,
                new ByteArrayInputStream(new byte[]{1, 2, 3, 4})
        ));
        TemplateController controller = new TemplateController(templateService, previewService, variableService);

        ResponseEntity<InputStreamResource> response = controller.previewTemplate(10L);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getHeaders().getContentLength()).isEqualTo(4L);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .contains("inline")
                .contains("filename*=");
        assertThat(response.getHeaders().getFirst(HttpHeaders.CACHE_CONTROL)).isEqualTo("no-store");
    }
}
