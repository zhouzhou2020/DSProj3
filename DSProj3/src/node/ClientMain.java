package node;

import java.io.Console;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Scanner;

import communication.Message;
import config.ParseConfig;
import dfs.Splitter;

/*
 * for dfs:
 * 1. upload file
 * 2. split file
 * 3. get file
 */
public class ClientMain {
	public static enum CMD {
		
		put
		
	}
	public static void main(String[] args) {
		
		CMD cmd_type = null;
		
		//connect to master.
		Socket socket = null;
		try {
        	new ParseConfig(args[0]);
			socket = new Socket(ParseConfig.MasterIP, ParseConfig.MasterMainPort);
		} catch (Exception e) {
			System.out.println(e.toString());
		} 
        
		
		Scanner scanner = new Scanner(new InputStreamReader(System.in));
		System.out.println("Enter your cmd ");
		String cmd = scanner.nextLine();
		
        //accept cmd from console
        switch (CMD.valueOf(cmd)) {
		case put:
			Message msg = new Message(Message.MSG_TYPE.FILE_PUT_REQ_TO_MASTER, "I will upload a file later");
			try {
				msg.send(socket);
//				System.out.println(msg.receive(socket).getContent().toString());
				
				//get the slave list
				msg = Message.receive(socket);
				System.out.println("the client receives a message from the master");
				
				ArrayList<SlaveInfo> slaveList = (ArrayList<SlaveInfo>) msg.getContent();
				//connect the slaves via socket
				for (SlaveInfo s: slaveList) {
					InetAddress add = s.address;
//					InetSocketAddress add = (InetSocketAddress) s.address;
					System.out.println("here is all right");
					System.out.println("the client will connect:" +add +" "+ParseConfig.SlaveMainPort);
					socket = new Socket(add,ParseConfig.SlaveMainPort);
					
					msg = new Message(Message.MSG_TYPE.FILE_PUT_REQ_TO_SLAVE, "I will upload a file to the slave");
					System.out.println("receive the msg: "+msg.getContent());
					msg.send(socket);
					System.out.println("client send the msg to slave");
					
					msg = Message.receive(socket);
					System.out.println("the client receives a message from the slave");
					//connect the slave via the port assigned by the slave
					socket = new Socket(add, Integer.parseInt(msg.getContent().toString()));
					System.out.println("client connect to slave via assigned port "+msg.getContent().toString());
					
					
					//Notice!!! this msg is used for test... need to change the MSG Type!
					msg = new Message(Message.MSG_TYPE.FILE_PUT_START_TO_SLAVE,"start to put file");
					Splitter splitter = new Splitter("src/harrypotter.txt", 4194304L, "");
					FileInputStream in = new FileInputStream("harrypotter.txt_blk");
					msg.setContent("");
					msg.send(socket);
				}
				
			} catch (Exception e) {
				System.out.println("Some wrong with put message " + e.toString());
			}
			break;

		default:
			break;
		}
		
	}
	
}



/*package node;

import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Scanner;

import communication.Message;
import config.ParseConfig;

/*
 * for dfs:
 * 1. upload file
 * 2. split file
 * 3. get file
 
public class ClientMain {
	public static enum CMD {
		
		put
		
	}
	public static void main(String[] args) {
		System.out.println("start the client");
		CMD cmd_type = null;
		//connect to master.
		Socket socket = null;
		try {
        	socket = new Socket(ParseConfig.MasterIP, ParseConfig.MasterMainPort);
		} catch (Exception e) {
			System.out.println(e.toString());
		} 
        
		/*
		Scanner scanner = new Scanner(new InputStreamReader(System.in));
		System.out.println("Enter your cmd ");
		String cmd = scanner.nextLine();
		
        //accept cmd from console
        switch (CMD.valueOf(cmd)) {
		case put:
			Message msg = new Message(Message.MSG_TYPE.FILE_PUT_REQ_TO_MASTER, "I will upload a file later");
			try {
				msg.send(socket);
//				System.out.println(msg.receive(socket).getContent().toString());
				
				//get the slave list
				msg = Message.receive(socket);
				System.out.println("the client receives a message from the master");
				ArrayList<SlaveInfo> slaveList = (ArrayList<SlaveInfo>) msg.getContent();
				//connect the slaves via socket
				for (SlaveInfo s: slaveList) {
					InetAddress add = s.address;
					System.out.println("the client will connect:" +add.toString()+": "+ParseConfig.SlaveMainPort);
					socket = new Socket(add, ParseConfig.SlaveMainPort);

					msg = new Message(Message.MSG_TYPE.FILE_PUT_REQ_TO_SLAVE, "I will upload a file to the slave");
					System.out.println("client try to connect slave");
					msg.send(socket);
					
					msg = Message.receive(socket);
					System.out.println("the client receives the msg from slave: connect port" + msg.getContent());
					
				}
			} catch (Exception e) {
				System.out.println("Some wrong with put message " + e.toString());
			}
			break;

		default:
			break;
		}
		
	}
	
}
*/