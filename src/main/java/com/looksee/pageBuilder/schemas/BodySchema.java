package com.looksee.pageBuilder.schemas;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Schema definition for the top-level request body
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(
    name = "Body",
    description = "Top-level request body containing the message",
    requiredProperties = {"message"}
)
public class BodySchema {
    
    @Schema(
        description = "Message containing the audit request data",
        required = true
    )
    private MessageSchema message;
} 