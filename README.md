# Google Checks plugin for Jenkins

## Introduction

This pluging uploads your mobile app build (.apk, .aab or .ipa) to Google Checks to run a report.

## Getting started

# Variables

|       Name        |  Type   | Default |                                                                                                                                                                                                                                      Description                                                                                                                                                                                                                                      |
| :---------------: | :-----: | :-----: | :-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------: |
|     accountId     | string  |    –    |                                                                                                                                                                                       Google Checks account ID from [Checks settings page](https://checks.area120.google.com/console/settings)                                                                                                                                                                                        |
|       appId       | string  |    –    |                                                                                                                                                                                                                             Google Checks application ID                                                                                                                                                                                                                              |
|    binaryPath     | string  |    –    |                                                                                                                                                                                                                Path to the application binary file: .apk, .aab or .ipa                                                                                                                                                                                                                |
|   credentialsId   | string  |    –    | Store the contents of your service account JSON file as a secret text credential in Jenkins. For instructions on generating a service account, refer to the [Authenticate Google Checks with a service account](https://developers.google.com/checks/guide/integrate/cli/install-checks-cli#authenticate-cli) documentation. To learn how to add a new secret in Jenkins, refer to the [Jenkins Credentials](https://www.jenkins.io/doc/book/using/using-credentials/) documentation. |
|  generateReport   | boolean |  true   |                                                                                                                                                                          If `false`, the step won't upload and run the report for binaryPath. It is useful to test your authentication and other paramaters.                                                                                                                                                                          |
|   waitForReport   | boolean |  true   |                                                                                                                                                                                              If `false`, the step won't wait for the report completion and the pipeline will keep going.                                                                                                                                                                                              |
| severityThreshold | string  |    –    |                                                                                                                                                                                                                Valid values are: `PRIORITY` `POTENTIAL` `OPPORTUNITY`                                                                                                                                                                                                                 |
|      failOn       | string  |    –    |                                                                                                                                                                            if `ALL` then step will fail if there are any failed checks following `severityThreshold` condition. It won't fail by default.                                                                                                                                                                             |

```
pipeline {
    agent any

    stages {
        stage('Upload to checks') {
            steps {
                uploadToChecks(
                    accountId: '<your Google Checks account ID>',
                    appId: '<your Google Checks app ID>',
                    binaryPath: '<path to .apk/.aab/.ipa>',
                    credentialsId: '<credentials ID from Jenkins Credentials>',
                )
            }
        }
    }
}

```

## Run pipeline locally

We'll be assuming that you have some knowledge of Jenkins and Checks.

1. Clone this repository
2. Run `mvn hpi:run`
3. Go to http://localhost:8080/jenkins
4. Update the installed plugins.
5. Install [Pipeline](https://plugins.jenkins.io/workflow-aggregator/) plugin. It isn't added as a dependency of the plugin because it [isn't recommended](https://plugins.jenkins.io/workflow-aggregator/#plugin-content-developer-notes).
6. Add your [service account content](https://developers.google.com/checks/guide/integrate/cli/install-checks-cli#authenticate-service) into a [Jenkins credentials under Global domain](https://www.jenkins.io/doc/book/using/using-credentials/)
7. Create and run a pipeline (see [README](README.md) to find an example, and we suggest to use: `generateReport: false` at the beginning to make sure the authentication and other parameters are valid.)
8. if you want to upload an APK file, you can place an APK file at the root of this repository, and it'll be accessible with `binaryPath: './my-app.apk'`

## Run tests

run `mvn test` or when using [Intellij IDEA](https://www.jetbrains.com/idea/) you can open test files and run individual classes and/or tests.

## Architecture

The plugin focuses on running as a step within a pipeline. Here are a quick explanation of the files:

Classes that interact with Jenkins:

- [UploadToChecksStep](src/main/java/io/jenkins/plugins/googlechecks/UploadToChecksStep.java) is defining the parameters and name of the plugin
- [UploadToChecksStepExecution](src/main/java/io/jenkins/plugins/googlechecks/UploadToChecksStepExecution.java) is the code that the plugin runs

Classes that interact with Google Checks:

- [GoogleChecks](src/main/java/io/jenkins/plugins/googlechecks/GoogleChecks.java): manage communication with Google Checks API alongs with [models](src/main/java/io/jenkins/plugins/googlechecks/models) to define the API responses types
- [GoogleCredentialsHelper](src/main/java/io/jenkins/plugins/googlechecks/GoogleCredentialsHelper.java) and [GoogleCredentialsHelperFactory](src/main/java/io/jenkins/plugins/googlechecks/GoogleCredentialsHelperFactory.java) deal with Google authentication and the factory pattern help us mock the authenication when running tests
