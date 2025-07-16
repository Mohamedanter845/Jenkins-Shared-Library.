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
            sh "docker build -t mohamedanter845/jenkins-app:${env.BUILD_NUMBER} ."
        }
    }

    stage('ScanImage') {
        sh "trivy image mohamedanter845/jenkins-app:${env.BUILD_NUMBER} || true"
    }

    stage('PushImage') {
        withCredentials([usernamePassword(credentialsId: 'docker-hub-token', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
            sh """
                echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin
                docker push mohamedanter845/jenkins-app:${env.BUILD_NUMBER}
            """
        }
    }

    stage('RemoveImageLocally') {
        sh "docker rmi mohamedanter845/jenkins-app:${env.BUILD_NUMBER} || true"
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

