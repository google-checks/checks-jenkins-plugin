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
import io.jenkins.plugins.googlechecks.models.GoogleChecksCheck;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.ArrayList;
import java.util.List;

public class SeverityThresholdTest {
    @Rule
    public JenkinsRule jenkins = new JenkinsRule();
    @Test
    public void testSeverityCheckDependingOnThreshold() throws Exception {
        Assert.assertFalse(SeverityThreshold.PRIORITY.includes("OPPORTUNITY"));
        Assert.assertFalse(SeverityThreshold.PRIORITY.includes("POTENTIAL"));
        Assert.assertTrue(SeverityThreshold.PRIORITY.includes("PRIORITY"));

        Assert.assertFalse(SeverityThreshold.POTENTIAL.includes("OPPORTUNITY"));
        Assert.assertTrue(SeverityThreshold.POTENTIAL.includes("POTENTIAL"));
        Assert.assertTrue(SeverityThreshold.POTENTIAL.includes("PRIORITY"));

        Assert.assertTrue(SeverityThreshold.OPPORTUNITY.includes("OPPORTUNITY"));
        Assert.assertTrue(SeverityThreshold.OPPORTUNITY.includes("POTENTIAL"));
        Assert.assertTrue(SeverityThreshold.OPPORTUNITY.includes("PRIORITY"));
    }

    @Test
    public void testValidateReport() {
        ArrayList<GoogleChecksCheck> checks = new ArrayList<>();
        checks.add(new GoogleChecksCheck("Priority Check (failed)", "FAILED", "PRIORITY"));
        checks.add(new GoogleChecksCheck("Priority Check (passed)", "PASSED", "PRIORITY"));
        checks.add(new GoogleChecksCheck("Priority Check (unchecked)", "UNCHECKED", "PRIORITY"));
        checks.add(new GoogleChecksCheck("Potential Check (failed)", "FAILED", "POTENTIAL"));
        checks.add(new GoogleChecksCheck("Potential Check (passed)", "PASSED", "POTENTIAL"));
        checks.add(new GoogleChecksCheck("Potential Check (unchecked)", "UNCHECKED", "POTENTIAL"));
        checks.add(new GoogleChecksCheck("Opportunity Check (failed)", "FAILED", "OPPORTUNITY"));
        checks.add(new GoogleChecksCheck("Opportunity Check (passed)", "PASSED", "OPPORTUNITY"));
        checks.add(new GoogleChecksCheck("Opportunity Check (unchecked)", "UNCHECKED", "OPPORTUNITY"));

        List<String> failingPriorityChecks = GoogleChecks.validateReport(checks, SeverityThreshold.PRIORITY);
        Assert.assertEquals(failingPriorityChecks.size(), 1);

        List<String> failingPotentialChecks = GoogleChecks.validateReport(checks, SeverityThreshold.POTENTIAL);
        Assert.assertEquals(failingPotentialChecks.size(), 2);

        List<String> failingOpportunityChecks = GoogleChecks.validateReport(checks, SeverityThreshold.OPPORTUNITY);
        Assert.assertEquals(failingOpportunityChecks.size(), 3);
    }


}
