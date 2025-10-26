# SharedCache

Project organization as follows:
bin : contains compiled classes
src/irc: contains chat app used for testing
src/jvn:
- Client: File for client
- Coordinator: File for coordinator (coordinator itself + implemented interface)
- Server: File for server (server itself + implemented interface)
- Utils: All interfaces + exception class + MyObject class (class we will use to initialize shared objects + add methods)
Makefile: This will be used to compile the project. Run 'make help' to view list of commands, but essentially:
1) Make compile
2) Make setup
3) Make run-coordinator (then same for server and client)
4) Make stop-registry

Yes, AI was used to write this Makefile (because who writes makefiles anymore)