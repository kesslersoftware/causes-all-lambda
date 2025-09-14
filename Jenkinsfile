pipeline {
    agent any
    
    // Using Maven Wrapper - no tool configuration required
    
    parameters {
        booleanParam(
            name: 'SKIP_SONAR',
            defaultValue: false,
            description: 'Skip SonarQube analysis'
        )
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
                script {
                    env.BUILD_VERSION = sh(
                        script: 'echo "${BUILD_NUMBER}-$(echo ${GIT_COMMIT} | cut -c1-7)"',
                        returnStdout: true
                    ).trim()
                }
            }
        }
        
        stage('Setup Dependencies') {
            steps {
                script {
                    echo "📦 Building and installing boycottpro-common-models dependency"
                    try {
                        sh '''
                            # Clone and build common models dependency
                            if [ -d "common-models-temp" ]; then
                                rm -rf common-models-temp
                            fi
                            git clone https://github.com/kesslersoftware/boycottpro-common-models.git common-models-temp
                            cd common-models-temp
                            git checkout development
                            chmod +x ./mvnw
                            ./mvnw clean install -DskipTests -q
                            cd ..
                            rm -rf common-models-temp
                            
                            # Remove GitHub repository from pom.xml to force local resolution
                            sed -i '/<repositories>/,/<\\/repositories>/d' pom.xml
                            echo "Removed GitHub repository from pom.xml - using local Maven repository only"
                        '''
                        echo "✅ Common models dependency installed and pom.xml updated"
                    } catch (Exception e) {
                        echo "⚠️ Failed to setup dependencies: ${e.getMessage()}"
                        currentBuild.result = 'UNSTABLE'
                    }
                }
            }
        }
        
        stage('Build & Test') {
            steps {
                script {
                    try {
                        sh '''
                            chmod +x ./mvnw
                            ./mvnw clean test
                        '''
                        echo "✅ Tests completed successfully"
                    } catch (Exception e) {
                        echo "⚠️ Tests failed but continuing build: ${e.getMessage()}"
                        currentBuild.result = 'UNSTABLE'
                    }
                }
            }
            post {
                always {
                    script {
                        try {
                            junit(
                                testResults: 'target/surefire-reports/*.xml',
                                allowEmptyResults: true
                            )
                            echo "✅ Test results published successfully"
                        } catch (Exception e) {
                            echo "⚠️ Failed to publish test results: ${e.getMessage()}"
                        }
                        
                        // JaCoCo plugin not configured in Lambda projects - skipping coverage report
                        echo "ℹ️  JaCoCo coverage reports not configured for Lambda projects"
                    }
                }
            }
        }
        
        stage('SonarQube Analysis') {
            when {
                expression { !params.SKIP_SONAR }
            }
            steps {
                script {
                    try {
                        echo "🔍 Running SonarQube analysis (direct Maven execution)"
                        sh '''
                            ./mvnw sonar:sonar \\
                                -Dsonar.host.url=http://localhost:9000 \\
                                -Dsonar.projectKey=${JOB_NAME} \\
                                -Dsonar.projectName="${JOB_NAME}" \\
                                -Dsonar.projectVersion=${BUILD_VERSION}
                        '''
                        echo "✅ SonarQube analysis completed successfully"
                    } catch (Exception e) {
                        echo "⚠️ SonarQube analysis failed but continuing build: ${e.getMessage()}"
                        currentBuild.result = 'UNSTABLE'
                    }
                }
            }
        }
        
        stage('SonarQube Report') {
            when {
                expression { !params.SKIP_SONAR }
            }
            steps {
                script {
                    echo "📊 SonarQube analysis completed"
                    echo "🔗 View results at: http://localhost:9000/projects"
                    echo "📁 Project key: ${JOB_NAME}"
                    echo "ℹ️  Quality gate checks are available in SonarQube web UI"
                }
            }
        }
        
        stage('Package Lambda') {
            steps {
                sh '''
                    chmod +x ./mvnw
                    echo "Packaging AWS Lambda function..."
                    ./mvnw package -DskipTests -B
                '''
                
                archiveArtifacts(
                    artifacts: 'target/*.jar',
                    fingerprint: true
                )
            }
        }
    }
    
    post {
        always {
            cleanWs()
        }
        success {
            echo "✅ Lambda pipeline completed successfully"
        }
        failure {
            echo "❌ Lambda pipeline failed"
        }
    }
}