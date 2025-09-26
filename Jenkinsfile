// Environment variables
environment {
    // Defines the Nexus artifact ID used for uploading
    ARTIFACT_ID = 'taskmanager'
    // Base Nexus URL for the snapshot repository (assuming you use a snapshot repo for non-releases)
    NEXUS_SNAPSHOT_URL = 'http://13.235.255.5:8081/repository/taskmanager-snapshots'
    // Docker image details
    DOCKER_IMAGE = "docker.io/akshaysriramoju/${ARTIFACT_ID}"
}

stages {
    stage('Declarative: Checkout SCM') {
        steps {
            // Ensure the workspace is clean and checkout the code
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
                // Wait up to 15 minutes for the Quality Gate result
                timeout(time: 15, unit: 'MINUTES') {
                    // Retry loop to poll for results
                    def qg = retry(20) {
                        def result = waitForQualityGate()
                        if (result == 'SUCCESS' || result == 'FAILED') {
                            return result
                        }
                        // Sleep for a short period before checking again
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
                // Get the base project version from the pom.xml
                def baseVersion = sh(returnStdout: true, script: 'mvn help:evaluate -Dexpression=project.version -q -DforceStdout').trim()
                
                // Create a unique, timestamped version for the CI build (e.g., 0.0.1-SNAPSHOT-29)
                env.BUILD_NUMBER = sh(returnStdout: true, script: 'echo ${BUILD_ID}').trim()
                env.APP_VERSION = "${baseVersion}-${env.BUILD_NUMBER}"
                echo "Unique Project Version: ${APP_VERSION}"
                
                // Update the pom.xml version in the workspace
                sh "mvn versions:set -DnewVersion=${APP_VERSION}"
                sh "mvn versions:commit"
            }
        }
    }

    stage('Build JAR') {
        steps {
            echo "Building Spring Boot JAR..."
            // Build the application, skipping tests since they ran in the quality stage
            sh "mvn clean package -DskipTests"
        }
    }

    stage('Upload Artifact to Nexus') {
        steps {
            script {
                // --- FIX FOR HTTP 400 ERROR ---
                // The Nexus Maven 2/3 repository (which the error message indicates)
                // requires a structured path: groupId/artifactId/version/file.
                // We need to use the full Maven coordinates and a SNAPSHOT repo.
                
                // Note: Your pom.xml's groupId is assumed to be 'com.example'
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
                    // 1. Login to Docker Hub
                    sh "echo ${DOCKER_PSW} | docker login -u ${DOCKER_USER} --password-stdin"
                    
                    // 2. Build the image using the newly generated JAR file
                    sh "docker build --build-arg JAR_FILE=target/${ARTIFACT_ID}-${APP_VERSION}.jar -t ${DOCKER_IMAGE}:${APP_VERSION} ."
                    
                    // 3. Push the image
                    sh "docker push ${DOCKER_IMAGE}:${APP_VERSION}"
                    
                    sh "docker logout"
                }
            }
        }
    }

    stage('Deploy on EC2 with Docker & Nginx') {
        steps {
            script {
                // --- FIX FOR NoSuchMethodError 'sshagent' ---
                // The 'sshagent' error means the SSH Agent Plugin is not installed.
                // We'll replace it with a simple SSH command using the sh step.
                // NOTE: This assumes you have the private key installed on the Jenkins agent
                // or are using a different, installed plugin like 'ssh-steps' or 'ssh-publisher'.
                // If you want to use the SSH Agent Plugin, you must install it first.

                def EC2_USER = "ec2-user"
                def EC2_HOST = "your.ec2.public.ip" // <-- UPDATE THIS
                def KEY_PATH = "/var/lib/jenkins/.ssh/id_rsa" // <-- UPDATE THIS to your key file path

                // Ensure your Jenkins user has permissions to run SSH commands without interaction
                // using the key file, or use the 'ssh-publisher' plugin for a more robust solution.

                // Using a simple 'sh' block to execute the commands on the remote EC2 instance
                sh """
                    echo "--- Connecting to EC2 to deploy new image ---"
                    
                    # Use 'ssh' to execute commands on the remote EC2 instance
                    # The commands stop the old container, pull the new image, and run it.

                    ssh -i ${KEY_PATH} ${EC2_USER}@${EC2_HOST} << EOF
                        
                        echo "Stopping and removing old container..."
                        docker stop ${ARTIFACT_ID} || true
                        docker rm ${ARTIFACT_ID} || true

                        echo "Pulling new image: ${DOCKER_IMAGE}:${APP_VERSION}"
                        docker pull ${DOCKER_IMAGE}:${APP_VERSION}

                        echo "Running new container..."
                        # Assuming your Spring Boot app runs on port 8080. 
                        # Nginx would typically proxy requests from port 80/443 to 8080.
                        docker run -d --name ${ARTIFACT_ID} -p 8080:8080 ${DOCKER_IMAGE}:${APP_VERSION}
                        
                        echo "Deployment complete."
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
