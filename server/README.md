# Server

To run on a local cluster:

```bash
lein run
# OR
lein run -m server.topology/run! debug false workers 10
```

To run on a distributed cluster:

```bash
lein uberjar
# copy jar to nimbus, and then on nimbus:
bin/storm jar path/to/uberjar.jar server.TopologySubmitter workers 30 debug false
```

or use `[storm-deploy](https://github.com/nathanmarz/storm-deploy/wiki)`
