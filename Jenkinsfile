pipeline {
    agent any

    triggers {
        githubPush() 
    }

    environment {
        SONAR_HOST_URL = 'http://13.235.255.5:9000'
        SONAR_TOKEN    = credentials('sonar-token')
        NEXUS_URL      = 'http://13.235.255.5:8081/repository/taskmanager-releases/'
        NEXUS_CRED     = credentials('nexus-credentials')
        IMAGE_NAME     = 'taskmanager'
        DOCKER_REGISTRY = 'docker.io/akshaysriramoju'
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
                echo "Running SonarQube analysis..."
                withSonarQubeEnv('SonarQubeServer') {
                    sh """
                        mvn clean verify sonar:sonar \
                        -Dsonar.host.url=${SONAR_HOST_URL} \
                        -Dsonar.login=${SONAR_TOKEN}
                    """
                }
            }
        }

        stage('Quality Gate') {
            steps {
                timeout(time: 1, unit: 'HOURS') {
                    def qg = waitForQualityGate()
                    if (qg.status != 'OK') {
                        error "Pipeline aborted due to quality gate failure: ${qg.status}"
                    }
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
                    def version = sh(
                        script: "mvn help:evaluate -Dexpression=project.version -q -DforceStdout",
                        returnStdout: true
                    ).trim()

                    sh """
                        docker build -t ${IMAGE_NAME}:${version} .
                        docker tag ${IMAGE_NAME}:${version} ${DOCKER_REGISTRY}/${IMAGE_NAME}:${version}
                    """
                }
            }
        }

        stage('Push Docker Image') {
            steps {
                script {
                    def version = sh(
                        script: "mvn help:evaluate -Dexpression=project.version -q -DforceStdout",
                        returnStdout: true
                    ).trim()

                    withCredentials([usernamePassword(
                        credentialsId: 'dockerhub-credentials',
                        usernameVariable: 'DOCKERHUB_USR',
                        passwordVariable: 'DOCKERHUB_PSW'
                    )]) {
                        sh """
                            echo "${DOCKERHUB_PSW}" | docker login -u "${DOCKERHUB_USR}" --password-stdin
                            docker push ${DOCKER_REGISTRY}/${IMAGE_NAME}:${version}
                            docker logout
                        """
                    }
                }
            }
        }

        stage('Deploy on EC2') {
            steps {
                script {
                    def version = sh(
                        script: "mvn help:evaluate -Dexpression=project.version -q -DforceStdout",
                        returnStdout: true
                    ).trim()

                    sh """
                        docker pull ${DOCKER_REGISTRY}/${IMAGE_NAME}:${version}
                        docker stop ${IMAGE_NAME} || true
                        docker rm ${IMAGE_NAME} || true
                        docker run -d --name ${IMAGE_NAME} -p 8080:8080 ${DOCKER_REGISTRY}/${IMAGE_NAME}:${version}
                    """
                }
                echo "Deployment Completed on EC2."
            }
        }

        stage('Clean Workspace') {
            steps {
                echo "Cleaning workspace..."
                cleanWs()
            }
        }
    }
}
