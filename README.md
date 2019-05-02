
# Thread Weaver

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


## デプロイ方法

minikubeにデプロイします。

```sh
$ minikube start
$ eval $(minikube docker-env)
$ sbt docker:publishLocal
$ kubectl create -f k8s/rbac.yml
$ kubectl create -f k8s/deployment.yml
$ kubectl create -f k8s/service.yml
```

## 動作確認方法

### minikubeでの動作確認方法

```sh
$ KUBE_IP=$(minikube ip)
$ MANAGEMENT_PORT=$(kubectl get svc thread-weaver-api -ojsonpath="{.spec.ports[?(@.name==\"management\")].nodePort}")
$ curl http://$KUBE_IP:$MANAGEMENT_PORT/cluster/members | jq
$ API_PORT=$(kubectl get svc thread-weaver-api -ojsonpath="{.spec.ports[?(@.name==\"api\")].nodePort}")
$ curl http://$KUBE_IP:$API_PORT/
```
