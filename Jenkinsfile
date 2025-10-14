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
//         DOMAIN_NAME     = '13.235.255.5' 
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
//                             error " Quality Gate failed: ${qg.status}"
//                         } else {
//                             echo " Quality Gate passed: ${qg.status}"
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
//                     echo " Project Version: ${env.VERSION}"

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

//         stage('Deploy Frontend + JAR from Nexus to EC2') {
//     steps {
//         sshagent(['ec2-deploy-key']) {
//             sh """
//                 ssh -o StrictHostKeyChecking=no ${EC2_USER}@${EC2_HOST} '
//                     # Setup app directory
//                     mkdir -p ${REMOTE_APP_DIR}
//                     cd ${REMOTE_APP_DIR}

//                     # Download latest JAR from Nexus
//                     echo "Downloading taskmanager-${VERSION}.jar from Nexus..."
//                     curl -u ${NEXUS_CRED_USR}:${NEXUS_CRED_PSW} -O ${NEXUS_URL}taskmanager-${VERSION}.jar

//                     # Stop any running instance on APP_PORT
//                     echo "Stopping any running app on port ${APP_PORT}..."
//                     fuser -k ${APP_PORT}/tcp || true

//                     # Start the app in background
//                     echo "Starting the JAR app..."
//                     nohup java -jar ${REMOTE_APP_DIR}/taskmanager-${VERSION}.jar \\
//                         --server.port=${APP_PORT} > ${REMOTE_APP_DIR}/app.log 2>&1 &
//                 '

//                 # Upload frontend static files
//                 echo "Uploading static frontend files..."
//                 scp -o StrictHostKeyChecking=no -r ./build/* ${EC2_USER}@${EC2_HOST}:/tmp/taskmanager_static/

//                 ssh -o StrictHostKeyChecking=no ${EC2_USER}@${EC2_HOST} '
//                     # === Serve static files with Nginx ===
//                     sudo mkdir -p /var/www/taskmanager
//                     sudo rm -rf /var/www/taskmanager/*
//                     sudo cp -r /tmp/taskmanager_static/* /var/www/taskmanager/
//                     rm -rf /tmp/taskmanager_static

//                     sudo ln -sf /etc/nginx/sites-available/taskmanager /etc/nginx/sites-enabled/taskmanager
//                     sudo nginx -t && sudo systemctl reload nginx
//                 '
//             """
//         }
//         echo " Deployment completed: Frontend → http://${DOMAIN_NAME}/, Backend → ${EC2_HOST}:${APP_PORT}"
//     }
// }

//     }

//     post {
//         always {
//             cleanWs()
//         }
//         success {
//             echo " Pipeline completed successfully!"
//         }
//         failure {
//             echo " Pipeline failed!"
//         }
//     }
// }


pipeline {
    agent any

    triggers {
        githubPush() // Trigger the pipeline whenever there is a push to GitHub
    }

    environment {
        SONAR_HOST_URL = "http://13.234.11.123:9000"
        SONAR_TOKEN = credentials('SONAR_TOKEN') // Use Jenkins credentials
    }

    stages {
        stage('Checkout') {
            steps {
                git branch: 'main', url: 'https://github.com/Akshaysriramoju/TaskManager.git'
            }
        }

        stage('SonarQube Analysis') {
    steps {
        withSonarQubeEnv('SonarQubeServer') {
            sh '''
                mvn clean verify sonar:sonar \
                -Dsonar.projectKey=taskmanager \
                -Dsonar.projectName=taskmanager \
                -Dsonar.host.url=$SONAR_HOST_URL \
                -Dsonar.login=$SONAR_TOKEN \
                -Dspring.profiles.active=test
            '''
        }
    }
}



        stage('Quality Gate') {
            steps {
                timeout(time: 3, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }
    }
}

