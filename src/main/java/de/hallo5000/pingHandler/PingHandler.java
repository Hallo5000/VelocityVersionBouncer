package de.hallo5000.pingHandler;

import com.velocitypowered.api.proxy.server.RegisteredServer;
import de.hallo5000.main.VelocityVersionBouncer;

import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class PingHandler {

    /**
     * Tries to do a Handshake with the given backend server and returns the JSON Response from the Status Response
     * @param server a backend server from the proxy this is called on
     * @return the JSON Response by the backend server containing the server information, which might be incomplete/invalid json
     */
    public static @Nullable String ping(RegisteredServer server){
        InetSocketAddress host = server.getServerInfo().getAddress();
        try(Socket socket = new Socket()){
            VelocityVersionBouncer.getLogger.info("Connecting to "+host.getAddress().toString()+"...");
            socket.connect(host);
            VelocityVersionBouncer.getLogger.info("Done!");

            VelocityVersionBouncer.getLogger.info("Making streams...");
            DataInputStream input = new DataInputStream(socket.getInputStream());
            DataOutputStream output = new DataOutputStream(socket.getOutputStream());
            VelocityVersionBouncer.getLogger.info("Done!");

            VelocityVersionBouncer.getLogger.info("Attempting handshake...");

            byte [] handshakeMessage = createHandshakeMessage(host.getAddress().toString(), host.getPort());

            // C->S : Handshake State=1
            // send packet length and packet
            writeVarInt(output, handshakeMessage.length);
            output.write(handshakeMessage);

            // C->S : Request
            writeVarInt(output, 0x01); //size is only 1
            output.writeByte(0x00); //packet id for "Status Request"


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
            String json = new String(in);


            // C->S : Ping
            long now = System.currentTimeMillis();
            writeVarInt(output, 0x09); //size of packet (1 for id and 8 for long)
            output.writeByte(0x01); //0x01 for ping
            output.writeLong(now); //notchian clients sent there time stamp otherwise this field ("Payload") is useless

            // S->C : Pong
            readVarInt(input); //packet length
            packetId = readVarInt(input);
            if (packetId == -1) {
                throw new IOException("Premature end of stream.");
            }

            if (packetId != 0x01) {
                throw new IOException("Invalid packetID");
            }
            long pingtime = input.readLong(); //read response
            if(now != pingtime) VelocityVersionBouncer.getLogger.warn("Something went wrong: the Pong Response Payload wasn't the same as the previously sent timestamp.");

            System.out.println("Done!");

            return json;
        }catch(IOException ex){
            VelocityVersionBouncer.getLogger.error("Error while pinging a backend server: ", ex);
        }
        return null;
    }

    private static byte [] createHandshakeMessage(String host, int port) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        DataOutputStream handshake = new DataOutputStream(buffer);
        handshake.writeByte(0x00); //packet id for handshake
        writeVarInt(handshake, -1); //protocol version (-1 by convention for only getting server info)
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
