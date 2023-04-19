# notification service

This service implements features such as:

* fetching the most recent notifications of a user
* fetching the number of the unread notifications
* marking a single notification as 'read'
* marking all notifications as 'read'

## notification API

<details>
<summary><code>GET</code> <code><b>/api/notifications</b></code> <code>(fetch the most recent notifications of the Bearer token's owner)</code></summary>

##### Required OAuth2 Scopes

* notification.read

##### Query Parameters

| Name | Type     | Data type | Description                 |
|------|----------|-----------|-----------------------------|
| page | optional | integer   | Number of the page to fetch |

##### Body

N/A

##### Example Responses

| Http Code | Response                                                                                                                                                                                                             | Reason                                                 |
|-----------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------|
| `200`     | page containing `{"content":[{"notificationId":"7b5fb618-c4c4-439d-ba8b-d0fb98d3a2dc","dateCreated":"2023-04-07T19:52:21.317+00:00","notificationSource":"7c58c7bc-f279-4948-b254-1771c868bf86","type":"RESPONSE"]}` | Request valid                                          |
| `401`     |                                                                                                                                                                                                                      | Bearer token not provided or lacks the required scopes |

</details>

<details>
<summary><code>GET</code> <code><b>/api/notifications/unread-counter</b></code> <code>(fetch the number of unread notifications of the Bearer token's owner)</code></summary>

##### Required OAuth2 Scopes

* notification.read

##### Query Parameters

N/A

##### Body

N/A

##### Example Responses

| Http Code | Response          | Reason                                                 |
|-----------|-------------------|--------------------------------------------------------|
| `200`     | `{"unread": 10 }` | Request valid                                          |
| `401`     |                   | Bearer token not provided or lacks the required scopes |


</details>

<details>
<summary><code>POST</code> <code><b>/api/notifications/read-all</b></code> <code>(mark all notifications of the Bearer token's owner as read)</code></summary>

##### Required OAuth2 Scopes

* notification.read
* notification.write

##### Query Parameters

N/A

##### Body

N/A

##### Example Responses

| Http Code | Response        | Reason                                                 |
|-----------|-----------------|--------------------------------------------------------|
| `200`     | `{"read": 15 }` | Request valid                                          |
| `401`     |                 | Bearer token not provided or lacks the required scopes |


</details>

<details>
<summary><code>POST</code> <code><b>/api/notifications/{id}/read</b></code> <code>(mark a single notifications of the Bearer token's owner as read)</code></summary>

##### Required OAuth2 Scopes

* notification.read
* notification.write

##### Query Parameters

N/A

##### Body

N/A

##### Example Responses

| Http Code | Response                                                                                                                                       | Reason                                                             |
|-----------|------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------|
| `200`     | `{"read": 1 }`                                                                                                                                 | Request valid                                                      |
| `401`     |                                                                                                                                                | Bearer token not provided or lacks the required scopes             |
| `404`     | `{"messages":["Notification with id 0fd6d248-9ba1-4ef0-a5e0-ac09add7d894 could not be found"]}`                                                | Notification does not exists                                       |
| `403`     | `{"messages":["User with id ff4ddaf7-238a-4d18-aa53-1cfc09ed0e73 cannot read a notification with id 'd082ddeb-eea1-4f38-ab95-273a8086e052'"]}` | Owner of the Bearer token is not the recipient of the notification |

</details>
