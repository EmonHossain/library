pipeline {
    agent {
        docker {
            image 'maven:3.9.16-eclipse-temurin-25'
            args '--entrypoint=""--network ci-cd-net -v maven-repo-cache:/root/.m2'
        }
    }

    stages {
        stage('Build, Test & Checkstyle') {
            steps {
                echo "=== Running Build, Tests, and Checkstyle ==="
                // Executes tests (JaCoCo) and Checkstyle XML generation in a single pass
                sh 'mvn -B clean prepare-package checkstyle:checkstyle'
            }
        }

        stage('SonarQube Analysis') {
            steps {
                echo "=== Starting SonarQube Analysis ==="
                withSonarQubeEnv('SonarQubeServer') {
                    sh 'mvn -B sonar:sonar'
                }
            }
        }

        stage('Quality Gate') {
            steps {
                echo "=== Waiting for SonarQube Quality Gate Result ==="
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }
    }

    post {
        always {
            // Parses target/checkstyle-result.xml and publishes visual trend charts to Jenkins
            recordIssues(
                tools: [checkstyle(pattern: '**/target/checkstyle-result.xml')]
            )
        }
        success {
            archiveArtifacts artifacts: '**/target/*.jar', allowEmptyArchive: false, fingerprint: true
        }
    }
}