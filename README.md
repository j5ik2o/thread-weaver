
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

## ローカルでのGatlingシミュレーションの動作確認

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

### EKS環境の構築

以下で本番環境を構築する。

```sh
$ cd tools/terraform
tools/terraform $ cp eks.tfvars.default eks.tfvars
tools/terraform $ vi eks.tfvars # 編集する
tools/terraform $ ./terraform-init.sh
tools/terraform $ ./terraform-plan.sh
tools/terraform $ ./terraform-apply.sh
```

生成されると`config/`に`kubeconfig`が生成されます。KUBECONFIGを設定してください。

```sh
$ export KUBECONFIG=$(pwd)/config/kubeconfig_j5ik2o-eks-XXXXX
$ kubectl get pod --all-namespaces                                                                                                                                                                                       ✔  5944  19:45:55
NAMESPACE       NAME                                        READY   STATUS      RESTARTS   AGE
kube-system     aws-node-kffbb                              1/1     Running     0          4h25m
kube-system     aws-node-ngtpz                              1/1     Running     0          4h25m
kube-system     aws-node-phhs7                              1/1     Running     0          4h25m
kube-system     coredns-57df9447f5-8p9rl                    1/1     Running     0          4h28m
kube-system     coredns-57df9447f5-vhw7x                    1/1     Running     0          4h28m
kube-system     kube-proxy-bcnb6                            1/1     Running     0          4h25m
kube-system     kube-proxy-nzh9h                            1/1     Running     0          4h25m
kube-system     kube-proxy-rq4tn                            1/1     Running     0          4h25m
```

### 初期設定

```sh
tools/deploy $ ./k8s-setup.sh
```

ネームスペース,サービスアカウント,RBAC設定,Helm(Tiller)のインストールなどが作られます。


### Auroraのスキーマ作成(flywayの実行)

```sh
$ cd tools/flyway
tools/flyway $ make release # docker build & push
tools/flyway $ cd ../deploy
tools/deploy $ ./deploy-flyway.sh -e prod
```

### Auroraパスワード用Secretを作成

```sh
$ cd tools/deploy/eks
tools/deploy/eks $ cp secret.yaml.default secret.yaml
tools/deploy/eks $ echo -n 'xxxx' | base64 # terraform構築時出力されたパスワードをエンコードする
tools/deploy/eks $ vi secret.yaml # エンコードしたパスワードを設定する
tools/deploy/eks $ kubectl apply -f secret.yaml
```

### ECRへのpush 

```sh
$ AWS_DEFAULT_PROFILE=xxxxx sbt api-server/ecr:push # docker build & push
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

### 負荷試験の実行

```sh
$ AWS_DEFAULT_PROFILE=xxxxx sbt gatling-runner/ecr:push # docker build & push
$ AWS_DEFAULT_PROFILE=xxxxx sbt gatling-aggregate-runner/ecr:push # docker build & push
$ cd tools/gatling-s3-reporter
tools/gatling-s3-reporter $ make release # docker build & push
```


