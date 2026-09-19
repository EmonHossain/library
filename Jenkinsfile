pipeline {
    
    agent none

    options {
        skipDefaultCheckout()
    }

    environment {
        NEXUS_CREDS = credentials('6e114bfa-4783-40a7-b859-8390849767df')
        AWS_ECR_CREDS = 'ecr:us-east-1:awscreds'

        IMAGE_NAME = "jenkins/images"
        ARTIFACT_REGISTRY = "637254479904.dkr.ecr.us-east-1.amazonaws.com"
    }

    stages {

        /*
         * ============================================================
         * APPLICATION BUILD & ANALYSIS
         * ============================================================
         */
        stage('Application Build & Analysis') {

            agent {
                label 'docker-app-builder-agent-jdk25'
            }

            tools {
                maven 'maven-3.9.16'
            }

            stages {

                /*
                 * ----------------------------------------------------
                 * BUILD, TEST & CHECKSTYLE
                 * ----------------------------------------------------
                 */
                stage('Build, Test & Checkstyle') {
                    steps {
                        echo "=== Running Build, Tests, and Checkstyle ==="

                        sh '''
                            mvn -B clean package checkstyle:checkstyle \
                                -s settings.xml \
                                -Dnexus.username=${NEXUS_CREDS_USR} \
                                -Dnexus.password=${NEXUS_CREDS_PSW}
                        '''

                        echo "=== Stashing Docker Build Artifacts ==="

                        stash(
                            name: 'docker-build-artifacts',
                            includes: 'Dockerfile,target/*.jar',
                            allowEmpty: false
                        )
                    }
                }

                /*
                 * ----------------------------------------------------
                 * SONARQUBE ANALYSIS
                 * ----------------------------------------------------
                 */
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

                /*
                 * ----------------------------------------------------
                 * QUALITY GATE
                 * ----------------------------------------------------
                 */
                stage('Quality Gate') {
                    steps {
                        echo "=== Waiting for SonarQube Quality Gate Result ==="

                        timeout(time: 5, unit: 'MINUTES') {
                            waitForQualityGate abortPipeline: true
                        }
                    }
                }

                /*
                 * ----------------------------------------------------
                 * DEPLOY MAVEN ARTIFACT TO NEXUS
                 * ----------------------------------------------------
                 */
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
                        script {
                            currentBuild.description = """  <a href="http://nexus:8081/#browse/browse:maven-snapshots:com/example/library-management-system/0.0.1-SNAPSHOT">
                                                                Nexus Artifact
                                                            </a>"""
                        }
                    }
                }
            }

            /*
             * --------------------------------------------------------
             * CHECKSTYLE REPORT
             * --------------------------------------------------------
             */
            post {
                always {
                    echo "=== Recording Checkstyle Results ==="

                    recordIssues(
                        tools: [
                            checkStyle(
                                pattern: '**/target/checkstyle-result.xml'
                            )
                        ]
                    )
                }
            }
        }

        /*
         * ============================================================
         * DOCKER BUILD & PUSH
         * ============================================================
         */
        stage('Docker Build & Push') {

            agent {
                label 'docker-image-builder-agent-jdk25'
            }

            stages {

                /*
                 * ----------------------------------------------------
                 * DOCKER BUILD
                 * ----------------------------------------------------
                 *
                 * No checkout here.
                 *
                 * Dockerfile and JAR were already stashed by the
                 * Application Builder.
                 * ----------------------------------------------------
                 */
                stage('Docker Build') {
                    steps {
                        echo "=== Restoring Docker Build Artifacts ==="

                        unstash 'docker-build-artifacts'

                        echo "=== Building Docker Image ==="

                        script {
                            dockerImage = docker.build(
                                "${ARTIFACT_REGISTRY}/${IMAGE_NAME}:${BUILD_NUMBER}",
                                "."
                            )
                        }
                    }
                }

                /*
                 * ----------------------------------------------------
                 * DOCKER PUSH
                 * ----------------------------------------------------
                 */
                stage('Docker Push') {
                    steps {
                        echo "=== Pushing Docker Image to AWS ECR ==="

                        script {
                            docker.withRegistry(
                                "https://${ARTIFACT_REGISTRY}",
                                'AWS_ECR_CREDS'
                            ) {
                                dockerImage.push("${BUILD_NUMBER}")
                                dockerImage.push("latest")
                            }
                        }
                    }
                }

                /*
                 * ----------------------------------------------------
                 * CLEANUP
                 * ----------------------------------------------------
                 */
                stage('Cleanup Docker Images') {
                    steps {
                        echo "=== Cleaning Up Docker Images ==="

                        sh '''
                            docker rmi -f $(docker images -a -q) || true
                        '''
                    }
                }
            }
        }
    }
}