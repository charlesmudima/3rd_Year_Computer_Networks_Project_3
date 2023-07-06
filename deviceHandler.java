
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.LinkedList;

@SuppressWarnings("deprecation")
class deviceHandler extends Thread {

  private DataInputStream clientMessage = null;
  private DataOutputStream output = null;
  private PrintStream print = null;
  private tableContent self = null;
  public Socket client = null;
  private deviceHandler[] clientThreads;
  private boolean running = true;
  private LinkedList<tableContent> natTable = new LinkedList<tableContent>();
  private LinkedList<tableContent> pool = new LinkedList<tableContent>();
  private boolean INTERNAL = true;
  public String message;

  public deviceHandler(Socket client, deviceHandler[] clientThreads, LinkedList<tableContent> natTable,
      tableContent self,
      LinkedList<tableContent> pool) {
    extracted(client, clientThreads, natTable, self, pool);

    if (self.getInternalIP().equals(self.getExternalIP())) {
      setINTERNAL(false);
    } else {
      setINTERNAL(true);
    }
  }

  private void extracted(Socket client, deviceHandler[] clientThreads, LinkedList<tableContent> natTable,
      tableContent self, LinkedList<tableContent> pool) {
    this.client = client;
    this.clientThreads = clientThreads;
    this.natTable = natTable;
    this.setSelf(self);
    this.setPool(pool);
  }

  public void run() {
    deviceHandler[] clientThreads = this.clientThreads;
    try {
      setClientMessage(new DataInputStream(client.getInputStream()));
      setOutput(new DataOutputStream(client.getOutputStream()));
      setPrint(new PrintStream(client.getOutputStream()));
      byte[] packet = null;

      while (isRunning()) {
        message = getClientMessage().readLine();
        if (message.equals("e")) {
          break;
        }

        packet = new byte[Integer.parseInt(message)];
        getClientMessage().read(packet);
        String[] packet_data = new String(packet).split("@");
        String source = packet_data[0];
        String dest = packet_data[1];
        String packetmessage = packet_data[2];
        boolean sourceInt = false;
        boolean destInt = false;
        boolean intIP = false;

        for (deviceHandler c : clientThreads) {
          if (c != null && (c.getSelf().getInternalIP().equals(source))) {
            sourceInt = c.isINTERNAL();
          }

          if (c != null && (c.getSelf().getExternalIP().equals(dest) || c.getSelf().getInternalIP().equals(dest))) {
            destInt = c.isINTERNAL();
            if (c.getSelf().getInternalIP().equals(dest)) {
              intIP = true;
            }
          }
        }
        System.out.println("\nPacket recieved by NATBox:");
        System.out.println(Arrays.toString(packet_data));
        if (sourceInt) {
          if (destInt) {

            if (!intIP) {
              System.out.println("DESTINATION INVALID: " + dest);
              dest = "-1";
            } else {
              System.out.println("No packet alteration.");
            }
          } else {

            for (deviceHandler c : clientThreads) {
              if (c != null && c.getSelf().getInternalIP().equals(source)) {
                source = c.getSelf().getExternalIP();
                System.out.println("Source translated.");
              }
            }
          }
        } else {
          if (destInt) {
            for (deviceHandler c : clientThreads) {
              if (c != null && c.getSelf().getExternalIP().equals(dest)) {
                dest = c.getSelf().getInternalIP();
                System.out.println("Destination translated.");
              }
            }
          } else {
            System.out.println("**Packet Dropped**");
            for (deviceHandler c : clientThreads) {
              if (c != null
                  && (c.getSelf().getInternalIP().equals(source) || c.getSelf().getExternalIP().equals(source))) {
                packet = (source + "@" + dest + "@" + packetmessage + "@Packet Could not be delivered").getBytes();
                c.getPrint().println(packet.length);
                c.getOutput().write(packet);
                System.out.println("\nERROR PACKET SENT:");
                System.out.println(
                    packetmessage + " was not delieverd to:" + "[" + dest + "]" + " from:" + "[" + source + "]");
                break;
              }
            }
            System.out.println("***********************************************************");
            continue;
          }
        }
        packet = device.packet_build(source, dest, packetmessage.getBytes());
        boolean sent = false;
        for (deviceHandler c : clientThreads) {
          if (c != null && c.getSelf().getInternalIP().equals(dest)) {
            c.getPrint().println(packet.length);
            c.getOutput().write(packet);
            System.out.println("\nSENT PACKET:");
            System.out.println("[" + source + ", " + dest + ", " + packetmessage + "]");
            sent = true;
            break;
          }
        }
        if (!sent) {
          System.out.println("PACKET COULD NOT BE DELIVERED:");
          for (deviceHandler c : clientThreads) {
            if (c != null
                && (c.getSelf().getInternalIP().equals(source) || c.getSelf().getExternalIP().equals(source))) {
              packet = (source + "@" + dest + "@" + packetmessage + "@Packet Could not be delivered").getBytes();
              c.getPrint().println(packet.length);
              c.getOutput().write(packet);
              System.out.println("\nERROR PACKET SENT:");
              System.out
                  .println(packetmessage + "was not delieverd to:" + "[" + dest + "]" + " from:" + "[" + source + "]");

              break;
            }
          }
        }
        System.out.println("***********************************************************");
      }
      natTable.remove(getSelf());
      for (tableContent t : getPool()) {
        if (t.getExternalIP().equals(getSelf().getExternalIP())) {
          t.setExternalIP("0");
        }
      }

      for (deviceHandler c : clientThreads) {
        if (c == this) {
          System.out.println(c.getSelf().toString() + " *** HAS DISCONNECTED *** ");
          c = null;
        }
      }
      System.out.println("NAT Table (after disconnect): ");
      System.out.println("Allocated InternalIP" + "\t" + "Allocated ExternalIP" + "\t \t " + "Date&Time");
      natBox.printNatTable(natTable);
      System.out.println("Pool (after disconnect): ");
      System.out.println("Allocated InternalIP" + "\t" + "Allocated ExternalIP" + "\t \t " + "Date&Time");
      natBox.printNatTable(getPool());
      getClientMessage().close();
      getOutput().close();
      client.close();
    } catch (IOException e) {
      for (deviceHandler c : clientThreads) {
        if (c == this) {
          System.out.println("NATBOX TIMEOUT ERROR");
          System.out.println(c.getSelf().toString());
          c = null;
        }
      }
    }

  }

  public DataInputStream getClientMessage() {
    return clientMessage;
  }

  public void setClientMessage(DataInputStream clientMessage) {
    this.clientMessage = clientMessage;
  }

  public DataOutputStream getOutput() {
    return output;
  }

  public void setOutput(DataOutputStream output) {
    this.output = output;
  }

  public PrintStream getPrint() {
    return print;
  }

  public void setPrint(PrintStream print) {
    this.print = print;
  }

  public tableContent getSelf() {
    return self;
  }

  public void setSelf(tableContent self) {
    this.self = self;
  }

  public boolean isRunning() {
    return running;
  }

  public void setRunning(boolean running) {
    this.running = running;
  }

  public LinkedList<tableContent> getPool() {
    return pool;
  }

  public void setPool(LinkedList<tableContent> pool) {
    this.pool = pool;
  }

  public boolean isINTERNAL() {
    return INTERNAL;
  }

  public void setINTERNAL(boolean INTERNAL) {
    this.INTERNAL = INTERNAL;
  }
}
