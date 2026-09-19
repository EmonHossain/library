pipeline {
    agent none

    parameters {

        booleanParam(
            name: 'SKIP_APPLICATION_BUILD',
            defaultValue: false,
            description: 'Skip application build and analysis'
        )

        booleanParam(
            name: 'SKIP_DOCKER_BUILD',
            defaultValue: false,
            description: 'Skip Docker build and push'
        )

        booleanParam(
            name: 'SKIP_SONARQUBE_ANALYSIS',
            defaultValue: false,
            description: 'Skip SonarQube analysis'
        )

        booleanParam(
            name: 'SKIP_NEXUS_DEPLOY',
            defaultValue: false,
            description: 'Skip Nexus deployment'
        )

        booleanParam(
            name: 'SKIP_CHECKSTYLE',
            defaultValue: false,
            description: 'Skip Checkstyle analysis'
        )

        booleanParam(
            name: 'SKIP_TESTS',
            defaultValue: false,
            description: 'Skip unit tests'
        )

        booleanParam(
            name: 'SKIP_QUALITY_GATE',
            defaultValue: false,
            description: 'Skip SonarQube Quality Gate check'
        )
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
            when {
                beforeAgent true
                expression {
                    return !params.SKIP_APPLICATION_BUILD
                }
            }
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
                        echo "Current Jenkins branch: ${env.BRANCH_NAME}"
                        sh 'git branch --show-current'
                        sh '''
                            mvn -B clean package checkstyle:checkstyle \
                                -s settings.xml \
                                -Dnexus.username=${NEXUS_CREDS_USR} \
                                -Dnexus.password=${NEXUS_CREDS_PSW}
                        '''

                        echo "=== Stashing Docker Build Artifacts ==="
                        /*
                        stash(
                            name: 'docker-build-artifacts',
                            includes: 'Dockerfile,target/*.jar',
                            allowEmpty: false
                        )*/
                    }
                }

                /*
                 * ----------------------------------------------------
                 * SONARQUBE ANALYSIS
                 * ----------------------------------------------------
                 */
                stage('SonarQube Analysis') {
                    when {
                        expression {
                            return params.SKIP_SONARQUBE_ANALYSIS
                        }
                    }
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
                    when {
                        expression {
                            return params.SKIP_QUALITY_GATE
                        }
                    }
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
                    when {
                        allOf {
                            branch 'master'
                            expression {
                                return params.SKIP_NEXUS_DEPLOY
                            }
                        }
                    }
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
                            currentBuild.description = """  <a href="http://localhost:8081/#browse/browse:maven-snapshots:com/example/library-management-system/0.0.1-SNAPSHOT">
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
            /*
             * This stage is disabled for now. It can be enabled
             * when the Docker build and push is required.
             * ----------------------------------------------------
             */
            when {
                beforeAgent true
                expression {
                    return params.SKIP_DOCKER_BUILD
                }
            }

            agent {
                label 'docker-image-builder-agent-jdk25'
            }

            options {
                skipDefaultCheckout()
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
                        // unstash 'docker-build-artifacts'
                        unstash 'docker-build-artifacts'

                        echo "=== Building Docker Image ==="

                        script {
                            dockerImage = docker.build(
                                "${ARTIFACT_REGISTRY}/${IMAGE_NAME}:${BUILD_NUMBER}","."
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
                                "https://${ARTIFACT_REGISTRY}",AWS_ECR_CREDS) {
                                dockerImage.push("${BUILD_NUMBER}")
                                dockerImage.push("latest")
                            }
                        }

                        echo "=== Docker Image to AWS ECR Done ==="
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
                        script {
                            dockerImage.remove()
                        }
                    }
                }
            }
        }
    }
}