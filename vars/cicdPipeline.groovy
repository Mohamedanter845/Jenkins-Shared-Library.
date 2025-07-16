def call(String projectPath) {
    stage('RunUnitTest') {
        dir(projectPath) {
            echo "Running unit tests..."
            sh 'mvn test'
        }
    }

    stage('BuildApp') {
        dir(projectPath) {
            echo "Building application JAR..."
            sh 'mvn clean package -DskipTests'
        }
    }

    stage('BuildImage') {
        dir(projectPath) {
            echo "Building Docker image..."
            sh "docker build -t your-dockerhub-username/jenkins-app:${env.BUILD_NUMBER} ."
        }
    }

    stage('ScanImage') {
        dir(projectPath) {
            echo "Scanning Docker image..."
            sh "trivy image your-dockerhub-username/jenkins-app:${env.BUILD_NUMBER} || true"
        }
    }

    stage('PushImage') {
        dir(projectPath) {
            withCredentials([string(credentialsId: 'docker-hub-token', variable: 'DOCKER_PASSWORD')]) {
                sh '''
                    echo $DOCKER_PASSWORD | docker login -u your-dockerhub-username --password-stdin
                    docker push your-dockerhub-username/jenkins-app:${BUILD_NUMBER}
                '''
            }
        }
    }

    stage('RemoveImageLocally') {
        dir(projectPath) {
            sh "docker rmi your-dockerhub-username/jenkins-app:${BUILD_NUMBER} || true"
        }
    }

    stage('DeployOnK8s') {
        dir(projectPath) {
            sh '''
                kubectl apply -f k8s/deployment.yaml
                kubectl apply -f k8s/service.yaml
            '''
        }
    }
}

