pipeline {
    agent any

    environment {
        DOCKER_USER = 'mohamedanter845'
        IMAGE_NAME = "jenkins-mb-app"
        BRANCH_NAME = "${env.BRANCH_NAME}"
    }

    stages {
        stage('Build App') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }

        stage('Build Docker Image') {
            steps {
                script {
                    env.IMAGE_TAG = "${DOCKER_USER}/${IMAGE_NAME}:${BRANCH_NAME}-${BUILD_NUMBER}"
                    sh "docker build -t ${IMAGE_TAG} ."
                }
            }
        }

        stage('Push Docker Image') {
            steps {
                withCredentials([string(credentialsId: 'Docker-Password', variable: 'DOCKER_PASS')]) {
                    sh "echo $DOCKER_PASS | docker login -u ${DOCKER_USER} --password-stdin"
                    sh "docker push ${IMAGE_TAG}"
                }
            }
        }

        stage('Deploy on K8s') {
            steps {
                script {
                    NAMESPACE = (BRANCH_NAME == 'main') ? 'main' : (BRANCH_NAME == 'stag') ? 'stag' : 'dev'
                    sh "kubectl -n ${NAMESPACE} set image deployment/jenkins-app jenkins-app=${IMAGE_TAG} --record || kubectl -n ${NAMESPACE} apply -f k8s/"
                }
            }
        }
    }

    post {
        always {
            sh "docker rmi ${IMAGE_TAG} || true"
        }
    }
}
