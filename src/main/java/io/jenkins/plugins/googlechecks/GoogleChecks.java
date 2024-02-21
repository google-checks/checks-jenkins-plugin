// Copyright 2024 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package io.jenkins.plugins.googlechecks;

import com.google.api.client.http.*;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.gson.GsonFactory;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import io.jenkins.plugins.googlechecks.models.GoogleChecksCheck;
import io.jenkins.plugins.googlechecks.models.GoogleChecksOperation;
import io.jenkins.plugins.googlechecks.models.GoogleChecksReport;
import io.jenkins.plugins.googlechecks.models.GoogleChecksUpload;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class GoogleChecks {
    private String baseUrl = "https://checks.googleapis.com";
    private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
    private static final JsonFactory JSON_FACTORY = new GsonFactory();

    private String projectId;
    private String accountId;
    private String appId;
    private String accessTokenValue;

    public GoogleChecks(String baseUrl, String projectId, String accountId, String appId) {
        if (baseUrl != null) {
            this.baseUrl = baseUrl;
        }
        this.projectId = projectId;
        this.accountId = accountId;
        this.appId = appId;
    }

    public void authenticate(String credentialsId) throws IOException {
        GoogleCredentialsHelper googleCredentialsHelper = GoogleCredentialsHelperFactory.getInstance().getOrCreateGoogleCredentialsHelper();
        this.accessTokenValue = googleCredentialsHelper.authenticate(credentialsId);
    }

    private HttpRequest createRequest(String requestMethod, String url) throws IOException {
        HttpRequestFactory requestFactory =
                HTTP_TRANSPORT.createRequestFactory(
                        new HttpRequestInitializer() {
                            @Override
                            public void initialize(HttpRequest request) {
                                request.setParser(new JsonObjectParser(JSON_FACTORY));
                            }
                        });
        HttpRequest request = requestFactory.buildRequest(requestMethod, new GenericUrl(url), null);
        request.getHeaders().set("X-Goog-User-Project", this.projectId);
        request.getHeaders().set("Authorization", String.format("Bearer %s", accessTokenValue));
        return request;
    }

    private String executeAndParseAsString(HttpRequest request) throws IOException {
        HttpResponse response = request.execute();
        try {
            return response.parseAsString();
        } finally {
            response.disconnect();
        }
    }

    private <T> T executeAndParse(HttpRequest request, Class<T> destinationClass) throws IOException {
        HttpResponse response = request.execute();
        if (!response.isSuccessStatusCode()) {
            handleNotOKStatusCode(response.getStatusCode());
        }
        try {
            return response.parseAs(destinationClass);
        } finally {
            response.disconnect();
        }
    }

    private void handleNotOKStatusCode(int statusCode) {
        switch (statusCode) {
            case 400:
                throw new IllegalArgumentException("Bad Request");
            case 403:
                throw new IllegalArgumentException("Forbidden");
            default:
                throw new RuntimeException("API Error returned with status code=" + statusCode);
        }
    }

    public String getAppsList() throws IOException {
        String url = buildUrl("/v1alpha/accounts/%s/apps/", this.accountId);
        HttpRequest request = createRequest("GET", url);
        // TODO use GoogleChecksAppList model
        // something like Type type = new TypeToken<ArrayList<AppListResponse>>() {}.getType();
        return executeAndParseAsString(request);
    }

    public GoogleChecksUpload uploadBinary(String binaryPath) throws IOException {
        String url = buildUrl("/upload/v1alpha/accounts/%s/apps/%s/reports:analyzeUpload", this.accountId, this.appId);
        HttpRequest request = createRequest("POST", url);

        // TODO check if binaryPath exists
        byte[] binaryData = Files.readAllBytes(Path.of(binaryPath));
        System.out.printf("binaryPath=%s, len=%s\n", Path.of(binaryPath), binaryData.length);
        request.setContent(new ByteArrayContent("application/octet-stream", binaryData));
        request.getHeaders().set("X-Goog-Upload-Protocol", "raw");
        return executeAndParse(request, GoogleChecksUpload.class);
    }

    public GoogleChecksOperation checkOperation(String operationId) throws IOException {
        String url = buildUrl("/v1alpha/accounts/%s/apps/%s/operations/%s", this.accountId, this.appId, operationId);
        HttpRequest request = createRequest("GET", url);
        return executeAndParse(request, GoogleChecksOperation.class);
    }

    public GoogleChecksReport getReport(String reportId) throws IOException {
        String url = buildUrl("/v1alpha/accounts/%s/apps/%s/reports/%s?fields=name,checks(type,state,severity)", this.accountId, this.appId, reportId);
        HttpRequest request = createRequest("GET", url);
        return executeAndParse(request, GoogleChecksReport.class);
    }

    public static List<String> validateReport(List<GoogleChecksCheck> checks, SeverityThreshold severityThreshold) {
        List<String> failingChecks  = new ArrayList<String>();
        for (GoogleChecksCheck check : checks) {
            if (severityThreshold.includes(check.severity) && check.state.equals("FAILED")) {
                failingChecks.add(String.format("Type: %s. Details. %s", check.type, check));
            }
        }
        return failingChecks;
    }

    public String buildUrl(String format, Object... args) {
        String url = String.format("%s%s", this.baseUrl, format);
        return String.format(url, args);
    }

    public static String getOperationIdFromName(String name) {
        String[] parts = name.split("/");
        return parts[parts.length - 1];
    }

    public static String getReportIdFromName(String name) {
        String[] parts = name.split("/");
        return parts[parts.length - 1];
    }
}

