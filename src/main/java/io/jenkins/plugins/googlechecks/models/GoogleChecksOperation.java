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

package io.jenkins.plugins.googlechecks.models;

import com.google.api.client.json.GenericJson;
import com.google.api.client.util.Key;

public final class GoogleChecksOperation extends GenericJson {
    /*

    // Pending:
    { "name": "accounts/1175905440/apps/629751234/operations/13814345427134797112" }

    // Done:
    {
        "name": "accounts/1175905440/apps/629751234/operations/13814345427134797112",
        "done": true,
        "response": {
            "@type": "type.googleapis.com/google.checks.report.v1alpha.Report",
            "name": "accounts/1175905440/apps/629751234/reports/13814345427134796149",
            "resultsUri": "https://checks.area120.google.com/console/dashboard/13814345427134796149?a=629751234"
        }
    }
    */
    @Key
    public String name;
    @Key
    public Boolean done;
    @Key
    public ReportResponse response;

    public static class ReportResponse extends GenericJson {
        @Key
        public String name;
        @Key
        public String resultsUri;
    }
}
