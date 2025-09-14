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
        
        stage('Build & Test') {
            steps {
                script {
                    try {
                        sh '''
                            chmod +x ./mvnw
                            ./mvnw clean test jacoco:report
                        '''
                        echo "✅ Tests and coverage completed successfully"
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
                        
                        try {
                            archiveArtifacts(
                                artifacts: 'target/site/jacoco/**/*',
                                allowEmptyArchive: true,
                                fingerprint: false
                            )
                            echo "✅ JaCoCo coverage report archived"
                        } catch (Exception e) {
                            echo "⚠️ Failed to archive JaCoCo report: ${e.getMessage()}"
                        }
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