package crinysoft.yellowduck;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.concurrent.Semaphore;

public class TcpUtil {
    //private static final String SERVER_HOST = "192.168.1.3";
    private static final String SERVER_HOST = "criny.asuscomm.com";
    private static final int SERVER_PORT = 9999;

    public static final byte PACKET_OPCODE_RESV_HELLO = (byte) 0x80;
    public static final byte PACKET_OPCODE_RESV_MSG = (byte) 0x81;
    public static final byte PACKET_OPCODE_SEND_MSG = (byte) 0x00;

    public static final int HANDLER_ID_RESV_HELLO = 0;
    public static final int HANDLER_ID_RESV_MSG = 1;
    public static final int HANDLER_ID_DISCONNECTED = 2;
    public static final int HANDLER_ID_SEND_MSG_SUCCESS = 3;

    public static final int BODY_CONTENT_MAX = 4096 - 2 - 1; // 65536 - 2 - 1;

    private static TcpUtil instance;
    Socket sock;
    InetSocketAddress ipep;
    OutputStream sendstream;
    InputStream recvstream;
    Handler handler;

    boolean isConnected = false;
    boolean getFeedbackMsg = false;

    private TcpUtil() {
    }

    byte[] readn(int size) throws IOException {
        byte[] buf = new byte[size];
        int ok = 0;

        while (ok < size) {
            int cnt = -1;

            try {
                Log.e("yellowduck-net-dbg", "before read");
                cnt = recvstream.read(buf, ok, size - ok);
                Log.e("yellowduck-net-dbg", "after read");
            } catch (SocketTimeoutException e) {
                Log.e("yellowduck-net-dbg", "exception!!!");

                if (isConnected == true)
                    continue;
                else
                    throw new IOException();
            }

            if (cnt <= 0) {
                throw new IOException();
            }
            ok += cnt;
        }

        Log.e("yellowduck-net", "TcpUtil-readn : readn complete(size:" + ok + ")");
        return buf;
    }

    public void startWorker() {
        Thread receiveThread = new Thread(new MyRunnable() {
            @Override
            public void run() {
                try {
                    isConnected = true;
                    getFeedbackMsg = true;

                    Thread.sleep(2000); // 임시 접속 딜레이 (접속 에니메이션 테스트용)

                    InetAddress[] ia = Inet4Address.getAllByName(SERVER_HOST);
                    String[] returnStr = new String[ia.length];
                    for (int i = 0; i < ia.length; i++)
                        returnStr[i] = ia[i].getHostAddress();

                    sock = new Socket();
                    ipep = new InetSocketAddress(returnStr[0], SERVER_PORT);
                    sock.setSoTimeout(100);
                    sock.connect(ipep);
                    sendstream = sock.getOutputStream();
                    recvstream = sock.getInputStream();

                    /*****************************
                     * Packet structure
                     *****************************/
                    // 2 byte : Packet length (Max 65,536)
                    // 1 byte : Operation code
                    // * byte : Content
                    while (true) {
                        // 1. Receive Packet Header
                        Log.e("yellowduck-net", "TcpUtil-startWorker : Packet Head Receiving...");
                        byte[] packetHeader = readn(2);
                        int packetLength = (packetHeader[0] << 8) + packetHeader[1];
                        int packetBodySize = packetLength - 2;

                        // 2. Receive Packet Body
                        Log.e("yellowduck-net", "TcpUtil-startWorker : Packet Body Receiving (BodySize:" + packetBodySize + ")...");
                        byte[] packetBody = readn(packetBodySize);

                        // 3. Handle Receive Data
                        byte opCode = packetBody[0];
                        byte[] bodyContent = Arrays.copyOfRange(packetBody, 1, packetBody.length);
                        if (handleReceiveData(opCode, bodyContent) == false) {
                            Log.e("yellowduck-net", "TcpUtil-startWorker : Wrong OPCode, throw exception!");
                            throw new IOException();
                        }
                    }
                } catch (Exception e) {
                    Log.e("yellowduck-net", "TcpUtil-startWorker : Catch exception!");
                    closeConnection();

                    if (getFeedbackMsg == true)
                        handler.sendEmptyMessage(HANDLER_ID_DISCONNECTED);
                }
            }
        });

        receiveThread.start();
    }

    public void CloseConnection(boolean flag) {
        isConnected = false;
        getFeedbackMsg = flag;
    }

    boolean handleReceiveData(byte opCode, byte[] bodyContent) {
        Log.e("yellowduck-net", "TcpUtil-handleReceiveData : Get Message(OPCode:" + Integer.toHexString(opCode) + ")");

        switch (opCode) {
            case PACKET_OPCODE_RESV_HELLO:
                handler.sendEmptyMessage(HANDLER_ID_RESV_HELLO);
                Log.e("yellowduck-net", "TcpUtil-handleReceiveData : HANDLER_RESV_HELLO");
                break;
            case PACKET_OPCODE_RESV_MSG:
                Message m = handler.obtainMessage();
                m.what = HANDLER_ID_RESV_MSG;
                m.obj = (Object) new String(bodyContent);

                handler.sendMessage(m);
                Log.e("yellowduck-net", "TcpUtil-handleReceiveData : PACKET_OPCODE_RESV_MSG(Msg:" + new String(bodyContent) + ")");
                break;
            default:
                return false;
        }

        return true;
    }

    public void sendCrcPacket(byte opCode, byte[] bodyContent) {
        new Thread(new MyRunnable(opCode, bodyContent) {
            @Override
            public void run() {
                try {
                    byte opCode = (byte) o1;
                    byte[] bodyContent = (byte[]) o2;

                    /*****************************
                     * Packet structure
                     *****************************/
                    // 2 byte : Packet length (Max 65,536)
                    // 1 byte : Operation code
                    // * byte : Content
                    byte[] packet = new byte[2 + 1 + bodyContent.length];

                    packet[0] = (byte) ((packet.length & 0xff00) >> 8);
                    packet[1] = (byte) (packet.length & 0xff);
                    packet[2] = opCode;
                    System.arraycopy(bodyContent, 0, packet, 3, bodyContent.length);

                    sendstream.write(packet, 0, packet.length);

                    handler.sendEmptyMessage(HANDLER_ID_SEND_MSG_SUCCESS);
                } catch (IOException e) {
                    Log.e("yellowduck-net", "TcpUtil-sendCrcPacket : Send exception!");
                    isConnected = false;
                }
            }
        }).start();
    }

    public void SendMessage(String sendText) {
        byte[] text = sendText.getBytes();

        if (text.length > BODY_CONTENT_MAX)
            return;

        sendCrcPacket(PACKET_OPCODE_SEND_MSG, text);
    }

    private void closeConnection() {
        try {
            if (sendstream != null) {
                sendstream.close();
                sendstream = null;
            }

            if (recvstream != null) {
                recvstream.close();
                recvstream = null;
            }

            if (sock != null) {
                sock.close();
                sock = null;
            }
        } catch (IOException e) {
            Log.e("yellowduck-net", "TcpUtil-closeConnection : close error");
        }
    }

    public void setHandler(Handler h) {
        handler = h;
    }

    public static TcpUtil getInstance() {
        if (instance == null) {
            instance = new TcpUtil();
        }
        return instance;
    }
}