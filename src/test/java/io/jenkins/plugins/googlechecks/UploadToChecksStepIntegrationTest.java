// Copyright 2024 Google LLC

// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at

//     https://www.apache.org/licenses/LICENSE-2.0

// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package io.jenkins.plugins.googlechecks;


import hudson.model.Result;
import org.apache.tools.ant.taskdefs.condition.Http;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockserver.configuration.Configuration;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.matchers.Times;
import org.mockserver.model.HttpRequest;

import java.io.*;
import java.lang.reflect.Field;

import static org.mockito.Mockito.*;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockito.Mockito.mock;

public class UploadToChecksStepIntegrationTest {
    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();
    public File tmpApkFile;
    private static ClientAndServer mockServer;

    @BeforeClass
    public static void startMockServer() throws IOException {
        Configuration configuration = new Configuration();
        configuration.logLevel("ERROR");
        mockServer = ClientAndServer.startClientAndServer(configuration, 1080);
    }

    @AfterClass
    public static void stopMockServer() {
        mockServer.stop();
    }

    @Before
    public void setUp() throws IOException {
        tmpApkFile = tmpFolder.newFile();
    }

    @After
    public void tearDown() {
        mockServer.reset();
    }

    @Test
    public void testWithMissingServiceAccount() throws Exception {
        WorkflowJob job = jenkins.createProject(WorkflowJob.class);
        // define pipeline script to test
        String script = "uploadToChecks(" +
            "baseUrl: 'http://localhost:1080'," +
            "projectId: 'checks-upload'," +
            "accountId: '1175905440'," +
            "appId: '629751234'," +
            "binaryPath: \"" + tmpApkFile.getAbsolutePath() + "\"," +
            "severityThreshold: 'POTENTIAL'," +
        ")";
        job.setDefinition(new CpsFlowDefinition(script, true));

        // While testing, there are 2 pieces that are mocked:
        //   1. call to the Checks REST API (https://checks.googleapis.com). This is handled by mockServer which is initialized at the Class level (see above).
        //   1. Google Credentials
        GoogleCredentialsHelper mocked = mockGoogleCredentialsWithMissingCredentialsId();
        
        // run pipeline
        WorkflowRun run = jenkins.assertBuildStatus(Result.FAILURE, job.scheduleBuild2(0));

        // verify that the pipeline ran as expected.
        verify(mocked, times(1)).authenticate(null);
        String expectedString = "You must provide initialized credentials id (content of your serviceaccount.json)";
        jenkins.assertLogContains(expectedString, run);
    }

    @Test
    public void testWithInvalidBinaryPath() throws Exception {
        WorkflowJob job = jenkins.createProject(WorkflowJob.class);
        String script = "uploadToChecks(" +
                "baseUrl: 'http://localhost:1080'," +
                "credentialsId: 'checks-service-account-content'," +
                "projectId: 'checks-upload'," +
                "accountId: '1175905440'," +
                "appId: '629751234'," +
                "binaryPath: \"./wrong-path-to-binary.apk\"," +
                "severityThreshold: 'POTENTIAL'," +
                ")";

        job.setDefinition(new CpsFlowDefinition(script, true));
        mockValidGoogleCredentials();
        WorkflowRun run = jenkins.assertBuildStatus(Result.FAILURE, job.scheduleBuild2(0));

        String expectedString = "java.nio.file.NoSuchFileException";
        jenkins.assertLogContains(expectedString, run);
    }

    @Test
    public void testWithOnePriorityIssue() throws Exception {
        createMockUploadResponse();
        createMockOperationResponse();

        String report = "{\"checks\": [{ \"severity\": \"PRIORITY\", \"state\": \"FAILED\"}]}";
        HttpRequest reportRequest = createMockReportResponse("/v1alpha/accounts/1/apps/12/reports/123", report, 200);
        mockServer.when(reportRequest, Times.exactly(2));

        WorkflowJob job = jenkins.createProject(WorkflowJob.class);
        String script = "uploadToChecks(" +
                "baseUrl: 'http://localhost:1080'," +
                "credentialsId: 'checks-service-account-content'," +
                "projectId: 'checks-upload'," +
                "accountId: '1'," +
                "appId: '12'," +
                "binaryPath: \"" + tmpApkFile.getAbsolutePath() + "\"," +
                "severityThreshold: 'PRIORITY'," +
                "failOn: 'ALL'," +
            ")";

        job.setDefinition(new CpsFlowDefinition(script, true));
        mockValidGoogleCredentials();
        WorkflowRun run = jenkins.assertBuildStatus(Result.FAILURE, job.scheduleBuild2(0));

        String expectedString = "1 issue(s) detected:";
        jenkins.assertLogContains(expectedString, run);
    }


