// Environment variables
environment {
    // Defines the Nexus artifact ID used for uploading
    ARTIFACT_ID = 'taskmanager'
    // Base Nexus URL for the snapshot repository 
    NEXUS_SNAPSHOT_URL = 'http://13.235.255.5:8081/repository/taskmanager-snapshots'
    // Docker image details
    DOCKER_IMAGE = "docker.io/akshaysriramoju/${ARTIFACT_ID}"
}

stages {
    stage('Declarative: Checkout SCM') {
        steps {
            checkout scm
        }
    }

    stage('Code Quality - SonarQube') {
        steps {
            withSonarQubeEnv('SonarQubeServer') {
                sh "mvn clean verify sonar:sonar -Dsonar.projectKey=${ARTIFACT_ID} -Dsonar.projectName=${ARTIFACT_ID} -Dsonar.host.url=${SONAR_HOST_URL} -Dsonar.login=${SONAR_TOKEN} -DskipTests=false"
            }
        }
    }

    stage('Quality Gate') {
        steps {
            script {
                timeout(time: 15, unit: 'MINUTES') {
                    def qg = retry(20) {
                        def result = waitForQualityGate()
                        if (result == 'SUCCESS' || result == 'FAILED') {
                            return result
                        }
                        sleep(time: 10, unit: 'SECONDS')
                    }
                    
                    if (qg == 'FAILED') {
                        error "SonarQube Quality Gate failed!"
                    } else {
                        echo "Quality Gate passed: ${qg}"
                    }
                }
            }
        }
    }
    
    stage('Set Version') {
        steps {
            script {
                def baseVersion = sh(returnStdout: true, script: 'mvn help:evaluate -Dexpression=project.version -q -DforceStdout').trim()
                env.BUILD_NUMBER = sh(returnStdout: true, script: 'echo ${BUILD_ID}').trim()
                env.APP_VERSION = "${baseVersion}-${env.BUILD_NUMBER}"
                echo "Unique Project Version: ${APP_VERSION}"
                
                sh "mvn versions:set -DnewVersion=${APP_VERSION}"
                sh "mvn versions:commit"
            }
        }
    }

    stage('Build JAR') {
        steps {
            echo "Building Spring Boot JAR..."
            sh "mvn clean package -DskipTests"
        }
    }

    stage('Upload Artifact to Nexus') {
        steps {
            script {
                def groupPath = 'com/example' 
                def artifactFile = "${ARTIFACT_ID}-${APP_VERSION}.jar"
                def uploadPath = "${NEXUS_SNAPSHOT_URL}/${groupPath}/${ARTIFACT_ID}/${APP_VERSION}/${artifactFile}"

                withCredentials([usernamePassword(credentialsId: 'NEXUS_CRED', usernameVariable: 'NEXUS_USER', passwordVariable: 'NEXUS_PSW')]) {
                    sh """
                    echo "Uploading artifact to Nexus: ${uploadPath}"
                    curl -v -u "${NEXUS_USER}:${NEXUS_PSW}" \\
                        --upload-file target/${artifactFile} \\
                        ${uploadPath}
                    """
                }
            }
        }
    }

    stage('Build & Push Docker Image') {
        steps {
            script {
                withCredentials([usernamePassword(credentialsId: 'DOCKERHUB_CRED', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PSW')]) {
                    sh "echo ${DOCKER_PSW} | docker login -u ${DOCKER_USER} --password-stdin"
                    sh "docker build --build-arg JAR_FILE=target/${ARTIFACT_ID}-${APP_VERSION}.jar -t ${DOCKER_IMAGE}:${APP_VERSION} ."
                    sh "docker push ${DOCKER_IMAGE}:${APP_VERSION}"
                    sh "docker logout"
                }
            }
        }
    }

    stage('Deploy on EC2 with Docker & Nginx') {
        steps {
            script {
                // --- UPDATED EC2 CONFIGURATION ---
                def EC2_USER = "ec2-user"
                // Extracted IP from the URL 13.235.255.5
                def EC2_HOST = "13.235.255.5" 
                // Assumed full path to the key file on the Jenkins agent. VERIFY THIS PATH!
                def KEY_PATH = "/var/lib/jenkins/.ssh/akshay.pem" 

                sh """
                    echo "--- Connecting to EC2 (${EC2_HOST}) to deploy new image ---"
                    
                    ssh -i ${KEY_PATH} ${EC2_USER}@${EC2_HOST} << EOF
                        
                        echo "Stopping and removing old container..."
                        docker stop ${ARTIFACT_ID} || true
                        docker rm ${ARTIFACT_ID} || true

                        echo "Pulling new image: ${DOCKER_IMAGE}:${APP_VERSION}"
                        docker pull ${DOCKER_IMAGE}:${APP_VERSION}

                        echo "Running new container..."
                        docker run -d --name ${ARTIFACT_ID} -p 8080:8080 ${DOCKER_IMAGE}:${APP_VERSION}
                        
                        echo "Deployment complete. App available at http://${EC2_HOST}:8080"
                    EOF
                    
                    echo "Deployment commands sent successfully. Check EC2 logs."
                """
            }
        }
    }
}

post {
    always {
        echo "Cleaning workspace..."
        cleanWs()
        echo "Pipeline finished."
    }
    failure {
        echo "Pipeline failed! Review logs for errors."
    }
}
