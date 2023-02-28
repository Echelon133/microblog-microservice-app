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

# build all jars
./gradlew build

# build docker images using Dockerfiles
docker build --tag=user:0.0.1-SNAPSHOT ./user
docker build --tag=gateway:0.0.1-SNAPSHOT ./gateway
docker build --tag=auth:0.0.1-SNAPSHOT ./auth

echo "DONE"

echo "---------------------------------------------"
echo "    4. Apply K8s configuration files"
echo "---------------------------------------------"
# create namespace and permissions first
kubectl apply -f k8s/namespace.yml
kubectl apply -f k8s/permissions.yml

kubectl create secret generic user-postgres-secret --from-env-file=k8s/user/postgres-secret.env -n microblog-app
kubectl apply -f k8s/gateway/
kubectl apply -f k8s/user/
kubectl apply -f k8s/auth/

echo "DONE"

echo "---------------------------------------------"
echo "    5. Show all pods in the namespace"
echo "---------------------------------------------"
kubectl get pods -n microblog-app