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

            //사용자가 채팅 내용을 입력 및 서버로 전송하는 쓰레드 생성 및 시작
            eventHandler = new Thread(new EventHandler(socketChannel));
            eventHandler.start();

            out.write(ByteBuffer.wrap("ID를 입력해주세요(예 : 서진하) :".getBytes()));

            //'입장' 키보드 입력
            while (!client.isLoggedIn) {
                in.read(inbuf); // 읽어올때까지 블로킹되어 대기상태
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
            System.out.println("서버와 연결이 종료되었습니다.");
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

    // 연결된 소켓 채널을 생성자로 받음
    SystemIn(SocketChannel socket) {
        this.socket = socket;
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
            System.out.println("채팅 불가.");
        }
    }
}