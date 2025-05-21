package com.looksee.pageBuilder.models;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.HashSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.looksee.pageBuilder.gcp.GoogleCloudStorage;
import com.looksee.pageBuilder.models.enums.BrowserType;

@ExtendWith(MockitoExtension.class)
class PageStateTest {

    @Mock
    private GoogleCloudStorage googleCloudStorage;

    @InjectMocks
    private PageState pageState;

    private static final String TEST_URL = "https://example.com/test";
    private static final String TEST_HTML = "<html><body>Test Content</body></html>";
    private static final String EXPECTED_KEY = "example.com/pages/pagestate-1" + 
        "8f7e6d5c4b3a2c1d0e9f8a7b6c5d4e3f2a1b0c9d8e7f6a5b4c3d2e1f0a9b8c7d6";
    private static final String EXPECTED_GCS_URL = "https://storage.googleapis.com/test-bucket/" + EXPECTED_KEY;

    @BeforeEach
    void setUp() {
        // Set required fields for PageState
        pageState.setUrl(TEST_URL);
        pageState.setAuditRecordId(1L);
        ReflectionTestUtils.setField(pageState, "googleCloudStorage", googleCloudStorage);
    }

    @Test
    void setSrc_ShouldUploadHtmlContent() throws IOException {
        // Arrange
        when(googleCloudStorage.uploadHtmlContent(eq(TEST_HTML), eq(EXPECTED_KEY)))
            .thenReturn(EXPECTED_GCS_URL);

        // Act
        pageState.setSrc(TEST_HTML);

        // Assert
        verify(googleCloudStorage).uploadHtmlContent(TEST_HTML, EXPECTED_KEY);
        assertEquals(EXPECTED_GCS_URL, pageState.getSrc());
    }

    @Test
    void setSrc_ShouldHandleIOException() throws IOException {
        // Arrange
        when(googleCloudStorage.uploadHtmlContent(anyString(), anyString()))
            .thenThrow(new IOException("Upload failed"));

        // Act & Assert
        assertThrows(IOException.class, () -> pageState.setSrc(TEST_HTML));
    }

    @Test
    void constructor_ShouldUploadHtmlContent() throws IOException {
        // Arrange
        when(googleCloudStorage.uploadHtmlContent(eq(TEST_HTML), eq(EXPECTED_KEY)))
            .thenReturn(EXPECTED_GCS_URL);

        // Act
        PageState newPageState = new PageState(
            "screenshot.jpg",
            TEST_HTML,
            0L,
            0L,
            1024,
            768,
            BrowserType.CHROME,
            "full-screenshot.jpg",
            1920,
            1080,
            TEST_URL,
            "Test Page",
            true,
            200,
            TEST_URL,
            1L,
            new HashSet<>(),
            new HashSet<>(),
            new HashSet<>(),
            new HashSet<>()
        );

        // Assert
        verify(googleCloudStorage).uploadHtmlContent(TEST_HTML, EXPECTED_KEY);
        assertEquals(EXPECTED_GCS_URL, newPageState.getSrc());
    }

    @Test
    void clone_ShouldHandleIOException() throws IOException {
        // Arrange
        when(googleCloudStorage.uploadHtmlContent(anyString(), anyString()))
            .thenThrow(new IOException("Upload failed"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            try {
                pageState.clone();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
} 