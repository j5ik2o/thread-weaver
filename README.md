
# Thread Weaver

このプロジェクトはAWS EKS上で動作する CQRS+Event Sourcing システムです(ローカルではDocker for MacのKubernetes上で動作確認できます)

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
$ brew install kubernetes-cli kubernetes-helm gettext
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


## Kubernetes on Docker for Mac

Docker for MacのKubernetes機能を有効にして、コンテキストを切り替えてください。

```sh
$ cd tools/deploy
tools/deploy $ ./k8s-setup.sh
tools/deploy $ ./k8s-d4m/deploy-local-db.sh -e local
tools/deploy $ ./k8s-d4m/migrate-local-db.sh
tools/deploy $ kubectl apply -f ./k8s-d4m/secrets.yaml
tools/deploy $ ./k8s-d4m/build-image.sh
tools/deploy $ ./deploy-app.sh -e local
```

以下のコマンドで動作確認できます。

```sh
tools/deploy $ ./k8s-d4m/test-application.sh
tools/deploy $ ./k8s-d4m/test-management.sh
```

## EKS環境の構築

### IAM Userの作成とアクセスキーの発行

AWS CONSOLEなどからIAM Userを作成しアクセスキーを発行してください。
アクセス権は面倒なのでAdministratorにしています。

### `~/.aws/credentails`にプロファイルを追加する。

プロファイル名は`thread-weaver`にしてください。

```bash
$ vi ~/.aws/credentials
```

```
[thread-weaver]
aws_access_key_id = XXXXX
aws_secret_access_key = XXXXX
region = ap-northeast-1
```

### EKS環境の構築

以下で本番環境を構築する。
`tfstate`ファイルはS3で管理します。`eks-terraform-env.sh`の`TF_BUCKET_NAME`のバケットを事前に作成してください。

```sh
$ cd tools/terraform
tools/terraform $ cp eks-terraform-env.sh.default eks-terraform-env.sh
tools/terraform $ vi eks-terraform-env.sh # 編集する。TF_BUCKET_NAMEはユニークなものが必要です。
tools/terraform $ cp eks.tfvars.default eks.tfvars
tools/terraform $ vi eks.tfvars # 編集する
tools/terraform $ ./eks-terraform-init.sh
tools/terraform $ ./eks-terraform-plan.sh
tools/terraform $ ./eks-terraform-apply.sh
```

生成されると`config/`に`kubeconfig`が生成されます。KUBECONFIGを設定してください。
`KUBECONFIG`にパスを指定して、pod一覧が取得できるか確認してみてください。

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
tools/deploy $ AWS_PROFILE=thread-weaver ./deploy-flyway.sh -e prod
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
$ AWS_DEFAULT_PROFILE=thread-weaver sbt api-server/ecr:push # docker build & push
```

### アプリケーションのデプロイ

```sh
$ cd tools/deploy
tools/deploy $ AWS_PROFILE=thread-weaver ./deploy-app.sh -e prod
```

### アプリケーションの動作確認

```sh
$ cd tools/deploy
tools/deploy $ ./eks/test-application.sh
tools/deploy $ ./eks/test-management.sh
```

### 負荷試験

####  概要

ECS(Fargate)上にGatlingコンテナを起動してS3にレポートを出力します。

#### Dockerイメージ

- gatling-runner
    - Gatlingシナリオを実行するコンテナ(複数起動される場合があります)。実行後にS3にログを出力します
- gatling-s3-reporter
    - gatling-runnerが出力したログからレポートを出力します
- gatling-aggregate-runner
    - 上記のコンテナを使った一連のタスクを実行するコンテナ。具体的には複数のgatling-runnerの実行→レポート出力までのワークフローを制御します。


#### Dockerイメージの準備

```sh
# Gatling RunnerイメージのBuild & Push
$ AWS_DEFAULT_PROFILE=thread-weaver sbt gatling-runner/ecr:push # docker build & push
# Gatling Aggregate RunnerイメージのBuild & Push
$ AWS_DEFAULT_PROFILE=thread-weaver sbt gatling-aggregate-runner/ecr:push # docker build & push
# Gatling ReporterイメージのBuild & Push
$ cd tools/gatling-s3-reporter
tools/gatling-s3-reporter $ make release # docker build & push
```

#### Gatlingの実行

```sh
# Gatling Aggregate Runnerの設定調整
$ vi project/Settings.scala # gatlingAggregateRunTaskSettings の設定を調整
# Gatling Aggregate Runnerの実行
$ cd ../../
$ AWS_PROFILE=thread-weaver sbt gatling-aggregate-runner/gatling::runTask
```


