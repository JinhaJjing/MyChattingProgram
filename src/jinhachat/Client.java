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

            out.write(ByteBuffer.wrap("ID를 입력해주세요(예 : 서진하) :".getBytes()));

            //'입장' 키보드 입력
            boolean isLoggedIn = false;
            while (!isLoggedIn) {
                in.read(inbuf); // 읽어올때까지 블로킹되어 대기상태
                inbuf.flip();

                ProtocolHeader header = new ProtocolHeader()
                        .setProtocolType(ProtocolHeader.PROTOCOL_OPT.REQ_LOGIN)
                        .setIDLength(inbuf.limit() - 1)
                        .build();

                socketChannel.write(header.packetize());
                inbuf.clear();
            }

            //서버로부터 온 데이터 읽기
            while (true) {

                // TODO : read 실패시 처리
                socketChannel.read(outbuf); // 읽어서 버퍼에 저장

                ProtocolHeader header = new ProtocolHeader();
                header.parse(outbuf); //HEADER_LENGTH 만큼 읽고 파싱

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
                        //사용자가 채팅 내용을 입력 및 서버로 전송하는 쓰레드 생성 및 시작
                        systemIn = new Thread(new SystemIn(socketChannel));
                        systemIn.start();
                        break;
                    //TODO : Fail 처리
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
            System.out.println("서버와 연결이 종료되었습니다.");
            e.printStackTrace();
        }
    }
}

class SystemIn implements Runnable {
    Client client = Client.getInstance();
    private SocketChannel socket;

    // 연결된 소켓 채널과 모니터 출력용 채널을 생성자로 받음
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