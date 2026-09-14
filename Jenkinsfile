pipeline {
    agent {
        label 'jenkins-app_builder-agent-jdk-25'
    }

    tools {
        maven 'maven-3.9.16'
    }

    stages {
        stage('Build') {
            steps {
                echo "Java version:"
                sh 'java -version'
                echo "Maven version:"
                sh 'mvn -version'
                echo "Maven build started at: '\$(date)'"
                sh 'mvn -B clean package'
                echo "Maven build completed at: '\$(date)'"
            }
        }
    }

    post {
        success {
            archiveArtifacts artifacts: '**/*.jar', fingerprint: true
        }
    }
}