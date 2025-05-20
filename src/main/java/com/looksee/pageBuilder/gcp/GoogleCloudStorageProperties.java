package com.looksee.pageBuilder.gcp;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.context.annotation.Configuration;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Configuration
@ConfigurationProperties(prefix = "gcs.bucket")
@ConstructorBinding
@AllArgsConstructor
public class GoogleCloudStorageProperties {
    @Getter
    @Setter
    private String bucketName;

    @Getter
    @Setter
    private String publicUrl;
}
