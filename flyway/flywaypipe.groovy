import groovy.transform.Field

@Library('nec-pipeline-lib') _

//内部常量
@Field def cloudName= 'kubernetes'
@Field def flywayImage= 'boxfuse/flyway:5.2.1'
@Field def versionTableName= 'external_flyway_schema_history'
@Field def gitBranch= 'master'
@Field def gitUrl= 'http://h.quyiyuan.com/scm/~wuxinghua/flywayscripts.git'
@Field def gitCred= 'git-cred'

properties([
        parameters([
                choice(name: 'env',choices: ['dev', 'rls'], description: 'flyway脚本执行环境'),
                string(name: 'version',description:'发布的版本号')
        ])
])
def dbsecretName="${params.env}dbsecret"

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
        if(params.version) assert params.version.matches(~/^([1-9]\d*)(\.(\d+)){2}$/): '版本号无效，参考值：1.1.1'
        def targetFlywayVersion
        dir(params.env){
          targetFlywayVersion=target(params.version)
        }
        sh "flyway baseline migrate -locations=filesystem:./${params.env} ${targetFlywayVersion?"-target=${targetFlywayVersion}":""}"
      }
    }
  }
}

private def target(def versionParam){
  if(!versionParam)return null
  List<String> versions=[]
  String smallMax;
  String currentMax;
  def files=sh(returnStdout: true, script: 'find . -name "*.sql"')

  files.split("\\r?\\n").each {file ->
    (file.trim()=~/^.\/V(.+)__(.*).sql$/).each{
      versions.add(it[1])
    }
  }
  for (int i=0;i<versions.size();i++){
    if(versions[i].startsWith("$versionParam.")&&versionCompare(versions[i],currentMax)>0){
      currentMax=versions[i]
    }else if(!versions[i].startsWith("$versionParam.") && versionCompare(versions[i],versionParam)<0 && versionCompare(versions[i],smallMax)>0){
      smallMax=versions[i]
    }
  }
  return currentMax?currentMax:smallMax
}


private int versionCompare(a,b){
  if(!a)return -1
  if(!b)return 1
  List verA = a.tokenize('.')
  List verB = b.tokenize('.')
  def commonIndices = Math.min(verA.size(), verB.size())

  for (int i = 0; i < commonIndices; ++i) {
    def numA = verA[i].toInteger()
    def numB = verB[i].toInteger()
    if (numA != numB) {
      return numA <=> numB
    }
  }
  verA.size() <=> verB.size()
}
