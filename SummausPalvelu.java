package hajo1;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;

public class SummausPalvelu implements Runnable {

	private static int[] porttiNumerot;
	private static ArrayList<Integer> luvut = new ArrayList<Integer>();
	private static ArrayList<Thread> summaajat = new ArrayList<Thread>();
	private static boolean yhteysValmis;
	private static int lisattyjenMaara = 0;

	public static void main(String[] args) {
		SummausPalvelu summausPalvelu = new SummausPalvelu();
		summausPalvelu.run();
	}

	@Override
	public void run() {
		Socket soketti = new Socket();
		ObjectOutputStream oOut = null;
		ObjectInputStream oIn = null;

		try {
			soketti = muodostaTCP();
			System.out.println("Oma portti on " + soketti.getPort());

			OutputStream oS = soketti.getOutputStream();
			InputStream iS = soketti.getInputStream();
			oOut = new ObjectOutputStream(oS);
			oIn = new ObjectInputStream(iS);

			odotaT(soketti, oIn, oOut);
			alustaJaLaheta(soketti, oOut);
			yhteysValmis = true;
			while (yhteysValmis) {
				odotaKyselya(soketti, oIn, oOut);
			}
			soketti.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * Samalla kun palvelin Y työllistää summauspalvelijoita, se voi kysyä
	 * sovellukselta X kolmenlaista tietoa: (1) mikä on tähän mennessä
	 * välitettyjen lukujen kokonaissumma, (2) mille summauspalvelijalle
	 * välitettyjen lukujen summa on suurin ja (3) mikä on tähän mennessä
	 * kaikille summauspalvelimille välitettyjen lukujen kokonaismäärä.
	 * Edelliset utelut palvelin Y tekee välittämällä X:lle niiden välisen
	 * oliovirran yli kokonaisluvun 1, 2 tai 3 (vastaavasti). Sovelluksen X
	 * tulee vastata takaisin sen hetkisen tilanteen mukaisella kokonaisluvulla.
	 * Jos sovellus X saa tässä utelutilassa palvelimelta Y jonkin muun numeron
	 * kuin 1, 2, 3 tai 0, niin sen tulee vastata takaisin luvulla −1.
	 * 
	 * @param soketti
	 * @param oIn
	 * @param oOut
	 * @throws IOException
	 */
	private void odotaKyselya(Socket soketti, ObjectInputStream oIn, ObjectOutputStream oOut) throws IOException {
		// Odotetaan Y:ltä lukuja 1, 2, 3 tai 0 jos joku muu luku, niin
		// palautetaan -1
		// soketti.setSoTimeout(5000);
		while (yhteysValmis) {
			int tapaus = oIn.readInt();
			try {
				switch (tapaus) {

				case 0:

					yhteysValmis = false;
					for (Thread saie : summaajat) {
						saie.join();
					}
					System.out.println("Sain " + tapaus + ", lopetetaan...");
					break;

				case 1:
					System.out.println(luvut.toString());
					System.out.println("Sain " + tapaus + ", vastaan " + annaSum());
					oOut.writeInt(annaSum());
					oOut.flush();
					break;

				case 2:
					System.out.println(luvut.toString());
					System.out.println("Sain " + tapaus + ", vastaan " + annaSuurin());
					oOut.writeInt(annaSuurin());
					oOut.flush();
					break;

				case 3:

					System.out.println(luvut.toString());
					System.out.println("Sain " + tapaus + ", vastaan " + annaLkm());
					oOut.writeInt(annaLkm());
					oOut.flush();
					break;

				default:

					yhteysValmis = false;
					System.out.println("Sain " + tapaus + ", lopetetaan...");
					oOut.writeInt(-1);
					oOut.flush();
					break;

				}

			} catch (Exception e) {
				e.toString();
			}

		} // while

	}

	public static void alustaJaLaheta(Socket soketti, ObjectOutputStream oOut) throws IOException {

		// luodaaan säikeet ja lisätään ne listaan
		for (int i = 0; i < porttiNumerot.length; i++) {
			summaajat.add(new SummausPalvelija(i, porttiNumerot[i]));
			luvut.add(i, 0);
			// luodaan SummausPalvelija ja lisätään listaan
			summaajat.get(i).start();
			// käynnistetään säie
		}
		lahetaPortit(soketti, oOut);

	}

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

	private static Socket muodostaTCP() throws IOException {
		// Kuuntele 1-5 s, sen jälkeen lähetä uudelleen
		// Viidennen uudelleen lähetyksen jälkeen terminate
		int porttiNo = 1337;
		int yrityskerta = 0;
		ServerSocket kuuntelevaSoketti = new ServerSocket(porttiNo);
		Socket soketti = new Socket();

		while (yrityskerta < 5) {
			try {
				lahetaUDP(); // lähetetään UDP paketti asiakkaalle
				kuuntelevaSoketti.setSoTimeout(5000); // soketti odottaa
														// yhteydenottoa 5 sek
				soketti = kuuntelevaSoketti.accept();
				kuuntelevaSoketti.close();
				System.out.println("TCP muodostettu");
				break;

			} catch (SocketException e) {
				yrityskerta++;
				System.out.println("Ei onnistunut");
			}
		} // while
		return soketti;
	} // void kuuntele()

	private static void odotaT(Socket soketti, ObjectInputStream oIn, ObjectOutputStream oOut) throws Exception {
		// Saa parametreina aikaisemmin muodostetut oliovirrat ja soketin
		// Odottaa t:n arvoa oliovirrasta, jonka mukaan SummausPalvelijaa
		// aletaan käyttämään

		int t;
		try {
			t = oIn.readInt(); // yritetään lukea oliovirrasta kokonaislukua

			if (t >= 2 || t <= 10) { // tarkistetaan, kelpaako vastaanotettu
										// luku
				porttiNumerot = new int[t]; // alustetaan porttiNumerot oikean
											// kokoiseksi
				// generoidaan porttinumero t-kertaa ja lisätään se
				// porttiNumerot-taulukkoon
				for (int i = 0; i < t; i++) {
					porttiNumerot[i] = (int) (1025 + (Math.random() * 64510));
				}
				System.out.println("Summauspalvelu luo " + t + " porttia");

			} // if

		} catch (SocketException e) {
			oOut.writeInt(-1);
			oOut.flush();
			yhteysValmis = false;
		} // jos vastausta ei tule 5 sek. kuluessa, lähetä -1

	} // odotaT()

	public static void lahetaPortit(Socket soketti, ObjectOutputStream oOut) throws IOException {
		for (int i = 0; i < porttiNumerot.length; i++) {
			oOut.writeInt(porttiNumerot[i]);
			oOut.flush();
		}
	}

	public static int annaSum() { // kun Y lähettää X:lle (int) 1
		int sum = 0;
		synchronized (luvut) {
			for (Integer i : luvut) {
				sum += i;
			}
			return sum;
		}
	} // annaSum()

	public static int annaSuurin() {
		synchronized (luvut) {
			return luvut.indexOf(Collections.max(luvut)) + 1;
		}
	} // annaSuurin

	public static int annaLkm() { // kun Y lähettää X:lle (int) 3
		return lisattyjenMaara;
	} // annaLkm()

	static class SummausPalvelija extends Thread {
		/**
		 * Jokainen SummausPalvelija muodostaa TCP-yhteyden WorkDistributoriin.
		 * Sen jälkeen lukee oliovirran yli kokonaislukuja ja lopulta
		 * vastaanottaa nollan ja sulkee itsensä.
		 * 
		 * @var int portti: Portti, jota SummausPalvelija kuuntelee
		 * @var int saieId: Numero säikeelle josta sen voi tunnistaa
		 * @var omaSum: yksittäisen SummausPalvelijan vastaanottama summa
		 */
		private final int portti;
		private final int saieId;
		private int omaSum = 0;
		private ServerSocket kuuntelevaSoketti;

		SummausPalvelija(int saieId, int portti) {
			this.portti = portti;
			this.saieId = saieId;
		} // konstruktori

		@Override
		public void run() {
			try {
				kuuntelevaSoketti = new ServerSocket(portti);
				kuuntelevaSoketti.setSoTimeout(5000);
				Socket soketti = kuuntelevaSoketti.accept(); // TCP muodostettu
																// säikeen ja
																// palvelimen
																// välille
				System.out.println("Säikeen " + saieId + " soketin TCP muodostettu portissa " + portti);

				// Avataan oliovirrat sisääntulevalle liikenteelle.
				// Säie ei lähetä mitään palvelimelle.
				InputStream iS = soketti.getInputStream();
				ObjectInputStream oIn = new ObjectInputStream(iS);

				int lisattava = 0;
				int tmp;
				// Silmukka pyörii kunnes tulee vastaan 0

				while (yhteysValmis) {
					lisattava = oIn.readInt();
					if (lisattava == 0)
						break;
					tmp = luvut.get(saieId);
					lisattava += tmp;
					luvut.set(saieId, lisattava);
					lisattyjenMaara++;

				} // while
				soketti.close();
			} catch (IOException e) {
				try {
					join(1000);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			} // catch

		} // run

	} // class SummausPalvelija

}
