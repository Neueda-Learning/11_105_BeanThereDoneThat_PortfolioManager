pipeline {

    agent any

    environment {
        GIT_URL = 'https://github.com/Neueda-Learning/11_105_BeanThereDoneThat_PortfolioManager.git'
        BRANCH = 'main'
    }

    stages {

        stage('Checkout Source') {
            steps {
                git branch: "${BRANCH}",
                    url: "${GIT_URL}"
            }
        }


         stage('Build Spring Boot') {
                    steps {
                        sh 'mvn clean package -DskipTests'
                    }
                }


        stage('Stop Existing Containers') {
            steps {
                // reference the compose file explicitly (docker-compose.yml)
                sh 'docker compose -f docker-compose.yml down || true'
            }
        }


        stage('Build Docker Images') {
            steps {
                sh 'docker compose -f docker-compose.yml build --no-cache'
            }
        }


        stage('Deploy') {
            steps {
                sh 'docker compose -f docker-compose.yml up -d'
            }
        }


        stage('Verify') {
            steps {
                sh 'docker ps'
            }
        }
    }


    post {

        success {
            echo 'Portfolio Manager deployment successful'
        }

        failure {
            echo 'Portfolio Manager deployment failed'
        }
    }
}