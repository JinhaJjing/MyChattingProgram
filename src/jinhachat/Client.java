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

            //사용자가 채팅 내용을 입력 및 서버로 전송하기 위한 쓰레드 생성 및 시작
            new Thread(() -> {
                // 키보드 입력받을 채널과 저장할 버퍼 생성
                ReadableByteChannel in = Channels.newChannel(System.in);
                ByteBuffer inbuf = ByteBuffer.allocate(1024);

                try {
                    while (true) {
                        in.read(inbuf); // 읽어올때까지 블로킹되어 대기상태
                        inbuf.flip();

                        String id = new String(inbuf.array());

                        // TODO : 입력한 명령에 따라 어떤 프로토콜인지 분석하고 맞는 패킷 서버로 전송
                        Protocol nprotocol = new Protocol(Protocol.PT_RES_LOGIN);
                        nprotocol.setId(id);
                        System.out.println("로그인 정보 전송");
                        socketChannel.write(ByteBuffer.wrap(nprotocol.getPacket()));

                        inbuf.clear();
                    }

                } catch (IOException e) {
                    System.out.println("채팅 불가.");
                }
            }).start();

            /*
            서버로부터 데이터 읽기
             */
            while (true) {
                Protocol protocol = new Protocol();
                byte[] bytes = protocol.getPacket();

                // TODO : read 실패시 처리
                socketChannel.read(outbuf); // 읽어서 버퍼에 저장
                outbuf.rewind();
                bytes = outbuf.array();

                int packetType = bytes[0];
                protocol.setPacket(packetType, bytes);

                switch (packetType) {

                    //TODO : 요청에 맞게 처리
                    case Protocol.PT_UNDEFINED:
                        break;
                    case Protocol.PT_EXIT:
                        break;
                    case Protocol.PT_REQ_LOGIN:
                        System.out.println("ID를 입력해주세요(예 : 진하찡) :");
                        //System.out.println("ID와 채팅방 이름을 입력해주세요(예 : 진하찡/드루와) :");
                        break;
                    case Protocol.PT_LOGIN_RESULT:
                        System.out.println("서버가 로그인 결과 전송");
                        System.out.println(protocol.getId() + "님이 입장하셨습니다.");

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
