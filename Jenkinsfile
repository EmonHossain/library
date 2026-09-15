pipeline {
    agent {
        docker {
            image 'maven:3.9.16-eclipse-temurin-25'
            args '--entrypoint="" -v maven-repo-cache:/root/.m2'
        }
    }

    stages {
        stage('Build') {
            steps {
                echo "=== Starting Build Process ==="
                sh '''
                    echo "Java Version:"
                    java -version
                    
                    echo "Maven Version:"
                    mvn -version
                    
                    echo "Maven build started at: $(date)"
                    mvn -B clean package
                    echo "Maven build completed at: $(date)"
                '''
            }
        }

        stage('Unit Tests') {
            steps {
                echo "=== Starting Unit Tests ==="
                sh '''
                    echo "Unit tests started at: $(date)"
                    mvn test
                    echo "Unit tests completed at: $(date)"
                '''
            }
        }

        stage('CheckStyle') {
            steps {
                echo "=== Starting Checkstyle Analysis ==="
                sh '''
                    echo "Checkstyle analysis started at: $(date)"
                    mvn checkstyle:checkstyle
                    echo "Checkstyle analysis completed at: $(date)"
                '''
                }
        }
    }

    post {
        success {
            archiveArtifacts artifacts: '**/target/*.jar', allowEmptyArchive: false, fingerprint: true
        }
    }
}