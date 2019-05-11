import java.net.*;
import java.io.*;
import java.util.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import static java.nio.file.StandardOpenOption.APPEND;
import java.net.InetAddress;

public class Client
{
	public static DataInputStream sInput;
	public static DataOutputStream sOutput;
	public Socket socket;
	public String server;
	public static String username;
	public int port;

	Client(String server, int port, String username)
	{
		this.server = server;
		this.port = port;
		this.username = username;
	}

	public boolean start()
	{
		try {
			socket = new Socket(server, port);
		}
		catch(Exception ec) {
			System.out.println("Error, can't connect to the server:" + ec);
			return false;
		}

		String msg = "Connection accepted " + socket.getInetAddress() + ":" + socket.getPort();
		System.out.println(msg);

		try
		{
			sInput  = new DataInputStream(socket.getInputStream());
			sOutput = new DataOutputStream(socket.getOutputStream());
		}
		catch (IOException eIO) {
			System.out.println("Exception creating new Input/output Streams: " + eIO);
			return false;
		}
		try
		{
			sOutput.writeUTF(username);
		}
		catch (IOException eIO) {
			System.out.println("Exception doing login : " + eIO);
			return false;
		}
		ListenFromMasterServer masterListener = new ListenFromMasterServer();
		masterListener.start();
		return true;
	}

	private void disconnect() {
		try {
			if(sInput != null) sInput.close();
		}
		catch(Exception e) {}
		try {
			if(sOutput != null) sOutput.close();
		}
		catch(Exception e) {}
        try{
			if(socket != null) socket.close();
		}
		catch(Exception e) {}
	}

	void sendMessage(String msg) {
		try {
			sOutput.writeUTF(msg);
		}
		catch(IOException e) {
			System.out.println("Exception writing to server: " + e);
		}
	}


	public static void main(String[] args)
	{
		int portNumber = 6006;
		String serverAddress = "localhost";
		Scanner scan = new Scanner(System.in);

		System.out.println("Enter the username: ");
		String userName = scan.nextLine();

		// Read port number, server address
		// Example 6006, localhost
		serverAddress = args[1];
		try {
			portNumber = Integer.parseInt(args[0]);
		}
		catch(Exception e) {
			System.out.println("Give a proper port number");
			return;
		}
		Client client = new Client(serverAddress, portNumber, userName);
		if(!client.start())
			return;

		String whichRoom = "Lobby";

		while(true)
		{
			System.out.print(">> ");
			String message = scan.nextLine();
			String[] elements = message.split(" ");
			client.sendMessage(message);
			BufferedInputStream bis = null;

			if(elements[0].equalsIgnoreCase("send"))
			{
				 boolean flag = elements[1].equalsIgnoreCase("tcp") || elements[1].equalsIgnoreCase("udp");
				 if(flag)
				 {
					 try{
						 	 // System.out.println("Come here");
							 ServerSocket ssock = new ServerSocket(5000);
		        	 Socket socker = ssock.accept();

		        	//The InetAddress specification
		        	 InetAddress IA = InetAddress.getByName("localhost");


						 File myFile = new File(elements[2]);
						 // byte[] mybytearray = new byte[(int) myFile.length()];

		      	 try {
							 bis = new BufferedInputStream(new FileInputStream(myFile));
						 }
						 catch(FileNotFoundException fe) {
							 System.out.println(fe);
						 }

						 try {
							  byte[] contents;
				        long fileLength = myFile.length();
				        long current = 0;

								// sOutput.writeUTF(Long.toString(fileLength));

				        long start = System.nanoTime();
								OutputStream os = socker.getOutputStream();
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
								socker.close();
								ssock.close();
						}
							catch(Exception e){
	 						 System.out.println(e);
	 					 }
					 	// bis.read(mybytearray, 0, mybytearray.length);
						// sOutput.write(mybytearray, 0, mybytearray.length);
	       	  // sOutput.flush();
					 }
					 catch(IOException ioe) {
						 System.out.println(ioe);
					 }
			 	}
				else if(elements[1].equalsIgnoreCase("Udp"))
				{
					byte[] receiveData = new byte[1024];
		      try
					{
		         byte[] data= Files.readAllBytes(Paths.get(elements[2]));
		         int messagesSize=1;
		         int count = data.length/messagesSize;
		         if(data.length % messagesSize != 0){
		            count++;
		         }
		         int currentCount=0;
						 sOutput.writeUTF(Integer.toString(count));
		         sOutput.flush();
		         byte[] sendFile = new byte[1];
		         DatagramSocket serverSocket = new DatagramSocket(5067);
		         while(currentCount < count)
						 {
		            int j=0;
		            for(int i = currentCount*messagesSize ; i < data.length && i < (currentCount + 1)*messagesSize ; i++){
		               sendFile[j] = data[i];
		               j++;
		            }
		            currentCount++;
		            // THIS IS THE SENDING PART
		            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
		            serverSocket.receive(receivePacket);
		            String a = new String(receivePacket.getData());
		            InetAddress IPAddress = receivePacket.getAddress();
		            int port = receivePacket.getPort();
		            DatagramPacket sendPacket = new DatagramPacket(sendFile, sendFile.length, IPAddress, port);
		            serverSocket.send(sendPacket);

								for(j=0; j < 1 ; j++) {
		               sendFile[j] = 0;
		            }
							}
							serverSocket.close();
						}
					catch(Exception e) {
		         System.out.println(e);
		      }
				}
			}

			if(elements[0].equalsIgnoreCase("leave")) {
				whichRoom = "Lobby";
			}
			else if(elements[0].equalsIgnoreCase("logout")) {
				client.disconnect();
				break;
			}
		}
	}

