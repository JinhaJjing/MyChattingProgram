package jinhachat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;

public class Client {
    private String ID;
    private ProtocolBody body = null;

    private static Client client = new Client();

    public static Client getInstance() {
        return client;
    }

    public String getID() {
        return ID;
    }

    public void setID(String ID) {
        this.ID = ID;
    }

    public static void main(String[] args) {
        Client client = Client.getInstance();
        Thread systemIn;

        try (SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress("localhost", 15000))) {
            WritableByteChannel out = Channels.newChannel(System.out);
            ByteBuffer outbuf = ByteBuffer.allocate(1024);

            ReadableByteChannel in = Channels.newChannel(System.in);
            ByteBuffer inbuf = ByteBuffer.allocate(1024);

            out.write(ByteBuffer.wrap("ID�� �Է����ּ���(�� : ������) :".getBytes()));

            //'����' Ű���� �Է�
            boolean isLoggedIn = false;
            while (!isLoggedIn) {
                in.read(inbuf); // �о�ö����� ���ŷ�Ǿ� ������
                inbuf.flip();

                ProtocolHeader header = new ProtocolHeader()
                        .setProtocolType(ProtocolHeader.PROTOCOL_OPT.REQ_LOGIN)
                        .setIDLength(inbuf.limit() - 1)
                        .build();

                socketChannel.write(header.packetize());
                inbuf.clear();
            }

            //�����κ��� �� ������ �б�
            while (true) {

                // TODO : read ���н� ó��
                socketChannel.read(outbuf); // �о ���ۿ� ����

                ProtocolHeader header = new ProtocolHeader();
                header.parse(outbuf); //HEADER_LENGTH ��ŭ �а� �Ľ�

                byte[] temp = new byte[header.getIDLength()];
                outbuf.get(temp);
                String id = new String(temp);

                temp = new byte[header.getMSGLength()];
                outbuf.get(temp);
                String responseMSG = new String(temp);

                switch (header.getProtocolType()) {
                    case RES_LOGIN_SUCCESS:
                        client.setID(id);
                        isLoggedIn = true;
                        //����ڰ� ä�� ������ �Է� �� ������ �����ϴ� ������ ���� �� ����
                        systemIn = new Thread(new SystemIn(socketChannel));
                        systemIn.start();
                        break;
                    //TODO : Fail ó��
                    case RES_LOGIN_FAIL:
                        System.out.println(responseMSG);
                        break;
                    case RES_CHAT_FAIL:
                        System.out.println(responseMSG);
                        break;
                    case RES_CHAT_SUCCESS:
                        break;
                    default:
                }

                outbuf.flip();
                outbuf.clear();
            }

        } catch (IOException e) {
            System.out.println("������ ������ ����Ǿ����ϴ�.");
            e.printStackTrace();
        }
    }
}

class SystemIn implements Runnable {
    Client client = Client.getInstance();
    private SocketChannel socket;

    // ����� ���� ä�ΰ� ����� ��¿� ä���� �����ڷ� ����
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