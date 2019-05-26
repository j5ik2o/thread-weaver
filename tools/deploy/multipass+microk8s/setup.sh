#!/usr/bin/env bash

# https://qiita.com/mumoshu/items/6ff56badcfabe5ab1f49
# https://nnao45.hatenadiary.com/entry/2018/12/20/085706

VM_NAME=microk8s
CPUS=8
MEM=8G
DISK=40G
K8S_VERSION=1.14/stable

root_exec() {
	multipass exec ${VM_NAME} -- sudo sh -c "$@"
}

do_exec() {
	multipass exec ${VM_NAME} -- "$@"
}

install_microk8s() {
    root_exec "snap install microk8s --classic --channel=${K8S_VERSION}"
    root_exec 'iptables -P FORWARD ACCEPT'
    root_exec 'while [ ! $(/snap/bin/microk8s.status > /dev/null; echo $?) -eq 0 ]; do echo -n .; sleep 1; done'
    root_exec '/snap/bin/microk8s.enable dns dashboard metrics-server registry'
    root_exec '/snap/bin/microk8s.kubectl cluster-info'
}

install_kubectl() {
    root_exec 'snap install kubectl --classic'
    root_exec '/snap/bin/microk8s.config > /home/multipass/.kube/kubeconfig'
}


install_docker() {
    root_exec 'apt-get -y update'
    root_exec 'apt-get install -y docker.io'
    root_exec 'addgroup --system docker'
    root_exec 'adduser multipass docker'
    root_exec 'newgrp docker'

    CONFIG="{ "insecure-registries" : ["$(get_ip):32000"] }"
    root_exec "echo ${CONFIG} > /etc/docker/daemon.json"
}

function get_ip() {
    multipass info ${VM_NAME} | grep IPv4 | sed 's/IPv4\://' | xargs
}

multipass launch --name ${VM_NAME} --mem ${MEM} --disk ${DISK} --cpus ${CPUS}
sleep 1
install_microk8s
install_kubectl
# install_docker

do_exec '/snap/bin/microk8s.config' > ./microk8s-kubeconfig


