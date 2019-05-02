
# Thread Weaver

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
