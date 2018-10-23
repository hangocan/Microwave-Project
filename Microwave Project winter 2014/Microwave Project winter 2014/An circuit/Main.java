package client;

import java.io.IOException;




public class Main {
	
	
	
	
	public static void main(String[] args) throws IOException {

		 SipClient client = new SipClient();	
		 Thread thread1 = new Thread(client);
		 thread1.start();
		
		 // client.sendMessage("hello", "sip:test@10.51.172.171:5060");
		 
		  // Logger logger = Logger.getLogger();
		  // logger.openlogWindow("05.12.2014");
		
		 
		 
		 
		
		// 	sleep(3*1000);
		//	System.out.println("exit");
		//	System.exit(0);
		//	System.out.println("exit 2");
	}



}
