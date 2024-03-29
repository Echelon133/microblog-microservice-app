# auth service

This is an OAuth2 auth service. It gives authorized users 
access tokens which contain a certain set of scopes (depends on which scopes
were requested by the user).

## Scopes 

### Regular users

Scopes currently available for regular users:

* post.read/post.write
* user.read/user.write
* follow.read/follow.write
* like.read/like.write
* notification.read/notification.write

### Administrators

Scopes currently available for administrators:

* report.read/report.write

## How to generate an access token

This service uses oauth's **Authorization Code Flow with PKCE** to let public clients 
safely exchange user's information for an access token.

### 1. Generate a code verifier

The code verifier should be random and long enough to ensure that it cannot be feasibly brute forced. 

UUID generators which use strong sources of randomness are a good choice.

Example code verifier: 

```
d6b67927-f07f-4bae-b63e-7e398017fc11
```

### 2. Generate a code challenge

Code challenge is generated by taking the code verifier and 
calculating its SHA256, then taking that hash and using url-safe base64 to
encode it.

```
code_challenge = base64urlencode(sha256(code_verifier))
```

Example code challenge (calculated from the example code verifier above): 

```
LvDhUzx7t7WSIxDVJ037cU_jHWN3fDs2hVXh8trgeIQ
```

### 3. Make a GET request to /oauth2/authorize

This GET request must contain query parameters:

* response_type=code
* client_id=public-client
* scope=\[a set of valid scopes\]
* redirect_uri=http://127.0.0.1:9999 (placeholder value used during development)
* code_challenge=\[value calculated in the second step\]
* code_challenge_method=S256

Example request: 

```
GET /oauth2/authorize?response_type=code&
                      client_id=public-client&
                      scope=user.read user.write&
                      redirect_uri=http://127.0.0.1:9999&
                      code_challenge=LvDhUzx7t7WSIxDVJ037cU_jHWN3fDs2hVXh8trgeIQ&
                      code_challenge_method=S256
```

### 4. Provide username+password after a redirect to a login page

If:

* user's username and password are valid
* the client actually grants scopes which were requested
* the client's redirect uri and the redirect uri in the request are the same

then the user is redirected and receives a code.

Example code:

```
LM0YvEwnoSqO0kLJ4AKtCLxpj2DfSdjN-AmqcVYhkZRghoNZkp7v2dlR8tlWck167fI5Ej_ihLXhg7S6YAyAGWA7NG5gq8ZPk0rj-_ESosya4xLUk3Q53CePiqLP4Ibp
```

### 5. Exchange the code for an access token by making a POST request to /oauth2/token

In this step, the code received by the user in the previous step can be exchanged for an access token, 
but only if the auth service can verify that the code verifier provided with the request had been
used to generate the code challenge from the third step. This validation process ensures that the auth service still 
talks to the same client as the one who initialized this authorization process, because it assumes that
only the client knows the code verifier from which the code challenge had been generated.

This POST request must contain query parameters:

* grant_type=authorization_code
* client_id=public-client
* code=\[code received in the previous step\]
* redirect_uri=http://127.0.0.1:9999 (placeholder value used during development)
* code_verifier=\[code verifier generated during the first step\]

```
POST /oauth2/token?grant_type=authorization_code&
                   code=LM0YvEwnoSqO0kLJ4AKtCLxpj2DfSdjN-AmqcVYhkZRghoNZkp7v2dlR8tlWck167fI5Ej_ihLXhg7S6YAyAGWA7NG5gq8ZPk0rj-_ESosya4xLUk3Q53CePiqLP4Ibp&
                   client_id=public-client&
                   code_verifier=d6b67927-f07f-4bae-b63e-7e398017fc11&
                   redirect_uri=http://127.0.0.1:9999
```

If the service verifies the user, an access token is granted.

Example access token response:

```
{
    "access_token": "LFOCmpHMTc7bYMN1wQHv1S93hMkzmXFjD_yTwzCm8B3uzQfy5ujr9Yvol4Z0ut7JzXYvrRN6zbhG6Cg6soX0Vc8RjXHSlFVA32yaUIuoWsoK-NcjlX0VtP8Pdouf-ZdP",
    "scope": "user.read like.read notification.write notification.read follow.read follow.write post.write like.write post.read",
    "token_type": "Bearer",
    "expires_in": 10799
}
```

This access token should be attached to requests in the **Authorization** header, prefixed by keyword: **Bearer**.

Example header:

```
Authorization: Bearer LFOCmpHMTc7bYMN1wQHv1S93hMkzmXFjD_yTwzCm8B3uzQfy5ujr9Yvol4Z0ut7JzXYvrRN6zbhG6Cg6soX0Vc8RjXHSlFVA32yaUIuoWsoK-NcjlX0VtP8Pdouf-ZdP
```