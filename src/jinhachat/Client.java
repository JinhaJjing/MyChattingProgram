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

            while (true) {
                Protocol protocol = new Protocol();
                byte[] bytes = protocol.getPacket();

                socketChannel.read(outbuf); // �о ���ۿ� ����
                outbuf.rewind();
                bytes = outbuf.array();

                int packetType = bytes[0];

                //protocol.setPacket(packetType, bytes);

                switch (packetType) {

                    //TODO : ��û�� �°� ó��

                    case Protocol.PT_UNDEFINED:
                        break;
                    case Protocol.PT_EXIT:
                        break;
                    case Protocol.PT_REQ_LOGIN:
                        System.out.println("ID�� �Է����ּ���(�� : ������) :");
                        //System.out.println("ID�� ä�ù� �̸��� �Է����ּ���(�� : ������/����) :");

                        new Thread(() -> {
                            // Ű���� �Է¹��� ä�ΰ� ������ ���� ����
                            ReadableByteChannel in = Channels.newChannel(System.in);
                            ByteBuffer inbuf = ByteBuffer.allocate(1024);

                            try {
                                while (true) {
                                    in.read(inbuf); // �о�ö����� ���ŷ�Ǿ� ������
                                    inbuf.flip();
                                    socketChannel.write(inbuf); // �Է��� ������ ������ ���
                                    inbuf.clear();
                                }

                            } catch (IOException e) {
                                System.out.println("ä�� �Ұ�.");
                            }
                        }).start();

                        break;
                    case Protocol.PT_RES_LOGIN:
                        break;
                    case Protocol.PT_LOGIN_RESULT:
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
