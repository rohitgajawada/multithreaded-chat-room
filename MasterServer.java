import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;
import java.net.InetAddress;

public class MasterServer {
	private static int uniqueId = 0;
	private ArrayList<ClientThread> al;
	private int port;
	private boolean keepGoing;
	private String[] chatRooms= new String[20];
	private String[] inWhichRoom= new String[20];
	private int roomCounter = 0;

	public MasterServer(int port) {
		this.port = port;
		al = new ArrayList<ClientThread>();
	}

	public void start() {
		keepGoing = true;
		try
		{
			ServerSocket serverSocket = new ServerSocket(port);
			while(keepGoing)
			{
				System.out.println("Server waiting for Clients on port " + port + ".");
				Socket socket = serverSocket.accept();
				if(!keepGoing)
					break;
				ClientThread t = new ClientThread(socket);
				al.add(t);
				t.start();
			}
			// stop server
			try {
				serverSocket.close();
				for(int i = 0; i < al.size(); ++i) {
					ClientThread tc = al.get(i);
					try {
					tc.sInput.close();
					tc.sOutput.close();
					tc.socket.close();
					}
					catch(IOException ioE) {
					}
				}
			}
			catch(Exception e) {
				System.out.println("Exception closing the server and clients: " + e);
			}
		}
		catch (IOException e) {
			System.out.println(e);
		}
	}

	// to broadcast a message to room members
	private synchronized boolean broadcast(String message) {

		System.out.println(message);
		for(int i = al.size(); --i >= 0;) {
			ClientThread ct = al.get(i);
			if(!ct.sendMessage(message)) {
				al.remove(i);
				System.out.println("Disconnected Client " + ct.username + " removed from list.");
			}
		}
		return true;
	}

	// if client logs out
	synchronized void remove(int id) {
		for(int i = 0; i < al.size(); ++i) {
			ClientThread ct = al.get(i);
			if(ct.id == id) {
				al.remove(i);
				break;
			}
		}
	}

	public static void main(String[] args) {
		int portNumber = 6006;
		try {
			portNumber = Integer.parseInt(args[0]);
		}
		catch(Exception e) {
			System.out.println("Give a proper port number");
			return;
		}

		MasterServer server = new MasterServer(portNumber);
		server.start();
	}

	class ClientThread extends Thread
	{
		Socket socket;
		DataInputStream sInput;
		DataOutputStream sOutput;
		int id;
		String username;

		ClientThread(Socket socket) {
			id = ++uniqueId;
			inWhichRoom[id] = "Lobby";
			this.socket = socket;
			try
			{
				sOutput = new DataOutputStream(socket.getOutputStream());
				sInput  = new DataInputStream(socket.getInputStream());
				username = sInput.readUTF();
				broadcast(username + " has joined Lobby");
			}
			catch (IOException e) {
				System.out.println(e);
				return;
			}
		}

