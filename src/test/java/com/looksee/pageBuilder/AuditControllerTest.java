package com.looksee.pageBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.junit.jupiter.api.extension.ExtendWith;

import com.looksee.gcp.PubSubErrorPublisherImpl;
import com.looksee.gcp.PubSubJourneyVerifiedPublisherImpl;
import com.looksee.gcp.PubSubPageAuditPublisherImpl;
import com.looksee.gcp.PubSubPageCreatedPublisherImpl;
import com.looksee.models.Browser;
import com.looksee.models.PageState;
import com.looksee.models.enums.BrowserEnvironment;
import com.looksee.models.enums.BrowserType;
import com.looksee.pageBuilder.schemas.BodySchema;
import com.looksee.pageBuilder.schemas.MessageSchema;
import com.looksee.services.AuditRecordService;
import com.looksee.services.BrowserService;
import com.looksee.services.DomainMapService;
import com.looksee.services.ElementStateService;
import com.looksee.services.JourneyService;
import com.looksee.services.PageStateService;
import com.looksee.services.StepService;
import com.looksee.utils.BrowserUtils;

@ExtendWith(MockitoExtension.class)
class AuditControllerTest {
    private AuditController controller;

    @Mock private AuditRecordService auditRecordService;
    @Mock private BrowserService browserService;
    @Mock private JourneyService journeyService;
    @Mock private StepService stepService;
    @Mock private PageStateService pageStateService;
    @Mock private ElementStateService elementStateService;
    @Mock private DomainMapService domainMapService;
    @Mock private PubSubErrorPublisherImpl errorPublisher;
    @Mock private PubSubJourneyVerifiedPublisherImpl verifiedPublisher;
    @Mock private PubSubPageCreatedPublisherImpl pageCreatedPublisher;
    @Mock private PubSubPageAuditPublisherImpl pageAuditPublisher;

    @BeforeEach
    void setUp() {
        controller = new AuditController();
        ReflectionTestUtils.setField(controller, "audit_record_service", auditRecordService);
        ReflectionTestUtils.setField(controller, "browser_service", browserService);
        ReflectionTestUtils.setField(controller, "journey_service", journeyService);
        ReflectionTestUtils.setField(controller, "step_service", stepService);
        ReflectionTestUtils.setField(controller, "page_state_service", pageStateService);
        ReflectionTestUtils.setField(controller, "element_state_service", elementStateService);
        ReflectionTestUtils.setField(controller, "domain_map_service", domainMapService);
        ReflectionTestUtils.setField(controller, "pubSubErrorPublisherImpl", errorPublisher);
        ReflectionTestUtils.setField(controller, "pubSubJourneyVerifiedPublisherImpl", verifiedPublisher);
        ReflectionTestUtils.setField(controller, "pubSubPageCreatedPublisherImpl", pageCreatedPublisher);
        ReflectionTestUtils.setField(controller, "audit_record_topic", pageAuditPublisher);
    }

