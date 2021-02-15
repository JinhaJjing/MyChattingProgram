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

                socketChannel.read(outbuf); // 읽어서 버퍼에 저장
                outbuf.rewind();
                bytes = outbuf.array();

                int packetType = bytes[0];

                //protocol.setPacket(packetType, bytes);

                switch (packetType) {

                    //TODO : 요청에 맞게 처리

                    case Protocol.PT_UNDEFINED:
                        break;
                    case Protocol.PT_EXIT:
                        break;
                    case Protocol.PT_REQ_LOGIN:
                        System.out.println("ID를 입력해주세요(예 : 진하찡) :");
                        //System.out.println("ID와 채팅방 이름을 입력해주세요(예 : 진하찡/드루와) :");

                        new Thread(() -> {
                            // 키보드 입력받을 채널과 저장할 버퍼 생성
                            ReadableByteChannel in = Channels.newChannel(System.in);
                            ByteBuffer inbuf = ByteBuffer.allocate(1024);

                            try {
                                while (true) {
                                    in.read(inbuf); // 읽어올때까지 블로킹되어 대기상태
                                    inbuf.flip();
                                    socketChannel.write(inbuf); // 입력한 내용을 서버로 출력
                                    inbuf.clear();
                                }

                            } catch (IOException e) {
                                System.out.println("채팅 불가.");
                            }
                        }).start();

                        break;
                    case Protocol.PT_RES_LOGIN:
                        break;
                    case Protocol.PT_LOGIN_RESULT:
                        break;

                }

                outbuf.flip();
                //out.write(outbuf); // 모니터에 출력
                outbuf.clear();
            }

        } catch (IOException e) {
            System.out.println("서버와 연결이 종료되었습니다.");
            e.printStackTrace();
        }
    }
}
