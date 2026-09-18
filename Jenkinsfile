pipeline {
    agent {
        docker {
            image 'maven:3.9.16-eclipse-temurin-25'
            args '--entrypoint="" --network ci-cd-net -v maven-repo-cache:/root/.m2 -v /var/run/docker.sock:/var/run/docker.sock -v /usr/local/bin/docker:/usr/local/bin/docker'
        }
    }

    environment {
        NEXUS_CREDS = credentials('6e114bfa-4783-40a7-b859-8390849767df')
        AWS_ECR_CREDS = credentials('ecr:us-east-1:awscreds')
        IMAGE_NAME = "jenkins/images"
        ARTIFACT_REGISTRY = "https://637254479904.dkr.ecr.us-east-1.amazonaws.com"
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

        stage('Docker Build') {
            steps {
                echo "=== Building and Pushing Docker Image ==="
                script {
                    dockerImage = docker.build("${ARTIFACT_REGISTRY}/${IMAGE_NAME}:$BUILD_NUMBER", ".")

                }
            }
        }

        stage('Docker Push') {
            steps {
                echo "=== Pushing Docker Image to Registry ==="
                script {
                    docker.withRegistry("${ARTIFACT_REGISTRY}", "${AWS_ECR_CREDS}") {
                        dockerImage.push("$BUILD_NUMBER")
                        dockerImage.push('latest')
                    }
                }
            }
        }

        stage('Cleanup Docker Images') {
            steps {
                echo "=== Cleaning Up Docker Images ==="
                //script {
                //    docker.clean()
                //}
                sh 'docker rmi -f $(docker images -a -q)'
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