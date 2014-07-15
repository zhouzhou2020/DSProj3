package node;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import mapred.Job;
import mapred.Task;
import communication.Message;
import communication.WriteFileMsg;
import config.ParseConfig;

public class Scheduler extends Thread{
	//slave pool
	public static ConcurrentHashMap<Integer,SlaveInfo> slavePool = new ConcurrentHashMap<Integer,SlaveInfo>();
	//job pool
	public static ConcurrentHashMap<Integer, Job> jobPool = new ConcurrentHashMap<Integer, Job>();
	//file layout record key: slaveInfo value : fileName with blk id
	public static ConcurrentHashMap<String,ArrayList<SlaveInfo>> fileLayout = new ConcurrentHashMap<String,ArrayList<SlaveInfo>>();
	public static int slaveId = 0;
	public static int curJobId = 0;
	ServerSocket listener;
	public Scheduler(int port) throws IOException{
		listener = new ServerSocket(port);
	}
	
	public void run() {
		
		while(true) {
			Socket socket = null;
			Message msg = null;
			ParseConfig conf = null;
			try {
				socket = listener.accept();
				conf = MasterMain.conf;
				msg = Message.receive(socket);
				System.out.println("the scheduler receives a "+msg.getType()+" messge " + msg.getContent().toString());
				
			} catch (Exception e) {
				System.out.println("There is no message " + e.toString());
				continue;
			}
			
			switch (msg.getType()) {
			case REG_NEW_SLAVE:
				regNewSlaveHandler(socket);
				break;
				
			case FILE_PUT_REQ_TO_MASTER:
				
				WriteFileMsg writeFileMsg = (WriteFileMsg) msg.getContent(); 
				System.out.println("name:" + writeFileMsg.fileBaseName +" blk:" + writeFileMsg.fileBlk);
				ArrayList<SlaveInfo> slaveList = null; //slaveList to be sent to client
				for(int rep = 0; rep <= conf.Replica; rep++) {
					System.out.println("cur rep is: " + rep);
					if(slaveList==null) {
						slaveList = fileLayoutGenerate(slavePool, writeFileMsg,false);
					}else {
						slaveList.addAll(fileLayoutGenerate(slavePool,writeFileMsg,true));
					}
					
				}
				//console debug
				System.out.println(msg.getContent().toString() + "files");
				//generate file layout policy
				System.out.println("slave list are :");
				for(SlaveInfo info : slaveList) {
					
					System.out.println(info.slaveId);
				}
				filePutReqToMasterHandler(socket,msg,slaveList);
				
				/*
				 * for testing...
				 */
				System.out.println("SlavePool:");
				for(int i : slavePool.keySet()) {
					System.out.println("id: " + i + "\t" + slavePool.get(i).address);
				}
				System.out.println("File Layout:");
				for(String fileName : fileLayout.keySet()) {
					for(SlaveInfo info : fileLayout.get(fileName)){
						System.out.println("filename: " + fileName + "\t" + info.slaveId);
					}
				}
			
				
				break;
				
			case NEW_JOB:
				submitJob((Job) msg.getContent(), socket);
				
				break;
			
			
			default:
				break;
			}

		}
	}


	
	private void submitJob(Job job, Socket socket) {
		job.setJobId();
		
		jobPool.put(job.getJobId(), job);
		// how to copy the file to the master? here is the master
		HashMap<String, SlaveInfo> fileToSlave = new HashMap<String, SlaveInfo>(); 
//		ArrayList<SlaveInfo> slavesWithFile = new ArrayList<SlaveInfo>();
		String baseFileName = job.getInputFileName();
		int length = baseFileName.length();
		ArrayList<SlaveInfo> tempSlave = new ArrayList<SlaveInfo>();
		for(String fileName : fileLayout.keySet()){
			if(fileName.substring(0, length).equals(baseFileName)){
				fileToSlave.put(fileName,fileLayout.get(fileName).get(0));
			}
		}
		
		/*
		 * for test
		 */
		for(String file : fileToSlave.keySet()){
			System.out.println(file + "is on " + fileToSlave.get(file).slaveId);
		}
		
	
		//assign the tasks to different slaves
		//send file name to each slaves via socket
		for(String file : fileToSlave.keySet()){
			SlaveInfo curSlave = fileToSlave.get(file);
			try {
				//build soc
				Socket soc =  new Socket(curSlave.address.getHostName(),MasterMain.conf.SlaveMainPort);
				Task task = (Task)job;
				Message taskMsg = new Message(Message.MSG_TYPE.NEW_TASK, task);
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		
	}

	/*
	 * used for generate file replica policy
	 */
	/*
	private void fileReplicaHandler(WriteFileMsg writeFileMsg, int replica) {
		Random rng = new Random();
		String baseFileName = writeFileMsg.fileBaseName;
		for(int i = 0; i <= writeFileMsg.fileBlk;i++){
			int curRep = 0;
			while(curRep != MasterMain.conf.Replica){
				Integer next = rng.nextInt(slavePool.size());
				if(fileLayout.get(baseFileName + "_blk" +i).contains(slavePool.get(next))) {
					continue;
				}else {
					ArrayList<SlaveInfo> curSlaveList = fileLayout.get(writeFileMsg.fileBaseName + "_blk" + i);
					curSlaveList.add(slavePool.get(next));
					fileLayout.put(writeFileMsg.fileBaseName + "_blk" + i,curSlaveList);
					curRep++;
					break;
				}
			}
			
		}
		
	}
	*/

	/*
	 * Generate file layout policy and update fileLayout table.
	 * Notice, replica is not included
	 */
	private ArrayList<SlaveInfo> fileLayoutGenerate(ConcurrentHashMap<Integer, SlaveInfo> slavePool, WriteFileMsg writeFileMsg, boolean isReplica) {
		if(!isReplica){//when it is first time upload
			Random rng = new Random(); 
//			Set<Integer> idSet = new HashSet<Integer>();
			ArrayList<Integer> idSet = new ArrayList<Integer>();
			//select random slave id for file to be input
			//======
			//trad-off: it maintains load balance, but when there is not enough slave nodes, the function will crush.
			//======
			
			while (idSet.size() <= writeFileMsg.fileBlk)
			{
			    Integer next = rng.nextInt(slavePool.size());
//			    if(slavePool.contains(next)){
			    	 idSet.add(next); //if slave is down, its slave ID will not be used for a while.
			    	 System.out.println("ram next is " + next);
//			    }
			   
			}
			System.out.println("id set size is " + idSet.size());
			ArrayList<SlaveInfo> slaveList = new ArrayList<SlaveInfo>();
			int fileId = 0;
			Iterator it = idSet.iterator();
			while(it.hasNext()) {
				int slaveId = (Integer) it.next();
				
				SlaveInfo slaveInfo = slavePool.get(slaveId);
				slaveList.add(slaveInfo);
				if(fileLayout.keySet().contains(writeFileMsg.fileBaseName + "_blk" + fileId)) {
//					System.out.println("contains is true");
					ArrayList<SlaveInfo> curSlaveList = fileLayout.get(writeFileMsg.fileBaseName + "_blk" + fileId);
					curSlaveList.add(slaveInfo);
					fileLayout.put(writeFileMsg.fileBaseName + "_blk" + fileId,curSlaveList);
				}else {
					ArrayList<SlaveInfo> newSlaveList = new ArrayList<SlaveInfo>();
					newSlaveList.add(slaveInfo);
					fileLayout.put(writeFileMsg.fileBaseName + "_blk" + fileId,newSlaveList);
				}
				fileId++;
			}
			return slaveList;
		} else {//when this is replica
			ArrayList<SlaveInfo> slaveList = new ArrayList<SlaveInfo>();
			for(int i = 0 ; i <= writeFileMsg.fileBlk; i++) {
				//replica policy is put replica into slave id which is 1 bigger than previous copy.
				ArrayList<SlaveInfo> tempList = fileLayout.get(writeFileMsg.fileBaseName + "_blk" + i);
				SlaveInfo last = tempList.get(tempList.size()-1);
				SlaveInfo repSlave = slavePool.get((last.slaveId+1)%slavePool.size());
				slaveList.add(repSlave);
				//update file layout
				tempList.add(repSlave);
				fileLayout.put(writeFileMsg.fileBaseName + "_blk" + i, tempList);
			}
			return slaveList;
		}
		
	}

	/*
	 * reply client which slaves are to be connected to upload file
	 */
	private void filePutReqToMasterHandler(Socket socket, Message msg, ArrayList<SlaveInfo> slaveList) {
		System.out.println("Message received from " + socket.getRemoteSocketAddress() + " type: FILE_PUT_REQ_TO_MASTER; content: " + msg.getContent().toString());
		Message reply = new Message(Message.MSG_TYPE.AVAIL_SLAVES, slaveList);
		
		//tell the client which slaves are available
		try {
			
			reply.send(socket);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		System.out.println("send the slave list from the master to the client");
		
		
	}

	/*
	 * update slavePool when a new slave register on master
	 * @params socket
	 */
	private void regNewSlaveHandler(Socket socket) {
		InetAddress address = socket.getInetAddress();
		SlaveInfo slave = new SlaveInfo(slaveId, address);
		System.out.println("connect to slave "+slave.slaveId+ " "+ address);
		slavePool.put(slaveId,slave);
		//notice:put first then add slaveid
		slaveId++;
	}


}
