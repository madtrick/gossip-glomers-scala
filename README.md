# Development

Uses `nvim-metals` (and its embedded `millw`) together with `mill`.

# Building and running

Generate JAR

```sh
 ./.metals/millw Glomers.assembly
```

Show folder keeping the generated JAR

```sh
./.metals/millw show Glomers.assembly
```

Run the app

```sh
./out/Glomers/assembly.dest/out.jar
```

## Tests

This project uses the `FunSuite` from [scalatests](https://www.scalatest.org/at_a_glance/FunSuite) (since that seems to be the one used in the Databricks codebase).

To run the Tests

```sh
./.metals/millw Glomers.test
```

To run the tests in watch mode

```sh
./.metals/millw --watch Glomers.test
```

# Glomers

Note that the commands in the following sections assume that you have a local
copy of [maelstrom](https://github.com/jepsen-io/maelstrom/) in the same folder
as this project.

## Echo

[Challenge#1 Echo](https://fly.io/dist-sys/1/)

```sh
./maelstrom/maelstrom test -w echo --bin out/Glomers/assembly.dest/out.jar --time-limit 10
```

## Unique id

[Challenge #2: Unique ID Generation](https://fly.io/dist-sys/2/)

```sh
./maelstrom/maelstrom test -w unique-ids --bin out/Glomers/assembly.dest/out.jar --time-limit 30 --rate 1000 --node-count 3 --availability total --nemesis partition
```

## Broadcast

[Challenge #3a: Single-Node Broadcast](https://fly.io/dist-sys/3a/)

```sh
./maelstrom test -w broadcast --bin ~/go/bin/maelstrom-broadcast --node-count 1 --time-limit 20 --rate 10
```

[Challenge #3b: Multi-Node Broadcast](https://fly.io/dist-sys/3b/)

On this iteration of the glomer I create node ensembles. Each node ensemble has
a head (or leader). The messages can flow from non-leader nodes to **their**
leader, from the leader of an ensemble to the rest of the nodes in the ensemble
and from a leader to its previous and next leader. Nodes from different
ensembles can't communicate with each other.

```sh
GLOMER=broadcast ./maelstrom/maelstrom test -w broadcast --bin out/Glomers/assembly.dest/out.jar --node-count 5 --time-limit 20 --rate 1
```

[Challenge #3c: Fault Tolerant Broadcast](https://fly.io/dist-sys/3c/)

On this iteration of the glomer I use a quite simple approach to guarantee
message delivery. For each message delivered to another node in a node ensemble
I keep track of it until I receive a delivery confirmation
`deliver_broadcast_ok`. While the confirmation is not received I keep sending
the message.

```sh
GLOMER=broadcast ./maelstrom/maelstrom test -w broadcast --bin out/Glomers/assembly.dest/out.jar --node-count 5 --time-limit 20 --rate 10 --nemesis partition
```

## Grow only counter

[Challenge #4: Grow-Only Counter](https://fly.io/dist-sys/4/)

```sh
GLOMER=counter ./maelstrom/maelstrom test -w g-counter --bin out/Glomers/assembly.dest/out.jar --node-count 3 --rate 100 --time-limit 20 --nemesis partitio
```

The maelstrom check for this workload failed [once](https://github.com/madtrick/gossip-glomers-scala/issues/3). Every other time I tried it worked fine.

## Kafka-style log

[Challenge #5a: Single-Node Kafka-Style Log](https://fly.io/dist-sys/5a/)

```sh
GLOMER=kafka-log ./maelstrom/maelstrom test -w kafka --bin out/Glomers/assembly.dest/out.jar --node-count 1 --concurrency 2n --time-limit 20 --rate 1000
```
