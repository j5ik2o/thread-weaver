
# (WIP) Thread Weaver

## ドメイン

- Account
    - 本システムの利用者を識別するアカウント情報
- Thread
    - Messageを交換するための場を示す
- Message
    - 何らかの言語で表現された伝言
- Administrator
    - 当該Threadの管理者
- Member
    - 当該Threadの利用者

## アーキテクチャ

CQRS+ESを採用。実装には以下のakkaのツールキットを利用しています。

- akka-actor
- akka-persistence
- akka-cluster
- akka-cluster-sharding
- akka-cluster-tools

関連するakka-typedモジュールも含まれます。

## セットアップ

- 必須

```sh
$ brew update 
$ brew install kubernetes-cli kubernetes-helm
$ brew cask install docker minikube virtualbox
```

- 任意

```sh
$ brew install hyperkit
$ curl -LO https://storage.googleapis.com/minikube/releases/latest/docker-machine-driver-hyperkit \
&& sudo install -o root -g wheel -m 4755 docker-machine-driver-hyperkit /usr/local/bin/
```
　

## ローカルでの動作確認

```sh
# terminal 1
$ sbt
> local-dynamodb/run
```

```sh
# terminal 2
$ sbt
> local-mysql/run
```

```sh
# terminal 3
$ THREAD_WEAVER_API_SERVER_HTTP_PORT=18080 THREAD_WEAVER_MANAGEMENT_PORT=8558 sbt api-server/run
```

```sh
# terminal 4
$ THREAD_WEAVER_API_SERVER_HTTP_PORT=18081 THREAD_WEAVER_MANAGEMENT_PORT=8559 sbt api-server/run
```

```sh
# terminal 5
$ THREAD_WEAVER_API_SERVER_HTTP_PORT=18082 THREAD_WEAVER_MANAGEMENT_PORT=8560 sbt api-server/run
```

```sh
# terminal 6
$ curl -X POST "http://127.0.0.1:18080/v1/threads/create" -H "accept: application/json" -H "Content-Type: application/json" -d "{\"accountId\":\"01DB5QXD4NP0XQTV92K42B3XBF\",\"title\":\"string\",\"remarks\":\"string\",\"administratorIds\":[\"01DB5QXD4NP0XQTV92K42B3XBF\"],\"memberIds\":[\"01DB5QXD4NP0XQTV92K42B3XBF\"],\"createAt\":10000}"
{"threadId":"01DB6VK6E7PTQQFYJ6NMMEMTEB","error_messages":[]}%

$ curl -X GET "http://127.0.0.1:18080/v1/threads/01DB6VK6E7PTQQFYJ6NMMEMTEB?account_id=01DB5QXD4NP0XQTV92K42B3XBF" -H "accept: application/json"
{"result":{"id":"01DB6VK6E7PTQQFYJ6NMMEMTEB","creatorId":"01DB5QXD4NP0XQTV92K42B3XBF","parentThreadId":null,"title":"string","remarks":"string","createdAt":10000,"updatedAt":10000},"error_messages":[]}%
```

## Gatling

```sh
$ sbt gatling-test/gatling-it:testOnly com.github.j5ik2o.gatling.ThreadSimulation
```


## minikubeでの動作確認

minikubeにデプロイします。

```sh
$ cd tools/deploy
tools/deploy $ ./minikube/start-minikube.sh
tools/deploy $ ./k8s-setup.sh
# tillerが有効になるまで待つ
tools/deploy $ ./minikube/deploy-local-db.sh
# mysql, dynamodbのpodが立ち上がるまで待つ
tools/deploy $ ./minikube/migrate-local-db.sh
tools/deploy $ kubectl apply -f ./minikube/secret.yaml
tools/deploy $ ./minikube/build-image.sh
tools/deploy $ ./deploy-app.sh
```

```sh
tools/deploy $ ./minikube/test-application.sh
tools/deploy $ ./minikube/test-management.sh
```

## EKS環境の構築

### AWS環境の構築

以下で本番環境を構築する。

```sh
$ cd tools/terraform
tools/terraform $ cp eks.tfvars.default eks.tfvars
tools/terraform $ vi eks.tfvars # 編集する
tools/terraform $ ./terraform-init.sh
tools/terraform $ ./terraform-plan.sh
tools/terraform $ ./terraform-apply.sh
```

などが生成されます

- VPC
- ECR
- DynamoDB
- RDS(Aurora Cluster)

### Auroa接続確認

```sh
$ cd eks/terraform
# viなどでaurora_public_access を trueにする
$ terraform plan
$ terraform apply
$ mysql -u thread_weaver -p -h $AURORA_ENDPOINT thread_weaver
```

### Auroraのスキーマ作成

パブリックアクセス化にしたAuroraに以下のコマンドを実行する

```sh
$ cd tools/deploy/eks
tools/deploy/eks $ cp flyway.conf.default flyway.conf
tools/deploy/eks $ vi flyway.conf # 編集する
tools/deploy/eks $ ./migrate-db.sh
```

### EKSの構築

```sh
tools/terraform $ ./eksctl-create-cluster.sh
```

しばらく待つと構築が完了します。`./eksctl-get-cluster.sh`でクラスター情報を確認できます。

```
$ ./eksctl-get.sh
NAME		VERSION	STATUS	CREATED			VPC			SUBNETS														SECURITYGROUPS
j5ik2o-eks	1.12	ACTIVE	2019-05-24T00:52:22Z	vpc-08b6708f6ecc882a8	subnet-02dac1f21d5a615a5,subnet-043eb530606a93243,subnet-07cf3036da717fc39,subnet-0c925b21299b32e99,subnet-0d95b9b58619a28f0,subnet-0f57e4727746109ef	sg-0dd522f9e95ca4571
```

kubectlから使う場合はコンテキストを切り替えます。

```
$ kubectl config get-contexts # コンテキストの確認
CURRENT   NAME                                             CLUSTER                               AUTHINFO                                         NAMESPACE
*         j5ik2o@j5ik2o-eks.ap-northeast-1.eksctl.io   j5ik2o-eks.ap-northeast-1.eksctl.io   cw_junichi@j5ik2o-eks.ap-northeast-1.eksctl.io
          docker-for-desktop                               docker-for-desktop-cluster            docker-for-desktop
          minikube                                         minikube                              minikube
$ kubectl config use-context j5ik2o@j5ik2o-eks.ap-northeast-1.eksctl.io # コンテキストを切り替える
```

### EKSのセットアップ

以下のコマンドで、helm(tiller)のインストール, ネームスペース、サービスアカウントなどが作成されます。

```sh
$ tools/eks/helm
tools/eks/helm $ kubectl apply -f ./rbac-config.yaml
$ cd tools/deploy
tools/deploy $ ./k8s-setup.sh
```

### Auroraパスワード用Secretを作成

```sh
$ cd tools/deploy/eks
tools/deploy/eks $ cp secret.yaml.default secret.yaml
tools/deploy/eks $ echo -n 'xxxx' | base64 # パスワードをエンコードする
tools/deploy/eks $ vi secret.yaml # エンコードしたパスワードを設定する
tools/deploy/eks $ kubectl apply -f secret.yaml
```

### ECRへのpush 

```sh
$ AWS_DEFUALT_PROFILE=xxxxx sbt api-server/ecr:push
```

### アプリケーションのデプロイ

```sh
$ cd tools/deploy
tools/deploy $ ./deploy-app.sh -e prod
```

### アプリケーションの動作確認

```sh
$ cd tools/deploy
tools/deploy $ ./eks/test-application.sh
tools/deploy $ ./eks/test-management.sh
```
