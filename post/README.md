# post service

This service implements features such as:

* creating and deleting posts/quotes/responses
* liking/unliking posts
* fetching information about how many likes/responses/quotes a post has 
* tagging posts
* sending notifications to users about them being mentioned/quoted/responded to
* fetching the most popular tags
* fetching contents of tags
* fetching post's quotes and responses
* fetching the feed for anonymous or authenticated users
* reporting content which violates rules

## post API

### Post/Quote/Response Management

<details>
<summary><code>GET</code> <code><b>/api/posts/{id}</b></code> <code>(fetch post/quote/response with the specified id)</code></summary>

##### Required OAuth2 Scopes

* post.read

##### Query Parameters

N/A

##### Body

N/A

##### Example Responses

| Http Code | Response                                                                                                                                                                           | Reason                                                 |
|-----------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------|
| `200`     | `{"id":"0fd6d248-9ba1-4ef0-a5e0-ac09add7d894","dateCreated":1681826364537,"content":"test","authorId":"36afeafd-686d-427e-a8e2-66b0b9ad3c47","quotedPost":null,"parentPost":null}` | Request valid                                          |
| `404`     | `{"messages":["Post with id 0fd6d248-9ba1-4ef0-a5e0-ac09add7d894 could not be found"]}`                                                                                            | Post does not exists                                   |
| `401`     |                                                                                                                                                                                    | Bearer token not provided or lacks the required scopes |


</details>

<details>
<summary><code>GET</code> <code><b>/api/posts/{id}/responses</b></code> <code>(fetch responses to a post with the specified id)</code></summary>

##### Required OAuth2 Scopes

* post.read

##### Query Parameters

| Name | Type     | Data type | Description                 |
|------|----------|-----------|-----------------------------|
| page | optional | integer   | Number of the page to fetch |

##### Body

N/A

##### Example Responses

| Http Code | Response                                                                                                                                                                                                                                             | Reason                                                 |
|-----------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------|
| `200`     | page with `{"content":[{"id":"0fd6d248-9ba1-4ef0-a5e0-ac09add7d894","dateCreated":1681826364537,"content":"test content","authorId":"36afeafd-686d-427e-a8e2-66b0b9ad3c47","quotedPost":null,"parentPost":"0fd6d248-9ba1-4ef0-a5e0-ac09add7d894"}]}` | Request valid                                          |
| `404`     | `{"messages":["Post with id 0fd6d248-9ba1-4ef0-a5e0-ac09add7d894 could not be found"]}`                                                                                                                                                              | Post does not exists                                   |
| `401`     |                                                                                                                                                                                                                                                      | Bearer token not provided or lacks the required scopes |

</details>

<details>
<summary><code>GET</code> <code><b>/api/posts/{id}/quotes</b></code> <code>(fetch quotes of a post with the specified id)</code></summary>

##### Required OAuth2 Scopes

* post.read

##### Query Parameters

| Name | Type     | Data type | Description                 |
|------|----------|-----------|-----------------------------|
| page | optional | integer   | Number of the page to fetch |

##### Body

N/A

##### Example Responses

| Http Code | Response                                                                                                                                                                                                                                             | Reason                                                 |
|-----------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------|
| `200`     | page with `{"content":[{"id":"0fd6d248-9ba1-4ef0-a5e0-ac09add7d894","dateCreated":1681826364537,"content":"test content","authorId":"36afeafd-686d-427e-a8e2-66b0b9ad3c47","quotedPost":"0fd6d248-9ba1-4ef0-a5e0-ac09add7d894","parentPost":null}]}` | Request valid                                          |
| `404`     | `{"messages":["Post with id 0fd6d248-9ba1-4ef0-a5e0-ac09add7d894 could not be found"]}`                                                                                                                                                              | Post does not exists                                   |
| `401`     |                                                                                                                                                                                                                                                      | Bearer token not provided or lacks the required scopes |

</details>

<details>
<summary><code>GET</code> <code><b>/api/posts/{id}/post-counters</b></code> <code>(fetch likes/responses/quotes counters of a post with the specified id)</code></summary>

##### Required OAuth2 Scopes

* post.read

##### Query Parameters

N/A

##### Body

N/A

##### Example Responses

| Http Code | Response                                                                                | Reason                                                 |
|-----------|-----------------------------------------------------------------------------------------|--------------------------------------------------------|
| `200`     | `{"likes": 0, "quotes": 0, "responses": 0}`                                             | Request valid                                          |
| `404`     | `{"messages":["Post with id 0fd6d248-9ba1-4ef0-a5e0-ac09add7d894 could not be found"]}` | Post does not exists                                   |
| `401`     |                                                                                         | Bearer token not provided or lacks the required scopes |

