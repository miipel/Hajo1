
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
import java.util.ArrayList;

/**
 * 
 * @author Miika Peltotalo ja Peetu Seilonen
 * @version 27.11.2016 18:15
 * 
 * @var int[] porttiNumerot: sisältää porttien numeroit, joita SummausPalvelin
 *      kuuntelee
 * @var boolean yhteysValmis: kun yhteys asiakkaaseen on saatu ja on aika
 *      ailoittaa SummausPalvelimen käyttö
 * @var ArrayList<int> luvut: tänne kerätään kaikki vastaanotetut luvut
 */
public class SummausPalvelu {
	private static int[] porttiNumerot;
	private static ArrayList<Integer> luvut = new ArrayList<Integer>();
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

	}

	private static void muodostaTCP() throws IOException {
		// Kuuntele 1-5 s, sen jälkeen lähetä uudelleen
		// Viidennen uudelleen lähetyksen jälkeen terminate
		Socket soketti;
		int porttiNo = 1337;
		int yrityskerta = 0;

		while (yrityskerta < 5) {
			try {
				lahetaUDP(); // lähetetään UDP paketti asiakkaalle
				soketti = new Socket(InetAddress.getLocalHost(), porttiNo);
				soketti.setSoTimeout(5000); // soketti odottaa yhteydenottoa 5
											// sek.
				// avataan oliovirrat
				OutputStream oS = soketti.getOutputStream();
				InputStream iS = soketti.getInputStream();
				ObjectOutputStream oOut = new ObjectOutputStream(oS);
				ObjectInputStream oIn = new ObjectInputStream(iS);
				try {
					// kun yhteys on saatu, lähetetään soketti ja oliovirta..
					// ..odotaT() -metodille, joka odottaa asiakkaalta
					// kokonaislukua t
					odotaT(soketti, oOut, oIn);
				} catch (Exception e) {
					e.toString();
				}
			} catch (SocketException e) {
				yrityskerta++;
			}
		} // while
	} // void kuuntele()

	private static void odotaT(Socket soketti, ObjectOutputStream oOut, ObjectInputStream oIn) throws Exception {
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
				// kun kaikki porttinumerot on lisätty taulukkoon,
				// lähetetään taulukko asiakkaalle ja käynnistetään
				// summauspalvelin toimimaan ko. porttiin
				for (int i = 0; i < porttiNumerot.length; i++) {
					oOut.writeInt(porttiNumerot[i]);
					oOut.flush();
					new SummausPalvelu.SummausPalvelija(porttiNumerot[i], InetAddress.getLocalHost()).start();
				}
				yhteysValmis = true;
				soketti.setSoTimeout(600); // minuutin time-out
				// Odotetaan Y:ltä lukuja 1, 2 tai 3, jos joku muu luku, niin
				// palautetaan -1
				while (yhteysValmis) {
					try {
						switch (oIn.readInt()) {

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
			oOut.writeInt(-1); // jos t ei ole väliltä 2...10, niin lähetetään
								// -1
			oOut.flush();
			// soketti.close(); // ja suljetaan soketti.
		} catch (SocketException e) {
			oOut.writeInt(-1);
			oOut.flush();
			yhteysValmis = false;
		} // jos vastausta ei tule 5 sek. kuluessa, lähetä -1

	} // odotaT()

	static class SummausPalvelija extends Thread {
		/**
		 * @var int portti: Portti, jota SummausPalvelija kuuntelee
		 * @var InetAddress clientAddress: asiakkaan IP-osoite
		 * @var omaSum: yksittäisen SummausPalvelijan vastaanottama summa
		 */
		private final int portti;
		private final InetAddress clientAddress;
		private int omaSum = 0;
		private Socket soketti;
		private OutputStream oS;
		private InputStream iS;
		private ObjectOutputStream oOut;
		private ObjectInputStream oIn;

		private SummausPalvelija(int portti, InetAddress clientAddress) {
			this.portti = portti;
			this.clientAddress = clientAddress;
			try {
				this.soketti = new Socket(clientAddress, portti);
				this.oS = soketti.getOutputStream();
				this.iS = soketti.getInputStream();
				this.oOut = new ObjectOutputStream(oS);
				this.oIn = new ObjectInputStream(iS);

			} catch (IOException e) {
				e.toString();
			}
		} // konstruktori

		@Override
		public void run() {
			// Onnea tän metodin keksimiselle

		} // run

	} // class SummausPalvelija
		// gettereitä

	public static int annaSum() { // kun Y lähettää X:lle (int) 1
		int sum = 0;
		for (int i = 0; i < luvut.size(); i++) {
			sum = sum + luvut.get(i);
		}
		return sum;
	} // annaSum()

	public static int annaSuurin() {
		// jaahas, ei vittu tätä metodia
		return 12345;
	}

	public static int annaLkm() { // kun Y lähettää X:lle (int) 3
		return luvut.size();
	} // annaLkm()
} // class SummausPalvelu
