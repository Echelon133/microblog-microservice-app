# Microblog app

A microblogging platform composed of multiple containerized services.

## auth

Configures a public client which uses the **Authorization Code Flow with PKCE** during the process of granting 
access tokens to users.

## gateway

API gateway of the application.

## user

Implements:

* creation of accounts for new users
* searching for users by their username (exact or partial)
* fetching/updating user's profile information
* following and unfollowing users
* sending notifications to users when they are followed
* fetching lists of follows/followers of users
* fetching a list of known followers (i.e. "this user is followed by these people that you follow")

## post

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

## notification

Implements:

* fetching the most recent notifications of a user
* fetching the number of the unread notifications
* marking a single notification as 'read'
* marking all notifications as 'read'

## report

Implements:

* fetching reports based on their status (checked/unchecked)
* accepting or rejecting reports (accepting a report results in the deletion of the reported post)