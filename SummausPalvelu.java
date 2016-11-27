

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
 * @version 27.11.2016 13:50
 */
public class SummausPalvelu {
	private static int[] porttiNumerot; 
	private boolean yhteysValmis;

	private static void main(String[] args) throws Exception {		
		lahetaUDP();			
		kuunteleTCP();
		// SummausPalvelija.start();
		

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
		// Kuuntele 1-5 s, sen j�lkeen l�het� uudelleen
		// Viidennen uudelleen l�hetyksen j�lkeen terminate
		Socket soketti;
		int porttiNo = 0;
		int yrityskerta = 0;
		
		
		while (yrityskerta < 5) {
			try {
				lahetaUDP(); // l�hetet��n UDP paketti asiakkaalle
				soketti = new Socket(InetAddress.getLocalHost(), porttiNo);
				soketti.setSoTimeout(5000); // soketti odottaa yhteydenottoa 5 sek.
				//avataan oliovirrat
				OutputStream oS = soketti.getOutputStream();
				InputStream iS = soketti.getInputStream();
				ObjectOutputStream oOut = new ObjectOutputStream(oS);
				ObjectInputStream oIn = new ObjectInputStream(iS);
				try {
					// kun yhteys on saatu, l�hetet��n soketti ja oliovirta..
					// ..odotaT() -metodille, joka odottaa asiakkaalta kokonaislukua t
					odotaT(soketti, oOut, oIn);
				} catch (Exception e) {e.toString();}
			}catch (SocketException e) {yrityskerta++;}
		} // while			
	} // void kuuntele()
	
	private static void odotaT(Socket soketti, ObjectOutputStream oOut, ObjectInputStream oIn) throws Exception {
		// Saa parametreina aikaisemmin muodostetut oliovirrat ja soketin
		// Odottaa t:n arvoa oliovirrasta, jonka mukaan SummausPalvelijaa aletaan k�ytt�m��n
		int t;
		try {
			t = oIn.readInt(); // yritet��n lukea oliovirrasta kokonaislukua
			if (t >= 2 || t <= 10) { // tarkistetaan, kelpaako vastaanotettu luku
				porttiNumerot = new int[t]; // alustetaan porttiNumerot oikean kokoiseksi		
				// generoidaan porttinumero t-kertaa ja lis�t��n se porttiNumerot-taulukkoon
				for (int i = 0; i < t; i++) {
					porttiNumerot[i] = (int) (1025 + (Math.random() * 64510));
				}
				// kun kaikki porttinumerot on lis�tty taulukkoon,
				// l�hetet��n taulukko asiakkaalle ja k�ynnistet��n summauspalvelin toimimaan ko. porttiin
				for (int i = 0; i < porttiNumerot.length; i++) {
					oOut.writeInt(porttiNumerot[i]);
					oOut.flush();
					SummausPalvelija lukuWelho = new SummausPalvelija(porttiNumerot[i]);
				}
			}
			oOut.writeInt(-1); // jos t ei ole v�lilt� 2...10, niin l�hetet��n -1
			oOut.flush();
			soketti.close();   // ja suljetaan soketti.
		}catch (SocketException e) {oOut.writeInt(-1); oOut.flush();} // jos vastausta ei tule 5 sek. kuluessa, l�het� -1
		
	} // odotaT()

	static class SummausPalvelija extends Thread {
		/**
		 * @param portti: Portti, jota SummausPalvelija kuuntelee
		 * @param lukujenLkm: Vastaanotettujen lukujen lukum��r�
		 * @param lukujenSum: Vastaanotettujen lukujen summa
		 */
		int portti; 
		int lukujenLkm;
		int lukujenSum;
		
		private SummausPalvelija (int portti) {
			this.portti = portti;
			
		} // konstruktori
		@Override
		public void run(){
			
		}
	} // class SummausPalvelija
	
} // class SummausPalvelu