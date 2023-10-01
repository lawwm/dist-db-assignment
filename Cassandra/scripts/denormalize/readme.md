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
