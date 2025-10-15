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

// pipeline {
//     agent any

//     triggers {
//         githubPush() // Trigger on GitHub push
//     }

//     environment {
//         SONAR_HOST_URL = "http://52.66.228.227:9000"
//         SONAR_TOKEN = credentials('SONAR_TOKEN')   // SonarQube token stored in Jenkins
//         NEXUS_CRED = credentials('NEXUS_CREDENTIALS') // Nexus username:password stored as single Jenkins credential
//         NEXUS_URL = "http://52.66.228.227:8081/repository/taskmanager-releases/"
//         GROUP_ID = "com/example"  // Convert dots to slashes for Maven repo path
//         ARTIFACT_ID = "taskmanager"
//         IMAGE_NAME = "taskmanager"   // Docker image name
//         DOCKER_REGISTRY = "docker.io/akshaysriramoju" // Replace with your DockerHub username

//         // --- EC2 / deployment targets ---
//         EC2_USER = "ubuntu"
//         EC2_HOST = "52.66.228.227"               // <-- CHANGE to your EC2 public IP or DNS
//         REMOTE_FRONTEND_DIR = "/var/www/html"     // Nginx root
//         REMOTE_BACKEND_DIR = "/home/ubuntu/backend"
//         BACKEND_HOST_PORT = "8084"                // host port (not 8080/8081/9000)
//         BACKEND_CONTAINER_PORT = "8080"           // container exposes 8080
//     }

//     stages {
//         stage('Checkout') {
//             steps {
//                 git branch: 'main', url: 'https://github.com/Akshaysriramoju/TaskManager.git'
//             }
//         }

//         stage('SonarQube Analysis') {
//             steps {
//                 withSonarQubeEnv('SonarQubeServer') {
//                     sh '''
//                         mvn clean verify sonar:sonar \
//                         -Dsonar.projectKey=taskmanager \
//                         -Dsonar.projectName=taskmanager \
//                         -Dsonar.host.url=$SONAR_HOST_URL \
//                         -Dsonar.login=$SONAR_TOKEN \
//                         -Dspring.profiles.active=test
//                     '''
//                 }
//             }
//         }

//         stage('Quality Gate') {
//             steps {
//                 script {
//                     timeout(time: 3, unit: 'MINUTES') {
//                         def qg = waitForQualityGate()
//                         if (qg.status != 'OK') {
//                             error "Quality Gate failed: ${qg.status}"
//                         } else {
//                             echo "Quality Gate passed: ${qg.status}"
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
//                     echo "Project Version: ${env.VERSION}"

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
//                 script {
//                     // Split the stored credentials into username and password
//                     def nexusCredentials = NEXUS_CRED.split(':')
//                     def nexusUser = nexusCredentials[0]
//                     def nexusPassword = nexusCredentials[1]

//                     // Construct proper Maven path for Nexus 2 repository
//                     def jarFile = "target/${ARTIFACT_ID}-${env.VERSION}.jar"
//                     def nexusPath = "${NEXUS_URL}/${GROUP_ID}/${ARTIFACT_ID}/${env.VERSION}/${ARTIFACT_ID}-${env.VERSION}.jar"

//                     echo "Uploading JAR to Nexus: ${nexusPath}"

//                     sh """
//                         curl -v -u ${nexusUser}:${nexusPassword} \
//                         --upload-file ${jarFile} \
//                         ${nexusPath}
//                     """
//                 }
//             }
//         }

//         stage('Build Docker Image') {
//             steps {
//                 sh """
//                     docker build --build-arg JAR_FILE=target/${ARTIFACT_ID}-${env.VERSION}.jar \
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

//         stage('Prepare Frontend') {
//     steps {
//         script {
//             // API URL replacement
//             def apiUrl = "http://${EC2_HOST}:${BACKEND_HOST_PORT}"
//             echo "Replacing frontend API placeholder with: ${apiUrl}?v=${env.VERSION}"

//             sh """
//                 cd src/main/resources/static || exit 1
//                 # Backup index.html then replace placeholder
//                 cp index.html index.html.bak || true
//                 sed -i 's|__API_URL__|${apiUrl}?v=${env.VERSION}|g' index.html || true

//                 # Package frontend for deployment
//                 zip -r ../../../frontend-${env.VERSION}.zip * || true
//                 cd ../../../
//             """
//         }
//     }
// }


//         /* ---------- NEW: Deploy frontend + pull & restart backend container on EC2 ---------- */
//         stage('Deploy to EC2 (Frontend + Backend docker)') {
//             steps {
//                 // SSH deploy using ssh-agent credential 'ec2-deploy-key' in Jenkins (must exist)
//                 sshagent(['ec2-deploy-key']) {
//                     script {
//                         // upload frontend zip
//                         sh """
//                             scp -o StrictHostKeyChecking=no frontend-${env.VERSION}.zip ${EC2_USER}@${EC2_HOST}:/tmp/
//                         """

