package io.jenkins.plugins.AIErrorAnalyzer;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import hudson.model.Run;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
public class AIErrorAnalysisActionTest {

    @Mock
    private Run<?, ?> mockRun;
    private AIErrorAnalysisAction action;
    @Mock
    private AIIntegration mockAIIntegration;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        // Prepare a fake log output
        String fakeLogOutput = "Error: NullPointerException";
        InputStream fakeLogInputStream = new ByteArrayInputStream(fakeLogOutput.getBytes(StandardCharsets.UTF_8));
        when(mockRun.getLogInputStream()).thenReturn(fakeLogInputStream);

        // Mock the behavior of the AIIntegration's getResponse method
        when(mockAIIntegration.getResponse(anyString())).thenReturn("AI response for NullPointerException");

        // Instantiate the AIErrorAnalysisAction with a mocked AIIntegration
        action = new AIErrorAnalysisAction("testService", "testApiKey", Collections.singletonList("Error: (.+)"));
        injectMockAIIntegration(action, mockAIIntegration);
    }

    @Test
    public void constructorTest() {
        // Test if the constructor correctly initializes the object
        // Arrange
        String expectedServiceName = "testService";
        String expectedApiKey = "testApiKey";
        List<String> expectedTemplates = Arrays.asList("Error 1", "Error 2");

        // Act
        AIErrorAnalysisAction action = new AIErrorAnalysisAction(expectedServiceName, expectedApiKey, expectedTemplates);

        // Assert
        assertNotNull("aiIntegration should not be null", action.aiIntegration);
        assertEquals("errorTemplates should be initialized with the provided list", expectedTemplates, action.errorTemplates);
        assertTrue("analyzerResponses should be initialized and empty", action.analyzerResponses.isEmpty());
    }

    @Test
    public void readResponseTest() {
        action.readResponse();
        assertTrue("isResponseRead should be true after calling readResponse", action.isResponseRead);
    }

    @Test
    public void getAnalyzerResponseTest_WhenNoResponse() {
        // Scenario when there is no response
        String expectedMessage = "Pipiline is still working. Please wait until the result is ready.";

        String actualMessage = action.getAnalyzerResponse();

        assertEquals("When there are no responses, should return the message indicating the pipeline is still working", expectedMessage, actualMessage);
    }

    @Test
    public void getAnalyzerResponseTest_WhenThereAreResponses() {
        // Scenario when there are responses
        String response = "Error identified: NullPointerException at line 42";
        action.analyzerResponses.add(response);

        String actualMessage = action.getAnalyzerResponse();

        assertEquals("When there are responses, should return the first response", response, actualMessage);
    }

    @Test
    public void getAnalyzerResponseTest_WhenResponsesAreCleared() {
        // Scenario when responses were present but then cleared
        action.analyzerResponses.add("Some error response");
        action.analyzerResponses.clear();

        String expectedMessage = "Pipiline is still working. Please wait until the result is ready.";

        String actualMessage = action.getAnalyzerResponse();

        assertEquals("When responses are cleared, should return the message indicating the pipeline is still working", expectedMessage, actualMessage);
    }


    @Test
    public void getIconFileNameTest() {
        String expectedIconFileName = "document.png";
        String actualIconFileName = action.getIconFileName();
        assertEquals("The icon file name should match the expected value.", expectedIconFileName, actualIconFileName);
    }

    @Test
    public void getDisplayNameTest() {
        String expectedDisplayName = "AI assistant";
        String actualDisplayName = action.getDisplayName();
        assertEquals("The display name should match the expected value.", expectedDisplayName, actualDisplayName);
    }

    @Test
    public void getUrlNameTest() {
        String expectedUrlName = "ai_assistant";
        String actualUrlName = action.getUrlName();
        assertEquals("The URL name should match the expected value.", expectedUrlName, actualUrlName);
    }

    @Test
    public void onAttachedTest() {
        action.onAttached(mockRun);
        assertEquals("The run object should be the same as the mockRun after onAttached", mockRun, action.getRun());
    }
    @Test
    public void onLoadTest() {
        action.onLoad(mockRun);
        assertEquals("The run object should be the same as mockRun after onLoad", mockRun, action.getRun());
    }

    private void injectMockAIIntegration(AIErrorAnalysisAction action, AIIntegration mockAIIntegration) throws NoSuchFieldException, IllegalAccessException {
        Field aiIntegrationField = AIErrorAnalysisAction.class.getDeclaredField("aiIntegration");
        aiIntegrationField.setAccessible(true);
        aiIntegrationField.set(action, mockAIIntegration);
    }

    @Test
    public void requestAIAnalyzeTest() throws Exception {
        // Arrange
        // Mock the Run object and its methods
        when(mockRun.getLogReader()).thenReturn(new BufferedReader(new StringReader("Error: NullPointerException")));
        doNothing().when(mockRun).save(); // If the save method is called inside the AIErrorAnalysisAction

        // Act
        action.onLoad(mockRun); // This will set the 'run' object in the action class
        action.requestAIAnalyze();

        // Assert
        assertFalse("analyzerResponses should not be empty after requestAIAnalyze", action.analyzerResponses.isEmpty());
        assertEquals("The response from AI should be added to analyzerResponses", "AI response for NullPointerException", action.analyzerResponses.get(0));
    }
    @Test
    public void readLogTest() throws IOException {
        // Arrange
        String fakeLog = "Build started\nError: NullPointerException at line 42\nBuild completed";
        ByteArrayInputStream logStream = new ByteArrayInputStream(fakeLog.getBytes(StandardCharsets.UTF_8));
        when(mockRun.getLogReader()).thenReturn(new InputStreamReader(logStream, StandardCharsets.UTF_8));

        action.onLoad(mockRun); // Ensures that the run object is set

        // Act
        String errorDetails = action.readLog();

        // Assert
        assertEquals("Error details should match the expected log message", "Error: NullPointerException at line 42", errorDetails);
    }

    @Test(expected = IOException.class)
    public void requestAIAnalyze_ShouldHandleExceptionFromAIIntegration() throws Exception {
        // Arrange
        when(mockRun.getLogReader()).thenReturn(new BufferedReader(new StringReader("Error: NullPointerException")));
        when(mockAIIntegration.getResponse(anyString())).thenThrow(new IOException("AI Service Failed"));

        action.onLoad(mockRun);

        // Act
        action.requestAIAnalyze(); // This should throw CustomAIIntegrationException

        // Assert is handled by the expected exception
    }

    @Test
    public void readLog_ShouldReturnEmptyStringForEmptyLog() throws IOException {
        // Arrange
        when(mockRun.getLogReader()).thenReturn(new BufferedReader(new StringReader("")));

        action.onLoad(mockRun);

        // Act
        String errorDetails = action.readLog();

        // Assert
        assertTrue("Error details should be empty for an empty log", errorDetails.isEmpty());
    }

    @Test
    public void readLog_ShouldCaptureLastError() throws IOException {
        // Arrange
        String logContent = "Error: NullPointerException\nError: ArrayIndexOutOfBoundsException\nError: IllegalArgumentException";
        when(mockRun.getLogReader()).thenReturn(new BufferedReader(new StringReader(logContent)));

        action.onLoad(mockRun);
        action.errorTemplates = Arrays.asList("Error: (.+)");

        // Act
        String errorDetails = action.readLog();

        // Assert
        assertEquals("Should capture the last error from the log", "Error: IllegalArgumentException", errorDetails);
    }
}