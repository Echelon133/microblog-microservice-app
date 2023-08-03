pipeline {
    agent any

    environment {
        DOCKERHUB_CREDENTIALS = credentials("dockerhub-credentials")
        GATEWAY_VERSION = "${sh(script:'cat gateway/build.gradle | grep -o \'version = [^,]*\' | cut -d\"\'\" -f2', returnStdout: true).trim()}"
        AUTH_VERSION = "${sh(script:'cat auth/build.gradle | grep -o \'version = [^,]*\' | cut -d\"\'\" -f2', returnStdout: true).trim()}"
        USER_VERSION = "${sh(script:'cat user/build.gradle | grep -o \'version = [^,]*\' | cut -d\"\'\" -f2', returnStdout: true).trim()}"
        POST_VERSION = "${sh(script:'cat post/build.gradle | grep -o \'version = [^,]*\' | cut -d\"\'\" -f2', returnStdout: true).trim()}"
        REPORT_VERSION = "${sh(script:'cat report/build.gradle | grep -o \'version = [^,]*\' | cut -d\"\'\" -f2', returnStdout: true).trim()}"
        NOTIFICATION_VERSION = "${sh(script:'cat notification/build.gradle | grep -o \'version = [^,]*\' | cut -d\"\'\" -f2', returnStdout: true).trim()}"
    }

    stages {

        stage("Build the project") {
            steps {
                withGradle {
                    sh "gradle wrapper assemble"
                }
            }
        }

        stage("Test all of the built services") {
            steps {
                withGradle {
                    sh "gradle wrapper test"
                }
            }
        }

        stage("Login to dockerhub") {
            steps {
                sh 'echo $DOCKERHUB_CREDENTIALS_PSW | docker login -u $DOCKERHUB_CREDENTIALS_USR --password-stdin'
            }
        }

        stage("Build gateway and push it to dockerhub") {
            when {
//              If this version of the image is already on dockerhub, this stage can be skipped
                expression { sh(returnStdout: true, script: 'docker manifest inspect echelon133/gateway:$GATEWAY_VERSION') == 1 }
            }
            steps {
                sh "docker build --tag=echelon133/gateway:$GATEWAY_VERSION ./gateway"
                sh "docker push echelon133/gateway:$GATEWAY_VERSION"
            }
        }

        stage("Build user and push it to dockerhub") {
            when {
//              If this version of the image is already on dockerhub, this stage can be skipped
                expression { sh(returnStatus: true, returnStdout: true, script: 'docker manifest inspect echelon133/user:$USER_VERSION') == 1 }
            }
            steps {
                sh "docker build --tag=echelon133/user:$USER_VERSION ./user"
                sh "docker push echelon133/user:$USER_VERSION"
            }
        }

        stage("Build post and push it to dockerhub") {
            when {
//              If this version of the image is already on dockerhub, this stage can be skipped
                expression { sh(returnStatus: true, returnStdout: true, script: 'docker manifest inspect echelon133/post:$POST_VERSION') == 1 }
            }
            steps {
                sh "docker build --tag=echelon133/post:$POST_VERSION ./post"
                sh "docker push echelon133/post:$POST_VERSION"
            }
        }

        stage("Build auth and push it to dockerhub") {
            when {
//              If this version of the image is already on dockerhub, this stage can be skipped
                expression { sh(returnStatus: true, returnStdout: true, script: 'docker manifest inspect echelon133/auth:$AUTH_VERSION') == 1 }
            }
            steps {
                sh "docker build --tag=echelon133/auth:$AUTH_VERSION ./auth"
                sh "docker push echelon133/auth:$AUTH_VERSION"
            }
        }

        stage("Build notification and push it to dockerhub") {
            when {
//              If this version of the image is already on dockerhub, this stage can be skipped
                expression { sh(returnStatus: true, returnStdout: true, script: 'docker manifest inspect echelon133/notification:$NOTIFICATION_VERSION') == 1 }
            }
            steps {
                sh "docker build --tag=echelon133/notification:$NOTIFICATION_VERSION ./notification"
                sh "docker push echelon133/notification:$NOTIFICATION_VERSION"
            }
        }

        stage("Build report and push it to dockerhub") {
            when {
//              If this version of the image is already on dockerhub, this stage can be skipped
                expression { sh(returnStatus: true, returnStdout: true, script: 'docker manifest inspect echelon133/report:$REPORT_VERSION') == 1 }
            }
            steps {
                sh "docker build --tag=echelon133/report:$REPORT_VERSION ./report"
                sh "docker push echelon133/report:$REPORT_VERSION"
            }
        }

        stage("Configure k8s' cluster namespaces and permissions") {
            steps {
                withKubeConfig([credentialsId: 'echelon133-credentials', serverUrl: 'https://192.168.49.2:8443']) {
                    sh 'kubectl apply -f k8s/namespace.yml'
                    sh 'kubectl apply -f k8s/permissions.yml'
                }
            }
        }
    }
}