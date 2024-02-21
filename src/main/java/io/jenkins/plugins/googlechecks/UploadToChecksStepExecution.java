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
import hudson.model.TaskListener;
import hudson.security.ACL;
import io.jenkins.plugins.googlechecks.models.GoogleChecksOperation;
import io.jenkins.plugins.googlechecks.models.GoogleChecksReport;
import io.jenkins.plugins.googlechecks.models.GoogleChecksUpload;
import jenkins.model.Jenkins;
import jenkins.util.Timer;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.jenkinsci.plugins.workflow.steps.AbstractStepExecutionImpl;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UploadToChecksStepExecution extends AbstractStepExecutionImpl {
    private static final Logger LOGGER = Logger.getLogger(UploadToChecksStepExecution.class.getName());

    private static final long serialVersionUID = 1L;
    private static final int TIMEOUT_AFTER = 30 * 60; // in seconds
    private static final int CHECK_OPERATION_INTERVAL = 10; // seconds
    @Inject
    transient UploadToChecksStep  step;
    private transient volatile ScheduledFuture<?> task;
    private long end;
    private String operationId;
    private transient GoogleChecks checks;

    protected UploadToChecksStepExecution(UploadToChecksStep step, @Nonnull StepContext context) {
        super(context);
        this.step = step;
        this.checks = new GoogleChecks(this.step.getBaseUrl(), this.step.getProjectId(), this.step.getAccountId(), this.step.getAppId());
    }

    @Override public boolean start() throws Exception {
        checks = new GoogleChecks(this.step.getBaseUrl(), this.step.getProjectId(), this.step.getAccountId(), this.step.getAppId());
        checks.authenticate(this.step.getCredentialsId());

        if (this.step.getGenerateReport()) {
            getListener().getLogger().printf("Uploading %s\n", this.step.getBinaryPath());

            // upload
            GoogleChecksUpload response = checks.uploadBinary(this.step.getBinaryPath());
            operationId = GoogleChecks.getOperationIdFromName(response.name);

            long now = System.currentTimeMillis();
            end = now + TIMEOUT_AFTER * 1000;
            if (this.step.getWaitForReport()) {
                waitReport(now);
                return false;
            } else {
                getListener().getLogger().println("Not waiting for the report to be generated. You'll receive an email once the report is ready.");
                getContext().onSuccess(null);
                return true;
            }
        } else {
            getListener().getLogger().println("Generating a report is disabled. Testing authentication by get the list of apps");
            getListener().getLogger().println(checks.getAppsList());
            getContext().onSuccess(null);
            return true;
        }
    }

    private TaskListener getListener() {
        TaskListener listener;
        try {
            listener = getContext().get(TaskListener.class);
        } catch (Exception x) {
            LOGGER.log(Level.WARNING, null, x);
            listener = TaskListener.NULL;
        }

        return listener;
    }

    private void waitReport(long now) {
        if (end > now) {
            TaskListener listener = getListener();
            task = Timer.get().scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    long now = System.currentTimeMillis();
                    listener.getLogger().printf("Checking on operationId=%s\n", operationId);
                    try {
                        GoogleChecksOperation response = checks.checkOperation(operationId);
                        if (end < now || response.done != null && response.done) {
                            task.cancel(false);
                            Boolean isValid = isValidReport(listener, response, step.getSeverityThreshold());
                            if (step.getFailOn() == FailOn.ALL) {
                                if (isValid) {
                                    getContext().onSuccess(null);
                                } else {
                                    getContext().onFailure(new RuntimeException("Report has errors"));
                                }
                            } else {
                                getContext().onSuccess(null);
                            }
                        }
                    } catch (IOException e) {
                        task.cancel(false);
                        getContext().onFailure(e);
                        throw new RuntimeException(e);
                    }
                }
            }, 0, CHECK_OPERATION_INTERVAL, TimeUnit.SECONDS);
        } else {
            getContext().onSuccess(null);
        }
    }

    public Boolean isValidReport(TaskListener listener, GoogleChecksOperation operation) throws IOException {
        return isValidReport(listener, operation, SeverityThreshold.PRIORITY);
    }

    public Boolean isValidReport(TaskListener listener, GoogleChecksOperation operation, SeverityThreshold severityThreshold) throws IOException {
        listener.getLogger().printf("Report console URL: %s\n", operation.response.resultsUri);
        String reportId = GoogleChecks.getReportIdFromName(operation.response.name);
        GoogleChecksReport report = checks.getReport(reportId);
        List<String> failingChecks = GoogleChecks.validateReport(report.checks, severityThreshold);
        if (!failingChecks.isEmpty()) {
            listener.getLogger().printf("%s issue(s) detected:\n", failingChecks.size());
            for (String check : failingChecks) {
                listener.getLogger().println(check);
            }
        } else {
            listener.getLogger().println("No issues detected.");
        }
        return failingChecks.isEmpty();
    }

    @Override public void stop(@Nonnull Throwable cause) throws Exception {
        if (task != null) {
            task.cancel(false);
        }
        super.stop(cause);
    }

    @Override public void onResume() {
        waitReport(System.currentTimeMillis());
    }
}