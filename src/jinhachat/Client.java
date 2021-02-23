package jinhachat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;

public class Client {
    String ID;
    boolean isLoggedIn = false;

    private static Client client = new Client();

    public static Client getInstance() {
        return client;
    }

    public static void main(String[] args) {
        Client client = Client.getInstance();
        Thread eventHandler;

        try (SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress("localhost", 15000))) {
            WritableByteChannel out = Channels.newChannel(System.out);

            ReadableByteChannel in = Channels.newChannel(System.in);
            ByteBuffer inbuf = ByteBuffer.allocate(1024);

            //����ڰ� ä�� ������ �Է� �� ������ �����ϴ� ������ ���� �� ����
            eventHandler = new Thread(new EventHandler(socketChannel));
            eventHandler.start();

            out.write(ByteBuffer.wrap("ID�� �Է����ּ���(�� : ������) :".getBytes()));

            //'����' Ű���� �Է�
            while (!client.isLoggedIn) {
                in.read(inbuf); // �о�ö����� ���ŷ�Ǿ� ������
                inbuf.rewind();

                String id = new String(inbuf.array()).trim();

                ProtocolHeader header = new ProtocolHeader()
                        .setProtocolType(ProtocolHeader.PROTOCOL_OPT.REQ_LOGIN)
                        .setIDLength(id.length())
                        .build();

                ProtocolBody body = new ProtocolBody();
                body.setID(id);

                inbuf.clear();

                int bodyLength = header.getIDLength() + header.getMSGLength();
                inbuf.put(header.packetize());
                inbuf.put(body.packetize(ByteBuffer.allocate(bodyLength)));
                inbuf.flip();
                socketChannel.write(inbuf);

                inbuf.clear();
            }

        } catch (IOException e) {
            System.out.println("������ ������ ����Ǿ����ϴ�.");
            e.printStackTrace();
        }
    }

    public String getID() {
        return ID;
    }

    public void setID(String ID) {
        this.ID = ID;
    }

    public boolean isLoggedIn() {
        return isLoggedIn;
    }

    public void setLoggedIn(boolean loggedIn) {
        isLoggedIn = loggedIn;
    }

}

class SystemIn implements Runnable {

    private SocketChannel socket;

    // ����� ���� ä���� �����ڷ� ����
    SystemIn(SocketChannel socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        // Ű���� �Է¹��� ä�ΰ� ������ ���� ����
        ReadableByteChannel in = Channels.newChannel(System.in);
        ByteBuffer inbuf = ByteBuffer.allocate(1024);

        try {
            while (true) {
                in.read(inbuf); // �о�ö����� ���ŷ�Ǿ� ������
                inbuf.flip();

                String msg = new String(inbuf.array());

                ProtocolHeader header = new ProtocolHeader()
                        .setProtocolType(ProtocolHeader.PROTOCOL_OPT.REQ_CHAT)
                        .setIDLength(client.getID().length())
                        .setMSGLength(msg.length())
                        .build();
                ProtocolBody body = new ProtocolBody();
                body.setMsg(msg);

                inbuf.put(header.packetize()).put(body.packetize());
                socket.write(inbuf);
                inbuf.clear();
            }

        } catch (IOException e) {
            System.out.println("ä�� �Ұ�.");
        }
    }
}