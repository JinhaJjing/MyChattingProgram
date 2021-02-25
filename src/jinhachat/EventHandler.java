package jinhachat;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class EventHandler extends Thread {

    private SocketChannel socket;
    private Client client = null;

    // 연결된 소켓 채널을 생성자로 받음
    EventHandler(SocketChannel socket, Client client) {
        this.socket = socket;
        this.client = client;
    }

    @Override
    public void run() {
        ByteBuffer outbuf = ByteBuffer.allocate(1024);

        try {
            while (true) {
                socket.read(outbuf); // 읽어서 버퍼에 저장
                outbuf.flip();

                ProtocolHeader header = new ProtocolHeader();
                header.parse(outbuf); //HEADER_LENGTH 만큼 읽고 파싱

                byte[] idTemp = new byte[header.getIDLength()];
                outbuf.get(idTemp);
                String responseID = new String(idTemp);

                byte[] msgTemp = new byte[header.getMSGLength()];
                outbuf.get(msgTemp);
                String responseMSG = new String(msgTemp);

                switch (header.getProtocolType()) {
                    case RES_LOGIN_SUCCESS:
                        System.out.print(responseID + "님, 로그인에 성공하였습니다!");
                        client.setID(responseID);
                        client.setLoggedIn(true);
                        break;

                    case RES_LOGIN_FAIL:
                        System.out.print("이미 있는 아이디입니다.");
                        break;
                }

                outbuf.clear();
            }

        } catch (IOException e) {
            System.out.println("서버와 연결이 종료되었습니다.");
        }

    }
}