		public void run()
		{
			boolean keepGoing = true;
			BufferedOutputStream bos = null;

			while(keepGoing)
			{
				String message;
				try {
					message = sInput.readUTF();
				}
				catch (IOException e) {
					System.out.println(username + " unable to read streams: " + e);
					break;
				}
				String[] elements = message.split(" ");

				if(elements[0].equalsIgnoreCase("list"))
				{
					if(elements[1].equalsIgnoreCase("users"))
					{
						String senderRoom = inWhichRoom[id];
						for(int i = 0; i < al.size(); ++i)
						{
							ClientThread ct = al.get(i);
							String recRoom = inWhichRoom[ct.id];
							if(recRoom.equals(senderRoom))
							{
								sendMessage((i+1) + ") " + ct.username);
							}
						}
					}
					else if(elements[1].equalsIgnoreCase("chatrooms"))
					{
						for(int i = 0; i < roomCounter; ++i) {
							sendMessage((i+1) + ") " + chatRooms[i]);
						}
					}
				}

				else if(elements[0].equalsIgnoreCase("add"))
				{
					if(!inWhichRoom[id].equalsIgnoreCase("Lobby"))
					{
						for(int i = 0; i < al.size(); ++i)
						{
							ClientThread ct = al.get(i);
							// System.out.println(ct.username + ":::" + inWhichRoom[ct.id]);
							if(inWhichRoom[ct.id].equalsIgnoreCase("Lobby") && ct.username.equalsIgnoreCase(elements[1]))
							{
								System.out.println(ct.username + " joined " + inWhichRoom[id]);
								inWhichRoom[ct.id] = inWhichRoom[id];
							}
						}
					}
					else {
						sendMessage("You can't add people if you are in Lobby");
					}
				}

				else if(elements[0].equalsIgnoreCase("join"))
				{
					inWhichRoom[id] = elements[1];
					System.out.println(username + " joined " + inWhichRoom[id]);
				}

				else if(elements[0].equalsIgnoreCase("create"))
				{
					if(elements[1].equalsIgnoreCase("chatroom"))
					{
						chatRooms[roomCounter] = elements[2];
						roomCounter += 1;
						sendMessage("New chatroom created");
					}
				}

				else if(elements[0].equalsIgnoreCase("logout"))
				{
						System.out.println(username + " disconnected");
						keepGoing = false;
						break;
				}

				else if(elements[0].equalsIgnoreCase("leave"))
				{
						System.out.println(username + " left " + inWhichRoom[id]);
						inWhichRoom[id] = "Lobby";
				}

				else if(elements[0].equalsIgnoreCase("send"))
				{
					boolean flag = elements[1].equalsIgnoreCase("tcp") || elements[1].equalsIgnoreCase("udp");
					if(flag)
					{
						byte[] mybytearray = new byte[1024];
						try {
							Socket socky = new Socket(InetAddress.getByName("localhost"), 5000);
					    FileOutputStream fos = new FileOutputStream("temp_" + elements[2]);
					    bos = new BufferedOutputStream(fos);
					    // int bytesRead = sInput.read(mybytearray, 0, mybytearray.length);
							int bytesRead = 0;
							InputStream is = socky.getInputStream();

			        while((bytesRead=is.read(mybytearray))!=-1)
			            bos.write(mybytearray, 0, bytesRead);

			        bos.flush();
							socky.close();
			        System.out.println("File saved successfully!");
							// bos.write(mybytearray, 0, bytesRead);
	    				bos.close();
							fos.close();
						}
						catch(Exception e) {
							System.out.println(e);
						}


						// BufferedInputStream bis = null;
						// File myFile = new File("temp_" + elements[2]);
	 				 	// mybytearray = new byte[(int) myFile.length()];
						//
						// try {
	 					//   bis = new BufferedInputStream(new FileInputStream(myFile));
	 				  // }
	 				  // catch(FileNotFoundException fe) {
	 					//   System.out.println(fe);
	 				  // }

						String senderRoom = inWhichRoom[id];
						for(int i = al.size(); --i >= 0;)
						{
							ClientThread ct = al.get(i);
							String recRoom = inWhichRoom[ct.id];
							if(recRoom.equals(senderRoom) && (id != ct.id))
							{
							 System.out.println("Sending to " + ct.username);
							 ct.sendMessage("Download " + elements[2]);
							 // try {
							 // 	bis.read(mybytearray, 0, mybytearray.length);
								// ct.sOutput.write(mybytearray, 0, mybytearray.length);
			       	 //  ct.sOutput.flush();
							 // }
							 // catch(IOException ioe) {
								//  System.out.println(ioe);
							 // }
							 try {
								 ServerSocket ssock = new ServerSocket(5001);
			        	 Socket soc = ssock.accept();

			        	//The InetAddress specification
			        	 InetAddress IA = InetAddress.getByName("localhost");
								 File myFile = new File("temp_" + elements[2]);
								 BufferedInputStream bis = new BufferedInputStream(new FileInputStream(myFile));
			 					 // mybytearray = new byte[(int) myFile.length()];
								 // System.out.println("h1");

								  byte[] contents;
					        long fileLength = myFile.length();
					        long current = 0;
									// System.out.println("h2");
					        long start = System.nanoTime();
									OutputStream os = soc.getOutputStream();
					        while(current!=fileLength){

					            int size = 1024;
					            if(fileLength - current >= size)
					                current += size;
					            else{
					                size = (int)(fileLength - current);
					                current = fileLength;
					            }
					            contents = new byte[size];
					            bis.read(contents, 0, size);
					            os.write(contents);
					            // System.out.print("Sending file ... "+(current*100)/fileLength+"% complete!");
					        }
									bis.close();
									os.flush();
									soc.close();
									ssock.close();
							 }
							 catch(IOException ioe) {
								 System.out.println(ioe);
							 }
							}
						}
					}
					else if(elements[1].equalsIgnoreCase("Udp"))
					{
						String countval;
						int counter;
						try {
			         DatagramSocket clientSocket = new DatagramSocket();
			         InetAddress IPAddress = InetAddress.getByName("localhost");
			         byte[] sendData = new byte[1];
			         String sentence = "Contacting";
			         sendData = sentence.getBytes();
			         DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, 5067);

							 countval = sInput.readUTF();
							 // System.out.println(countval);
							 counter = Integer.parseInt(countval);
			         //Receive
			         Files.write(Paths.get("temp_" + elements[2]), "".getBytes());
			         for(int i = 0 ; i < counter ; i++){
			            byte[] receiveData = new byte[1];
			            // Getting the data
			            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
			            clientSocket.send(sendPacket);
									System.out.println("entered here");
			            clientSocket.receive(receivePacket);
			            String data = new String(receivePacket.getData());
			            Files.write(Paths.get("temp_" + elements[2]), data.getBytes(), APPEND);
			         }
			         // System.out.println(" ");
			         clientSocket.close();
			      }
			      catch(Exception e) {
			         System.out.println(e);
			      }

						byte[] receiveData = new byte[1024];
						int count = 0;
						byte[] data = "0".getBytes();
						int sizeOfMessage=1;
			      try
						{
			         data= Files.readAllBytes(Paths.get(elements[2]));
			         sizeOfMessage=1;
			         count = data.length/sizeOfMessage;
			         if(data.length % sizeOfMessage != 0){
			            count++;
			         }
					 	}
						 catch(Exception e){
							 System.out.println(e);
						 }

						// String senderRoom = inWhichRoom[id];
						// for(int i = al.size(); --i >= 0;)
						// {
						// 	ClientThread ct = al.get(i);
						// 	String recRoom = inWhichRoom[ct.id];
						// 	if(recRoom.equals(senderRoom) && (id != ct.id))
						// 	{
						// 	 ct.sendMessage("UDP " + elements[2]);
						//
						// 	 try {
						// 		 int currentCount=0;
  					// 		 sOutput.writeUTF(Integer.toString(count));
  			    //      sOutput.flush();
  			    //      byte[] sendFile = new byte[1];
  			    //      DatagramSocket serverSocket = new DatagramSocket(5050);
						// 		 while(currentCount < count)
						// 		 {
				    //         int j=0;
				    //         for(int z = currentCount*sizeOfMessage ; z < data.length && z < (currentCount + 1)*sizeOfMessage ; z++){
				    //            sendFile[j] = data[z];
				    //            j++;
				    //         }
				    //         currentCount++;
				    //         DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
				    //         serverSocket.receive(receivePacket);
				    //         String a = new String(receivePacket.getData());
				    //         InetAddress IPAddress = receivePacket.getAddress();
				    //         int port = receivePacket.getPort();
				    //         DatagramPacket sendPacket = new DatagramPacket(sendFile, sendFile.length, IPAddress, port);
				    //         serverSocket.send(sendPacket);
						//
						// 				for(j=0; j < 1 ; j++) {
				    //            sendFile[j] = 0;
				    //         }
						// 			}
						// 			serverSocket.close();
						// 	 }
						// 	 catch(IOException ioe) {
						// 		 System.out.println(ioe);
						// 	 }
							// }
						// }

					}
				}

				else
				{
					String senderRoom = inWhichRoom[id];
					for(int i = al.size(); --i >= 0;)
					{
						ClientThread ct = al.get(i);
						String recRoom = inWhichRoom[ct.id];
						if(recRoom.equals(senderRoom)  && (id != ct.id))
						{
							if(!ct.sendMessage(message))
							{
								al.remove(i);
								System.out.println("Disconnected Client " + ct.username + " removed from list.");
							}
						}
					}
				}
			}
			remove(id);
			close();
		}

		private void close() {
			try {
				if(sOutput != null) sOutput.close();
			}
			catch(Exception e) {}
			try {
				if(sInput != null) sInput.close();
			}
			catch(Exception e) {};
			try {
				if(socket != null) socket.close();
			}
			catch (Exception e) {}
		}

		private boolean sendMessage(String msg) {
			if(!socket.isConnected()) {
				close();
				return false;
			}
			try {
				sOutput.writeUTF(msg);
			}
			catch(IOException e) {
				System.out.println("Error sending message to " + username);
				System.out.println(e.toString());
			}
			return true;
		}
	}
}
