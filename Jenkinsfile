pipeline {
    agent any

    triggers {
        githubPush()
    }

    environment {
        SONAR_HOST_URL  = 'http://13.235.255.5:9000'
        SONAR_TOKEN     = credentials('sonar-token1') // Jenkins secret ID
        NEXUS_URL       = 'http://13.235.255.5:8081/repository/taskmanager-releases/'
        NEXUS_CRED      = credentials('nexus-credentials')
        IMAGE_NAME      = 'taskmanager'
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
                withSonarQubeEnv('SonarQubeServer') {
                    sh """
                        mvn clean verify sonar:sonar \
                        -Dsonar.projectKey=taskmanager \
                        -Dsonar.projectName=taskmanager \
                        -Dsonar.host.url=${SONAR_HOST_URL} \
                        -Dsonar.login=${SONAR_TOKEN} \
                        -DskipTests=false
                    """
                }
            }
        }

        stage('Quality Gate') {
            steps {
                script {
                    timeout(time: 15, unit: 'MINUTES') {
                        retry(2) { 
                            def qg = waitForQualityGate()
                            if (qg.status != 'OK') {
                                error "Pipeline aborted due to quality gate failure: ${qg.status}"
                            } else {
                                echo "Quality Gate passed: ${qg.status}"
                            }
                        }
                    }
                }
            }
        }

        stage('Set Version') {
            steps {
                script {
                    // Use Maven version + Jenkins build number for unique versioning
                    env.VERSION = sh(
                        script: "mvn help:evaluate -Dexpression=project.version -q -DforceStdout",
                        returnStdout: true
                    ).trim() + "-${env.BUILD_NUMBER}"
                    echo "Unique Project Version: ${env.VERSION}"
                }
            }
        }

        stage('Build JAR') {
            steps {
                echo "Building Spring Boot JAR..."
                sh 'mvn clean package -DskipTests'
            }
        }

        stage('Upload Artifact to Nexus') {
            steps {
                script {
                    sh """
                        curl -v -u ${NEXUS_CRED_USR}:${NEXUS_CRED_PSW} --upload-file target/taskmanager-${env.VERSION}.jar \
                        ${NEXUS_URL}taskmanager-${env.VERSION}.jar
                    """
                }
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

        stage('Deploy on EC2 with NGINX') {
            steps {
                script {
                    // Stop and remove previous container
                    sh """
                        docker pull ${DOCKER_REGISTRY}/${IMAGE_NAME}:${env.VERSION}
                        docker stop ${IMAGE_NAME} || true
                        docker rm ${IMAGE_NAME} || true
                        docker run -d --name ${IMAGE_NAME} -p 8080:8080 ${DOCKER_REGISTRY}/${IMAGE_NAME}:${env.VERSION}
                    """

                    // Optionally copy JAR from Nexus to NGINX static folder
                    sh """
                        curl -u ${NEXUS_CRED_USR}:${NEXUS_CRED_PSW} -O ${NEXUS_URL}taskmanager-${env.VERSION}.jar
                        # For example, copy to /usr/share/nginx/html if needed
                        # cp taskmanager-${env.VERSION}.jar /usr/share/nginx/html/
                    """
                }
                echo "Deployment Completed on EC2/NGINX."
            }
        }
    }

    post {
        always {
            echo "Cleaning workspace..."
            cleanWs()
        }
        success {
            echo "Pipeline completed successfully!"
        }
        failure {
            echo "Pipeline failed!"
        }
    }
}
