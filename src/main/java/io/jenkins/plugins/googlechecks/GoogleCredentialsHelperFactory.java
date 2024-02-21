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

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/*
 Factory Class to help us mock it when running tests
 */
public class GoogleCredentialsHelperFactory {
    private static final GoogleCredentialsHelperFactory INSTANCE = new GoogleCredentialsHelperFactory();
    private final Lock accessLock = new ReentrantLock();
    private GoogleCredentialsHelper googleCredentialsHelper;

    public static GoogleCredentialsHelperFactory getInstance() {
        return INSTANCE;
    }

    public GoogleCredentialsHelper getOrCreateGoogleCredentialsHelper() {
        accessLock.lock();
        try {
            if (googleCredentialsHelper == null) {
                googleCredentialsHelper = new GoogleCredentialsHelper();
            }
            return googleCredentialsHelper;
        } finally {
            accessLock.unlock();
        }
    }
}