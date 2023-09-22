## Compilation

How to compile and run the project

```
.\gradlew shadowJar
java -jar .\build\libs\CassandraProcessor.jar 127.0.0.1 9042
```

## Denormalize data files

Data is located in `Cassandra/scripts/denormalize` folder.

1. Start postgresql container

```
docker run --name some-postgres -v H:\Desktop\NUS\Y4S1\CS4224\projects\files\project_files\data_files:/container/dir -e POSTGRES_PASSWORD=password -d postgres
```

2. enter via bash

```
docker exec -it some-postgres bash
```

3.  Enter directory

```
cd container/dir
```

4. Running script

```
cat createtable.sql | psql -U postgres -d postgres
cat populate.sql | psql -U postgres -d postgres
cat denormalize.sql | psql -U postgres -d postgres
```

## Run Cassandra on Docker

Start cassandra container

```
docker run -v H:\Desktop\NUS\Y4S1\CS4224\projects\cs4224-ay2324-2-TeamH\Cassandra\scripts\data:/container/dir -p 9042:9042 cassandra:latest
```
