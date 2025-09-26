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
        APP_PORT        = '8080'
        EC2_USER        = 'ubuntu'  // or 'ec2-user' depending on your AMI
        EC2_HOST        = '<EC2_PUBLIC_IP>'
        REMOTE_APP_DIR  = '/var/www/taskmanager'
        DOMAIN_NAME     = 'your-domain.com' // if using domain
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
                        curl -v -u ${NEXUS_CRED_USR}:${NEXUS_CRED_PSW} --upload-file target/taskmanager-${env.VERSION}.jar \
                        ${NEXUS_URL}taskmanager-${env.VERSION}.jar
                    """
                }
            }
        }

        stage('Deploy Latest JAR on EC2 via SSH & Nginx') {
            steps {
                sshagent(['ec2-deploy-key']) {
                    sh """
                        ssh -o StrictHostKeyChecking=no ${EC2_USER}@${EC2_HOST} '
                            mkdir -p ${REMOTE_APP_DIR}
                            cd ${REMOTE_APP_DIR}

                            # Download latest JAR from Nexus
                            curl -u ${NEXUS_CRED_USR}:${NEXUS_CRED_PSW} -O ${NEXUS_URL}taskmanager-${env.VERSION}.jar

                            # Stop existing app if running
                            pkill -f "java -jar" || true

                            # Start new version
                            nohup java -jar ${REMOTE_APP_DIR}/taskmanager-${env.VERSION}.jar --server.port=${APP_PORT} > ${REMOTE_APP_DIR}/app.log 2>&1 &

                            # Update Nginx reverse proxy
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
                                }" > /etc/nginx/sites-available/taskmanager
                                ln -s /etc/nginx/sites-available/taskmanager /etc/nginx/sites-enabled/
                            fi

                            # Reload Nginx
                            sudo nginx -t && sudo systemctl reload nginx
                        '
                    """
                    echo "Deployment Completed. Access your app at http://${EC2_HOST}/"
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
