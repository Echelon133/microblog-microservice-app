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
                expression { sh(returnStatus: true, returnStdout: true, script: 'docker manifest inspect echelon133/gateway:$GATEWAY_VERSION') == 1 }
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

        stage("Create secrets required by services") {
            steps {
                withKubeConfig([credentialsId: 'echelon133-credentials', serverUrl: 'https://192.168.49.2:8443']) {

                    withCredentials([file(credentialsId: 'user-postgres-secret', variable: 'USER_SECRET')]) {
                        sh(returnStatus: true, returnStdout: true, script:
                            '''
                                kubectl create secret generic user-postgres-secret \
                                    --from-env-file=$USER_SECRET \
                                    -n microblog-app
                            '''
                        )
                    }

                    withCredentials([file(credentialsId: 'post-postgres-secret', variable: 'POST_SECRET')]) {
                        sh(returnStatus: true, returnStdout: true, script:
                            '''
                                kubectl create secret generic post-postgres-secret \
                                    --from-env-file=$POST_SECRET \
                                    -n microblog-app
                            '''
                        )
                    }

                    withCredentials([file(credentialsId: 'notification-postgres-secret', variable: 'NOTIFICATION_SECRET')]) {
                        sh(returnStatus: true, returnStdout: true, script:
                            '''
                                kubectl create secret generic notification-postgres-secret \
                                    --from-env-file=$NOTIFICATION_SECRET \
                                    -n microblog-app
                            '''
                        )
                    }

                    withCredentials([file(credentialsId: 'report-postgres-secret', variable: 'REPORT_SECRET')]) {
                        sh(returnStatus: true, returnStdout: true, script:
                            '''
                                kubectl create secret generic report-postgres-secret \
                                    --from-env-file=$REPORT_SECRET \
                                    -n microblog-app
                            '''
                        )
                    }

                    withCredentials([file(credentialsId: 'redis-auth-secret', variable: 'AUTH_SECRET')]) {
                        sh(returnStatus: true, returnStdout: true, script:
                            '''
                                kubectl create secret generic redis-auth-secret \
                                    --from-env-file=$AUTH_SECRET \
                                    -n microblog-app
                            '''
                        )
                    }

                    withCredentials([file(credentialsId: 'queue-secret', variable: 'QUEUE_SECRET')]) {
                        sh(returnStatus: true, returnStdout: true, script:
                            '''
                                kubectl create secret generic queue-secret \
                                    --from-env-file=$QUEUE_SECRET \
                                    -n microblog-app
                            '''
                        )
                    }

                    withCredentials([file(credentialsId: 'confidential-client-secret', variable: 'CLIENT_SECRET')]) {
                        sh(returnStatus: true, returnStdout: true, script:
                            '''
                                kubectl create secret generic confidential-client-secret \
                                    --from-env-file=$CLIENT_SECRET \
                                    -n microblog-app
                            '''
                        )
                    }
                }
            }
        }

        stage("Create/Update resources in the cluster by applying app's configs") {
            steps {
                withKubeConfig([credentialsId: 'echelon133-credentials', serverUrl: 'https://192.168.49.2:8443']) {
                    sh(returnStatus: true, script: 'kubectl apply -f k8s/gateway/')
                    sh(returnStatus: true, script: 'kubectl apply -f k8s/user/')
                    sh(returnStatus: true, script: 'kubectl apply -f k8s/auth/')
                    sh(returnStatus: true, script: 'kubectl apply -f k8s/post/')
                    sh(returnStatus: true, script: 'kubectl apply -f k8s/queue/')
                    sh(returnStatus: true, script: 'kubectl apply -f k8s/notification/')
                    sh(returnStatus: true, script: 'kubectl apply -f k8s/report/')
                }
            }
        }
    }
}