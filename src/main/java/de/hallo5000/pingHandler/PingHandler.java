package de.hallo5000.pingHandler;

import com.velocitypowered.api.proxy.server.RegisteredServer;
import de.hallo5000.main.VelocityVersionBouncer;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

public class PingHandler {
    
    private final VelocityVersionBouncer plugin;
    public PingHandler(VelocityVersionBouncer plugin){
        this.plugin = plugin;
    }
    /**
     * Tries to do a Handshake with the given backend server and returns the JSON Response from the Status Response
     * wrapped in a <code>CompletableFuture</code> as this is done async
     * @param server a backend server from the proxy this is called on
     * @return a <code>CompletableFuture</code> containing the JSON Response by the backend server containing the server information, which might be incomplete/invalid json
     */
    protected CompletableFuture<String> ping(RegisteredServer server){
        return CompletableFuture.supplyAsync(() -> {
            InetSocketAddress host = server.getServerInfo().getAddress();
            String json = null;
            try(Socket socket = new Socket()){
                plugin.getLogger().debug("Connecting to "+host.getAddress().getHostAddress()+":"+host.getPort());
                socket.connect(host);

                DataInputStream input = new DataInputStream(socket.getInputStream());
                DataOutputStream output = new DataOutputStream(socket.getOutputStream());

                byte [] handshakeMessage = createHandshakeMessage(host.getAddress().getHostAddress(), host.getPort());

                // C->S : Handshake State=1
                // send packet length and packet
                writeVarInt(output, handshakeMessage.length);
                output.write(handshakeMessage);
                output.flush();

                // C->S : Request
                writeVarInt(output, 0x01); //size is only 1
                output.writeByte(0x00); //packet id for "Status Request"
                output.flush();

                // S->C : Response
                readVarInt(input); //packet length
                int packetId = readVarInt(input);

                if (packetId == -1) {
                    throw new IOException("Premature end of stream.");
                }

                if (packetId != 0x00) { //we want a status response
                    throw new IOException("Invalid packetID");
                }

                int length = readVarInt(input); //length of json string (strings have their length prepended in addition to the packet length)

                if (length == -1) {
                    throw new IOException("Premature end of stream.");
                }

                if (length == 0) {
                    throw new IOException("Invalid string length.");
                }

                byte[] in = new byte[length];
                input.readFully(in);  //read json string
                json = new String(in);

            /* Ping and Pong are optionally and not needed for this case (I'll leave the code here if needed in the future)
            // C->S : Ping
            long now = System.currentTimeMillis();
            writeVarInt(output, 0x09); //size of packet (1 for id and 8 for long)
            output.writeByte(0x01); //0x01 for ping
            output.writeLong(now); //notchian clients sent there time stamp otherwise this field ("Payload") is useless
            output.flush();

            // S->C : Pong
            readVarInt(input); //packet length
            packetId = readVarInt(input);
            if (packetId == -1) {
                throw new IOException("Premature end of stream.");
            }

            if (packetId != 0x01) {
                throw new IOException("Invalid packetID");
            }
            long pingtime = input.readLong(); //read response (should be the same as 'Payload' in the Ping)
            if(now != pingtime) plugin.getLogger().warn("Something went wrong: the Pong Response Payload wasn't the same as the previously sent timestamp.");
            */

            }catch(IOException ex){
                if(ex instanceof ConnectException) plugin.getLogger().debug("Couldn't connect to " + server.getServerInfo().getName());
                else plugin.getLogger().error("Error while pinging a backend server: ", ex);
            }
            return json;
        });
    }

    private static byte [] createHandshakeMessage(String host, int port) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        DataOutputStream handshake = new DataOutputStream(buffer);
        handshake.writeByte(0x00); //packet id for handshake
        writeVarInt(handshake, 774); //protocol version (-1 by convention for only getting server info but not accepted by some servers)
        writeString(handshake, host); // "Server Address" field
        handshake.writeShort(port); //port
        writeVarInt(handshake, 1); //state (1 for status)

        return buffer.toByteArray();
    }

    private static void writeString(DataOutputStream out, String string) throws IOException {
        byte [] bytes = string.getBytes(StandardCharsets.UTF_8);
        writeVarInt(out, bytes.length);
        out.write(bytes);
    }

    private static void writeVarInt(DataOutputStream out, int paramInt) throws IOException {
        while (true) {
            if ((paramInt & 0xFFFFFF80) == 0) { //true if there are only max 7 bits left in paramInt
                out.writeByte(paramInt);
                return;
            }

            out.writeByte(paramInt & 0x7F | 0x80); //7bits + MSB=1
            paramInt >>>= 7;//unsigned shift
        }
    }

    private static int readVarInt(DataInputStream in) throws IOException {
        int i = 0;
        int j = 0;
        while (true) {
            int k = in.readByte();
            i |= (k & 0x7F) << j++ * 7;
            if (j > 5) throw new RuntimeException("VarInt too big");
            if ((k & 0x80) != 128) break;
        }
        return i;
    }
}
