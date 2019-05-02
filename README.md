
# Thread Weaver

## デプロイ方法

minikubeにデプロイします。

```bash
$ minikube start
$ eval $(minikube docker-env)
$ sbt docker:publishLocal
$ kubectl create -f k8s/simple-akka-cluster-rbac.yml
$ kubectl create -f k8s/simple-akka-cluster-deployment.yml
$ kubectl create -f k8s/simple-akka-cluster-service.yml
```

## 動作確認方法

### minikubeでの動作確認方法

```bash
$ KUBE_IP=$(minikube ip)
$ MANAGEMENT_PORT=$(kubectl get svc thread-weaver-api -ojsonpath="{.spec.ports[?(@.name==\"management\")].nodePort}")
$ curl http://$KUBE_IP:$MANAGEMENT_PORT/cluster/members | jq
$ API_PORT=$(kubectl get svc thread-weaver-api -ojsonpath="{.spec.ports[?(@.name==\"api\")].nodePort}")
$ curl http://$KUBE_IP:$API_PORT/
```# thread-weaver