    @Test
    void receiveMessageReturnsBadRequestWhenBodyMissing() throws Exception {
        ResponseEntity<String> response = controller.receiveMessage(null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void receiveMessageReturnsBadRequestWhenDataIsNotBase64() throws Exception {
        BodySchema body = new BodySchema(new MessageSchema("not-base64"));

        ResponseEntity<String> response = controller.receiveMessage(body);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void receiveMessageReturnsBadRequestWhenJsonPayloadIsInvalid() throws Exception {
        String encoded = Base64.getEncoder().encodeToString("not-json".getBytes(StandardCharsets.UTF_8));
        BodySchema body = new BodySchema(new MessageSchema(encoded));

        ResponseEntity<String> response = controller.receiveMessage(body);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void receiveMessagePublishesExtractionErrorWhenHttp404() throws Exception {
        BodySchema body = new BodySchema(new MessageSchema(base64AuditMessage("PAGE")));

        try (MockedStatic<BrowserUtils> browserUtils = Mockito.mockStatic(BrowserUtils.class)) {
            browserUtils.when(() -> BrowserUtils.sanitizeUserUrl(anyString())).thenReturn("https://example.com");
            browserUtils.when(() -> BrowserUtils.checkIfSecure(any(URL.class))).thenReturn(true);
            browserUtils.when(() -> BrowserUtils.getHttpStatus(any(URL.class))).thenReturn(404);

            ResponseEntity<String> response = controller.receiveMessage(body);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(errorPublisher).publish(anyString());
        }
    }

    @Test
    void receiveMessageProcessesPageAuditAndClosesBrowser() throws Exception {
        BodySchema body = new BodySchema(new MessageSchema(base64AuditMessage("PAGE")));
        Browser browser = mock(Browser.class);
        PageState pageState = mock(PageState.class);

        when(browserService.getConnection(BrowserType.CHROME, BrowserEnvironment.DISCOVERY)).thenReturn(browser);
        when(browserService.buildPageState(any(URL.class), any(Browser.class), anyBoolean(), anyInt(), anyString())).thenReturn(pageState);
        when(auditRecordService.findPageWithKey(anyString(), anyString())).thenReturn(null);
        when(browserService.extractAllUniqueElementXpaths(anyString())).thenReturn(Collections.singletonList("//div"));
        when(browserService.getDomElementStates(any(PageState.class), anyList(), any(Browser.class), anyString()))
                .thenReturn(Collections.emptyList());
        when(pageStateService.save(any(PageState.class))).thenReturn(pageState);
        when(pageStateService.getElementStates(anyString())).thenReturn(Collections.emptyList());
        when(pageState.getKey()).thenReturn("page-key");
        when(pageState.getId()).thenReturn("page-id");
        when(pageState.getSrc()).thenReturn("<html></html>");

        try (MockedStatic<BrowserUtils> browserUtils = Mockito.mockStatic(BrowserUtils.class)) {
            browserUtils.when(() -> BrowserUtils.sanitizeUserUrl(anyString())).thenReturn("https://example.com");
            browserUtils.when(() -> BrowserUtils.checkIfSecure(any(URL.class))).thenReturn(true);
            browserUtils.when(() -> BrowserUtils.getHttpStatus(any(URL.class))).thenReturn(200);

            ResponseEntity<String> response = controller.receiveMessage(body);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(pageAuditPublisher).publish(anyString());
            verify(auditRecordService).addPageToAuditRecord(anyString(), anyString());
            verify(browser).close();
        }
    }

    @Test
    void receiveMessagePublishesErrorAndReturnsInternalServerErrorOnUnhandledException() throws Exception {
        BodySchema body = new BodySchema(new MessageSchema(base64AuditMessage("PAGE")));
        Browser browser = mock(Browser.class);

        when(browserService.getConnection(BrowserType.CHROME, BrowserEnvironment.DISCOVERY)).thenReturn(browser);
        doThrow(new RuntimeException("boom")).when(browserService)
                .buildPageState(any(URL.class), any(Browser.class), anyBoolean(), anyInt(), anyString());

        try (MockedStatic<BrowserUtils> browserUtils = Mockito.mockStatic(BrowserUtils.class)) {
            browserUtils.when(() -> BrowserUtils.sanitizeUserUrl(anyString())).thenReturn("https://example.com");
            browserUtils.when(() -> BrowserUtils.checkIfSecure(any(URL.class))).thenReturn(true);
            browserUtils.when(() -> BrowserUtils.getHttpStatus(any(URL.class))).thenReturn(200);

            ResponseEntity<String> response = controller.receiveMessage(body);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            verify(errorPublisher).publish(anyString());
            verify(browser).close();
        }
    }

    private String base64AuditMessage(String type) {
        String json = "{\"url\":\"https://example.com\",\"type\":\"" + type + "\",\"accountId\":\"account-1\",\"auditId\":\"audit-1\"}";
        return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }
}
