package jinhachat;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class EventHandler extends Thread {

    Client client = Client.getInstance();
    private SocketChannel socket;

    // 연결된 소켓 채널을 생성자로 받음
    EventHandler(SocketChannel socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        // 서버에서 온 데이터를 저장할 버퍼 생성
        ByteBuffer outbuf = ByteBuffer.allocate(1024);

        try {
            while (true) {
                socket.read(outbuf); // 읽어서 버퍼에 저장
                outbuf.flip();

                //TODO : builder로 변경
                ProtocolHeader header = new ProtocolHeader();
                header.parse(outbuf); //HEADER_LENGTH 만큼 읽고 파싱

                byte[] idTemp = new byte[header.getIDLength()]; //protocolbody에 메소드로 만들어야하나?
                outbuf.get(idTemp);
                String responseID = new String(idTemp);

                byte[] msgTemp = new byte[header.getMSGLength()];
                outbuf.get(msgTemp);
                String responseMSG = new String(msgTemp);

                switch (header.getProtocolType()) {
                    case RES_LOGIN_SUCCESS:
                        client.setID(responseID);
                        client.setLoggedIn(true);
                        //사용자가 채팅 내용을 입력 및 서버로 전송하는 쓰레드 생성 및 시작
                        Thread systemIn = new Thread(new SystemIn(socket));
                        systemIn.start();
                        break;
                    //TODO : Fail 처리
                    case RES_LOGIN_FAIL:
                        //out.write(ByteBuffer.wrap("ID를 다!시! 입력해주세요(예 : 서진하) :".getBytes()));
                        System.out.println("LOGIN FAIL");
                        break;
                    case RES_CHAT_FAIL:
                        System.out.println("CHAT FAIL");
                        break;
                    case RES_CHAT_SUCCESS:
                        break;
                    default:
                }

                outbuf.flip();
                outbuf.clear();
            }

        } catch (IOException e) {
            System.out.println("서버와 연결이 종료되었습니다.");
        }

    }
}
