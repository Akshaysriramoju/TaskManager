
pipeline {
    agent any

    triggers {
        githubPush()
    }

    environment {
        SONAR_HOST_URL = "http://43.205.109.30:9000"
        SONAR_TOKEN = credentials('SONAR_TOKEN')
        NEXUS_CRED = credentials('NEXUS_CREDENTIALS')
        // FIX #1: Removed trailing slash from NEXUS_URL to prevent double slashes
        NEXUS_URL = "http://43.205.109.30:8081/repository/taskmanager-releases" 
        GROUP_ID = "com/example"
        ARTIFACT_ID = "taskmanager"
        IMAGE_NAME = "taskmanager"
        DOCKER_REGISTRY = "docker.io/akshaysriramoju"

        // EC2 / deployment targets
        EC2_USER = "ubuntu"
        EC2_HOST = "43.205.109.30"
        REMOTE_DB_HOST = "172.31.18.171"
        REMOTE_FRONTEND_DIR = "/var/www/html"
        REMOTE_BACKEND_DIR = "/home/ubuntu/backend"
        BACKEND_HOST_PORT = "8084"
        BACKEND_CONTAINER_PORT = "8080"
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
                        # 1. CLEAN, RUN TESTS, and GENERATE COVERAGE REPORT
                        mvn clean verify org.jacoco:jacoco-maven-plugin:report
                        
                        # 2. RUN SONAR ANALYSIS (Uses sonar.token for standard compliance)
                        mvn org.sonarsource.scanner.maven:sonar-maven-plugin:sonar \
                            -Dsonar.projectKey=taskmanager -Dsonar.projectName=taskmanager \
                            -Dsonar.host.url=$SONAR_HOST_URL -Dsonar.token=$SONAR_TOKEN \
                            -Dspring.profiles.active=test \
                            -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
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
                    def nexusCredentials = NEXUS_CRED.split(':')
                    def nexusUser = nexusCredentials[0]
                    def nexusPassword = nexusCredentials[1]

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
                    
                    // The API URL is the Nginx entry point (port 80)
                    def apiUrl = "http://${EC2_HOST}" 
                    
                    echo "Replacing frontend API placeholder with: ${apiUrl}"

                    sh """
                        # 1. Temporarily copy index.html to workspace root
                        cp ${staticDir}/index.html index.html.temp || exit 1
                        
                        # 2. Inject the host IP into the placeholder. 
                        sed -i 's|__API_URL__|${apiUrl}|g' index.html.temp || true
                        
                        # 3. Rename the modified temporary file to index.html for correct packaging
                        mv index.html.temp index.html
                        
                        # 4. Create the zip archive 
                        zip -j ${zipName} index.html ${staticDir}/script.js ${staticDir}/styles.css
                        
                        # 5. Clean up temporary files
                        rm index.html
                    """
                }
            }
        }


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
                                
                                # Use sudo for unzip/copy to ensure proper permissions on extracted files
                                sudo unzip -o /tmp/frontend-${env.VERSION}.zip -d /tmp/frontend_deploy || true
                                sudo cp -r /tmp/frontend_deploy/* ${REMOTE_FRONTEND_DIR}/
                                
                                # FIX #2: Ownership Check (Prevents 403 Forbidden errors)
                                sudo chown -R www-data:www-data ${REMOTE_FRONTEND_DIR}

                                # CRITICAL FIX: Use SUDO for cleanup of root-owned files
                                sudo rm -rf /tmp/frontend_deploy /tmp/frontend-${env.VERSION}.zip

                                # BACKEND: ensure backend dir exists
                                mkdir -p ${REMOTE_BACKEND_DIR}
                                cd ${REMOTE_BACKEND_DIR}

                                # Pull image and restart container
                                docker pull ${DOCKER_REGISTRY}/${IMAGE_NAME}:${env.VERSION} || true

                                # Stop & remove existing container (if present)
                                docker rm -f ${ARTIFACT_ID} || true

                                # Start new container mapped to host port ${BACKEND_HOST_PORT}
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