</details>

<details>
<summary><code>GET</code> <code><b>/api/posts</b></code> <code>(fetch posts/quotes/responses of a user with the specified id)</code></summary>

##### Required OAuth2 Scopes

* post.read

##### Query Parameters

| Name    | Type     | Data type | Description                                                    |
|---------|----------|-----------|----------------------------------------------------------------|
| user_id | required | uuid      | Id of the user whose posts/quotes/responses have to be fetched |
| page    | optional | integer   | Number of the page to fetch                                    |

##### Body

N/A

##### Example Responses

| Http Code | Response                                                                                                                                                                                                           | Reason                                                 |
|-----------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------|
| `200`     | page with `{"content":[{"id":"0fd6d248-9ba1-4ef0-a5e0-ac09add7d894","dateCreated":1681826364537,"content":"test content","authorId":"36afeafd-686d-427e-a8e2-66b0b9ad3c47","quotedPost":null,"parentPost":null}]}` | Request valid                                          |
| `400`     |                                                                                                                                                                                                                    | Required 'user_id' not provided                        |
| `401`     |                                                                                                                                                                                                                    | Bearer token not provided or lacks the required scopes |

</details>

<details>
<summary><code>POST</code> <code><b>/api/posts</b></code> <code>(create a new post as the owner of the Bearer token)</code></summary>

##### Required OAuth2 Scopes

* post.write

##### Query Parameters

N/A

##### Body

```json
{
    "content": "This content can use #tags or mention other users @testuser"
}
```

Requirements:

* content must be between 1 and 300 characters long
* content can be tagged (using only [a-zA-Z0-9] characters) as long as the length of the tagging phrase is between 2 and 50 characters, 
* users can be mentioned as long as their usernames are prefixed with '@'

##### Example Responses

| Http Code | Response                                          | Reason                                                 |
|-----------|---------------------------------------------------|--------------------------------------------------------|
| `200`     | `{"uuid":"0fd6d248-9ba1-4ef0-a5e0-ac09add7d894"}` | Request valid                                          |
| `422`     | `{"messages":["Length of the post invalid"]}`     | Post too short or too long                             |
| `422`     | `{"messages":["Post content not provided"]}`      | Body of the request missing                            |
| `401`     |                                                   | Bearer token not provided or lacks the required scopes |

</details>

<details>
<summary><code>POST</code> <code><b>/api/posts/{id}/responses</b></code> <code>(create a new response to a post as the owner of the Bearer token)</code></summary>

##### Required OAuth2 Scopes

* post.read
* post.write

##### Query Parameters

N/A

##### Body

```json
{
    "content": "This is a response that can use #tags or mention other users @testuser"
}
```

Requirements:

* content must be between 1 and 300 characters long
* content can be tagged (using only [a-zA-Z0-9] characters) as long as the length of the tagging phrase is between 2 and 50 characters,
* users can be mentioned as long as their usernames are prefixed with '@'

##### Example Responses

| Http Code | Response                                                                                | Reason                                                 |
|-----------|-----------------------------------------------------------------------------------------|--------------------------------------------------------|
| `200`     | `{"uuid":"0fd6d248-9ba1-4ef0-a5e0-ac09add7d894"}`                                       | Request valid                                          |
| `404`     | `{"messages":["Post with id 0fd6d248-9ba1-4ef0-a5e0-ac09add7d894 could not be found"]}` | Post being responded to does not exists                |
| `422`     | `{"messages":["Length of the post invalid"]}`                                           | Response too short or too long                         |
| `422`     | `{"messages":["Post content not provided"]}`                                            | Body of the request missing                            |
| `401`     |                                                                                         | Bearer token not provided or lacks the required scopes |

</details>

<details>
<summary><code>POST</code> <code><b>/api/posts/{id}/quotes</b></code> <code>(create a new quote of a post as the owner of the Bearer token)</code></summary>

##### Required OAuth2 Scopes

* post.read
* post.write

##### Query Parameters

N/A

##### Body

```json
{
    "content": "This is a quote that can use #tags or mention other users @testuser"
}
```

Requirements:

* content must be between 1 and 300 characters long
* content can be tagged (using only [a-zA-Z0-9] characters) as long as the length of the tagging phrase is between 2 and 50 characters,
* users can be mentioned as long as their usernames are prefixed with '@'

##### Example Responses

