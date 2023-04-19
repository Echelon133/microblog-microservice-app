# report service

This service implements features such as:

* fetching reports based on their status (checked/unchecked)
* accepting or rejecting reports (accepting a report results in the deletion of the reported post)

## report API

<details>
<summary><code>GET</code> <code><b>/api/reports</b></code> <code>(fetch reports filtered by their status)</code></summary>

##### Required OAuth2 Scopes

* report.read

##### Query Parameters

| Name    | Type                       | Data type | Description                                                             |
|---------|----------------------------|-----------|-------------------------------------------------------------------------|
| page    | optional                   | integer   | Number of the page to fetch                                             |
| checked | optional (default "false") | boolean   | Fetches unchecked reports when true, otherwise it shows checked reports |

##### Body

N/A

##### Example Responses

| Http Code | Response                                                                                                                                                                                                                                                                                                                     | Reason                                                 |
|-----------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------|
| `200`     | page containing `{"content":[{"reportId":"b4c53fde-228e-49aa-bfd7-cd2c0ebdacd8","dateCreated":"2023-04-11T11:18:47.941+00:00","reason":"SPAM","context":"test context","reportedPostId":"decc2b6d-992e-4d33-aa98-a472737795b5","reportingUserId":"188967d5-d165-4de4-bc60-cba0910bd5de","accepted":false,"checked":false}]}` | Request valid                                          |
| `401`     |                                                                                                                                                                                                                                                                                                                              | Bearer token not provided or lacks the required scopes |

</details>

<details>
<summary><code>POST</code> <code><b>/api/reports/{id}</b></code> <code>(accept or reject a report with the specified id)</code></summary>

##### Required OAuth2 Scopes

* report.write

##### Query Parameters

| Name   | Type     | Data type | Description                                                              |
|--------|----------|-----------|--------------------------------------------------------------------------|
| accept | required | boolean   | Deletes the reported post when true, otherwise leaves the post untouched |

##### Body

N/A

##### Example Responses

| Http Code | Response                                                                                        | Reason                                                          |
|-----------|-------------------------------------------------------------------------------------------------|-----------------------------------------------------------------|
| `200`     |                                                                                                 | Request valid                                                   |
| `404`     | `{"messages":["Report with id 0fd6d248-9ba1-4ef0-a5e0-ac09add7d894 could not be found"]}`       | Report does not exists                                          |
| `422`     | `{"messages":["Report with id 0fd6d248-9ba1-4ef0-a5e0-ac09add7d894 has already been checked"]}` | The initial decision on a report is final and cannot be changed |
| `401`     |                                                                                                 | Bearer token not provided or lacks the required scopes          |

</details>