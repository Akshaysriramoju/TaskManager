// pipeline {
//     agent any

//     triggers {
//         githubPush()
//     }

//     environment {
//         SONAR_HOST_URL  = 'http://13.235.255.5:9000'
//         SONAR_TOKEN     = credentials('sonar-token1')
//         NEXUS_URL       = 'http://13.235.255.5:8081/repository/taskmanager-releases/'
//         NEXUS_CRED      = credentials('nexus-credentials')
//         DOCKER_REGISTRY = 'docker.io/akshaysriramoju'
//         IMAGE_NAME      = 'taskmanager'
//         APP_PORT        = '8080'
//         EC2_USER        = 'ubuntu'
//         EC2_HOST        = '13.235.255.5'
//         REMOTE_APP_DIR  = '/var/www/taskmanager'
//         DOMAIN_NAME     = '13.235.255.5'  // or your real domain
//     }

//     stages {

//         stage('Checkout Code') {
//             steps {
//                 git branch: 'main',
//                     url: 'https://github.com/Akshaysriramoju/TaskManager.git'
//             }
//         }

//         stage('SonarQube Analysis') {
//             steps {
//                 withSonarQubeEnv('SonarQubeServer') {
//                     sh """
//                         mvn clean verify sonar:sonar \
//                         -Dsonar.projectKey=taskmanager \
//                         -Dsonar.projectName=taskmanager \
//                         -Dsonar.host.url=${SONAR_HOST_URL} \
//                         -Dsonar.login=${SONAR_TOKEN} \
//                         -DskipTests=false
//                     """
//                 }
//             }
//         }

//         stage('Quality Gate') {
//             steps {
//                 script {
//                     timeout(time: 15, unit: 'MINUTES') {
//                         def qg = waitForQualityGate()
//                         if (qg.status != 'OK') {
//                             error "❌ Quality Gate failed: ${qg.status}"
//                         } else {
//                             echo "✅ Quality Gate passed: ${qg.status}"
//                         }
//                     }
//                 }
//             }
//         }

//         stage('Set Version') {
//             steps {
//                 script {
//                     def baseVersion = sh(
//                         script: "mvn help:evaluate -Dexpression=project.version -q -DforceStdout",
//                         returnStdout: true
//                     ).trim()

//                     env.VERSION = "${baseVersion}-${env.BUILD_NUMBER}"
//                     echo "📦 Project Version: ${env.VERSION}"

//                     sh "mvn versions:set -DnewVersion=${env.VERSION}"
//                     sh "mvn versions:commit"
//                 }
//             }
//         }

//         stage('Build JAR') {
//             steps {
//                 sh 'mvn clean package -DskipTests'
//             }
//         }

//         stage('Upload JAR to Nexus') {
//             steps {
//                 sh """
//                     curl -v -u ${NEXUS_CRED_USR}:${NEXUS_CRED_PSW} \
//                     --upload-file target/taskmanager-${env.VERSION}.jar \
//                     ${NEXUS_URL}taskmanager-${env.VERSION}.jar
//                 """
//             }
//         }

//         stage('Build Docker Image') {
//             steps {
//                 sh """
//                     docker build --build-arg JAR_FILE=target/taskmanager-${env.VERSION}.jar \
//                                  -t ${IMAGE_NAME}:${env.VERSION} .
//                     docker tag ${IMAGE_NAME}:${env.VERSION} ${DOCKER_REGISTRY}/${IMAGE_NAME}:${env.VERSION}
//                 """
//             }
//         }

//         stage('Push Docker Image to DockerHub') {
//             steps {
//                 withCredentials([usernamePassword(
//                     credentialsId: 'dockerhub-credentials',
//                     usernameVariable: 'DOCKER_USER',
//                     passwordVariable: 'DOCKER_PASS'
//                 )]) {
//                     sh """
//                         echo "${DOCKER_PASS}" | docker login -u "${DOCKER_USER}" --password-stdin
//                         docker push ${DOCKER_REGISTRY}/${IMAGE_NAME}:${env.VERSION}
//                         docker logout
//                     """
//                 }
//             }
//         }

//         stage('Deploy JAR from Nexus on EC2 via Nginx') {
//             steps {
//                 sshagent(['ec2-deploy-key']) {
//                     sh """
//                         ssh -o StrictHostKeyChecking=no ${EC2_USER}@${EC2_HOST} '
//                             mkdir -p ${REMOTE_APP_DIR}
//                             cd ${REMOTE_APP_DIR}

//                             # Download latest JAR from Nexus
//                             curl -u ${NEXUS_CRED_USR}:${NEXUS_CRED_PSW} \
//                             -O ${NEXUS_URL}taskmanager-${env.VERSION}.jar

//                             # Stop running app
//                             PID=\$(pgrep -f "java -jar")
//                             if [ ! -z "\$PID" ]; then
//                                 kill -9 \$PID
//                                 echo "Stopped app (PID: \$PID)"
//                             fi

