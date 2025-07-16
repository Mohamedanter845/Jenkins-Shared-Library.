def call() {
    pipeline {
        agent any

        environment {
            DOCKER_IMAGE = "your-dockerhub-username/jenkins-app"
        }

        stages {
            stage('RunUnitTest') {
                steps {
                    echo "Running unit tests..."
                    sh 'mvn test'
                }
            }

            stage('BuildApp') {
                steps {
                    echo "Building application JAR..."
                    sh 'mvn clean package -DskipTests'
                }
            }

            stage('BuildImage') {
                steps {
                    echo "Building Docker image..."
                    sh "docker build -t ${DOCKER_IMAGE}:${BUILD_NUMBER} ."
                }
            }

            stage('ScanImage') {
                steps {
                    echo "Scanning Docker image for vulnerabilities..."
                    sh "trivy image ${DOCKER_IMAGE}:${BUILD_NUMBER} || true"
                }
            }

            stage('PushImage') {
                steps {
                    echo "Pushing image to Docker Hub..."
                    withCredentials([string(credentialsId: 'docker-hub-token', variable: 'DOCKER_PASSWORD')]) {
                        sh '''
                            echo $DOCKER_PASSWORD | docker login -u your-dockerhub-username --password-stdin
                            docker push ${DOCKER_IMAGE}:${BUILD_NUMBER}
                        '''
                    }
                }
            }

            stage('RemoveImageLocally') {
                steps {
                    echo "Removing Docker image locally..."
                    sh "docker rmi ${DOCKER_IMAGE}:${BUILD_NUMBER} || true"
                }
            }

            stage('DeployOnK8s') {
                steps {
                    echo "Deploying to Kubernetes..."
                    sh '''
                        kubectl apply -f k8s/deployment.yaml
                        kubectl apply -f k8s/service.yaml
                    '''
                }
            }
        }

        post {
            always {
                echo "Pipeline finished!"
            }
        }
    }
}
