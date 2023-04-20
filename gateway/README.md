# gateway

This is an API gateway for the entire application.

Service:

* **auth** receives requests with paths matching */oauth2/*** and */login*
* **user** receives requests with paths matching */api/users/***
* **post** receives requests with paths matching */api/posts***, */api/tags***, */api/feed***
* **notification** receives requests with paths matching */api/notifications***
* **report** receives requests with paths matching */api/reports***