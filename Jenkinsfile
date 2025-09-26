pipeline {
    agent any

    triggers {
        githubPush()
    }

    environment {
        SONAR_HOST_URL   = 'http://13.235.255.5:9000'
        SONAR_TOKEN      = 'squ_84ff2db241b9d31828adf68516f6fa38a4c61769' // updated SonarQube token
        NEXUS_URL        = 'http://13.235.255.5:8081/repository/taskmanager-releases/'
        NEXUS_CRED       = credentials('nexus-credentials')
        IMAGE_NAME       = 'taskmanager'
        DOCKER_REGISTRY  = 'docker.io/akshaysriramoju'
    }

    stages {

        stage('Checkout Code') {
            steps {
                git branch: 'main',
                    url: 'https://github.com/Akshaysriramoju/TaskManager.git'
            }
        }

        stage('Code Quality - SonarQube') {
            steps {
                withSonarQubeEnv('SonarQubeServer') { // your SonarQube installation name
                sh """
                mvn clean verify sonar:sonar \
                -Dsonar.projectKey=taskmanager \
                -Dsonar.projectName=taskmanager \
                -Dsonar.host.url=http://13.235.255.5:9000 \
                -Dsonar.token=squ_84ff2db241b9d31828adf68516f6fa38a4c61769
            """
        }
    }
}


        stage('Quality Gate') {
            steps {
                script {
                    timeout(time: 10, unit: 'MINUTES') {  // reduced timeout
                        def qg = waitForQualityGate()
                        if (qg.status != 'OK') {
                            error "Pipeline aborted due to quality gate failure: ${qg.status}"
                        }
                    }
                }
            }
        }

        stage('Set Version') {
            steps {
                script {
                    env.VERSION = sh(
                        script: "mvn help:evaluate -Dexpression=project.version -q -DforceStdout",
                        returnStdout: true
                    ).trim()
                    echo "Project Version: ${env.VERSION}"
                }
            }
        }

        stage('Build JAR') {
            steps {
                echo "Building Spring Boot JAR..."
                sh 'mvn clean package -DskipTests'
            }
        }

        stage('Build Docker Image') {
            steps {
                script {
                    sh """
                        docker build -t ${IMAGE_NAME}:${env.VERSION} .
                        docker tag ${IMAGE_NAME}:${env.VERSION} ${DOCKER_REGISTRY}/${IMAGE_NAME}:${env.VERSION}
                    """
                }
            }
        }

        stage('Push Docker Image') {
            steps {
                script {
                    withCredentials([usernamePassword(
                        credentialsId: 'dockerhub-credentials',
                        usernameVariable: 'DOCKERHUB_USR',
                        passwordVariable: 'DOCKERHUB_PSW'
                    )]) {
                        sh """
                            echo "${DOCKERHUB_PSW}" | docker login -u "${DOCKERHUB_USR}" --password-stdin
                            docker push ${DOCKER_REGISTRY}/${IMAGE_NAME}:${env.VERSION}
                            docker logout
                        """
                    }
                }
            }
        }

        stage('Deploy on EC2') {
            steps {
                script {
                    sh """
                        docker pull ${DOCKER_REGISTRY}/${IMAGE_NAME}:${env.VERSION}
                        docker stop ${IMAGE_NAME} || true
                        docker rm ${IMAGE_NAME} || true
                        docker run -d --name ${IMAGE_NAME} -p 8080:8080 ${DOCKER_REGISTRY}/${IMAGE_NAME}:${env.VERSION}
                    """
                }
                echo "Deployment Completed on EC2."
            }
        }

    }

    post {
        always {
            echo "Cleaning workspace..."
            cleanWs()
        }
    }
}
