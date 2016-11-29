package hajo1;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;

/**
 * 
 * @author Miika Peltotalo ja Peetu Seilonen
 * @version 29.11.2016 23:54
 * 
 * @var int[] porttiNumerot: sis�lt�� porttien numeroit, joita SummausPalvelin
 *      kuuntelee
 * @var boolean yhteysValmis: kun yhteys asiakkaaseen on saatu ja on aika
 *      ailoittaa SummausPalvelimen k�ytt�
 * @var ArrayList<int> luvut: t�nne ker�t��n kaikki vastaanotetut luvut
 * @var ArrayList<Thread> summaajat: kaikki s�ikeet samassa nipussa
 */
public class SummausPalvelu {
	private static int[] porttiNumerot;
	private static ArrayList<Integer> luvut = new ArrayList<Integer>();
	private static ArrayList<Thread> summaajat = new ArrayList<Thread>();
	private static boolean yhteysValmis;

	public static void main(String[] args) throws Exception {
		// lahetaUDP();
		muodostaTCP();

	} // main

	private static void lahetaUDP() throws IOException {
		int porttiNo = 1337;
		String portti = Integer.toString(porttiNo);
		DatagramSocket socketUDP = new DatagramSocket();
		byte[] data = portti.getBytes();
		DatagramPacket paketti = new DatagramPacket(data, data.length, InetAddress.getLocalHost(), 3126);
		socketUDP.send(paketti);
		socketUDP.close();
		System.out.println("UDP lahetetty");

	}

	private static void muodostaTCP() throws IOException {
		// Kuuntele 1-5 s, sen j�lkeen l�het� uudelleen
		// Viidennen uudelleen l�hetyksen j�lkeen terminate
		int porttiNo = 1337;
		int yrityskerta = 0;
		ServerSocket kuuntelevaSoketti = new ServerSocket(porttiNo);
		
		while (yrityskerta < 5) {
			try {
				lahetaUDP(); // l�hetet��n UDP paketti asiakkaalle
				kuuntelevaSoketti.setSoTimeout(5000); // soketti odottaa yhteydenottoa 5 sek
				Socket soketti = kuuntelevaSoketti.accept();
				System.out.println("TCP muodostettu");

				// avataan oliovirrat
				OutputStream oS = soketti.getOutputStream();
				InputStream iS = soketti.getInputStream();
				ObjectOutputStream oOut = new ObjectOutputStream(oS);
				ObjectInputStream oIn = new ObjectInputStream(iS);
				try {
					// kun yhteys on saatu, l�hetet��n soketti ja oliovirta..
					// ..odotaT() -metodille, joka odottaa asiakkaalta
					// kokonaislukua t
					odotaT(soketti, oOut, oIn);
				} catch (Exception e) {
					e.toString();
				}
			} catch (SocketException e) {
				yrityskerta++;
				System.out.println("Ei onnistunut");
			}
		} // while
	} // void kuuntele()