//                         // remote commands: unzip frontend -> /var/www/html and pull & run docker image
//                         sh """
//                             ssh -o StrictHostKeyChecking=no ${EC2_USER}@${EC2_HOST} <<'REMOTE'
//                                 set -e

//                                 # FRONTEND: deploy static files to nginx directory
//                                 sudo mkdir -p ${REMOTE_FRONTEND_DIR}
//                                 sudo rm -rf ${REMOTE_FRONTEND_DIR}/*
//                                 sudo unzip -o /tmp/frontend-${env.VERSION}.zip -d /tmp/frontend_deploy || true
//                                 sudo cp -r /tmp/frontend_deploy/* ${REMOTE_FRONTEND_DIR}/
//                                 rm -rf /tmp/frontend_deploy /tmp/frontend-${env.VERSION}.zip

//                                 # BACKEND: ensure backend dir exists
//                                 mkdir -p ${REMOTE_BACKEND_DIR}
//                                 cd ${REMOTE_BACKEND_DIR}

//                                 # Pull image and restart container
//                                 docker pull ${DOCKER_REGISTRY}/${IMAGE_NAME}:${env.VERSION} || true

//                                 # Stop & remove existing container (if present)
//                                 docker rm -f ${ARTIFACT_ID} || true

//                                 # Start new container mapped to host port ${BACKEND_HOST_PORT}
//                                 docker run -d --name ${ARTIFACT_ID} -p ${BACKEND_HOST_PORT}:${BACKEND_CONTAINER_PORT} \
//                                   --env SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/taskmanager \
//                                   --env SPRING_DATASOURCE_USERNAME=task_user \
//                                   --env SPRING_DATASOURCE_PASSWORD=task_pass \
//                                   ${DOCKER_REGISTRY}/${IMAGE_NAME}:${env.VERSION}

//                                 # Optional: check container status
//                                 sleep 3
//                                 docker ps --filter name=${ARTIFACT_ID}
//                             REMOTE
//                         """
//                     }
//                 }
//             }
//         }

//     } // stages

//     post {
//         always {
//             cleanWs()
//         }
//         success {
//             echo "✅ Pipeline completed successfully! Frontend served by Nginx and backend updated from DockerHub (port ${BACKEND_HOST_PORT})."
//             echo "Open in browser: http://${EC2_HOST}/"
//         }
//         failure {
//             echo "❌ Pipeline failed. Check logs."
//         }
//     }
// }



