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

import com.cloudbees.plugins.credentials.CredentialsMatcher;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import hudson.security.ACL;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

/*
 Class to be able to mock Google Credentials mechanism when testing
*/
public class GoogleCredentialsHelper {
    public StringCredentials lookupCredentials(String credentialId) {
        List<StringCredentials> credentials = CredentialsProvider.lookupCredentials(StringCredentials.class, Jenkins.get(), ACL.SYSTEM, Collections.emptyList());
        CredentialsMatcher matcher = CredentialsMatchers.withId(credentialId);
        return CredentialsMatchers.firstOrNull(credentials, matcher);
    }

    public String getSecret(String credentialsId) {
        try {
            return lookupCredentials(credentialsId).getSecret().getPlainText();
        } catch (NullPointerException exception) {
            throw new IllegalArgumentException("You must provide initialized credentials id (content of your serviceaccount.json)");
        }
    }

    public String authenticate(String credentialsId) throws IOException {
        String serviceAccountContent = getSecret(credentialsId);
        InputStream serviceAccountStream = new ByteArrayInputStream(serviceAccountContent.getBytes());
        GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccountStream)
                .createScoped("https://www.googleapis.com/auth/checks");
        credentials.refreshIfExpired();
        AccessToken accessToken = credentials.getAccessToken();
        return accessToken.getTokenValue();
    }
}
