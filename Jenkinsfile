pipeline {
    agent any
    tools {
        maven 'maven_3_9_9'
        jdk 'JDK_17'
    }
    stages {
        stage('Build Maven') {
            steps {
                checkout([
                    $class: 'GitSCM',
                    branches: [[name: '*/master']],
                    extensions: [],
                    userRemoteConfigs: [[url: 'https://github.com/VenkataKrishnaRepalle/ems-mybatis-liquibase']]
                ])

                bat 'mvn clean install'
            }
        }

        stage('Deploy with Docker Compose') {
            steps {
                script {
                        withCredentials([string(credentialsId: 'DOCKER_PASSWORD', variable: 'docker_password')]) {
                        bat 'docker login -u rvkrishna13052001 -p %docker_password%'
                    }
                    bat 'docker-compose down'
                    bat 'docker-compose up --build -d'
                }
            }
        }
    }

	post {
		success { echo 'Deploy successful.' }
		failure { echo 'Build/Deploy failed. Check logs.' }
		always  { cleanWs() }
	}
}