pipeline {
    agent any

    triggers {
        githubPush() // Trigger on GitHub push
    }

    environment {
        SONAR_HOST_URL = "http://52.66.228.227:9000"
        SONAR_TOKEN = credentials('SONAR_TOKEN')   // SonarQube token stored in Jenkins
        NEXUS_CRED = credentials('NEXUS_CREDENTIALS') // Nexus username:password stored as single Jenkins credential
        NEXUS_URL = "http://52.66.228.227:8081/repository/taskmanager-releases/"
        GROUP_ID = "com/example"   // Convert dots to slashes for Maven repo path
        ARTIFACT_ID = "taskmanager"
        IMAGE_NAME = "taskmanager"   // Docker image name
        DOCKER_REGISTRY = "docker.io/akshaysriramoju" // Replace with your DockerHub username

        // --- EC2 / deployment targets ---
        EC2_USER = "ubuntu"
        EC2_HOST = "52.66.228.227"                // <-- Public IP
        REMOTE_DB_HOST = "172.31.18.171"           // <--- CRITICAL: REPLACE with your EC2 PRIVATE IP
        REMOTE_FRONTEND_DIR = "/var/www/html"      // Nginx root
        REMOTE_BACKEND_DIR = "/home/ubuntu/backend"
        BACKEND_HOST_PORT = "8084"                 // host port 
        BACKEND_CONTAINER_PORT = "8080"            // container exposes 8080
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
                script {
                    timeout(time: 3, unit: 'MINUTES') {
                        def qg = waitForQualityGate()
                        if (qg.status != 'OK') {
                            error "Quality Gate failed: ${qg.status}"
                        } else {
                            echo "Quality Gate passed: ${qg.status}"
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
                    echo "Project Version: ${env.VERSION}"

                    sh "mvn versions:set -DnewVersion=${env.VERSION}"
                    sh "mvn versions:commit"
                }
            }
        }

        stage('Build JAR') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }

        stage('Upload JAR to Nexus') {
            steps {
                script {
                    // Split the stored credentials into username and password
                    def nexusCredentials = NEXUS_CRED.split(':')
                    def nexusUser = nexusCredentials[0]
                    def nexusPassword = nexusCredentials[1]

                    // Construct proper Maven path for Nexus 2 repository
                    def jarFile = "target/${ARTIFACT_ID}-${env.VERSION}.jar"
                    def nexusPath = "${NEXUS_URL}/${GROUP_ID}/${ARTIFACT_ID}/${env.VERSION}/${ARTIFACT_ID}-${env.VERSION}.jar"

                    echo "Uploading JAR to Nexus: ${nexusPath}"

                    sh """
                        curl -v -u ${nexusUser}:${nexusPassword} \
                        --upload-file ${jarFile} \
                        ${nexusPath}
                    """
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                sh """
                    docker build --build-arg JAR_FILE=target/${ARTIFACT_ID}-${env.VERSION}.jar \
                                 -t ${IMAGE_NAME}:${env.VERSION} .
                    docker tag ${IMAGE_NAME}:${env.VERSION} ${DOCKER_REGISTRY}/${IMAGE_NAME}:${env.VERSION}
                """
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
                        docker push ${DOCKER_REGISTRY}/${IMAGE_NAME}:${env.VERSION}
                        docker logout
                    """
                }
            }
        }

        stage('Prepare Frontend') {
        steps {
            script {
                def staticDir = "src/main/resources/static"
                def zipName = "frontend-${env.VERSION}.zip"
                def apiUrl = "http://${EC2_HOST}:${BACKEND_HOST_PORT}"
                echo "Replacing frontend API placeholder with: ${apiUrl}?v=${env.VERSION}"

                sh """
                    # 1. Temporarily copy the index.html to the workspace root for modification
                    cp ${staticDir}/index.html index.html.temp || exit 1
                    
                    # 2. Replace placeholder in the temporary index.html
                    sed -i 's|__API_URL__|${apiUrl}?v=${env.VERSION}|g' index.html.temp || true

                    # 3. Create the zip archive with ALL static files, placing the zip in the root.
                    #    -j (junk paths) is used to include only file names.
                    zip -j ${zipName} ${staticDir}/*
                    
                    # 4. DELETE the original index.html from INSIDE the zip (which was added in step 3)
                    zip -d ${zipName} index.html 
                    
                    # 5. Add the MODIFIED index.html.temp file back into the zip, renaming it to index.html
                    zip -u ${zipName} index.html.temp -j

                    # 6. Clean up temporary file
                    rm index.html.temp
                """
            }
        }
    }


        // --- CORRECTED: Deploy to EC2 (Frontend + Backend docker) ---
        stage('Deploy to EC2 (Frontend + Backend docker)') {
            steps {
                // SSH deploy using ssh-agent credential 'ec2-deploy-key' in Jenkins (must exist)
                sshagent(['ec2-deploy-key']) {
                    script {
                        // upload frontend zip
                        sh """
                            scp -o StrictHostKeyChecking=no frontend-${env.VERSION}.zip ${EC2_USER}@${EC2_HOST}:/tmp/
                        """

                        // remote commands: unzip frontend -> /var/www/html and pull & run docker image
                        sh """
                            ssh -o StrictHostKeyChecking=no ${EC2_USER}@${EC2_HOST} <<'REMOTE'
                                set -e

                                # FRONTEND: deploy static files to nginx directory
                                sudo mkdir -p ${REMOTE_FRONTEND_DIR}
                                sudo rm -rf ${REMOTE_FRONTEND_DIR}/*
                                sudo unzip -o /tmp/frontend-${env.VERSION}.zip -d /tmp/frontend_deploy || true
                                sudo cp -r /tmp/frontend_deploy/* ${REMOTE_FRONTEND_DIR}/
                                rm -rf /tmp/frontend_deploy /tmp/frontend-${env.VERSION}.zip

                                # BACKEND: ensure backend dir exists
                                mkdir -p ${REMOTE_BACKEND_DIR}
                                cd ${REMOTE_BACKEND_DIR}

                                # Pull image and restart container
                                docker pull ${DOCKER_REGISTRY}/${IMAGE_NAME}:${env.VERSION} || true

                                # Stop & remove existing container (if present)
                                docker rm -f ${ARTIFACT_ID} || true

                                # Start new container mapped to host port ${BACKEND_HOST_PORT}
                                # *** CORRECTED DB URL, USER, AND PASSWORD ***
                                docker run -d --name ${ARTIFACT_ID} -p ${BACKEND_HOST_PORT}:${BACKEND_CONTAINER_PORT} \
                                  --env SPRING_DATASOURCE_URL=jdbc:mysql://${REMOTE_DB_HOST}:3306/task_manager_db \
                                  --env SPRING_DATASOURCE_USERNAME=taskuser \
                                  --env SPRING_DATASOURCE_PASSWORD=Akshay@123 \
                                  ${DOCKER_REGISTRY}/${IMAGE_NAME}:${env.VERSION}

                                # Optional: check container status
                                sleep 3
                                docker ps --filter name=${ARTIFACT_ID}
                            REMOTE
                        """
                    }
                }
            }
        }

    } // stages

    post {
        always {
            cleanWs()
        }
        success {
            echo "✅ Pipeline completed successfully! Frontend served by Nginx and backend updated from DockerHub (port ${BACKEND_HOST_PORT})."
            echo "Open in browser: http://${EC2_HOST}/"
        }
        failure {
            echo "❌ Pipeline failed. Check logs."
        }
    }
}