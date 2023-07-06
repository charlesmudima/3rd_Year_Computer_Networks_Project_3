
import java.io.PrintStream;
import java.net.Socket;
import java.time.LocalDateTime;
import java.net.ServerSocket;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

public class natBox {

    private static ServerSocket serverSocket = null;
    private static PrintStream output = null;
    private static Boolean isConnected = true;
    private static Socket clientSocket = null;
    private static final int maxClients = 5;
    private static long timeLimit = 300000;
    private static final deviceHandler[] clientThreads = new deviceHandler[maxClients];
    public static LinkedList<tableContent> natTable = new LinkedList<tableContent>();
    public static LinkedList<tableContent> all_avail_IPS = new LinkedList<tableContent>();
    private static final String MAC = "6c:9e:ea:91:53:a2";
    private static final String IP = "67.148.165.144";
    private static int counter = 0;
    private static boolean canTime = true;
    public static int port = 2620;
    public static LocalDateTime time = LocalDateTime.now();

    public static void main(String[] args) {

        timeLimit = Long.parseLong(args[0]);
        Timer timer = new Timer();

        System.out.println("IP: " + IP);
        System.out.println("MAC: " + MAC);

        TimerTask timer1 = new TimerTask() {
            @Override
            public void run() {
                natTable.remove();
                all_avail_IPS.get(counter).setExternalIP("0");
                try {
                    clientThreads[counter].client.close();
                    Thread.sleep(10);
                } catch (IOException e) {
                } catch (InterruptedException e) {
                }
                counter++;
                System.out.println("NAT Table after disconnect: ");
                System.out.println("Allocated InternalIP" + "\t" + "Allocated ExternalIP" + "\t \t " + "Date&Time");
                printNatTable(natTable);
                System.out.println("IP available: ");
                System.out.println("Allocated InternalIP" + "\t" + "Allocated ExternalIP" + "\t \t " + "Date&Time");
                printNatTable(all_avail_IPS);
                canTime = true;
            }
        };

        TimerTask timer2 = new TimerTask() {
            @Override
            public void run() {
                natTable.remove();
                all_avail_IPS.get(counter).setExternalIP("0");
                try {
                    clientThreads[counter].client.close();
                    Thread.sleep(10);
                } catch (IOException e) {
                } catch (InterruptedException e) {
                }
                counter++;
                System.out.println("NAT Table (after timer): ");
                System.out.println("Allocated InternalIP" + "\t" + "Allocated ExternalIP" + "\t \t " + "Date&Time");
                printNatTable(natTable);
                System.out.println("Pool (after timer): ");
                System.out.println("Allocated InternalIP" + "\t" + "Allocated ExternalIP" + "\t \t " + "Date&Time");
                printNatTable(all_avail_IPS);
                canTime = false;
            }
        };

        avail_ipaddress(maxClients);

        startServerSocket(timer, timer1, timer2);

    }

    private static void startServerSocket(Timer timer, TimerTask timer1, TimerTask timer2) {
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            System.err.println(e);
        }

        while (isConnected) {
            int i;

            dhcp.server(port);
            if (timeLimit < 30000) {
                if (canTime) {
                    timer.schedule(timer1, timeLimit);
                    canTime = false;
                } else {
                    timer.schedule(timer2, timeLimit);
                    canTime = true;
                }
            }
            System.out.println("NAT Table: ");
            System.out.println("**********************************************************************");
            System.out.println("Allocated InternalIP" + "\t" + "Allocated ExternalIP" + "\t \t " + "Date&Time");
            printNatTable(natTable);
            System.out.println("**********************************************************************");
            try {
                clientSocket = serverSocket.accept();
                for (i = 0; i < maxClients; i++) {
                    if (clientThreads[i] == null) {
                        clientThreads[i] = new deviceHandler(clientSocket, clientThreads,
                                natTable, natTable.get(natTable.size() - 1),
                                all_avail_IPS);
                        clientThreads[i].start();
                        break;
                    }
                }
                if (i == maxClients) {

                    System.out.println("number of clients in : " + i);
                    output = new PrintStream(clientSocket.getOutputStream());
                    output.println("Too many clients.");
                    output.close();
                    clientSocket.close();
                }
            } catch (IOException e) {
                System.err.println(e);
            }
        }
    }

    private static void avail_ipaddress(int clientLimit) {
        String private_pre;

        private_pre = " 10.0.0.";
        int i;
        for (i = 0; i < clientLimit; i++) {
            String avail_IPS;
            avail_IPS = private_pre + i;

            tableContent all_IPS = new tableContent(avail_IPS, "0");
            all_avail_IPS.add(all_IPS);
        }
    }

    public static void printNatTable(LinkedList<tableContent> table) {
        int table_size = table.size();
        for (int i = 0; i < table_size; i++) {
            System.out.println(table.get(i).toString());
        }
    }
}
