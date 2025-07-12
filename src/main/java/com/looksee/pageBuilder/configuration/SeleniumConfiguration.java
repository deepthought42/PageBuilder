package com.looksee.pageBuilder.configuration;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import com.looksee.browsing.helpers.BrowserConnectionHelper;

/**
 * Configuration class for Selenium settings
 */
@Configuration
public class SeleniumConfiguration {
    
    @Value("${selenium.urls:}")
    private String seleniumUrls;
    
    /**
     * Initialize the BrowserConnectionHelper with configured selenium URLs
     */
    @PostConstruct
    public void initializeSeleniumUrls() {
        if (seleniumUrls != null && !seleniumUrls.trim().isEmpty()) {
            BrowserConnectionHelper.setConfiguredSeleniumUrls(seleniumUrls.split(","));
        }
    }
} 