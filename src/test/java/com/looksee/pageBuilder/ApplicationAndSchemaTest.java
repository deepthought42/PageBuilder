package com.looksee.pageBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

import com.looksee.pageBuilder.schemas.AuditStartMessageSchema;
import com.looksee.pageBuilder.schemas.BodySchema;
import com.looksee.pageBuilder.schemas.MessageSchema;

class ApplicationAndSchemaTest {

    @Test
    void applicationMainSetsWebdriverHttpFactorySystemProperty() {
        System.clearProperty("webdriver.http.factory");

        try (MockedStatic<SpringApplication> springApplication = org.mockito.Mockito.mockStatic(SpringApplication.class)) {
            Application.main(new String[]{});
        }

        assertEquals("jdk-http-client", System.getProperty("webdriver.http.factory"));
    }

    @Test
    void schemasRetainAssignedValues() {
        MessageSchema message = new MessageSchema("payload");
        BodySchema body = new BodySchema(message);
        AuditStartMessageSchema audit = new AuditStartMessageSchema("https://example.com", "PAGE", "acc", "audit");

        assertNotNull(body.getMessage());
        assertEquals("payload", body.getMessage().getData());
        assertEquals("https://example.com", audit.getUrl());
        assertEquals("PAGE", audit.getType());
        assertEquals("acc", audit.getAccountId());
        assertEquals("audit", audit.getAuditId());
    }
}
