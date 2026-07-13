# Riak Java Client (OpenRiak)

A Java client for interacting with a [Riak](https://github.com/OpenRiak/riak) cluster
over the Protocol Buffers API.

## Requirements

- Java 21 (the build uses a Gradle Java toolchain targeting 21).
- Git (for fetching the `riak_pb` submodule).
- A running Riak node (only for the optional integration tests).

## Submodules

This project uses one git submodule:

- [`riak_pb`](https://github.com/OpenRiak/riak_pb) (branch `openriak-3.4`) - the shared
  `.proto` files. Protobuf generates the Java message classes from these during the build.

The `riak-client-tools` scripts (used to configure a Riak node for integration tests) are
vendored directly into this repository under [`riak-client-tools/`](riak-client-tools/).
This was done to avoid having to depend on a repo still under the old Basho org.

The 

Initialize the submodule after cloning:

```bash
git submodule update --init
```

Update it to the latest commit on its tracked branch:

```bash
git submodule update --remote
```

## Building

Build and run unit tests:

```bash
./gradlew build
```

Build the jar without running tests:

```bash
./gradlew build -x test
```

Run only the unit tests:

```bash
./gradlew test
```

### `RiakMessageCodes`

`com.basho.riak.protobuf.RiakMessageCodes` is a generated file that is checked into the
repository. It is regenerated from `riak_pb/src/riak_pb_messages.csv` with:

```bash
./gradlew generateMessageCodes
```

Run this whenever the message codes in `riak_pb` change and you need to pick up new
constants, then rebuild.

## Integration tests

Integration tests are optional and are **not** run as part of `./gradlew build`.

```bash
./gradlew itest
```

- They run against an **already-running** Riak node; the build does not start one for you.
  Start a node (for example a local devrel) and configure its bucket types using the scripts
  under [`riak-client-tools/`](riak-client-tools/) before running the tests.
- Backend-specific test groups are off by default. Enable them when running against a
  suitable node, e.g. `-PriakTimeseries=true`, `-PriakTwoI=true`, `-PriakMapReduce=true`.

## Notes

### TLS and Authentication Behavior

TLS and authentication are independent:

- `withForceTls(true)` triggers STARTTLS even when no credentials are provided.
- Authentication is only attempted when non-empty credentials are configured.
- `withTls(KeyStore trustStore)` can be used to apply a custom trust store for
  TLS-only connections (no username/password required).
- If only `withForceTls(true)` is used, JVM default trust is used.

## License

Licensed under the Apache License, Version 2.0. See [`LICENSE`](LICENSE).
