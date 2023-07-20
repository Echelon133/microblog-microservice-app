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

### Configure postgres databases

All directories where **postgres-secret.env** file is required:

* /k8s/auth/
* /k8s/notification/
* /k8s/post/
* /k8s/report/
* /k8s/user/

Each file should contain unique values for keys *POSTGRES_DB*, *POSTGRES_USER* and *POSTGRES_PASSWORD*.

Example **/k8s/post/postgres-secret.env**:

```text
POSTGRES_PASSWORD=a0b5ac7a-4176-422f-b01c-8b3192325788
POSTGRES_DB=posts
POSTGRES_USER=8967d670-0f74-4870-80e6-53434a256590
```

### Configure redis databases

Example **/k8s/auth/redis-secret.env**:

```text
REQUIREPASS=30fb973f-829f-4cda-b69a-bb3632dbd472
```

Example **/k8s/queue/queue-secret.env**:

```text
REQUIREPASS=93681d65-f3e5-4212-833e-b7c88d6e244a
```

### Configure the confidential client

The confidential client is required during the process of token introspection. The entity which wants to introspect a token
needs to provide three pieces of information: the access token which is being introspected, the *CLIENT_ID*, and *CLIENT_SECRET*.

Example **/k8s/auth/confidential-client.env**:

```text
CLIENT_ID=305a117f-5a8c-4f3d-ba66-825a2aa09c7a
CLIENT_SECRET=8defcbde-b7cb-4ff8-8d14-4d56d8cda689
```

### Run the initialization script

The initialization script:

* deletes the old configuration of the application (if exists)
* builds a Docker image for each service
* configures the namespace
* configures the permissions of resources
* configures secrets
* applies the configuration of volumes, services, deployments, etc.

```
./initialize-app.sh
```