	public static class ListenFromMasterServer extends Thread {

		public void run() {
			while(true) {
				try {
					String msg = sInput.readUTF();
					String[] msgElems = msg.split(" ");
					if(msgElems[0].equalsIgnoreCase("Download"))
					{
						// byte[] mybytearray = new byte[1000000];
				    // FileOutputStream fos = new FileOutputStream(msgElems[1]);
				    // BufferedOutputStream bos = new BufferedOutputStream(fos);
				    // int bytesRead = sInput.read(mybytearray, 0, mybytearray.length);
				    // bos.write(mybytearray, 0, bytesRead);
				    // bos.close();
						// System.out.println("Downloading start");
						byte[] mybytearray = new byte[1024];
						try {
							System.out.println("Downloading");
							Socket soc = new Socket(InetAddress.getByName("localhost"), 5001);
							// System.out.println("1");
					    FileOutputStream fos = new FileOutputStream(username + "__" + msgElems[1]);
							// System.out.println("223");
					    BufferedOutputStream bos = new BufferedOutputStream(fos);
							// System.out.println("22");
					    // int bytesRead = sInput.read(mybytearray, 0, mybytearray.length);
							int bytesRead = 0;
							InputStream is = soc.getInputStream();

							// System.out.println("3");
			        while((bytesRead=is.read(mybytearray))!=-1)
			            bos.write(mybytearray, 0, bytesRead);

							// System.out.println("4");

			        System.out.println("File saved successfully!");
							// bos.write(mybytearray, 0, bytesRead);
							bos.flush();
							soc.close();
	    				bos.close();
							fos.close();

						}
						catch(Exception e) {
							System.out.println(e);
						}
					}
					else if(msgElems[0].equalsIgnoreCase("UDP"))
					{
						String countval;
						int counter;
						try {
			         DatagramSocket clientSocket = new DatagramSocket();
			         InetAddress IPAddress = InetAddress.getByName("localhost");
			         byte[] sendData = new byte[1];
			         String sentence = "Contacting";
			         sendData = sentence.getBytes();
			         DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, 5050);

							 countval = sInput.readUTF();
							 // System.out.println(countval);
							 counter = Integer.parseInt(countval);
			         //Receive
			         Files.write(Paths.get(username + msgElems[1]), "".getBytes());
			         for(int i = 0 ; i < counter ; i++){
			            byte[] receiveData = new byte[1];
			            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
			            clientSocket.send(sendPacket);
			            clientSocket.receive(receivePacket);
			            String data = new String(receivePacket.getData());
			            Files.write(Paths.get(username + msgElems[1]), data.getBytes(), APPEND);
			         }
			         // System.out.println(" ");
			         clientSocket.close();
			      }
			      catch(Exception e) {
			         System.out.println(e);
			      }
					}
					else
					{
						System.out.println(msg);
						System.out.print(">> ");
					}
				}
				catch(IOException e) {
					System.out.println("Server has closed the connection: " + e );
					break;
				}
			}
		}
	}
}
