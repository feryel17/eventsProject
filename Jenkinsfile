pipeline {
    agent any

    tools {
        maven 'M2_HOME'   // nom défini dans Manage Jenkins > Tools
        jdk   'JAVA_HOME' // idem
    }

    stages {
        stage('Checkout GIT') {
            steps {
                git branch: 'develop',
                    url: 'https://github.com/feryel17/eventsProject.git'
            }
        }

        stage('MVN Clean') {
            steps { bat 'mvn clean' }
        }

        stage('Artifact Construction') {
            steps { bat 'mvn package -DskipTests' }
        }

        stage('Unit Tests') {
            steps { bat 'mvn test' }
            post {
                always { junit 'target/surefire-reports/*.xml' }
            }
        }
    }
}