| Http Code | Response                                                                                | Reason                                                 |
|-----------|-----------------------------------------------------------------------------------------|--------------------------------------------------------|
| `200`     | `{"uuid":"0fd6d248-9ba1-4ef0-a5e0-ac09add7d894"}`                                       | Request valid                                          |
| `404`     | `{"messages":["Post with id 0fd6d248-9ba1-4ef0-a5e0-ac09add7d894 could not be found"]}` | Quoted post does not exists                            |
| `422`     | `{"messages":["Length of the post invalid"]}`                                           | Quote too short or too long                            |
| `422`     | `{"messages":["Post content not provided"]}`                                            | Body of the request missing                            |
| `401`     |                                                                                         | Bearer token not provided or lacks the required scopes |

</details>

<details>
<summary><code>DELETE</code> <code><b>/api/posts/{id}</b></code> <code>(delete post/quote/response with the specified id)</code></summary>

##### Required OAuth2 Scopes

* post.read
* post.write

##### Query Parameters

N/A

##### Body

N/A

##### Example Responses

| Http Code | Response                                                                                                                               | Reason                                                  |
|-----------|----------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------|
| `200`     | `{"deleted": true}`                                                                                                                    | Request valid                                           |
| `404`     | `{"messages":["Post with id 0fd6d248-9ba1-4ef0-a5e0-ac09add7d894 could not be found"]}`                                                | Post does not exists                                    |
| `401`     |                                                                                                                                        | Bearer token not provided or lacks the required scopes  |
| `403`     | `{"messages":["User with id ff4ddaf7-238a-4d18-aa53-1cfc09ed0e73 cannot delete a post with id d082ddeb-eea1-4f38-ab95-273a8086e052"]}` | Owner of the Bearer token is not the author of the post |

</details>

-------------------------------------------------------------------------------------------------------------------------

### Likes Management

<details>
<summary><code>GET</code> <code><b>/api/posts/{id}/like</b></code> <code>(check if the owner of the Bearer token likes the post with the specified id)</code></summary>

##### Required OAuth2 Scopes

* post.read
* like.read

##### Query Parameters

N/A

##### Body

N/A

##### Example Responses

| Http Code | Response           | Reason                                                 |
|-----------|--------------------|--------------------------------------------------------|
| `200`     | `{"likes": true }` | Request valid                                          |
| `401`     |                    | Bearer token not provided or lacks the required scopes |

</details>

<details>
<summary><code>POST</code> <code><b>/api/posts/{id}/like</b></code> <code>(like the post with the specified id as the owner of the Bearer token)</code></summary>

##### Required OAuth2 Scopes

* post.read
* like.read
* like.write

##### Query Parameters

N/A

##### Body

N/A

##### Example Responses

| Http Code | Response           | Reason                                                 |
|-----------|--------------------|--------------------------------------------------------|
| `200`     | `{"likes": true }` | Request valid                                          |
| `401`     |                    | Bearer token not provided or lacks the required scopes |

</details>

<details>
<summary><code>DELETE</code> <code><b>/api/posts/{id}/like</b></code> <code>(unlike the post with the specified id as the owner of the Bearer token)</code></summary>

##### Required OAuth2 Scopes

* post.read
* like.read
* like.write

##### Query Parameters

N/A

##### Body

N/A

##### Example Responses

| Http Code | Response            | Reason                                                 |
|-----------|---------------------|--------------------------------------------------------|
| `200`     | `{"likes": false }` | Request valid                                          |
| `401`     |                     | Bearer token not provided or lacks the required scopes |

</details>

-------------------------------------------------------------------------------------------------------------------------

### Feed

<details>
<summary><code>GET</code> <code><b>/api/feed</b></code> <code>(fetch the feed of the user)</code></summary>

##### Required OAuth2 Scopes

* Unauthorized users get served the most popular posts
* Authorized users get served posts based on whom they follow

##### Query Parameters

| Name    | Type                     | Data type               | Description                                                                         |
|---------|--------------------------|-------------------------|-------------------------------------------------------------------------------------|
| popular | optional (default false) | boolean                 | When true, fetches the most popular posts. Otherwise fetches the most recent posts. |
| last    | optional (default "6")   | integer (in range 1-24) | How many hours old can the oldest fetched post be.                                  |
| page    | optional                 | integer                 | Number of the page to fetch                                                         |

The query parameter *popular* only works for the users who provide a valid Bearer token. Anonymous users can only receive
the most popular posts, which means that in their case *popular* does not do anything and it always behaves as if it 
were set to "true", even if that parameter is set to "false" in the request.

