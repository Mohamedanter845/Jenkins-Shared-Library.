def call(String projectPath) {
    stage('RunUnitTest') {
        dir(projectPath) {
            sh 'mvn test'
        }
    }

    stage('BuildApp') {
        dir(projectPath) {
            sh 'mvn clean package -DskipTests'
        }
    }

    stage('BuildImage') {
        dir(projectPath) {
            sh "docker build -t your-dockerhub-username/jenkins-app:${env.BUILD_NUMBER} ."
        }
    }

    stage('ScanImage') {
        sh "trivy image your-dockerhub-username/jenkins-app:${env.BUILD_NUMBER} || true"
    }

    stage('PushImage') {
        withCredentials([string(credentialsId: 'docker-hub-token', variable: 'DOCKER_PASSWORD')]) {
            sh '''
                echo $DOCKER_PASSWORD | docker login -u your-dockerhub-username --password-stdin
                docker push your-dockerhub-username/jenkins-app:${BUILD_NUMBER}
            '''
        }
    }

    stage('RemoveImageLocally') {
        sh "docker rmi your-dockerhub-username/jenkins-app:${BUILD_NUMBER} || true"
    }

    stage('DeployOnK8s') {
        dir("${projectPath}/k8s") {
            sh '''
                kubectl apply -f deployment.yaml
                kubectl apply -f service.yaml
            '''
        }
    }
}