//                             # Start app
//                             nohup java -jar ${REMOTE_APP_DIR}/taskmanager-${env.VERSION}.jar \
//                                 --server.port=${APP_PORT} > ${REMOTE_APP_DIR}/app.log 2>&1 &

//                             # Configure Nginx
//                             sudo rm -f /etc/nginx/sites-enabled/default
//                             echo "server {
//                                 listen 80;
//                                 server_name ${DOMAIN_NAME};

//                                 location / {
//                                     proxy_pass http://localhost:${APP_PORT};
//                                     proxy_set_header Host \$host;
//                                     proxy_set_header X-Real-IP \$remote_addr;
//                                     proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
//                                     proxy_set_header X-Forwarded-Proto \$scheme;
//                                 }
//                             }" | sudo tee /etc/nginx/sites-available/taskmanager

//                             sudo ln -sf /etc/nginx/sites-available/taskmanager /etc/nginx/sites-enabled/
//                             sudo nginx -t && sudo systemctl reload nginx
//                         '
//                     """
//                 }
//                 echo "🚀 Deployment Completed → http://${DOMAIN_NAME}/"
//             }
//         }
//     }

//     post {
//         always {
//             cleanWs()
//         }
//         success {
//             echo "✅ Pipeline completed successfully!"
//         }
//         failure {
//             echo "❌ Pipeline failed!"
//         }
//     }
// }

 pipeline {
    agent any

    triggers {
        githubPush()
    }

    environment {
        DOCKER_REGISTRY = 'docker.io/akshaysriramoju'
        IMAGE_NAME      = 'taskmanager'
        APP_PORT        = '8080'
        EC2_USER        = 'ubuntu'
        EC2_HOST        = '13.235.255.5'
        REMOTE_APP_DIR  = '/var/www/taskmanager'
        DOMAIN_NAME     = '13.235.255.5'  // or your real domain
    }

    stages {

        stage('Checkout Code') {
            steps {
                git branch: 'main',
                    url: 'https://github.com/Akshaysriramoju/TaskManager.git'
            }
        }

        stage('Build JAR') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }

        stage('Build Docker Image') {
            steps {
                script {
                    // Use the JAR produced by Maven
                    def jarName = sh(script: "ls target/*.jar | grep -v 'original' | head -n 1", returnStdout: true).trim()
                    sh """
                        docker build --build-arg JAR_FILE=${jarName} \
                                     -t ${DOCKER_REGISTRY}/${IMAGE_NAME} .
                    """
                }
            }
        }

        stage('Push Docker Image to DockerHub') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'dockerhub-credentials',
                    usernameVariable: 'DOCKER_USER',
                    passwordVariable: 'DOCKER_PASS'
                )]) {
                    sh """
                        echo "${DOCKER_PASS}" | docker login -u "${DOCKER_USER}" --password-stdin
                        docker push ${DOCKER_REGISTRY}/${IMAGE_NAME}
                        docker logout
                    """
                }
            }
        }

        stage('Deploy Docker Container on EC2 via Nginx') {
             steps {
        sshagent(['ec2-deploy-key']) {
            sh """
                ssh -o StrictHostKeyChecking=no ${EC2_USER}@${EC2_HOST} '
                    # Create app directory in home (no sudo needed)
                    mkdir -p /home/ubuntu/taskmanager
                    cd /home/ubuntu/taskmanager

                    # Stop and remove old container if exists
                    docker rm -f ${IMAGE_NAME} || true

                    # Pull latest image
                    docker pull ${DOCKER_REGISTRY}/${IMAGE_NAME}:latest
                    
                    # Run container mapping container 8080 → host 8082
                    docker run -d --name ${IMAGE_NAME} -p 8082:8080 ${DOCKER_REGISTRY}/${IMAGE_NAME}:latest

                    # Configure Nginx
                    sudo rm -f /etc/nginx/sites-enabled/default
                    echo "server {
                        listen 80;
                        server_name ${DOMAIN_NAME};

                        location / {
                            proxy_pass http://localhost:8082;
                            proxy_set_header Host \$host;
                            proxy_set_header X-Real-IP \$remote_addr;
                            proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
                            proxy_set_header X-Forwarded-Proto \$scheme;
                        }
                    }" | sudo tee /etc/nginx/sites-available/taskmanager

                    sudo ln -sf /etc/nginx/sites-available/taskmanager /etc/nginx/sites-enabled/
                    sudo nginx -t && sudo systemctl reload nginx
                '
            """
        }
        echo "🚀 Deployment Completed → http://${DOMAIN_NAME}/"
    }
}

    }

    post {
        always {
            cleanWs()
        }
        success {
            echo "✅ Pipeline completed successfully!"
        }
        failure {
            echo "❌ Pipeline failed!"
        }
    }
}
