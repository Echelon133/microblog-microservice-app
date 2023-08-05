# Microblog app

A microblogging platform composed of multiple containerized services.

![A diagram showing the architecture of the project](https://github.com/Echelon133/microblog-microservice-app/blob/master/arch-diagram.png)

## API

| HTTP Method | Endpoint                          | Link to documentation                                                                       |
|-------------|-----------------------------------|---------------------------------------------------------------------------------------------|
| GET         | /login                            | [Docs](https://github.com/Echelon133/microblog-microservice-app/blob/master/auth/README.md) |
| GET         | /oauth2/authorize                 | [Docs](https://github.com/Echelon133/microblog-microservice-app/blob/master/auth/README.md)         |
| POST        | /oauth2/token                     | [Docs](https://github.com/Echelon133/microblog-microservice-app/blob/master/auth/README.md)         |
| POST        | /oauth2/introspect                | [Docs](https://github.com/Echelon133/microblog-microservice-app/blob/master/auth/README.md)         |
| POST        | /api/users/register               | [Docs](https://github.com/Echelon133/microblog-microservice-app/blob/master/user/README.md)         |
| GET         | /api/users                        | [Docs](https://github.com/Echelon133/microblog-microservice-app/blob/master/user/README.md)         |
| GET         | /api/users/me                     | [Docs](https://github.com/Echelon133/microblog-microservice-app/blob/master/user/README.md)         |
| PATCH       | /api/users/me                     | [Docs](https://github.com/Echelon133/microblog-microservice-app/blob/master/user/README.md)         |
| GET         | /api/users/{id}                   | [Docs](https://github.com/Echelon133/microblog-microservice-app/blob/master/user/README.md)         |
| GET         | /api/users/{id}/profile-counters  | [Docs](https://github.com/Echelon133/microblog-microservice-app/blob/master/user/README.md)         |
| GET         | /api/users/{id}/follow            | [Docs](https://github.com/Echelon133/microblog-microservice-app/blob/master/user/README.md)         |
| POST        | /api/users/{id}/follow            | [Docs](https://github.com/Echelon133/microblog-microservice-app/blob/master/user/README.md)         |
| DELETE      | /api/users/{id}/follow            | [Docs](https://github.com/Echelon133/microblog-microservice-app/blob/master/user/README.md)         |
| GET         | /api/users/{id}/followers         | [Docs](https://github.com/Echelon133/microblog-microservice-app/blob/master/user/README.md)         |
| GET         | /api/users/{id}/following         | [Docs](https://github.com/Echelon133/microblog-microservice-app/blob/master/user/README.md)         |
| GET         | /api/posts/{id}                   | [Docs](https://github.com/Echelon133/microblog-microservice-app/blob/master/post/README.md)         |
| GET         | /api/posts/{id}/responses         | [Docs](https://github.com/Echelon133/microblog-microservice-app/blob/master/post/README.md)         |
| GET         | /api/posts/{id}/quotes            | [Docs](https://github.com/Echelon133/microblog-microservice-app/blob/master/post/README.md)         |
| GET         | /api/posts/{id}/post-counters     | [Docs](https://github.com/Echelon133/microblog-microservice-app/blob/master/post/README.md)         |
| GET         | /api/posts                        | [Docs](https://github.com/Echelon133/microblog-microservice-app/blob/master/post/README.md)         |
| POST        | /api/posts                        | [Docs](https://github.com/Echelon133/microblog-microservice-app/blob/master/post/README.md)         |
| POST        | /api/posts/{id}/responses         | [Docs](https://github.com/Echelon133/microblog-microservice-app/blob/master/post/README.md)         |
| POST        | /api/posts/{id}/quotes            | [Docs](https://github.com/Echelon133/microblog-microservice-app/blob/master/post/README.md)         |
| DELETE      | /api/posts/{id}                   | [Docs](https://github.com/Echelon133/microblog-microservice-app/blob/master/post/README.md)         |
| GET         | /api/posts/{id}/like              | [Docs](https://github.com/Echelon133/microblog-microservice-app/blob/master/post/README.md)         |
| POST        | /api/posts/{id}/like              | [Docs](https://github.com/Echelon133/microblog-microservice-app/blob/master/post/README.md)         |
| DELETE      | /api/posts/{id}/like              | [Docs](https://github.com/Echelon133/microblog-microservice-app/blob/master/post/README.md)         |
| GET         | /api/feed                         | [Docs](https://github.com/Echelon133/microblog-microservice-app/blob/master/post/README.md)         |
| POST        | /api/posts/{id}/report            | [Docs](https://github.com/Echelon133/microblog-microservice-app/blob/master/post/README.md)         |
| GET         | /api/tags/{name}/posts            | [Docs](https://github.com/Echelon133/microblog-microservice-app/blob/master/post/README.md)         |
| GET         | /api/tags/popular                 | [Docs](https://github.com/Echelon133/microblog-microservice-app/blob/master/post/README.md)         |
| GET         | /api/notifications                | [Docs](https://github.com/Echelon133/microblog-microservice-app/blob/master/notification/README.md) |
| GET         | /api/notifications/unread-counter | [Docs](https://github.com/Echelon133/microblog-microservice-app/blob/master/notification/README.md) |
| POST        | /api/notifications/read-all       | [Docs](https://github.com/Echelon133/microblog-microservice-app/blob/master/notification/README.md) |
| POST        | /api/notifications/{id}/read      | [Docs](https://github.com/Echelon133/microblog-microservice-app/blob/master/notification/README.md) |
| GET         | /api/reports                      | [Docs](https://github.com/Echelon133/microblog-microservice-app/blob/master/report/README.md)       |
| POST        | /api/reports/{id}                 | [Docs](https://github.com/Echelon133/microblog-microservice-app/blob/master/report/README.md)       |


## Components of the application

### auth

Configures a public client which uses the **Authorization Code Flow with PKCE** during the process of granting 
access tokens to users.

### gateway

API gateway of the application.

### user

Implements:

* creation of accounts for new users
* searching for users by their username (exact or partial)
* fetching/updating user's profile information
* following and unfollowing users
* sending notifications to users when they are followed
* fetching lists of follows/followers of users
* fetching a list of known followers (i.e. "this user is followed by these people that you follow")

### post

Implements:

* creating and deleting posts/quotes/responses
* liking/unliking posts
* fetching information about how many likes/responses/quotes a post has
* tagging posts
* sending notifications to users when they are being mentioned/quoted/responded to
* fetching the most popular tags
* fetching contents of tags
* fetching post's quotes and responses
* fetching the feed for anonymous or authenticated users
* reporting content which violates rules

### notification

Implements:

* fetching the most recent notifications of a user
* fetching the number of the unread notifications
* marking a single notification as 'read'
* marking all notifications as 'read'

### report

Implements:

* fetching reports based on their status (checked/unchecked)
* accepting or rejecting reports (accepting a report results in the deletion of the reported post)

## Initializing the development version of the application

Jenkins Credentials required to execute the entire pipeline:

| Credential ID                | Credential Type   | Credential Description                                                                                                                                                                                         |
|------------------------------|-------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| dockerhub-credentials        | Username+Password | Username and password of the user whose dockerhub repository will host the built images.                                                                                                                       |
| echelon133-credentials       | Secret Text       | Static token which is bound to some user who has the privileges required to manage the cluster. [Static token file](https://kubernetes.io/docs/reference/access-authn-authz/authentication/#static-token-file) |
| user-postgres-secret         | Secret File       | .env file containing **POSTGRES_DB**, **POSTGRES_USER**, **POSTGRES_PASSWORD** for the database owned by the user service.                                                                                                 |
| post-postgres-secret         | Secret File       | .env file containing **POSTGRES_DB**, **POSTGRES_USER**, **POSTGRES_PASSWORD** for the database owned by the post service.                                                                                                 |
| notification-postgres-secret | Secret File       | .env file containing **POSTGRES_DB**, **POSTGRES_USER**, **POSTGRES_PASSWORD** for the database owned by the notification service.                                                                                         |
| report-postgres-secret       | Secret File       | .env file containing **POSTGRES_DB**, **POSTGRES_USER**, **POSTGRES_PASSWORD** for the database owned by the report service.                                                                                               |
| redis-auth-secret            | Secret File       | .env file containing **REQUIREPASS** for the Redis auth token storage owned by the auth service.                                                                                                                   |
| queue-secret                 | Secret File       | .env file containing **REQUIREPASS** for the Redis queue that is used by the services to communicate between each other.                                                                                           |
| confidential-client-secret   | Secret File       | .env file containing **CLIENT_ID**, **CLIENT_SECRET** for these services which need to use OAuth2 while sending HTTP requests to other services.                                                                       |

When Jenkins finishes the build successfully, these should be the expected results:

* All services have been built
* All services have been tested
* *The Docker image of gateway has been built and pushed to dockerhub
* *The Docker image of user has been built and pushed to dockerhub
* *The Docker image of post has been built and pushed to dockerhub
* *The Docker image of auth has been built and pushed to dockerhub
* *The Docker image of notification has been built and pushed to dockerhub
* *The Docker image of report has been built and pushed to dockerhub
* Cluster's namespaces and permissions have been configured 
* Cluster's secrets required by the services have been created 
* Cluster's resources have been created/updated by applying all .yml configuration files from the *k8s* folder

**Step is skipped if dockerhub already hosts that version of the service*
