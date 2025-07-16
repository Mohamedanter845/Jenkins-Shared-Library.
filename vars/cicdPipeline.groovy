def call() {
    stage('RunUnitTest') {
        echo "Running unit tests..."
        sh 'mvn test'
    }

    stage('BuildApp') {
        echo "Building application JAR..."
        sh 'mvn clean package -DskipTests'
    }

    stage('BuildImage') {
        echo "Building Docker image..."
        sh "docker build -t ${DOCKER_IMAGE}:${BUILD_NUMBER} ."
    }

    stage('ScanImage') {
        echo "Scanning Docker image for vulnerabilities..."
        sh "trivy image ${DOCKER_IMAGE}:${BUILD_NUMBER} || true"
    }

    stage('PushImage') {
        echo "Pushing image to Docker Hub..."
        withCredentials([string(credentialsId: 'docker-hub-token', variable: 'DOCKER_PASSWORD')]) {
            sh '''
                echo $DOCKER_PASSWORD | docker login -u your-dockerhub-username --password-stdin
                docker push ${DOCKER_IMAGE}:${BUILD_NUMBER}
            '''
        }
    }

    stage('RemoveImageLocally') {
        echo "Removing Docker image locally..."
        sh "docker rmi ${DOCKER_IMAGE}:${BUILD_NUMBER} || true"
    }

    stage('DeployOnK8s') {
        echo "Deploying to Kubernetes..."
        sh '''
            kubectl apply -f k8s/deployment.yaml
            kubectl apply -f k8s/service.yaml
        '''
    }
}

