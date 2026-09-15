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
    }

    post {
        success {
            archiveArtifacts artifacts: '**/target/*.jar', allowEmptyArchive: false, fingerprint: true
        }
    }
}