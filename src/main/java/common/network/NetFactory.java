package common.network;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;

/**
 * Created by colonel on 2014/5/15.
 */
class NetFactory {
    static final int TIMEOUT_MILLIS = 0;

    /**
     * 初始化用于接收的网络套接字
     *
     * @param groupAddress 组播地址
     * @param listenPort   端口
     * @param localAddress 可选的本机地址绑定
     * @return 完成初始化的MulticastSocket对象
     */
    public static MulticastSocket initUdpReceiverSocket(InetAddress groupAddress, int listenPort, InetAddress localAddress) throws IOException {
        MulticastSocket socket = new MulticastSocket(listenPort);
        socket.setLoopbackMode(true);
        socket.setReuseAddress(true);
        socket.setReceiveBufferSize(5 * 64 * 1024);

        try {
            socket.setInterface(localAddress);
        } catch (SocketException e) {
            InetAddress lAddress = InetAddress.getLocalHost();
            if (lAddress != null) {
                socket.setInterface(lAddress);
            }
            else
                return null;
        }
        if (groupAddress != null) {
            //如果传入的为多播地址，则加入多播组
            if (groupAddress.getHostAddress().compareToIgnoreCase("224.0.0.1") >= 0 &&
                    groupAddress.getHostAddress().compareToIgnoreCase("244.255.255.255") <= 0) {
                socket.joinGroup(groupAddress);
            }
        }
        socket.setSoTimeout(NetFactory.TIMEOUT_MILLIS);
        return socket;
    }

    /**
     * 初始化用于发送的网络套接字
     *
     * @param groupAddress 组播地址
     * @param port         端口
     * @param localAddress 可选的本机地址绑定
     * @return 完成初始化的MulticastSocket对象
     */
    public static MulticastSocket initUdpSenderSocket(InetAddress groupAddress, int port, InetAddress localAddress) throws Exception {
        MulticastSocket socket = new MulticastSocket();
        socket.setLoopbackMode(true);
        socket.setReuseAddress(true);
        if (localAddress != null) socket.setInterface(localAddress);
        if (groupAddress != null) {
            //如果传入的为多播地址，则加入多播组
            if (groupAddress.getHostAddress().compareToIgnoreCase("224.0.0.1") >= 0 && groupAddress.getHostAddress().compareToIgnoreCase("244.255.255.255") <= 0) {
                socket.joinGroup(groupAddress);
            }
            socket.connect(groupAddress, port);
        }
        return socket;
    }

}
