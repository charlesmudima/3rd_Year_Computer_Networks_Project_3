
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.io.IOException;
import java.io.PrintStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Random;
import java.awt.event.ActionEvent;
import java.awt.Component;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

@SuppressWarnings("deprecation")
public class device implements Runnable {

    public static String IP;
    public static String private_IP;
    private static Socket socket = null;
    private static DataInputStream in = null;
    private static DataInputStream reciever = null;
    private static DataOutputStream out = null;
    private static PrintStream print = null;
    private static String MAC;
    public static String host;
    private static int port;

    public static void main(String[] args) {
        host = args[0];
        port = Integer.parseInt(args[1]);

        genMac();
        IP = random_IP();

        chooseSendMethod();

        System.out.println("MAC: " + MAC);
        try {

            socket = new Socket(host, port);
            reciever = new DataInputStream(socket.getInputStream());
            in = new DataInputStream(new BufferedInputStream(System.in));
            out = new DataOutputStream(socket.getOutputStream());
            print = new PrintStream(socket.getOutputStream());
        } catch (UnknownHostException e) {
            System.err.println(e);
        } catch (IOException e) {
            System.err.println(e);
        }
        if (socket != null && reciever != null && out != null) {
            try {
                new Thread(new device()).start();
                String message = "";
                while (true) {
                    message = in.readLine().trim();
                    if (message.startsWith(">>")) {
                        String[] details;
                        details = message.split(" ");
                        byte pack_byteArr[];
                        pack_byteArr = packet_build(private_IP, details[1], details[2].getBytes());
                        print.println(pack_byteArr.length);
                        out.write(pack_byteArr);
                    }
                    if (message.startsWith("quit")) {
                        System.out.println(private_IP + " has disconnected from NAT box");
                        print.println("e");
                        break;
                    }
                }
                closeeverything(socket, reciever, out, in);
            } catch (IOException e) {
                System.err.println(e);
            }
        }
    }

    private static void closeeverything(Socket socket, DataInputStream reciever, DataOutputStream out,
            DataInputStream in) throws IOException {
        out.close();
        in.close();
        reciever.close();
        socket.close();
        System.exit(0);

    }

    // GUI
    private static void chooseSendMethod() {
        JFrame jframe = new JFrame("FILE SENDER");
        jframe.setSize(450, 450);
        jframe.setLayout(new BoxLayout(jframe.getContentPane(), BoxLayout.Y_AXIS));
        jframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JLabel jtitle = new JLabel("Internal or External");
        jtitle.setFont(new Font("Arial", Font.BOLD, 25));
        jtitle.setBorder(new EmptyBorder(20, 0, 10, 0));
        jtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel jbutton = new JPanel();
        jbutton.setBorder(new EmptyBorder(75, 0, 10, 0));
        JButton jbInternal = new JButton("Internal");
        jbInternal.setPreferredSize(new Dimension(250, 75));
        jbInternal.setFont(new Font("Arial", Font.BOLD, 20));

        JButton jbExternal = new JButton("External");
        jbExternal.setPreferredSize(new Dimension(250, 75));
        jbExternal.setFont(new Font("Arial", Font.BOLD, 20));

        JButton jclose = new JButton("Exit");
        jclose.setPreferredSize(new Dimension(250, 75));
        jclose.setFont(new Font("Arial", Font.BOLD, 20));

        jbutton.add(jbInternal);
        jbutton.add(jbExternal);
        jbutton.add(jclose);

        jclose.addActionListener((event) -> System.exit(0));

        jclose.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.out.println("You have exited!");
            }
        });

        jbInternal.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.out.println("Joining NAT BOX as an Internal Client");
                dhcp.dhcpClient();
                System.out.println("IP = " + IP + "; newIP = " + private_IP);
                jframe.setVisible(false);
            }
        });

        jbExternal.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.out.println("Joining NAT BOX as an External Client");
                DatagramSocket socket = null;
                int count = 0;
                IP = random_IP();
                private_IP = IP;
                try {
                    socket = new DatagramSocket();
                    byte[] payload = new byte[100];
                    payload[0] = 'e';
                    byte[] byteIP = IP.getBytes();

                    for (int i = 0; i < byteIP.length; i++) {
                        payload[i + 1] = byteIP[i];
                        count++;
                    }
                    payload[count + 1] = '%';
                    DatagramPacket p = new DatagramPacket(payload, payload.length, InetAddress.getByName(host), 2620);
                    socket.send(p);
                } catch (SocketException e1) {
                    System.err.println(e);
                } catch (IOException e2) {
                    System.err.println(e);
                }
                System.out.println("IP: " + IP);
                jframe.setVisible(false);

            }
        });
        jframe.add(jtitle);
        jframe.add(jbutton);
        jframe.setVisible(true);

    }

    @Override
    public void run() {
        messageListener();
    }

    public void messageListener() {
        byte[] packet = null;
        String delimeter = "@";

        try {
            extracted(packet, delimeter);
        } catch (IOException e) {
        }
    }

    private static void extracted(byte[] packet, String delimeter) throws IOException {

        receivemessage(packet, delimeter);

    }

    //
    private static void receivemessage(byte[] packet, String delimeter) throws IOException {
        String message;
        while (true) {

            if ((message = reciever.readLine()) != null) {
                int packet_length = Integer.parseInt(message);
                packet = new byte[packet_length];
                reciever.read(packet);
                String[] packet_data = new String(packet).split(delimeter);
                String message_sent = packet_data[2];
                int pack_length = packet_data.length;

                System.out.println("Message: " + message_sent + " " + " From Source: " + packet_data[0]);

                if (message_sent.equals("payload") && pack_length <= 3) {

                    System.out.println();
                    String ack;
                    ack = private_IP + "@" + packet_data[0] + "@recieved";
                    packet = ack.getBytes();
                    print.println(packet.length);
                    out.write(packet);
                }
            } else {
                System.out.println("connection lost from [port: " + natBox.port + "] !!");
                System.exit(0);
            }
        }
    }

    private static String random_IP() {
        Random rand_ip;
        rand_ip = new Random();
        return rand_ip.nextInt(256) + "." + rand_ip.nextInt(256) + "." + rand_ip.nextInt(256) + "."
                + rand_ip.nextInt(256);
    }

    private static void genMac() {
        Random rand = new Random();
        byte[] macAddr = new byte[6];
        rand.nextBytes(macAddr);
        macAddr[0] = (byte) (macAddr[0] & (byte) 254);
        StringBuilder sb = new StringBuilder(18);

        for (byte b : macAddr) {
            if (sb.length() > 0) {
                sb.append(":");
            }
            sb.append(String.format("%02x", b));
        }
        MAC = sb.toString();
    }

    public static byte[] packet_build(String source, String destination, byte[] packet_content) {
        byte[] from = source.getBytes();
        byte[] to = destination.getBytes();
        int to_length = to.length;
        int from_length = from.length;
        int payload_length = packet_content.length;
        byte delimiter = '@';
        byte[] packet = new byte[to_length + from_length + payload_length + 2];
        int counter = 0;

        for (int i = 0; i < from_length; i++) {
            packet[i] = from[counter];
            counter++;
        }
        packet[from_length] = delimiter;
        counter = 0;
        int extr = 1;
        for (int i = from_length + extr; i < to_length + from_length + extr; i++) {
            packet[i] = to[counter];
            counter++;
        }
        packet[from_length + to_length + extr] = delimiter;
        counter = 0;

        for (int i = to_length + from_length + 2; i < packet.length; i++) {
            packet[i] = packet_content[counter];
            counter++;
        }

        return packet;
    }

}
