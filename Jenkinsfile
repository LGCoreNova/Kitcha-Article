pipeline {
  agent any
  
  tools {
      gradle "gradle8.12.1"
  }
  
  parameters {
      booleanParam(name: 'DOCKER_BUILD', defaultValue: true, description: 'Docker 이미지 빌드 실행 여부')
      string(name: 'DOCKER_IMAGE_TAG', defaultValue: '', description: 'Docker 이미지 태그 (비워두면 빌드 번호 사용)')
  }
  
  stages {
    stage('Gradle Install') {
      agent any
      steps {
        checkout scm
        sh 'gradle clean build -x test'
      }
    }
    
    stage('Docker Image Build') {
      agent any
      when {
        expression { params.DOCKER_BUILD == true }
      }
      steps {
          script {
            def imageTag
            
            if (params.DOCKER_IMAGE_TAG != "") {
                imageTag = params.DOCKER_IMAGE_TAG
            } else {
                imageTag = "${env.BRANCH_NAME}-${env.BUILD_NUMBER}"
            }
            
            def ecrTagPrefix = "803691999553.dkr.ecr.us-west-1.amazonaws.com/kitcha/article"

            def deployTag = "latest"
            if (env.BRANCH_NAME != "main") {
                deployTag = env.BRANCH_NAME
            }

            sshPublisher(publishers: [
                sshPublisherDesc(
                    configName: 'toy-docker-server',
                    transfers: [sshTransfer(
                        cleanRemote: false,
                        excludes: '',
                        execCommand: """
                            cd kitcha/article
                            aws ecr get-login-password --region us-west-1 | docker login --username AWS --password-stdin 803691999553.dkr.ecr.us-west-1.amazonaws.com
                            docker build --tag kitcha/article:${imageTag} -f Dockerfile .
                            docker tag kitcha/article:${imageTag} ${ecrTagPrefix}:${imageTag}

                            docker tag kitcha/article:${imageTag} ${ecrTagPrefix}:${deployTag}
                            docker push ${ecrTagPrefix}:${deployTag}
                        """,
                        execTimeout: 600000,
                        flatten: false,
                        makeEmptyDirs: false,
                        noDefaultExcludes: false,
                        patternSeparator: '[, ]+',
                        remoteDirectory: './kitcha/article',
                        remoteDirectorySDF: false,
                        removePrefix: 'build/libs',
                        sourceFiles: 'build/libs/*.jar'
                    )],
                    usePromotionTimestamp: false,
                    useWorkspaceInPromotion: false,
                    verbose: true
                )
            ])
          }
      }
    }
    
    stage('Deploy to Development') {
      agent any
      when {
        expression { 
          return env.BRANCH_NAME.startsWith('feat-') || env.BRANCH_NAME == 'develop'
        }
      }
      steps {
        echo "개발 환경에 배포 중: ${env.BRANCH_NAME} 브랜치"
        // 개발 환경 배포 명령 추가
      }
    }
    
    stage('Deploy to Production') {
      agent any
      when {
        expression { 
          return env.BRANCH_NAME == 'main'
        }
      }
      steps {
        echo "프로덕션 환경에 배포 중: main 브랜치"
        sshPublisher(publishers: [
          sshPublisherDesc(
            configName: 'toy-docker-server',
            transfers: [sshTransfer(
              cleanRemote: false,
              excludes: '',
              execCommand: """
                cd kitcha/article
                # ECS 배포 스크립트 실행
                aws ecs describe-task-definition --task-definition kitcha-article --output json > task-article-definition.json
                jq '{family: .taskDefinition.family, networkMode: .taskDefinition.networkMode, containerDefinitions: .taskDefinition.containerDefinitions, requiresCompatibilities: .taskDefinition.requiresCompatibilities, cpu: .taskDefinition.cpu, memory: .taskDefinition.memory, executionRoleArn: .taskDefinition.executionRoleArn, volumes: .taskDefinition.volumes, placementConstraints: .taskDefinition.placementConstraints}' task-article-definition.json > clean-task-article-def.json
                aws ecs register-task-definition --cli-input-json file://clean-task-article-def.json
                aws ecs update-service --cluster LGCNS-Cluster-2 --service kitcha-article-service --task-definition kitcha-article
                echo "ECS 서비스 업데이트 완료: kitcha-article-service"
              """,
              execTimeout: 300000,
              flatten: false,
              makeEmptyDirs: false,
              noDefaultExcludes: false,
              patternSeparator: '[, ]+',
              remoteDirectory: '',
              remoteDirectorySDF: false,
              removePrefix: '',
              sourceFiles: ''
            )],
            usePromotionTimestamp: false,
            useWorkspaceInPromotion: false,
            verbose: true
          )
        ])
      }
    }
  }
  
  
  post {
    success {
      echo "빌드 및 배포 성공! 브랜치: ${env.BRANCH_NAME}"
    }
    failure {
      echo "빌드 또는 배포 실패! 브랜치: ${env.BRANCH_NAME}"
    }
    always {
      cleanWs()
    }
  }
}