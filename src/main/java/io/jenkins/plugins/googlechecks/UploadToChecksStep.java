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

import edu.umd.cs.findbugs.annotations.NonNull;

import com.google.common.collect.ImmutableSet;
import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.util.Set;


public class UploadToChecksStep extends Step {
    @DataBoundSetter
    private String projectId;
    @DataBoundSetter
    private String accountId;
    @DataBoundSetter
    private String appId;
    @DataBoundSetter
    private String binaryPath;
    @DataBoundSetter
    private String credentialsId;

    @DataBoundSetter
    private Boolean generateReport = true;
    @DataBoundSetter
    private Boolean waitForReport = true;
    @DataBoundSetter
    private SeverityThreshold severityThreshold = SeverityThreshold.PRIORITY;

    @DataBoundSetter
    private FailOn failOn;

    @DataBoundSetter
    private String baseUrl;

    public String getCredentialsId() {
        return credentialsId;
    }

    public void setCredentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getBinaryPath() {
        return binaryPath;
    }

    public void setBinaryPath(String binaryPath) {
        this.binaryPath = binaryPath;
    }

    @DataBoundConstructor
    public UploadToChecksStep() {
    }

    @Override
    public StepExecution start(StepContext stepContext) {
        return new UploadToChecksStepExecution(this, stepContext);
    }

    public Boolean getGenerateReport() {
        return generateReport;
    }

    public void setGenerateReport(Boolean generateReport) {
        this.generateReport = generateReport;
    }

    public Boolean getWaitForReport() {
        return waitForReport;
    }

    public void setWaitForReport(Boolean waitForReport) {
        this.waitForReport = waitForReport;
    }

    public SeverityThreshold getSeverityThreshold() {
        return severityThreshold;
    }

    public void setSeverityThreshold(SeverityThreshold severityThreshold) {
        this.severityThreshold = severityThreshold;
    }

    public FailOn getFailOn() {
        return failOn;
    }

    public void setFailOn(FailOn failOn) {
        this.failOn = failOn;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Extension
    public static class DescriptorImpl extends StepDescriptor {

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return ImmutableSet.of(Run.class, TaskListener.class);
        }

        @Override
        public String getFunctionName() {
            return "uploadToChecks";
        }

        @NonNull
        @Override
        public String getDisplayName() {
            return "Upload to Checks";
        }
    }
}