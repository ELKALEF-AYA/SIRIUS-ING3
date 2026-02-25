pipeline {
    agent any

    tools {
        jdk 'Java21'
    }

    environment {
        GITHUB_REPO     = 'https://github.com/ELKALEF-AYA/SIRIUS-ING3.git'
        GITHUB_BRANCH   = 'main'

        VM_FRONT        = '172.31.252.169'
        VM_AUTH         = '172.31.253.250'
        VM_CHAT_NOTIF   = '172.31.249.138'
        VM_INVOICE      = '172.31.249.10'

        SSH_USER        = 'jsa'

        IMG_AUTH        = 'jsahome/authentification'
        IMG_CHAT        = 'jsahome/chat'
        IMG_NOTIF       = 'jsahome/notification'
        IMG_INVOICE     = 'jsahome/rent-receipt'
        IMG_BFF         = 'jsahome/backend-for-frontend'
        IMG_FRONT       = 'jsahome/frontend'
    }

    stages {

        stage('Checkout') {
            steps {
                echo 'Recuperation du code depuis GitHub...'
                git branch: "${GITHUB_BRANCH}",
                    url: "${GITHUB_REPO}"
                echo 'Code recupere avec succes.'
            }
        }

        stage('Build') {
            steps {
                echo 'Build de tous les services depuis la racine...'
                sh 'mvn clean install -DskipTests'
                echo 'Build termine.'
            }
        }

        stage('Docker Build') {
            parallel {
                stage('Image Auth') {
                    steps {
                        echo 'Construction image authentification...'
                        sh "docker build -t ${IMG_AUTH}:latest authentification/"
                    }
                }
                stage('Image Chat') {
                    steps {
                        echo 'Construction image chat...'
                        sh "docker build -t ${IMG_CHAT}:latest chat/"
                    }
                }
                stage('Image Notification') {
                    steps {
                        echo 'Construction image notification...'
                        sh "docker build -t ${IMG_NOTIF}:latest notification/"
                    }
                }
                stage('Image Rent-Receipt') {
                    steps {
                        echo 'Construction image rent-receipt...'
                        sh "docker build -t ${IMG_INVOICE}:latest rent-receipt/"
                    }
                }
                stage('Image BFF') {
                    steps {
                        echo 'Construction image backend-for-frontend...'
                        sh "docker build -t ${IMG_BFF}:latest backend-for-frontend/"
                    }
                }
                stage('Image Frontend') {
                    steps {
                        echo 'Construction image frontend...'
                        sh "docker build -t ${IMG_FRONT}:latest frontend/"
                    }
                }
            }
        }

        stage('Transfer Images') {
            parallel {
                stage('vm-auth') {
                    steps {
                        echo 'Envoi image auth vers vm-auth...'
                        sh "docker save ${IMG_AUTH}:latest | ssh ${SSH_USER}@${VM_AUTH} 'docker load'"
                    }
                }
                stage('vm-chat-notif') {
                    steps {
                        echo 'Envoi images chat et notification vers vm-chat-notif...'
                        sh "docker save ${IMG_CHAT}:latest  | ssh ${SSH_USER}@${VM_CHAT_NOTIF} 'docker load'"
                        sh "docker save ${IMG_NOTIF}:latest | ssh ${SSH_USER}@${VM_CHAT_NOTIF} 'docker load'"
                    }
                }
                stage('vm-invoice') {
                    steps {
                        echo 'Envoi image rent-receipt vers vm-invoice...'
                        sh "docker save ${IMG_INVOICE}:latest | ssh ${SSH_USER}@${VM_INVOICE} 'docker load'"
                    }
                }
                stage('vm-front') {
                    steps {
                        echo 'Envoi images frontend et bff vers vm-front...'
                        sh "docker save ${IMG_BFF}:latest   | ssh ${SSH_USER}@${VM_FRONT} 'docker load'"
                        sh "docker save ${IMG_FRONT}:latest | ssh ${SSH_USER}@${VM_FRONT} 'docker load'"
                    }
                }
            }
        }

        stage('Restart Services') {
            parallel {
                stage('Restart vm-auth') {
                    steps {
                        echo 'Redemarrage authentification sur vm-auth...'
                        sh """
                            ssh ${SSH_USER}@${VM_AUTH} '
                                cd ~/jsahome &&
                                docker compose stop authentification &&
                                docker compose up -d authentification
                            '
                        """
                    }
                }
                stage('Restart vm-chat-notif') {
                    steps {
                        echo 'Redemarrage chat et notification sur vm-chat-notif...'
                        sh """
                            ssh ${SSH_USER}@${VM_CHAT_NOTIF} '
                                cd ~/jsahome &&
                                docker compose stop chat notification &&
                                docker compose up -d chat notification
                            '
                        """
                    }
                }
                stage('Restart vm-invoice') {
                    steps {
                        echo 'Redemarrage rent-receipt sur vm-invoice...'
                        sh """
                            ssh ${SSH_USER}@${VM_INVOICE} '
                                cd ~/jsahome &&
                                docker compose stop rent-receipt &&
                                docker compose up -d rent-receipt
                            '
                        """
                    }
                }
                stage('Restart vm-front') {
                    steps {
                        echo 'Redemarrage frontend et bff sur vm-front...'
                        sh """
                            ssh ${SSH_USER}@${VM_FRONT} '
                                cd ~/jsahome &&
                                docker compose stop frontend backend-for-frontend &&
                                docker compose up -d frontend backend-for-frontend traefik
                            '
                        """
                    }
                }
            }
        }
    }

    post {
        success {
            echo 'Deploiement JSAHome termine avec succes. Acces : http://172.31.252.169'
        }
        failure {
            echo 'Pipeline echoue. Consulte les logs ci-dessus.'
        }
    }
}