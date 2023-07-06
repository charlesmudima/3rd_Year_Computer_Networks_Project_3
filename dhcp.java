import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class dhcp {
    private static long timeLimit = 300000;
    public static final int port = 2620;

    public static void server(int port) {
        DatagramSocket socket = null;
        String clientIP = "";
        int count = 0;
        int poolPos = 0;

        try {
            socket = new DatagramSocket(port);
            byte[] payload = new byte[100];
            DatagramPacket p = new DatagramPacket(payload, payload.length);
            boolean listen = true;

            while (listen) {
                socket.receive(p);
                byte[] buff = p.getData();
                int clientPort = p.getPort();
                InetAddress address = p.getAddress();

                if (buff[0] == 'e') {
                    System.out.println("\tExternal client");

                    for (int i = 1; i < buff.length; i++) {
                        count++;
                        if (buff[i] == '%') {
                            break;
                        }
                    }
                    clientIP = new String(buff, 1, count - 1);
                    tableContent newClient = new tableContent(clientIP, clientIP);
                    natBox.natTable.add(newClient);
                    listen = false;
                    socket.close();
                } else {
                    System.out.println("\tInternal client");
                }
                if (buff[0] == 'd') {
                    count = 0;

                    for (int i = 1; i < buff.length; i++) {
                        count++;
                        if (buff[i] == '%') {
                            break;
                        }
                    }
                    clientIP = new String(buff, 1, count - 1);
                    System.out.println("The public address of the newly connected internal device: " + clientIP);
                    byte[] intIP;
                    payload = new byte[100];
                    payload[0] = 'o';

                    count = 0;
                    for (int i = 0; i < natBox.all_avail_IPS.size(); i++) {
                        if (natBox.all_avail_IPS.get(i).getExternalIP().equals("0")) {
                            intIP = natBox.all_avail_IPS.get(i).getInternalIP().getBytes();
                            poolPos = i;
                            for (int j = 0; j < intIP.length; j++) {
                                payload[j + 1] = intIP[j];
                                count++;
                            }
                            payload[count + 1] = '%';
                            break;
                        }
                        System.out.println("Too many clients");
                    }
                    p = new DatagramPacket(payload, payload.length, address, clientPort);
                    socket.send(p);
                    System.out.println("\tsend offer");
                }

                if (buff[0] == 'r') {

                    String newIP = "";
                    count = 0;

                    for (int i = 1; i < buff.length; i++) {
                        count++;
                        if (buff[i] == '%') {
                            break;
                        }
                    }

                    newIP = new String(buff, 1, count - 1);
                    tableContent newClient = new tableContent(newIP, clientIP, timeLimit);
                    natBox.natTable.add(newClient);
                    natBox.all_avail_IPS.get(poolPos).setExternalIP(clientIP);
                    payload = new byte[100];
                    payload[0] = 'a';
                    payload[1] = '%';
                    p = new DatagramPacket(payload, payload.length, address, clientPort);

                    socket.send(p);
                    System.out.println("\tsend ack");
                    listen = false;
                    socket.close();
                }
            }
        } catch (SocketException e) {
            System.err.println(e);
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    public static void dhcpClient() {
        DatagramSocket socket = null;
        String tempIP = "";
        int pos = 0;
        int count = 0;
        try {
            socket = new DatagramSocket();
            byte[] payload = new byte[100];
            payload[0] = 'd';
            // send IP as well
            for (int i = 0; i < device.IP.length(); i++) {
                payload[i + 1] = (byte) device.IP.charAt(i);
                pos = i + 1;
            }
            payload[pos + 1] = '%';
            for (int i = 1; i < payload.length; i++) {
                count++;
                if (payload[i] == '%') {
                    break;
                }
            }
            DatagramPacket p = new DatagramPacket(payload, payload.length, InetAddress.getByName(device.host), 2620);
            socket.send(p);
            System.out.println("\tsend discover");
            boolean listen = true;

            while (listen) {
                socket.receive(p);
                byte[] buff = p.getData();
                if (buff[0] == 'o') {
                    // send ip address you want
                    payload = new byte[100];
                    payload[0] = 'r';
                    count = 0;
                    for (int i = 1; i < buff.length; i++) {
                        payload[i] = buff[i];
                        count++;
                        if (buff[i] == '%') {
                            payload[i] = '%';
                            break;
                        }
                    }
                    tempIP = new String(buff, 1, count - 1);
                    p = new DatagramPacket(payload, payload.length, InetAddress.getByName(device.host), 2620);
                    socket.send(p);
                    System.out.println("\tsend request");
                }
                if (buff[0] == 'a') {
                    device.private_IP = tempIP;
                    listen = false;
                    socket.close();
                    System.out.println("\tacknoledgement recieved");
                }
            }
        } catch (SocketException e) {
            System.err.println(e);
        } catch (IOException e) {
            System.err.println(e);
        }
    }

}