##### Body

N/A

##### Example Responses

| Http Code | Response                                                                                                                                                                                                           | Reason                                    |
|-----------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------|
| `200`     | page with `{"content":[{"id":"0fd6d248-9ba1-4ef0-a5e0-ac09add7d894","dateCreated":1681826364537,"content":"test content","authorId":"36afeafd-686d-427e-a8e2-66b0b9ad3c47","quotedPost":null,"parentPost":null}]}` | Request valid                             |
| `400`     | `{"messages":["hours values not in 1-24 range are not valid"]}`                                                                                                                                                    | Parameter 'last' was given invalid values |

</details>

-------------------------------------------------------------------------------------------------------------------------

### Reports

<details>
<summary><code>POST</code> <code><b>/api/posts/{id}/report</b></code> <code>(report a post with the specified id)</code></summary>

##### Required OAuth2 Scopes

* post.read

##### Query Parameters

N/A

##### Body

```json
{
  "reason": "SPAM",
  "context": "Some context explaining why this post is being reported can be placed here."
}
```

Requirements:

* *reason* is required and can only contain values: "SPAM", "HARASSMENT", "IMPERSONATION", "DISTURBING_CONTENT"
* *context* can be between 0 and 300 characters long

##### Example Responses

| Http Code | Response                                                                                | Reason                                                 |
|-----------|-----------------------------------------------------------------------------------------|--------------------------------------------------------|
| `200`     |                                                                                         | Request valid                                          |
| `404`     | `{"messages":["Post with id 0fd6d248-9ba1-4ef0-a5e0-ac09add7d894 could not be found"]}` | Post does not exists                                   |
| `422`     | `{"messages":["Report's 'reason' is not valid"]}`                                       | Reason couldn't be parsed as one of the available ones |            
| `422`     | `{"messages":["Length of the report's context invalid"]}`                               | The length of the context is not valid                 |
| `422`     | `{"messages":["Users cannot report their own posts"]}`                                  | User tried to report their own post                    |
| `401`     |                                                                                         | Bearer token not provided or lacks the required scopes |

</details>

-------------------------------------------------------------------------------------------------------------------------

### Tags

<details>
<summary><code>GET</code> <code><b>/api/tags/{name}/posts</b></code> <code>(fetch the most recent posts from the tag with the specified name)</code></summary>

##### Required OAuth2 Scopes

* post.read

##### Query Parameters

| Name    | Type                     | Data type               | Description                                                                          |
|---------|--------------------------|-------------------------|--------------------------------------------------------------------------------------|
| page    | optional                 | integer                 | Number of the page to fetch                                                          |

##### Body

N/A

##### Example Responses

| Http Code | Response                                                                                                                                                                                                    | Reason                                                 |
|-----------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------|
| `200`     | page with `{"content":[{"id":"0fd6d248-9ba1-4ef0-a5e0-ac09add7d894","dateCreated":1681826364537,"content":"#test","authorId":"36afeafd-686d-427e-a8e2-66b0b9ad3c47","quotedPost":null,"parentPost":null}]}` | Request valid                                          |
| `401`     |                                                                                                                                                                                                             | Bearer token not provided or lacks the required scopes |

</details>

<details>
<summary><code>GET</code> <code><b>/api/tags/popular</b></code> <code>(fetch five tags most popular in the last X hours)</code></summary>

##### Required OAuth2 Scopes

N/A

##### Query Parameters

| Name    | Type                   | Data type               | Description                                                                             |
|---------|------------------------|-------------------------|-----------------------------------------------------------------------------------------|
| last    | optional (default "1") | integer (in range 1-24) | How many hours back should the database query go during evaluation of tag's popularity. |

##### Body

N/A

##### Example Responses

| Http Code | Response                                                                                                                                                                                                                                                                                                             | Reason                                    |
|-----------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------|
| `200`     | `[{"id":"741379ad-ac51-4412-af28-f78d2809e462","name":"test2"},{"id":"93d1840b-a9dd-487c-930b-860d91b88c13","name":"test5"},{"id":"9e83399e-667e-4c4b-b776-fa5289d74772","name":"test1"},{"id":"a3679103-7ae5-42af-928b-44be8d10114a","name":"test3"},{"id":"ff255d6a-69f8-47cd-8b9c-26132704815b","name":"test4"}]` | Request valid                             |
| `400`     | `{"messages":["hours values not in 1-24 range are not valid"]}`                                                                                                                                                                                                                                                      | Parameter 'last' was given invalid values |

</details>