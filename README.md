# Spelt

Matrix defines a set of open APIs for decentralized communication, suitable for
securely publishing, persisting and subscribing to data over a global open
federation of servers with no single point of control. Uses include Instant
Messaging (IM), Voice over IP (VoIP) signalling, Internet of Things (IoT)
communication, and bridging together existing communication silos—providing
the basis of a new, open, real-time communication ecosystem.

Spelt aims to be a server implementation of the Matrix API. The following are
the relevant components of the specification:

* [Matrix client-server
  specification](https://spec.matrix.org/v1.2/client-server-api/): provides
  messaging functionality used by Matrix-compliant clients (target version
  1.2)

* [Matrix server-server
  specification](https://spec.matrix.org/v1.2/server-server-api/):
  provides federation amongst servers (target version 1.2)

Spelt is implemented in [Scala](https://scala-lang.org/) using
[Scalatra](https://www.scalatra.org/) as the web app framework and
[Neo4j](https://neo4j.com/) as the database.

## License

Spelt is licensed under the three-clause BSD license. See LICENSE.txt.

## To Do

Spelt is under active development, and much work remains before this becomes a
functioning messaging server.

### Client-Server

This checklist tracks the progress of implementing the endpoints defined in the
client-server spec.

- [ ] API standards
- [ ] `GET /_matrix/client/versions`
- [ ] Server discovery
- [ ] `GET /.well-known/matrix/client`
- [ ] 4 Client authentication
- [ ] 4.4.4.7 Token-authenticated registration
- [ ] `GET /_matrix/client/v1/register/m.login.registration_token/validity`
- [ ] 4.5 Login
- [ ] 4.5.1 Appservice login
- [ ] `GET /_matrix/client/v3/login`
- [ ] `POST /_matrix/client/v3/login`
- [ ] `POST /_matrix/client/v3/logout`
- [ ] `POST /_matrix/client/v3/logout/all`
- [ ] 4.6 Account registration and management
- [ ] `POST /_matrix/client/v3/account/deactivate`
- [ ] `POST /_matrix/client/v3/account/password`
- [ ] `POST /_matrix/client/v3/account/password/email/requestToken`
- [ ] `POST /_matrix/client/v3/account/password/msisdn/requestToken`
- [ ] `POST /_matrix/client/v3/register`
- [ ] `GET /_matrix/client/v3/register/available`
- [ ] `POST /_matrix/client/v3/register/email/requestToken`
- [ ] `POST /_matrix/client/v3/register/msisdn/requestToken`
- [ ] 4.7 Adding account administrative contact information
- [ ] `GET /_matrix/client/v3/account/3pid`
- [ ] Deprecated: `POST /_matrix/client/v3/account/3pid`
- [ ] `POST /_matrix/client/v3/account/3pid/add`
- [ ] `POST /_matrix/client/v3/account/3pid/bind`
- [ ] `POST /_matrix/client/v3/account/3pid/delete`
- [ ] `POST /_matrix/client/v3/account/3pid/email/requestToken`
- [ ] `POST /_matrix/client/v3/account/3pid/msisdn/requestToken`
- [ ] `POST /_matrix/client/v3/account/3pid/unbind`
- [ ] 4.8 Current account information
- [ ] `GET /_matrix/client/v3/account/whoami`
- [ ] 5 Capabilities negotiation
- [ ] `GET /_matrix/client/v3/capabilities`
- [ ] 6 Filtering
- [ ] `POST /_matrix/client/v3/user/{userId}/filter`
- [ ] `GET /_matrix/client/v3/user/{userId}/filter/{filterId}`
- [ ] 7 Events
- [ ] 7.6 Syncing
- [ ] `GET /_matrix/client/v3/sync`
- [ ] Deprecated: `GET /_matrix/client/v3/events`
- [ ] Deprecated: `GET /_matrix/client/v3/events/{eventId}`
- [ ] Deprecated: `GET /_matrix/client/v3/initialSync`
- [ ] 7.7 Getting events for a room
- [ ] `GET /_matrix/client/v3/rooms/{roomId}/event/{eventId}`
- [ ] `GET /_matrix/client/v3/rooms/{roomId}/joined_members`
- [ ] `GET /_matrix/client/v3/rooms/{roomId}/members`
- [ ] `GET /_matrix/client/v3/rooms/{roomId}/state`
- [ ] `GET /_matrix/client/v3/rooms/{roomId}/state/{eventType}/{stateKey}`
- [ ] `GET /_matrix/client/v3/rooms/{roomId}/messages`
- [ ] Deprecated: `GET /_matrix/client/v3/rooms/{roomId}/initialSync`
- [ ] 7.8 Sending events to a room
- [ ] `PUT /_matrix/client/v3/rooms/{roomId}/state/{eventType}/{stateKey}`
- [ ] `PUT /_matrix/client/v3/rooms/{roomId}/send/{eventType}/{txnId}`
- [ ] 7.9 Redactions
- [ ] `PUT /_matrix/client/v3/rooms/{roomId}/redact/{eventId}/{txnId}`
- [ ] 8 Rooms
- [ ] 8.2 Creation
- [ ] `POST /_matrix/client/v3/createRoom`
- [ ] 8.3 Room aliases
- [ ] `GET /_matrix/client/v3/directory/room/{roomAlias}`
- [ ] `PUT /_matrix/client/v3/directory/room/{roomAlias}`
- [ ] `DELETE /_matrix/client/v3/directory/room/{roomAlias}`
- [ ] `GET /_matrix/client/v3/rooms/{roomId}/aliases`
- [ ] 8.5 Room membership
- [ ] `GET /_matrix/client/v3/joined_rooms`
- [ ] 8.5.1 Joining rooms
- [ ] `POST /_matrix/client/v3/rooms/{roomId}/invite`
- [ ] `POST /_matrix/client/v3/join/{roomIdOrAlias}`
- [ ] `POST /_matrix/client/v3/rooms/{roomId}/join`
- [ ] `POST /_matrix/client/v3/knock/{roomIdOrAlias}`
- [ ] 8.5.2 Leaving rooms
- [ ] `POST /_matrix/client/v3/rooms/{roomId}/forget`
- [ ] `POST /_matrix/client/v3/rooms/{roomId}/leave`
- [ ] `POST /_matrix/client/v3/rooms/{roomId}/kick`
- [ ] 8.5.2.1 Banning users in a room
- [ ] `POST /_matrix/client/v3/rooms/{roomId}/ban`
- [ ] `POST /_matrix/client/v3/rooms/{roomId}/unban`
- [ ] 8.6 Listing rooms
- [ ] `GET /_matrix/client/v3/directory/list/room/{roomId}`
- [ ] `PUT /_matrix/client/v3/directory/list/room/{roomId}`
- [ ] `GET /_matrix/client/v3/publicRooms`
- [ ] `POST /_matrix/client/v3/publicRooms`
- [ ] 9 User Data
- [ ] 9.1 User Directory
- [ ] `POST /_matrix/client/v3/user_directory/search`
- [ ] 9.2 Profiles
- [ ] `GET /_matrix/client/v3/profile/{userId}`
- [ ] `GET /_matrix/client/v3/profile/{userId}/avatar_url`
- [ ] `PUT /_matrix/client/v3/profile/{userId}/avatar_url`
- [ ] `GET /_matrix/client/v3/profile/{userId}/displayname`
- [ ] `PUT /_matrix/client/v3/profile/{userId}/displayname`
- [ ] 11 Modules
- [ ] 11.3 Voice over IP
- [ ] `GET /_matrix/client/v3/voip/turnServer`
- [ ] 11.4 Typing Notifications
- [ ] `PUT /_matrix/client/v3/rooms/{roomId}/typing/{userId}`
- [ ] 11.5 Receipts
- [ ] `POST /_matrix/client/v3/rooms/{roomId}/receipt/{receiptType}/{eventId}`
- [ ] 11.6 Fully read markers
- [ ] `POST /_matrix/client/v3/rooms/{roomId}/read_markers`
- [ ] 11.7 Presence
- [ ] `GET /_matrix/client/v3/presence/{userId}/status`
- [ ] `PUT /_matrix/client/v3/presence/{userId}/status`
- [ ] 11.8 Content repository
- [ ] `GET /_matrix/media/v3/config`
- [ ] `GET /_matrix/media/v3/download/{serverName}/{mediaId}`
- [ ] `GET /_matrix/media/v3/download/{serverName}/{mediaId}/{fileName}`
- [ ] `GET /_matrix/media/v3/preview_url`
- [ ] `GET /_matrix/media/v3/thumbnail/{serverName}/{mediaId}`
- [ ] `POST /_matrix/media/v3/upload`
- [ ] 11.9 Send-to-Device messaging
- [ ] `PUT /_matrix/client/v3/sendToDevice/{eventType}/{txnId}`
- [ ] 11.10 Device Management
- [ ] `POST /_matrix/client/v3/delete_devices`
- [ ] `GET /_matrix/client/v3/devices`
- [ ] `GET /_matrix/client/v3/devices/{deviceId}`
- [ ] `PUT /_matrix/client/v3/devices/{deviceId}`
- [ ] `DELETE /_matrix/client/v3/devices/{deviceId}`
- [ ] 11.11 End-to-End Encryption
- [ ] 11.11.2.3 Cross-signing
- [ ] `POST /_matrix/client/v3/keys/device_signing/upload`
- [ ] `POST /_matrix/client/v3/keys/signatures/upload`
- [ ] 11.11.3.2 Server-side key backups
- [ ] `GET /_matrix/client/v3/room_keys/keys`
- [ ] `PUT /_matrix/client/v3/room_keys/keys`
- [ ] `DELETE /_matrix/client/v3/room_keys/keys`
- [ ] `GET /_matrix/client/v3/room_keys/keys/{roomId}`
- [ ] `PUT /_matrix/client/v3/room_keys/keys/{roomId}`
- [ ] `DELETE /_matrix/client/v3/room_keys/keys/{roomId}`
- [ ] `GET /_matrix/client/v3/room_keys/keys/{roomId}/{sessionId}`
- [ ] `PUT /_matrix/client/v3/room_keys/keys/{roomId}/{sessionId}`
- [ ] `DELETE /_matrix/client/v3/room_keys/keys/{roomId}/{sessionId}`
- [ ] `GET /_matrix/client/v3/room_keys/version`
- [ ] `POST /_matrix/client/v3/room_keys/version`
- [ ] `GET /_matrix/client/v3/room_keys/version/{version}`
- [ ] `PUT /_matrix/client/v3/room_keys/version/{version}`
- [ ] `DELETE /_matrix/client/v3/room_keys/version/{version}`
- [ ] 11.11.5.2 Key management
- [ ] `GET /_matrix/client/v3/keys/changes`
- [ ] `POST /_matrix/client/v3/keys/claim`
- [ ] `POST /_matrix/client/v3/keys/query`
- [ ] `POST /_matrix/client/v3/keys/upload`
- [ ] 11.14 Push notifications
- [ ] `GET /_matrix/client/v3/pushers`
- [ ] `POST /_matrix/client/v3/pushers/set`
- [ ] 11.14.1.1 Listing notifications
- [ ] `GET /_matrix/client/v3/notifications`
- [ ] 11.14.1.5 Push rules
- [ ] `GET /_matrix/client/v3/pushrules/`
- [ ] `GET /_matrix/client/v3/pushrules/{scope}/{kind}/{ruleId}`
- [ ] `PUT /_matrix/client/v3/pushrules/{scope}/{kind}/{ruleId}`
- [ ] `DELETE /_matrix/client/v3/pushrules/{scope}/{kind}/{ruleId}`
- [ ] `GET /_matrix/client/v3/pushrules/{scope}/{kind}/{ruleId}/actions`
- [ ] `PUT /_matrix/client/v3/pushrules/{scope}/{kind}/{ruleId}/actions`
- [ ] `GET /_matrix/client/v3/pushrules/{scope}/{kind}/{ruleId}/enabled`
- [ ] `PUT /_matrix/client/v3/pushrules/{scope}/{kind}/{ruleId}/enabled`
- [ ] 11.15 Third-party invites
- [ ] `POST /_matrix/client/v3/rooms/{roomId}/invite`
- [ ] 11.16 Server-side search
- [ ] `POST /_matrix/client/v3/search`
- [ ] 11.18 Room previews
- [ ] `GET /_matrix/client/v3/events`
- [ ] 11.19 Room tagging
- [ ] `GET /_matrix/client/v3/user/{userId}/rooms/{roomId}/tags`
- [ ] `PUT /_matrix/client/v3/user/{userId}/rooms/{roomId}/tags/{tag}`
- [ ] `DELETE /_matrix/client/v3/user/{userId}/rooms/{roomId}/tags/{tag}`
- [ ] 11.20 Client config
- [ ] `GET /_matrix/client/v3/user/{userId}/account_data/{type}`
- [ ] `PUT /_matrix/client/v3/user/{userId}/account_data/{type}`
- [ ] `GET /_matrix/client/v3/user/{userId}/rooms/{roomId}/account_data/{type}`
- [ ] `PUT /_matrix/client/v3/user/{userId}/rooms/{roomId}/account_data/{type}`
- [ ] 11.21 Server administration
- [ ] `GET /_matrix/client/v3/admin/whois/{userId}`
- [ ] 11.22 Event context
- [ ] `GET /_matrix/client/v3/rooms/{roomId}/context/{eventId}`
- [ ] 11.23 SSO client login
- [ ] `GET /_matrix/client/v3/login/sso/redirect`
- [ ] `GET /_matrix/client/v3/login/sso/redirect/{idpId}`
- [ ] 11.27 Reporting content
- [ ] `POST /_matrix/client/v3/rooms/{roomId}/report/{eventId}`
- [ ] 11.28 Third-party networks
- [ ] `GET /_matrix/client/v3/thirdparty/location`
- [ ] `GET /_matrix/client/v3/thirdparty/location/{protocol}`
- [ ] `GET /_matrix/client/v3/thirdparty/protocol/{protocol}`
- [ ] `GET /_matrix/client/v3/thirdparty/protocols`
- [ ] `GET /_matrix/client/v3/thirdparty/user`
- [ ] `GET /_matrix/client/v3/thirdparty/user/{protocol}`
- [ ] 11.29 OpenID
- [ ] `POST /_matrix/client/v3/user/{userId}/openid/request_token`
- [ ] 11.32 Room upgrades
- [ ] `POST /_matrix/client/v3/rooms/{roomId}/upgrade`
- [ ] 11.35 Spaces
- [ ] `GET /_matrix/client/v1/rooms/{roomId}/hierarchy`


### Server-Server

The relevant endpoints for implementing the federation specification will
follow eventually.