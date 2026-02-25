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
        VM_STORAGE      = '172.31.250.54'

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
                echo 'Récupération du code depuis GitHub...'
                git branch: "${GITHUB_BRANCH}",
                    url: "${GITHUB_REPO}"
                echo ' Code récupéré !'
            }
        }

        stage('Build') {
            parallel {
                stage('Build Authentification') {
                    steps {
                        dir('authentification') {
                            sh 'mvn clean package -DskipTests'
                        }
                    }
                }
                stage('Build Chat') {
                    steps {
                        dir('chat') {
                            sh 'mvn clean package -DskipTests'
                        }
                    }
                }
                stage('Build Notification') {
                    steps {
                        dir('notification') {
                            sh 'mvn clean package -DskipTests'
                        }
                    }
                }
                stage('Build Rent-Receipt') {
                    steps {
                        dir('rent-receipt') {
                            sh 'mvn clean package -DskipTests'
                        }
                    }
                }
                stage('Build BFF') {
                    steps {
                        dir('backend-for-frontend') {
                            sh 'mvn clean package -DskipTests'
                        }
                    }
                }
            }
        }

        stage('Docker Build') {
            steps {
                echo ' Construction des images Docker...'
                sh """
                    docker build -t ${IMG_AUTH}:latest    authentification/
                    docker build -t ${IMG_CHAT}:latest    chat/
                    docker build -t ${IMG_NOTIF}:latest   notification/
                    docker build -t ${IMG_INVOICE}:latest rent-receipt/
                    docker build -t ${IMG_BFF}:latest     backend-for-frontend/
                    docker build -t ${IMG_FRONT}:latest   frontend/
                """
                echo ' Images Docker créées !'
            }
        }

        stage('Transfer Images') {
            parallel {
                stage('→ vm-auth') {
                    steps {
                        sh "docker save ${IMG_AUTH}:latest | ssh ${SSH_USER}@${VM_AUTH} 'docker load'"
                    }
                }
                stage('→ vm-chat-notif') {
                    steps {
                        sh "docker save ${IMG_CHAT}:latest  | ssh ${SSH_USER}@${VM_CHAT_NOTIF} 'docker load'"
                        sh "docker save ${IMG_NOTIF}:latest | ssh ${SSH_USER}@${VM_CHAT_NOTIF} 'docker load'"
                    }
                }
                stage('→ vm-invoice') {
                    steps {
                        sh "docker save ${IMG_INVOICE}:latest | ssh ${SSH_USER}@${VM_INVOICE} 'docker load'"
                    }
                }
                stage('→ vm-front') {
                    steps {
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
            echo 'Déploiement JSAHome terminé ! Accès : http://172.31.252.169'
        }
        failure {
            echo ' Pipeline échoué. Consulte les logs ci-dessus.'
        }
    }
}
