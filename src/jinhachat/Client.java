package jinhachat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;

public class Client {

    private String id="";

    // ID ���� ��ȯ
    String getID() {
        return id;
    }

    public Client(String id) {
        this.id = id;
    }

    // ID �Է�
    void setID(String id) {
        this.id = id;
    }

    public static void main(String[] args) {
        Thread systemIn;
        ProtocolHeader sharedData = new ProtocolHeader();

        try (SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress("localhost", 15000))) {
            WritableByteChannel out = Channels.newChannel(System.out);
            ByteBuffer outbuf = ByteBuffer.allocate(1024);

            boolean isLoggedIn = false;
            while(!isLoggedIn){
                //TODO : Ű���� �Է�(���̵�)
            }

            //����ڰ� ä�� ������ �Է� �� ������ �����ϴ� ������ ���� �� ����
            systemIn = new Thread(new SystemIn(socketChannel, sharedData));
            systemIn.start();

            //�����κ��� �� ������ �б�
            while (true) {

                // TODO : read ���н� ó��
                socketChannel.read(outbuf); // �о ���ۿ� ����

                ProtocolHeader header = new ProtocolHeader();
                header.parse(outbuf); //HEADER_LENGTH ��ŭ �а� �Ľ�

                switch (header.getProtocolType()) {
                    //TODO : ��û�� �°� ó��
                    //header.getBodyLength() ��ŭ byte �б�?

                    case ENTER_ROOM:
                        System.out.println("ID�� �Է����ּ���(�� : ������) :");
                        //���� �����忡 ������ ��û�� �� �˸�
                        sharedData.setProtocolType(ProtocolHeader.PROTOCOL_OPT.ENTER_ROOM.getValue());
                        break;
                    case SERVICE_RESPONSE:
                        System.out.println("������ �α��� ��� ����");
                        // TODO : �����̸� isLoggedIn=true; �ƴϸ�
                        System.out.println(body.getID() + "���� �����ϼ̽��ϴ�.");
                        break;
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

public class SystemIn implements Runnable {
    private SocketChannel socket;
    private ProtocolHeader sharedData;

    // ����� ���� ä�ΰ� ����� ��¿� ä���� �����ڷ� ����
    SystemIn(SocketChannel socket, ProtocolHeader sharedData) {
        this.socket = socket;
        this.sharedData = sharedData;
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

                switch(sharedData.getProtocolType()){
                    // �α��� ��û or ���û
                    case ENTER_ROOM:
                        String id = new String(inbuf.array());
                        // TODO : packetize

                        byte[] bodyBytes = bodyJson.marshal(body);
                        ProtocolHeader header = new ProtocolHeader();
                        header.setProtocolType(ProtocolHeader.PROTOCOL_OPT.ENTER_ROOM.getValue());
                        header.setBodyLength(bodyBytes.length);

                        ByteBuffer byteBuffer = ByteBuffer.allocate(ProtocolHeader.HEADER_LENGTH + bodyBytes.length);
                        byteBuffer = header.packetize(byteBuffer);
                        byteBuffer.put(bodyBytes);
                        byteBuffer.flip();

                        System.out.println("�α��� ���� ����");
                        socket.write(byteBuffer);
                        break;
                    case SEND_MESSAGE:
                        // TODO : packetize
                        //String msg = new String(inbuf.array());
                        //protocol.body.setId(id);
                        break;
                }

                inbuf.clear();
            }

        } catch (IOException e) {
            System.out.println("ä�� �Ұ�.");
        }
    }
}