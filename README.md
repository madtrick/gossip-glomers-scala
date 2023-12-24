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

# Glomers

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
