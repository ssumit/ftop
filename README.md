# ftop
flock.co testing framework prototype

## Scope
* easy to use testing abstractions
* complements and not duplicates the functional tests of the individual services
* multiple client simulation that can be used composed together for normal and stress testing.
* should cover each request and corresponding success and failure response.

Usually it would be used to confirm the specifications and analyze response time from client perspective.

## Door basics
Client can initiate TLS or RTLS connections with door. Messages are exchanged in a fixed format - door packet -
DoorEnvelope. One needs to consistently exchange messages on the socket for it to remain open. We use ping-pong
to achieve this. The "body" attribute inside the door packet contains -
* payload - request
* to - serialized jid
* id - request id
The response contains the request id (if mentioned).
Some of the actual packets are recorded in 'examples' file.