package com.looksee.pageBuilder.schemas;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Schema definition for the decoded audit start payload.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(
    name = "AuditStartMessage",
    description = "Decoded audit payload containing request details",
    requiredProperties = {"url", "type", "accountId", "auditId"}
)
public class AuditStartMessageSchema {

    @Schema(
        description = "URL to be processed and audited",
        example = "https://example.com",
        required = true,
        format = "uri"
    )
    private String url;

    @Schema(
        description = "Type of audit to perform",
        example = "PAGE",
        required = true,
        allowableValues = {"PAGE", "DOMAIN"}
    )
    private String type;

    @Schema(
        description = "Account identifier",
        example = "123",
        required = true
    )
    private String accountId;

    @Schema(
        description = "Audit identifier",
        example = "456",
        required = true
    )
    private String auditId;
}
