

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * 
 * @author Miika Peltotalo ja Peetu Seilonen
 * @version 25.11.2016 13:10
 */
public class SummausPalvelu {

	private static void main(String[] args) throws Exception {		
		lahetaUDP();			
		kuunteleTCP();
		

	} // main

	private static void lahetaUDP() throws IOException {
		int porttiNo = 0;
		String portti = Integer.toString(porttiNo);
		DatagramSocket socketUDP = new DatagramSocket();
		byte[] data = portti.getBytes();
		DatagramPacket paketti = new DatagramPacket(data, data.length, InetAddress.getLocalHost(), 3126);		
		socketUDP.send(paketti);
		socketUDP.close();
		
	}

	private static void kuunteleTCP() throws IOException {
		// Kuuntele 1-5 s, sen jälkeen lähetä uudelleen
		// Viidennen uudelleen lähetyksen jälkeen terminate
		Socket soketti;
		int porttiNo = 0;
		int yrityskerta = 0;
		
		
		while (yrityskerta < 5) {
			try {
				lahetaUDP();
				soketti = new Socket(InetAddress.getLocalHost(), porttiNo);
				soketti.setSoTimeout(5000);
				OutputStream oS = soketti.getOutputStream();
				InputStream iS = soketti.getInputStream();
				ObjectOutputStream oOut = new ObjectOutputStream(oS);
				ObjectInputStream oIn = new ObjectInputStream(iS);
				
			}catch (SocketException e) {yrityskerta++;}
			
		} // while
		
	} // void kuuntele()
		

	static class SummausPalvelija extends Thread {
		@Override
		public void run(){
			
		}
	} // class SummausPalvelija
	
} // class SummausPalvelu