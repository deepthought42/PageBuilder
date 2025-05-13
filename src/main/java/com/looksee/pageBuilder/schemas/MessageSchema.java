package com.looksee.pageBuilder.schemas;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Schema definition for the message wrapper containing Base64 encoded data
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(
    name = "Message",
    description = "Wrapper containing Base64 encoded audit message data",
    requiredProperties = {"data"}
)
public class MessageSchema {
    
    @Schema(
        description = "Base64 encoded JSON string containing AuditStartMessage",
        example = "eyJ1cmwiOiJodHRwczovL2V4YW1wbGUuY29tIiwidHlwZSI6IlBBR0UiLCJhY2NvdW50SWQiOiIxMjMiLCJhdWRpdElkIjoiNDU2In0=",
        required = true
    )
    private String data;
} 