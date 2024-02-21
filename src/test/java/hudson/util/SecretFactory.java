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

package hudson.util;

/**
 * Utility class to work around testing limitations in Jenkins in regards to Secret.
 */
public class SecretFactory {

    private SecretFactory() {
    }

    /**
     * Return a new {@link Secret} with the provided string as the <em>unencrypted</em> value. The
     * returned secret will <em>not</em> have the encrypted bytes set.
     *
     * @param secret
     * @return
     */
    public static Secret getSecret(String secret) {
        return new Secret(secret);
    }
}
