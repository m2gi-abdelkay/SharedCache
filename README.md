# SharedCache

Project organization as follows:
bin : contains compiled classes
src/irc: contains chat app used for testing
src/jvn:
- Client: Directory for client
- Coordinator: Directory for coordinator (coordinator itself + implemented interface)
- Server: Directory for server (server itself + implemented interface)
- Handler: Directory for invocation handler
- Annotations: Directory for defined annotations
- Utils: All interfaces + exception class + MyObject class (class we will use to initialize shared objects + add methods)
Makefile: This will be used to compile the project. Run 'make help' to view list of commands, but essentially:
1) Make compile
2) Make setup
3) Make run-coordinator
4) Make run-irc NAME=#yourObjectName
5) Make stop-registry

To run the stress test:
1) cd bin
2) java jvn.Client.StressTestClient


We also implement two extensions:
- Client Crashing: Using a shutdown hook, the client terminates his server and the server calls the coordinator to deal with the termination.
- Coordinator Crashing: We save the state of the coordinator in a file that we load once we start again. We don't however save the servers that have a lock on the objects (since they're RMI stubs), so the coordinator tells the servers before crashing to remove their locks. Once the coordinator is back online, the servers request locks as usual.