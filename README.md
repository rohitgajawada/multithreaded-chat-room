Rohit Gajawada
201401067

Compile with
javac Client.java;
javac MasterServer.java;

Then run server with:
java MasterServer 6006;

Then run clients with:
java Client 6006 localhost;

1) create chatroom x :: for making room x
2) list chatrooms
3) list users
4) join x :: for joining x
5) add x :: for adding user x to your room only if you are in a room
6) send tcp file :: for sending file
8) once in a chatroom, simply typing anything will broadcast it to all users in your room
# multithreaded-chat-room
