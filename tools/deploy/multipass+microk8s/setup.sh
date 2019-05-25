#!/usr/bin/env bash

# https://qiita.com/mumoshu/items/6ff56badcfabe5ab1f49
# https://nnao45.hatenadiary.com/entry/2018/12/20/085706

VM_NAME=microk8s
CPUS=8
MEM=8G
DISK=40G

function get_ip {
    ultipass info mircok8s-vm | grep IPv4 | sed 's/IPv4\://' | xargs
}

# multipass launch --name ${VM_NAME} --mem ${MEM} --disk ${DISK} --cpus ${CPUS}
slepp 3

multipass exec ${VM_NAME} -- sudo snap install microk8s --classic
multipass exec ${VM_NAME} -- sudo iptables -P FORWARD ACCEPT
#multipass exec ${VM_NAME} -- /snap/bin/microk8s.status
#multipass exec ${VM_NAME} -- /snap/bin/microk8s.enable dns dashboard metrics-server registry

#multipass exec ${VM_NAME} -- sudo snap install kubectl --classic
#multipass exec ${VM_NAME} -- sudo apt-get install docker.io
#multipass exec ${VM_NAME} -- sudo addgroup --system docker
#multipass exec ${VM_NAME} -- sudo adduser multipass docker
#multipass exec ${VM_NAME} -- newgrp docker

#multipass exec ${VM_NAME} -- sh -c '/snap/bin/microk8s.config > /home/multipass/.kube/kubeconfig'
#multipass exec ${VM_NAME} -- /snap/bin/microk8s.config > ./microk8s-kubeconfig