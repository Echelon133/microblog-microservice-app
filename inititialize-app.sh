#!/bin/bash

echo "---------------------------------------------"
echo "    1. Delete previous app config"
echo "---------------------------------------------"
kubectl delete all --all -n microblog-app
echo "DONE"

echo "---------------------------------------------"
echo "    2. Prepare for building docker images"
echo "---------------------------------------------"
eval $(minikube docker-env)
echo "DONE"

echo "---------------------------------------------"
echo "    3. Build docker images"
echo "---------------------------------------------"
./gradlew bootBuildImage
echo "DONE"

echo "---------------------------------------------"
echo "    4. Apply K8s configuration files"
echo "---------------------------------------------"
# create namespace and permissions first
kubectl apply -f k8s/namespace.yml
kubectl apply -f k8s/permissions.yml

echo "---------------------------------------------"
echo "    5. Show all pods in the namespace"
echo "---------------------------------------------"
kubectl get pods -n microblog-app