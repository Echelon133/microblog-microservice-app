# user service

This service implements features such as:

* creation of accounts for new users
* searching for users by their username (exact or partial)
* fetching/updating user's profile information
* following and unfollowing users
* sending notifications to users when they are followed
* fetching lists of follows/followers of users
* fetching a list of known followers (i.e. "this user is followed by these people that you follow")

## user API

### User Creation 

<details>
<summary><code>POST</code> <code><b>/api/users/register</b></code> <code>(create a new user)</code></summary>

##### Required OAuth2 Scopes

N/A

##### Query Parameters

N/A

##### Body 

```json
{
  "username":"testuser",
  "email":"testuser@gmail.com",
  "password":"Testpassword123;",
  "password2":"Testpassword123;"
}
```

Requirements:

* username should be between 1 and 30 characters long, consisting of [a-zA-Z0-9] characters
* password should be between 8 and 64 characters long, consisting of at least one a-z, one A-Z, one 0-9, and one special character from the [owasp list](https://owasp.org/www-community/password-special-characters)
* username cannot be taken
* passwords have to match

##### Example Responses

| Http Code | Response                                                             | Reason                        |
|-----------|----------------------------------------------------------------------|-------------------------------|
| `200`     | `{"uuid":"9e47825a-4a34-46d8-b191-fa981d0714cb"}`                    | Body of the request is valid  |
| `422`     | `{"messages":["Payload with new user data not provided"]}`           | Body of the request was empty |
| `422`     | `{"messages":["Passwords do not match"]}`                            | Passwords do not match        |
| `422`     | `{"messages":["User with username testuser already exists"]}`        | Username is already taken     |
| `422`     | `{"messages":["Email is not valid"]}`                                | Email failed validation       |
| `422`     | `{"messages":["Email is required"]}`                                 | Email field is missing        |
| `422`     | `{"messages":["Username is not valid"]}`                             | Username failed validation    |
| `422`     | `{"messages":["Password does not satisfy complexity requirements"]}` | User's password is too simple |

</details>

### User Info

<details>
<summary><code>GET</code> <code><b>/api/users</b></code> <code>(fetch users by their partial or exact username)</code></summary>

##### Required OAuth2 Scopes

N/A

##### Query Parameters

| Name              | Type                                                     | Data type | Description                                               |
|-------------------|----------------------------------------------------------|-----------|-----------------------------------------------------------|
| username_contains | required (but mutually exclusive with username_exact)    | text      | Partial phrase which has to occur in every found username |
| username_exact    | required (but mutually exclusive with username_contains) | text      | Exact username to find                                    |
| page              | optional                                                 | integer   | Number of the page to fetch                               |

##### Body

N/A

##### Example Responses

| Http Code | Response                                                                                                                                                    | Reason                                   |
|-----------|-------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------|
| `200`     | page containing `{"content":[{"id":"188967d5-d165-4de4-bc60-cba0910bd5de","username":"testuser","displayedName":"testuser","aviUrl":"","description":""}]}` | Request valid                            |
| `400`     | `{"messages":["either 'username_contains' or 'username_exact' request param is required"]}`                                                                 | Neither request parameter provided       |
| `400`     | `{"messages":["only one of 'username_contains' or 'username_exact' request params can be provided at a time"]}`                                             | Both request parameters provided at once |

</details>

<details>
<summary><code>GET</code> <code><b>/api/users/me</b></code> <code>(fetch info about the owner of the Bearer token)</code></summary>

##### Required OAuth2 Scopes

* user.read

##### Query Parameters

N/A

##### Body

N/A

##### Example Responses

| Http Code | Response                                                                                                                      | Reason                                                 |
|-----------|-------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------|
| `200`     | `{"id":"188967d5-d165-4de4-bc60-cba0910bd5de","username":"testuser","displayedName":"testuser","aviUrl":"","description":""}` | Request valid                                          |
| `404`     | `{"messages":["User with id 188967d5-d165-4de4-bc60-cba0910bd5df could not be found"]}`                                       | User does not exists                                   |
| `401`     |                                                                                                                               | Bearer token not provided or lacks the required scopes |


</details>

<details>
<summary><code>PATCH</code> <code><b>/api/users/me</b></code> <code>(update info about the owner of the Bearer token)</code></summary>

##### Required OAuth2 Scopes

* user.read
* user.write

##### Query Parameters

N/A

##### Body

```json
{
  "displayedName": "new displayed name",
  "aviUrl": "http://example.com",
  "description": "new description"
}
```

Requirements:

* displayedName is optional, when provided cannot be longer than 40 characters
* aviUrl is optional, when provided cannot be longer than 200 characters
* description is optional, when provided cannot be longer than 300 characters

##### Example Responses

| Http Code | Response                                                                                                                       | Reason                                                 |
|-----------|--------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------|
| `200`     | `{"id":"188967d5-d165-4de4-bc60-cba0910bd5de","username":"testuser","displayedName":"some name","aviUrl":"","description":""}` | Update successful                                      |
| `422`     | `{"messages":["Field 'displayedName' cannot be longer than 40 characters"]}`                                                   | Field 'displayedName' invalid                          |
| `422`     | `{"messages":["Field 'aviUrl' cannot be longer than 200 characters"]}`                                                         | Field 'aviUrl' invalid                                 |
| `422`     | `{"messages":["Field 'description' cannot be longer than 300 characters"]}`                                                    | Field 'description' invalid                            |
| `404`     | `{"messages":["User with id 188967d5-d165-4de4-bc60-cba0910bd5df could not be found"]}`                                        | User does not exists                                   |
| `401`     |                                                                                                                                | Bearer token not provided or lacks the required scopes |

</details>

<details>
<summary><code>GET</code> <code><b>/api/users/{id}</b></code> <code>(fetch a user with a specified id)</code></summary>

##### Required OAuth2 Scopes

* user.read

##### Query Parameters

N/A

##### Body

N/A

##### Example Responses

| Http Code | Response                                                                                                              | Reason                                                 |
|-----------|-----------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------|
| `200`     | `{"id":"188967d5-d165-4de4-bc60-cba0910bd5de","username":"testuser","displayedName":"","aviUrl":"","description":""}` | User exists                                            |
| `404`     | `{"messages":["User with id 188967d5-d165-4de4-bc60-cba0910bd5df could not be found"]}`                               | User does not exists                                   |
| `401`     |                                                                                                                       | Bearer token not provided or lacks the required scopes |

</details>

### Follows Info

<details>
<summary><code>GET</code> <code><b>/api/users/{id}/profile-counters</b></code> <code>(fetch follows/followers counters of a user with specified id)</code></summary>

##### Required OAuth2 Scopes

* user.read
* follow.read

##### Query Parameters

N/A

##### Body

N/A

##### Example Responses

| Http Code | Response                                                                                | Reason                                                 |
|-----------|-----------------------------------------------------------------------------------------|--------------------------------------------------------|
| `200`     | `{"following": 0, "followers": 0 }`                                                     | Request valid                                          |
| `404`     | `{"messages":["User with id 188967d5-d165-4de4-bc60-cba0910bd5df could not be found"]}` | User does not exists                                   |
| `401`     |                                                                                         | Bearer token not provided or lacks the required scopes |

</details>

<details>
<summary><code>GET</code> <code><b>/api/users/{id}/follow</b></code> <code>(check if the owner of the Bearer token follows a user with specified id)</code></summary>

##### Required OAuth2 Scopes

* user.read
* follow.read

##### Query Parameters

N/A

##### Body

N/A

##### Example Responses

| Http Code | Response                                                                                | Reason                                                 |
|-----------|-----------------------------------------------------------------------------------------|--------------------------------------------------------|
| `200`     | `{"follows": false }`                                                                   | Request valid                                          |
| `404`     | `{"messages":["User with id 188967d5-d165-4de4-bc60-cba0910bd5df could not be found"]}` | User does not exists                                   |
| `401`     |                                                                                         | Bearer token not provided or lacks the required scopes |

</details>

<details>
<summary><code>POST</code> <code><b>/api/users/{id}/follow</b></code> <code>(follow a user with specified id as the owner of the Bearer token)</code></summary>

##### Required OAuth2 Scopes

* user.read
* follow.read
* follow.write

##### Query Parameters

N/A

##### Body

N/A

##### Example Responses

| Http Code | Response                                                                                | Reason                                                 |
|-----------|-----------------------------------------------------------------------------------------|--------------------------------------------------------|
| `200`     | `{"follows": true }`                                                                    | Request valid                                          |
| `404`     | `{"messages":["User with id 188967d5-d165-4de4-bc60-cba0910bd5df could not be found"]}` | User does not exists                                   |
| `401`     |                                                                                         | Bearer token not provided or lacks the required scopes |

</details>

<details>
<summary><code>DELETE</code> <code><b>/api/users/{id}/follow</b></code> <code>(unfollow a user with specified id as the owner of the Bearer token)</code></summary>

##### Required OAuth2 Scopes

* user.read
* follow.read
* follow.write

##### Query Parameters

N/A

##### Body

N/A

##### Example Responses

| Http Code | Response              | Reason                                                 |
|-----------|-----------------------|--------------------------------------------------------|
| `200`     | `{"follows": false }` | Request valid                                          |
| `401`     |                       | Bearer token not provided or lacks the required scopes |

</details>

<details>
<summary><code>GET</code> <code><b>/api/users/{id}/followers</b></code> <code>(fetch who follows a user with specified id)</code></summary>

##### Required OAuth2 Scopes

* user.read
* follow.read

##### Query Parameters

| Name  | Type     | Data type | Description                                                                                                                                                                    |
|-------|----------|-----------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| known | optional | boolean   | Whether only followers who are known to the owner of the Bearer token should be fetched, i.e. "fetch only users who are following some user, and are also being followed by me |
| page  | optional | integer   | Number of the page to fetch                                                                                                                                                    |
##### Body

N/A

##### Example Responses

| Http Code | Response                                                                                                                                                    | Reason                                                 |
|-----------|-------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------|
| `200`     | page containing `{"content":[{"id":"188967d5-d165-4de4-bc60-cba0910bd5de","username":"testuser","displayedName":"testuser","aviUrl":"","description":""}]}` | Request valid                                          |
| `404`     | `{"messages":["User with id 188967d5-d165-4de4-bc60-cba0910bd5df could not be found"]}`                                                                     | User does not exists                                   |
| `401`     |                                                                                                                                                             | Bearer token not provided or lacks the required scopes |

</details>

<details>
<summary><code>GET</code> <code><b>/api/users/{id}/following</b></code> <code>(fetch who is being followed by a user with specified id)</code></summary>

##### Required OAuth2 Scopes

* user.read
* follow.read

##### Query Parameters

| Name | Type     | Data type | Description                 |
|------|----------|-----------|-----------------------------|
| page | optional | integer   | Number of the page to fetch |

##### Body

N/A

##### Example Responses

| Http Code | Response                                                                                                                                                    | Reason                                                 |
|-----------|-------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------|
| `200`     | page containing `{"content":[{"id":"188967d5-d165-4de4-bc60-cba0910bd5de","username":"testuser","displayedName":"testuser","aviUrl":"","description":""}]}` | Request valid                                          |
| `404`     | `{"messages":["User with id 188967d5-d165-4de4-bc60-cba0910bd5df could not be found"]}`                                                                     | User does not exists                                   |
| `401`     |                                                                                                                                                             | Bearer token not provided or lacks the required scopes |

</details>