    @Test
    public void testWithPotentialIssue() throws Exception {
        createMockUploadResponse();
        createMockOperationResponse();

        String report = "{\"checks\": [{ \"severity\": \"PRIORITY\", \"state\": \"PASSED\"},{ \"severity\": \"POTENTIAL\", \"state\": \"FAILED\"}], \"resultsUri\": \"https://checks.area120.google.com/console/dashboard/1234?a=12\"}";
        createMockReportResponse("/v1alpha/accounts/1/apps/12/reports/123", report, 200);

        WorkflowJob job = jenkins.createProject(WorkflowJob.class);
        String script = "uploadToChecks(" +
                "baseUrl: 'http://localhost:1080'," +
                "credentialsId: 'checks-service-account-content'," +
                "projectId: 'checks-upload'," +
                "accountId: '1'," +
                "appId: '12'," +
                "binaryPath: \"" + tmpApkFile.getAbsolutePath() + "\"," +
                "severityThreshold: 'PRIORITY'," +
                "failOn: 'ALL'," +
                ")";

        job.setDefinition(new CpsFlowDefinition(script, true));
        mockValidGoogleCredentials();
        WorkflowRun run = jenkins.assertBuildStatus(Result.SUCCESS, job.scheduleBuild2(0));

        String expectedResultsUriString = "Report console URL:";
        jenkins.assertLogContains(expectedResultsUriString, run);

        String expectedString = "No issues detected.";
        jenkins.assertLogContains(expectedString, run);
    }

    @Test
    public void testWithNoWait() throws Exception {
        createMockUploadResponse();
        createMockOperationResponse();

        String report = "{\"checks\": [{ \"severity\": \"PRIORITY\", \"state\": \"FAILED\"},{ \"severity\": \"POTENTIAL\", \"state\": \"FAILED\"}]}";
        HttpRequest reportRequest = createMockReportResponse("/v1alpha/accounts/1/apps/12/reports/123", report, 200);
        mockServer.when(reportRequest, Times.exactly(0));

        WorkflowJob job = jenkins.createProject(WorkflowJob.class);
        String script = "uploadToChecks(" +
                "baseUrl: 'http://localhost:1080'," +
                "credentialsId: 'checks-service-account-content'," +
                "projectId: 'checks-upload'," +
                "accountId: '1'," +
                "appId: '12'," +
                "binaryPath: \"" + tmpApkFile.getAbsolutePath() + "\"," +
                "severityThreshold: 'PRIORITY'," +
                "failOn: 'ALL'," +
                "waitForReport: false," +
                ")";

        job.setDefinition(new CpsFlowDefinition(script, true));
        GoogleCredentialsHelper mocked = mockValidGoogleCredentials();
        WorkflowRun run = jenkins.assertBuildStatus(Result.SUCCESS, job.scheduleBuild2(0));

        verify(mocked, times(1)).authenticate(anyString());
        String expectedString = "Not waiting for the report to be generated. You'll receive an email once the report is ready.";
        jenkins.assertLogContains(expectedString, run);
    }

    private void storeMockGoogleCredentialsHelperManager(GoogleCredentialsHelper googleCredentialsHelper) throws Exception {
        GoogleCredentialsHelperFactory factory = GoogleCredentialsHelperFactory.getInstance();
        // override GoogleCredentialsHelper with our mocked instance
        Field field = GoogleCredentialsHelperFactory.class.getDeclaredField("googleCredentialsHelper");
        field.setAccessible(true);
        field.set(factory, googleCredentialsHelper);
    }

    private GoogleCredentialsHelper mockValidGoogleCredentials() throws Exception {
        GoogleCredentialsHelper mocked = mock(GoogleCredentialsHelper.class);
        when(mocked.authenticate(anyString())).thenReturn("fake token");
        storeMockGoogleCredentialsHelperManager(mocked);
        return mocked;
    }
    private GoogleCredentialsHelper mockGoogleCredentialsWithMissingCredentialsId() throws Exception {
        GoogleCredentialsHelper mocked = mock(GoogleCredentialsHelper.class);
        when(mocked.authenticate(null)).thenThrow(new IllegalArgumentException("You must provide initialized credentials id (content of your serviceaccount.json)"));
        storeMockGoogleCredentialsHelperManager(mocked);
        return mocked;
    }


    private HttpRequest createMockResponse(String path, String body, int statusCode) {
        HttpRequest req = request().withPath(path);
        mockServer.when(req).respond(
                response()
                        .withStatusCode(statusCode)
                        .withBody(body)
        );
        return req;
    }

    private HttpRequest createMockReportResponse(String path, String body, int statusCode) {
        HttpRequest req = request().withPath(path).withQueryStringParameter("fields", "name,checks(type,state,severity)");
        mockServer.when(req).respond(
                response()
                        .withStatusCode(statusCode)
                        .withBody(body)
        );
        return req;
    }

    private HttpRequest createMockUploadResponse() {
        return createMockResponse("/upload/v1alpha/accounts/1/apps/12/reports:analyzeUpload", "{\"name\": \"accounts/1/apps/12/operations/123\"}", 200);
    }
    private HttpRequest createMockOperationResponse() {
        return createMockResponse("/v1alpha/accounts/1/apps/12/operations/123", "{\n" +
                "        \"name\": \"accounts/1/apps/12/operations/123\",\n" +
                "        \"done\": true,\n" +
                "        \"response\": {\n" +
                "            \"@type\": \"type.googleapis.com/google.checks.report.v1alpha.Report\",\n" +
                "            \"name\": \"accounts/1/apps/12/reports/123\",\n" +
                "            \"resultsUri\": \"https://checks.area120.google.com/console/dashboard/123?a=12\"\n" +
                "        }\n" +
                "    }", 200);
    }
}

