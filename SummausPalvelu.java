package hajo1;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * 
 * @author Miika Peltotalo ja Peetu Seilonen
 * @version 24.11.2016 21:51
 */
public class SummausPalvelu {

	private static void main(String[] args) throws Exception {		
		lahetaUDP();			
		kuuntele();
		

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

	private static void kuuntele() throws IOException {
		// Kuuntele 1-5 s, sen jälkeen lähetä uudelleen
		// Viidennen uudelleen lähetyksen jälkeen terminate
		
	}

	static class SummausPalvelija extends Thread {
		@Override
		public void run(){
			
		}
	} // class SummausPalvelija
	
} // class Sovellus

