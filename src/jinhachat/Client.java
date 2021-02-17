package jinhachat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;

public class Client {
    public static void main(String[] args) {
        try (SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress("localhost", 15000))) {
            WritableByteChannel out = Channels.newChannel(System.out);
            ByteBuffer outbuf = ByteBuffer.allocate(1024);

            //����ڰ� ä�� ������ �Է� �� ������ �����ϱ� ���� ������ ���� �� ����
            new Thread(() -> {
                // Ű���� �Է¹��� ä�ΰ� ������ ���� ����
                ReadableByteChannel in = Channels.newChannel(System.in);
                ByteBuffer inbuf = ByteBuffer.allocate(1024);

                try {
                    while (true) {
                        in.read(inbuf); // �о�ö����� ���ŷ�Ǿ� ������
                        inbuf.flip();

                        String id = new String(inbuf.array());

                        // TODO : �Է��� ��ɿ� ���� � ������������ �м��ϰ� �´� ��Ŷ ������ ����
                        Protocol nprotocol = new Protocol(Protocol.PT_RES_LOGIN);
                        nprotocol.setId(id);
                        System.out.println("�α��� ���� ����");
                        socketChannel.write(ByteBuffer.wrap(nprotocol.getPacket()));

                        inbuf.clear();
                    }

                } catch (IOException e) {
                    System.out.println("ä�� �Ұ�.");
                }
            }).start();

            /*
            �����κ��� ������ �б�
             */
            while (true) {
                Protocol protocol = new Protocol();
                byte[] bytes = protocol.getPacket();

                // TODO : read ���н� ó��
                socketChannel.read(outbuf); // �о ���ۿ� ����
                outbuf.rewind();
                bytes = outbuf.array();

                int packetType = bytes[0];
                protocol.setPacket(packetType, bytes);

                switch (packetType) {

                    //TODO : ��û�� �°� ó��
                    case Protocol.PT_UNDEFINED:
                        break;
                    case Protocol.PT_EXIT:
                        break;
                    case Protocol.PT_REQ_LOGIN:
                        System.out.println("ID�� �Է����ּ���(�� : ������) :");
                        //System.out.println("ID�� ä�ù� �̸��� �Է����ּ���(�� : ������/����) :");
                        break;
                    case Protocol.PT_LOGIN_RESULT:
                        System.out.println("������ �α��� ��� ����");
                        System.out.println(protocol.getId() + "���� �����ϼ̽��ϴ�.");

                        break;
                }

                outbuf.flip();
                //out.write(outbuf); // ����Ϳ� ���
                outbuf.clear();
            }

        } catch (IOException e) {
            System.out.println("������ ������ ����Ǿ����ϴ�.");
            e.printStackTrace();
        }
    }
}
