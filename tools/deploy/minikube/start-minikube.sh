#!/usr/bin/env bash

DRIVER="--vm-driver virtualbox"

while getopts d: OPT
do
    case ${OPT} in
        "d") DRIVER="--vm-driver $OPTARG" ;;
    esac
done

minikube start ${DRIVER} --kubernetes-version v1.12.8 --cpus 6 --memory 5000 --disk-size 30g