	private static void odotaT(Socket soketti, ObjectOutputStream oOut, ObjectInputStream oIn) throws Exception {
		// Saa parametreina aikaisemmin muodostetut oliovirrat ja soketin
		// Odottaa t:n arvoa oliovirrasta, jonka mukaan SummausPalvelijaa
		// aletaan k�ytt�m��n
		int t;
		try {
			t = oIn.readInt(); // yritet��n lukea oliovirrasta kokonaislukua
			System.out.println("Summauspalvelu luo " + t + " porttia");
			if (t >= 2 || t <= 10) { // tarkistetaan, kelpaako vastaanotettu
										// luku
				porttiNumerot = new int[t]; // alustetaan porttiNumerot oikean
											// kokoiseksi
				// generoidaan porttinumero t-kertaa ja lis�t��n se
				// porttiNumerot-taulukkoon
				for (int i = 0; i < t; i++) {
					porttiNumerot[i] = (int) (1025 + (Math.random() * 64510));
				}
				// kun kaikki porttinumerot on lis�tty taulukkoon,
				// l�hetet��n taulukko asiakkaalle ja k�ynnistet��n
				// summauspalvelin toimimaan ko. porttiin
				for (int i = 0; i < porttiNumerot.length; i++) {					
					oOut.writeInt(porttiNumerot[i]);
					oOut.flush();
					summaajat.add(new SummausPalvelu.SummausPalvelija(i , porttiNumerot[i]));
					//luodaan SummausPalvelija ja lis�t��n listaan
					summaajat.get(i).start();
					//k�ynnistet��n s�ie
				}
				yhteysValmis = true;
				soketti.setSoTimeout(600); // minuutin time-out
				// Odotetaan Y:lt� lukuja 1, 2, 3 tai 0 jos joku muu luku, niin
				// palautetaan -1
				while (yhteysValmis) {
					try {
						switch (oIn.readInt()) {

						case 0:
							yhteysValmis = false;
						
						case 1:
							oOut.writeInt(annaSum());

						case 2:
							oOut.writeInt(annaSuurin());

						case 3:
							oOut.writeInt(annaLkm());

						default:
							oOut.writeInt(-1);

						}

					} catch (Exception e) {
						e.toString();
					}

				} // while
			} // if
			oOut.writeInt(-1); // jos t ei ole v�lilt� 2...10, niin l�hetet��n
								// -1
			oOut.flush();
			// soketti.close(); // ja suljetaan soketti.
		} catch (SocketException e) {
			oOut.writeInt(-1);
			oOut.flush();
			yhteysValmis = false;
		} // jos vastausta ei tule 5 sek. kuluessa, l�het� -1

	} // odotaT()

	static class SummausPalvelija extends Thread {
		/**
		 * Jokainen SummausPalvelija muodostaa TCP-yhteyden
		 * WorkDistributoriin.
		 * Sen j�lkeen lukee oliovirran yli kokonaislukuja ja
		 * lopulta vastaanottaa nollan ja sulkee itsens�.
		 * @var int portti: Portti, jota SummausPalvelija kuuntelee
		 * @var int saieId: Numero s�ikeelle josta sen voi tunnistaa
		 * @var omaSum: yksitt�isen SummausPalvelijan vastaanottama summa
		 */
		private final int portti;
		private final int saieId;
		private int omaSum = 0;
		private ServerSocket kuuntelevaSoketti;

		
		private SummausPalvelija(int saieId, int portti) {
			this.portti = portti;
			this.saieId = saieId;
		} // konstruktori

		@Override
		public void run() {
			// Onnea t�n metodin keksimiselle
			try {
				kuuntelevaSoketti = new ServerSocket(portti);
				kuuntelevaSoketti.setSoTimeout(5000);
				Socket soketti = kuuntelevaSoketti.accept(); //TCP muodostettu s�ikeen ja palvelimen v�lille
				System.out.println("S�ikeen " + saieId + " soketin TCP muodostettu portissa " + portti);
				
				//Avataan oliovirrat sis��ntulevalle liikenteelle.
				//S�ie ei l�het� mit��n palvelimelle.
				InputStream iS = soketti.getInputStream();
				ObjectInputStream oIn = new ObjectInputStream(iS);
				
				boolean eiNolla = true;
				// Silmukka py�rii kunnes tulee vastaan 0
				while(eiNolla) {
					int lisattava = oIn.readInt(); // <---- t�m� perkele ei toimi
					if (lisattava == 0) eiNolla = false; 
					else omaSum += lisattava;
				}
				soketti.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
							

		} // run
		
	} // class SummausPalvelija
		// gettereit�

	public static int annaSum() { // kun Y l�hett�� X:lle (int) 1
		int sum = 0;
		for (int i = 0; i < luvut.size(); i++) {
			sum = sum + luvut.get(i);
		}
		return sum;
	} // annaSum()

	public static int annaSuurin() {
		// jaahas, ei vittu t�t� metodia
		return 12345;
	}

	public static int annaLkm() { // kun Y l�hett�� X:lle (int) 3
		return luvut.size();
	} // annaLkm()
} // class SummausPalvelu