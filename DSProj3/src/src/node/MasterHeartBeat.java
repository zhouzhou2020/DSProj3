package node;

import java.net.Socket;
import java.util.ArrayList;

import communication.Message;
import config.ParseConfig;

public class MasterHeartBeat extends Thread {
	public void run() {
		System.out.println("start the heart beat thread on master");
		while (true) {		

			Message msg = new Message(Message.MSG_TYPE.KEEP_ALIVE, null);
			ParseConfig conf = MasterMain.conf;
			ArrayList<SlaveInfo> failList = new ArrayList<SlaveInfo>();

			for (SlaveInfo slave : MasterMain.slavePool.values()) {
				try {
					Socket socket = new Socket(slave.address,
							conf.SlaveHeartBeatPort);
					System.out.println("try to connect slave and send KEEP_ALIVE msg");
					msg.send(socket);
					System.out.println("send heart beat to slave");
					msg = Message.receive(socket);
					if (!msg.getType().equals(Message.MSG_TYPE.KEEP_ALIVE)) {
						System.out.println("the slave " + slave.slaveId
								+ " fails");
						failList.add(slave);
					}
				} catch (Exception e) {
					System.out.println("fail to connect the slave heart beat port");
					e.printStackTrace();
				}
				
			}
			if (failList.size() > 0) {
				MasterMain.handleDeadSlaves(failList);
				
			}
			
			try {
				sleep(conf.HearBeatFreq);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
	}
}
