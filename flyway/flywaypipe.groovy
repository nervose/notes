import groovy.transform.Field

@Library('nec-pipeline-lib') _

//内部常量
@Field def cloudName= 'kubernetes'
@Field def flywayImage= 'boxfuse/flyway:5.2.1'
@Field def dbsecretName= 'dbsecret'
@Field def versionTableName= 'external_flyway_schema_history'
@Field def gitBranch= 'master'
@Field def gitUrl= 'https://gitee.com/nervose/useflyway.git'
@Field def gitCred= 'giteecred'



// properties([
//   parameters([
//     choice(name: 'cloud',choices: ['nec', 'dingedu'], description: '部署云名称')  ])
// ])

// utils.checkParams(['cloud','env','target'])

def label = "flyway-example-${UUID.randomUUID().toString()}"
podTemplate(
  name: "flyway-example", 
  label: label, 
  cloud: cloudName,
  nodeUsageMode: 'EXCLUSIVE',
  instanceCap: 1,
  yaml: """
apiVersion: v1
kind: Pod
metadata:
  labels:
    app: flyway-example
spec:
  initContainers:
  containers:
    - name: flyway
      image: ${flywayImage}
      command:
        - sh
        - -c
        - cat
      tty: true
      env:
        - name: FLYWAY_TABLE
          value: ${versionTableName}
        - name: FLYWAY_URL
          valueFrom:
            secretKeyRef:
              name: ${dbsecretName}
              key: databaseurl
        - name: FLYWAY_USER
          valueFrom:
            secretKeyRef:
              name: ${dbsecretName}
              key: username
        - name: FLYWAY_PASSWORD
          valueFrom:
            secretKeyRef:
              name: ${dbsecretName}
              key: password
"""
    ) {
      node (label) {
        stage("execute sql"){
          container('flyway'){
            checkoutRepo branch: gitBranch,credId: gitCred, url: gitUrl
            sh 'flyway baseline'
            sh 'flyway migrate -locations=filesystem:./sql'
          }
        }
      }
    }
