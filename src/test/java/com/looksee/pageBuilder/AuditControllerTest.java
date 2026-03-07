package com.looksee.pageBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import com.looksee.gcp.PubSubErrorPublisherImpl;
import com.looksee.gcp.PubSubJourneyVerifiedPublisherImpl;
import com.looksee.gcp.PubSubPageAuditPublisherImpl;
import com.looksee.gcp.PubSubPageCreatedPublisherImpl;
import com.looksee.models.Browser;
import com.looksee.models.PageState;
import com.looksee.models.enums.AuditLevel;
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

    @Mock
    private AuditRecordService auditRecordService;
    @Mock
    private BrowserService browserService;
    @Mock
    private JourneyService journeyService;
    @Mock
    private StepService stepService;
    @Mock
    private PageStateService pageStateService;
    @Mock
    private ElementStateService elementStateService;
    @Mock
    private DomainMapService domainMapService;
    @Mock
    private PubSubErrorPublisherImpl pubSubErrorPublisherImpl;
    @Mock
    private PubSubJourneyVerifiedPublisherImpl pubSubJourneyVerifiedPublisherImpl;
    @Mock
    private PubSubPageCreatedPublisherImpl pubSubPageCreatedPublisherImpl;
    @Mock
    private PubSubPageAuditPublisherImpl auditRecordTopic;

    private AuditController controller;

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
        ReflectionTestUtils.setField(controller, "pubSubErrorPublisherImpl", pubSubErrorPublisherImpl);
        ReflectionTestUtils.setField(controller, "pubSubJourneyVerifiedPublisherImpl", pubSubJourneyVerifiedPublisherImpl);
        ReflectionTestUtils.setField(controller, "pubSubPageCreatedPublisherImpl", pubSubPageCreatedPublisherImpl);
        ReflectionTestUtils.setField(controller, "audit_record_topic", auditRecordTopic);
    }

    @Test
    void receiveMessageReturnsBadRequestWhenMessageMissing() throws Exception {
        BodySchema body = new BodySchema();

        ResponseEntity<String> response = controller.receiveMessage(body);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(pubSubErrorPublisherImpl, never()).publish(anyString());
    }

    @Test
    void receiveMessageReturnsBadRequestWhenPayloadIsNotBase64() throws Exception {
        BodySchema body = new BodySchema(new MessageSchema("not-base64!"));

        ResponseEntity<String> response = controller.receiveMessage(body);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(pubSubErrorPublisherImpl, never()).publish(anyString());
    }

    @Test
    void receiveMessagePublishesErrorWhenHttpStatusIs404() throws Exception {
        BodySchema body = createBody("https://example.com", AuditLevel.PAGE, "acc", "audit");

        try (MockedStatic<BrowserUtils> browserUtils = org.mockito.Mockito.mockStatic(BrowserUtils.class)) {
            browserUtils.when(() -> BrowserUtils.sanitizeUserUrl(any())).thenReturn("https://example.com");
            browserUtils.when(() -> BrowserUtils.checkIfSecure(any(URL.class))).thenReturn(true);
            browserUtils.when(() -> BrowserUtils.getHttpStatus(any(URL.class))).thenReturn(404);

            ResponseEntity<String> response = controller.receiveMessage(body);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(pubSubErrorPublisherImpl).publish(anyString());
            verify(browserService, never()).getConnection(any(BrowserType.class), any(BrowserEnvironment.class));
        }
    }

    @Test
    void receiveMessagePublishesPageAuditMessageForPageAudits() throws Exception {
        BodySchema body = createBody("https://example.com", AuditLevel.PAGE, "acc", "audit");

        Browser browser = mock(Browser.class);
        PageState pageState = mock(PageState.class);
        when(pageState.getKey()).thenReturn("page-key");
        when(pageState.getSrc()).thenReturn("<html></html>");
        when(pageState.getId()).thenReturn("page-id");

        when(browserService.getConnection(BrowserType.CHROME, BrowserEnvironment.DISCOVERY)).thenReturn(browser);
        when(browserService.buildPageState(any(URL.class), any(Browser.class), any(Boolean.class), any(Integer.class), anyString())).thenReturn(pageState);
        when(auditRecordService.findPageWithKey("audit", "page-key")).thenReturn(null);
        when(browserService.extractAllUniqueElementXpaths("<html></html>")).thenReturn(Collections.emptyList());
        when(browserService.getDomElementStates(any(PageState.class), any(), any(Browser.class), anyString())).thenReturn(Collections.emptyList());
        when(pageStateService.save(pageState)).thenReturn(pageState);
        when(pageStateService.getElementStates("page-id")).thenReturn(Collections.emptyList());

        try (MockedStatic<BrowserUtils> browserUtils = org.mockito.Mockito.mockStatic(BrowserUtils.class)) {
            browserUtils.when(() -> BrowserUtils.sanitizeUserUrl(any())).thenReturn("https://example.com");
            browserUtils.when(() -> BrowserUtils.checkIfSecure(any(URL.class))).thenReturn(true);
            browserUtils.when(() -> BrowserUtils.getHttpStatus(any(URL.class))).thenReturn(200);

            ResponseEntity<String> response = controller.receiveMessage(body);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(auditRecordService).addPageToAuditRecord("audit", "page-id");
            verify(auditRecordTopic).publish(anyString());
            verify(browser).close();
        }
    }

    @Test
    void receiveMessageReturnsInternalServerErrorAndClosesBrowserOnUnhandledException() throws Exception {
        BodySchema body = createBody("https://example.com", AuditLevel.PAGE, "acc", "audit");

        Browser browser = mock(Browser.class);
        when(browserService.getConnection(BrowserType.CHROME, BrowserEnvironment.DISCOVERY)).thenReturn(browser);
        when(browserService.buildPageState(any(URL.class), any(Browser.class), any(Boolean.class), any(Integer.class), anyString()))
            .thenThrow(new RuntimeException("kaboom"));

        try (MockedStatic<BrowserUtils> browserUtils = org.mockito.Mockito.mockStatic(BrowserUtils.class)) {
            browserUtils.when(() -> BrowserUtils.sanitizeUserUrl(any())).thenReturn("https://example.com");
            browserUtils.when(() -> BrowserUtils.checkIfSecure(any(URL.class))).thenReturn(true);
            browserUtils.when(() -> BrowserUtils.getHttpStatus(any(URL.class))).thenReturn(200);

            ResponseEntity<String> response = controller.receiveMessage(body);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            verify(pubSubErrorPublisherImpl).publish(anyString());
            verify(browser).close();
        }
    }

    private BodySchema createBody(String url, AuditLevel type, String accountId, String auditId) {
        String json = String.format(
            "{\"url\":\"%s\",\"type\":\"%s\",\"accountId\":\"%s\",\"auditId\":\"%s\"}",
            url,
            type.name(),
            accountId,
            auditId
        );
        String encoded = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        return new BodySchema(new MessageSchema(encoded));
    }
}
