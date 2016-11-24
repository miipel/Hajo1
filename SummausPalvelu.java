package hajo1;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;

/**
 * 
 * @author Miika
 *
 */
public class SummausPalvelu {

	public static void main(String[] args) throws SocketException {
		DatagramSocket socketUDP = new DatagramSocket();
		Socket socketTCP = new Socket();

	} // main

	static class SummausPalvelija extends Thread {
		@Override
		public void run(){
			
		}
	} // class SummausPalvelija
	
} // class Sovellus

