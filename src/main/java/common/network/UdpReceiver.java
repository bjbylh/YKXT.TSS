package common.network;


import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;

/**
 * A simple TM data receiver
 * Created by Li Qiang on 2015/7/10.
 */
public class UdpReceiver {
    private int listenPort;
    private InetAddress groupAddr;
    private InetAddress localAddress;
    private int failureCount;
    private MulticastSocket receiver;

    public UdpReceiver(String groupAddr, int port, String localAddr) throws UnknownHostException {
        listenPort = port;
        assert listenPort > 1024 && listenPort < 65535;
        this.groupAddr = InetAddress.getByName(groupAddr);
        if (localAddr != null && !localAddr.trim().isEmpty()) {
            localAddress = InetAddress.getByName(localAddr.trim());
        } else localAddress = null;
    }

    /**
     * @param groupAddr
     * @param port
     * @param localAddr
     * @throws IllegalArgumentException
     */
    public UdpReceiver(InetAddress groupAddr, int port, String localAddr) throws UnknownHostException {
        listenPort = port;
        assert listenPort > 1024 && listenPort < 65535;
        this.groupAddr = groupAddr;
        if (localAddr != null && !localAddr.trim().isEmpty()) {
            localAddress = InetAddress.getByName(localAddr.trim());
        } else localAddress = null;
    }

    public ByteBuffer receiveFrame() throws SocketTimeoutException {
        if (failureCount > 10) {
            try {
                if (receiver != null) {
                    receiver.close();
                }
                receiver = null;
                failureCount = 0;
            } catch (Exception e) {
                System.out.println("FailureCount > 10 MulticastSocket close Exception: " + e.getMessage() + " ; port:" + listenPort);
            }
        }
        if (receiver == null) {
            try {
                receiver = NetFactory.initUdpReceiverSocket(groupAddr, listenPort, localAddress);
            } catch (IOException ex) {
                System.out.println("Failed to create socket Exception :" + ex.getCause() + " ; port:" + listenPort);
                receiver = null;
                return ByteBuffer.allocate(0);
            }
        }
        byte[] buf = new byte[128 * 1024];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        try {
            receiver.receive(packet);
        } catch (SocketTimeoutException ste) {
            throw ste;
        } catch (IOException e) {
            System.out.println("Receiving timed out.");
            failureCount++;
        }
        return ByteBuffer.wrap(packet.getData(), 0, packet.getLength());
    }

}
