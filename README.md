
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

## TODO

- [x] Threadの実装
- [x] Thread集約アクター（永続化対応）の実装
- [x] Thread集約アクター（非永続化対応）の実装
- [x] Thread用メッセージブローカーの実装
- [x] Thread用シャーディングアクターの実装
- [x] Thread用コントローラの実装(コマンド側)
- [x] Thread用リードモデルアップデータの実装
- [ ] Thread用コントローラの実装(クエリ側)
- [ ] Sagaの実装

## セットアップ

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
$ THREAD_WEAVER_API_SERVER_HTTP_PORT=18080 THREAD_WEAVER_MANAGEMENT_PORT=8558 sbt
> api-server/run
```

```sh
# terminal 4
$ THREAD_WEAVER_API_SERVER_HTTP_PORT=18081 THREAD_WEAVER_MANAGEMENT_PORT=8559 sbt
> api-server/run
```

```sh
# terminal 5
$ THREAD_WEAVER_API_SERVER_HTTP_PORT=18082 THREAD_WEAVER_MANAGEMENT_PORT=8560 sbt
> api-server/run
```

```sh
# terminal 6
$ curl -X POST "http://127.0.0.1:18080/v1/threads/create" -H "accept: application/json" -H "Content-Type: application/json" -d "{\"accountId\":\"01DB5QXD4NP0XQTV92K42B3XBF\",\"title\":\"string\",\"remarks\":\"string\",\"administratorIds\":[\"01DB5QXD4NP0XQTV92K42B3XBF\"],\"memberIds\":[\"01DB5QXD4NP0XQTV92K42B3XBF\"],\"createAt\":10000}"
{"threadId":"01DB6VK6E7PTQQFYJ6NMMEMTEB","error_messages":[]}%

$ curl -X GET "http://127.0.0.1:18080/v1/threads/01DB6VK6E7PTQQFYJ6NMMEMTEB?account_id=01DB5QXD4NP0XQTV92K42B3XBF" -H "accept: application/json"
{"result":{"id":"01DB6VK6E7PTQQFYJ6NMMEMTEB","creatorId":"01DB5QXD4NP0XQTV92K42B3XBF","parentThreadId":null,"title":"string","remarks":"string","createdAt":10000,"updatedAt":10000},"error_messages":[]}%
```

## デプロイ方法

minikubeにデプロイします。

```sh
$ minikube start --vm-driver hyperkit --cpus 6 --memory 4096 --disk-size 60g
$ eval $(minikube docker-env)
$ sbt api-server/docker:publishLocal
$ kubectl create namespace thread-weaver
$ kubectl create serviceaccount thread-weaver
$ kubectl create -f k8s/rbac.yml --namespace thread-weaver
$ kubectl create -f k8s/deployment.yml --namespace thread-weaver
$ kubectl create -f k8s/service.yml --namespace thread-weaver
```

## 動作確認方法

### minikubeでの動作確認方法

```sh
$ KUBE_IP=$(minikube ip)
$ MANAGEMENT_PORT=$(kubectl get svc thread-weaver-api-server -n thread-weaver -ojsonpath="{.spec.ports[?(@.name==\"management\")].nodePort}")
$ curl http://$KUBE_IP:$MANAGEMENT_PORT/cluster/members | jq
$ API_PORT=$(kubectl get svc thread-weaver-api-server -n thread-weaver -ojsonpath="{.spec.ports[?(@.name==\"api\")].nodePort}")
$ curl http://$KUBE_IP:$API_PORT/
$ curl -X POST "http://$KUBE_IP:$API_PORT/v1/threads/create" -H "accept: application/json" -H "Content-Type: application/json" -d "{\"accountId\":\"01DB5QXD4NP0XQTV92K42B3XBF\",\"title\":\"string\",\"remarks\":\"string\",\"administratorIds\":[\"01DB5QXD4NP0XQTV92K42B3XBF\"],\"memberIds\":[\"01DB5QXD4NP0XQTV92K42B3XBF\"],\"createAt\":10000}"
```

### sbtでの動作確認

```sh
$ sbt api/run
```

## EKS

### AWS環境の構築


### ECRへのpush 

```sh
$ AWS_DEFUALT_PROFILE=xxxxx sbt api-server/ecr:push
```

### Auroa接続確認

```sh
$ cd eks/terraform
# viなどでaurora_public_access を trueにする
$ terraform plan
$ terraform apply
$ mysql -u thread_weaver -p -h $AURORA_ENDPOINT thread_weaver
```
