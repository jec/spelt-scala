# Spelt

[![build & test](https://github.com/jec/spelt-scala/actions/workflows/build_and_test.yml/badge.svg)](https://github.com/jec/spelt-scala/actions/workflows/build_and_test.yml)
[![codecov](https://codecov.io/gh/jec/spelt-scala/branch/master/graph/badge.svg?token=D9VD3090GM)](https://codecov.io/gh/jec/spelt-scala)

Matrix defines a set of open APIs for decentralized communication, suitable for
securely publishing, persisting and subscribing to data over a global open
federation of servers with no single point of control. Uses include Instant
Messaging (IM), Voice over IP (VoIP) signalling, Internet of Things (IoT)
communication, and bridging together existing communication silos—providing
the basis of a new, open, real-time communication ecosystem.

Spelt aims to be a server implementation of the Matrix API. The following are
the relevant components of the specification:

* [Matrix client-server
  specification](https://spec.matrix.org/v1.14/client-server-api/): provides
  messaging functionality used by Matrix-compliant clients (target version
  1.14)

* [Matrix server-server
  specification](https://spec.matrix.org/v1.14/server-server-api/):
  provides federation amongst servers (target version 1.14)

Spelt is implemented in [Scala](https://scala-lang.org/) using
[Play](https://www.playframework.com/) as the web app framework and
[Neo4j](https://neo4j.com/) as the database.

## Setup

In each of `src/main/resources/` and `src/test/resources/`:

- Generate a keystore with an RSA private key.

      openssl genpkey -outform der -algorithm rsa -out pkey.der
      openssl pkcs8 -topk8 -inform der -outform der -in pkey.der -out pkey.pk8 -nocrypt

- Copy `application.example.conf` to `application.conf` and update as needed.

## License

Spelt is licensed under the three-clause BSD license. See LICENSE.txt.

## To Do

API implementation progress is tracked in [TODO.md](TODO.md).
