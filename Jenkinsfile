pipeline {
    agent {
        label 'jenkins-app-builder-agent-jdk-25' // Targets Container 2 directly
    }

    tools {
        maven 'maven-3.9.16' // Installed automatically by Jenkins on Container 2
    }

    environment {
        NEXUS_CREDS = credentials('6e114bfa-4783-40a7-b859-8390849767df')
        //registryCredential = 'ecr:us-east-1:awscreds'
        //imageName = "716657688884.dkr.ecr.us-east-1.amazonaws.com/vprofileappimg"
        //vprofileRegistry = "https://716657688884.dkr.ecr.us-east-1.amazonaws.com"
    }

    stages {
        stage('Build, Test & Checkstyle') {
            steps {
                echo "=== Running Build, Tests, and Checkstyle ==="
                // Executes tests (JaCoCo) and Checkstyle XML generation in a single pass
                sh '''
                    mvn -B clean package checkstyle:checkstyle \
                        -s settings.xml \
                        -Dnexus.username=${NEXUS_CREDS_USR} \
                        -Dnexus.password=${NEXUS_CREDS_PSW}
                '''
            }
        }

        stage('SonarQube Analysis') {
            steps {
                echo "=== Starting SonarQube Analysis ==="
                withSonarQubeEnv('SonarQubeServer') {
                    sh '''
                        mvn -B sonar:sonar \
                            -s settings.xml \
                            -Dnexus.username=${NEXUS_CREDS_USR} \
                            -Dnexus.password=${NEXUS_CREDS_PSW}
                    '''
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

        stage('Deploy to Nexus') {
            steps {
                echo "=== Deploying Artifacts to Nexus Repository ==="
                sh '''
                    mvn -B deploy \
                        -s settings.xml \
                        -DskipTests \
                        -Dnexus.username=${NEXUS_CREDS_USR} \
                        -Dnexus.password=${NEXUS_CREDS_PSW}
                '''
            }
        }
    }

    post {
        always {
            // Parses target/checkstyle-result.xml and publishes visual trend charts to Jenkins
            recordIssues(
                tools: [checkStyle(pattern: '**/target/checkstyle-result.xml')]
            )
        }
        success {
            archiveArtifacts artifacts: '**/target/*.jar', allowEmptyArchive: false, fingerprint: true
        }
    }
}