package com.looksee.pageBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.boot.SpringApplication;

import com.looksee.pageBuilder.schemas.AuditStartMessageSchema;
import com.looksee.pageBuilder.schemas.BodySchema;
import com.looksee.pageBuilder.schemas.MessageSchema;

class ApplicationAndSchemaTest {

    @Test
    void mainSetsWebdriverHttpFactoryProperty() {
        System.clearProperty("webdriver.http.factory");

        try (MockedStatic<SpringApplication> springApplication = Mockito.mockStatic(SpringApplication.class)) {
            Application.main(new String[]{});
        }

        assertEquals("jdk-http-client", System.getProperty("webdriver.http.factory"));
    }

    @Test
    void bodySchemaStoresMessage() {
        MessageSchema message = new MessageSchema("encoded-data");
        BodySchema body = new BodySchema(message);

        assertEquals("encoded-data", body.getMessage().getData());
    }

    @Test
    void auditStartMessageSchemaStoresValues() {
        AuditStartMessageSchema schema = new AuditStartMessageSchema("https://example.com", "PAGE", "acc", "audit");

        assertNotNull(schema);
        assertEquals("https://example.com", schema.getUrl());
        assertEquals("PAGE", schema.getType());
        assertEquals("acc", schema.getAccountId());
        assertEquals("audit", schema.getAuditId());
    }
}
