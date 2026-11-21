pipeline {
    agent any

    tools {
        maven 'M2_HOME'
    }

    stages {
        stage('Checkout GIT') {
            steps {
                git branch: 'develop',
                    url: 'https://github.com/feryel17/eventsProject.git'
            }
        }

        stage('MVN Clean') {
            steps { sh 'mvn clean' }
        }

        stage('Artifact Construction') {
            steps { sh 'mvn package -DskipTests' }
        }

        stage('Unit Tests') {
            steps { sh 'mvn test' }
            post {
                always { junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml' }
            }
        }
    }
}