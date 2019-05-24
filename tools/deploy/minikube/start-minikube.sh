#!/usr/bin/env bash

while getopts d: OPT
do
    case ${OPT} in
        "d") DRIVER="--vm-driver $OPTARG" ;;
    esac
done

minikube start ${DRIVER} --kubernetes-version v1.12.8 --cpus 8 --memory 10000 --disk-size 30g
