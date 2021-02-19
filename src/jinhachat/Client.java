package jinhachat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;

public class Client {

    private String id="";

    // ID 정보 반환
    String getID() {
        return id;
    }

    public Client(String id) {
        this.id = id;
    }

    // ID 입력
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
                //TODO : 키보드 입력(아이디만)
            }

            //사용자가 채팅 내용을 입력 및 서버로 전송하는 쓰레드 생성 및 시작
            systemIn = new Thread(new SystemIn(socketChannel, sharedData));
            systemIn.start();

            //서버로부터 온 데이터 읽기
            while (true) {

                // TODO : read 실패시 처리
                socketChannel.read(outbuf); // 읽어서 버퍼에 저장

                ProtocolHeader header = new ProtocolHeader();
                header.parse(outbuf); //HEADER_LENGTH 만큼 읽고 파싱

                switch (header.getProtocolType()) {
                    //TODO : 요청에 맞게 처리
                    //header.getBodyLength() 만큼 byte 읽기?

                    case ENTER_ROOM:
                        System.out.println("ID를 입력해주세요(예 : 서진하) :");
                        //전송 스레드에 서버가 요청한 값 알림
                        sharedData.setProtocolType(ProtocolHeader.PROTOCOL_OPT.ENTER_ROOM.getValue());
                        break;
                    case SERVICE_RESPONSE:
                        System.out.println("서버가 로그인 결과 전송");
                        // TODO : 성공이면 isLoggedIn=true; 아니면
                        System.out.println(body.getID() + "님이 입장하셨습니다.");
                        break;
                }

                outbuf.flip();
                outbuf.clear();
            }

        } catch (IOException e) {
            System.out.println("서버와 연결이 종료되었습니다.");
            e.printStackTrace();
        }
    }
}

public class SystemIn implements Runnable {
    private SocketChannel socket;
    private ProtocolHeader sharedData;

    // 연결된 소켓 채널과 모니터 출력용 채널을 생성자로 받음
    SystemIn(SocketChannel socket, ProtocolHeader sharedData) {
        this.socket = socket;
        this.sharedData = sharedData;
    }

    @Override
    public void run() {

        // 키보드 입력받을 채널과 저장할 버퍼 생성
        ReadableByteChannel in = Channels.newChannel(System.in);
        ByteBuffer inbuf = ByteBuffer.allocate(1024);

        try {
            while (true) {
                in.read(inbuf); // 읽어올때까지 블로킹되어 대기상태
                inbuf.flip();

                switch(sharedData.getProtocolType()){
                    // 로그인 요청 or 재요청
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

                        System.out.println("로그인 정보 전송");
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
            System.out.println("채팅 불가.");
        }
    }
}