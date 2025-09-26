pipeline {
    agent any

    triggers {
        githubPush()
    }

    environment {
        SONAR_HOST_URL  = 'http://13.235.255.5:9000'
        SONAR_TOKEN     = credentials('sonar-token1')
        NEXUS_URL       = 'http://13.235.255.5:8081/repository/taskmanager-releases/'
        NEXUS_CRED      = credentials('nexus-credentials')
        DOCKER_REGISTRY = 'docker.io/akshaysriramoju'
        IMAGE_NAME      = 'taskmanager'
        APP_PORT        = '8080'
        EC2_USER        = 'ubuntu'
        EC2_HOST        = '13.235.255.5'
        REMOTE_APP_DIR  = '/var/www/taskmanager'
        DOMAIN_NAME     = 'your-domain.com' // replace with domain or EC2 IP
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
                    def baseVersion = sh(
                        script: "mvn help:evaluate -Dexpression=project.version -q -DforceStdout",
                        returnStdout: true
                    ).trim()

                    env.VERSION = "${baseVersion}-${env.BUILD_NUMBER}"
                    echo "Unique Project Version: ${env.VERSION}"

                    sh "mvn versions:set -DnewVersion=${env.VERSION}"
                    sh "mvn versions:commit"
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
                        curl -v -u ${NEXUS_CRED_USR}:${NEXUS_CRED_PSW} \
                        --upload-file target/taskmanager-${env.VERSION}.jar \
                        ${NEXUS_URL}taskmanager-${env.VERSION}.jar
                    """
                }
            }
        }

        stage('Build & Push Docker Image') {
            steps {
                script {
                    withCredentials([usernamePassword(
                        credentialsId: 'dockerhub-credentials',
                        usernameVariable: 'DOCKERHUB_USR',
                        passwordVariable: 'DOCKERHUB_PSW'
                    )]) {
                        sh """
                            echo "${DOCKERHUB_PSW}" | docker login -u "${DOCKERHUB_USR}" --password-stdin
                            docker build --build-arg JAR_FILE=target/taskmanager-${env.VERSION}.jar -t ${DOCKER_REGISTRY}/${IMAGE_NAME}:${env.VERSION} .
                            docker push ${DOCKER_REGISTRY}/${IMAGE_NAME}:${env.VERSION}
                            docker logout
                        """
                    }
                }
            }
        }

        stage('Deploy on EC2 with Docker & Nginx') {
            steps {
                sshagent(['ec2-deploy-key']) {
                    sh """
                        ssh -o StrictHostKeyChecking=no ${EC2_USER}@${EC2_HOST} '
                            docker pull ${DOCKER_REGISTRY}/${IMAGE_NAME}:${env.VERSION}
                            docker stop ${IMAGE_NAME} || true
                            docker rm ${IMAGE_NAME} || true
                            docker run -d --name ${IMAGE_NAME} -p ${APP_PORT}:8080 --restart unless-stopped ${DOCKER_REGISTRY}/${IMAGE_NAME}:${env.VERSION}

                            # Configure Nginx reverse proxy
                            sudo rm -f /etc/nginx/sites-enabled/default
                            if [ ! -f /etc/nginx/sites-available/taskmanager ]; then
                                echo "server {
                                    listen 80;
                                    server_name ${DOMAIN_NAME};

                                    location / {
                                        proxy_pass http://localhost:${APP_PORT};
                                        proxy_set_header Host \$host;
                                        proxy_set_header X-Real-IP \$remote_addr;
                                        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
                                        proxy_set_header X-Forwarded-Proto \$scheme;
                                    }
                                }" | sudo tee /etc/nginx/sites-available/taskmanager
                                sudo ln -s /etc/nginx/sites-available/taskmanager /etc/nginx/sites-enabled/
                            fi

                            sudo nginx -t && sudo systemctl reload nginx
                        '
                    """
                }
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
