package jinhachat;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;

class SystemIn implements Runnable {
    private SocketChannel socket;
    private Client client = null;

    // 연결된 소켓 채널을 생성자로 받음
    SystemIn(SocketChannel socket, Client client) {
        this.socket = socket;
        this.client = client;
    }

    public ByteBuffer makeByPacket(ByteBuffer inbuf) {
        ProtocolHeader header;
        ProtocolBody body = new ProtocolBody();

        if (!client.isLoggedIn()) {
            //ID일때
            String id = new String(inbuf.array()).trim();

            header = new ProtocolHeader()
                    .setProtocolType(ProtocolHeader.PROTOCOL_OPT.REQ_LOGIN)
                    .setIDLength(id.length())
                    .build();

            body.setID(id);
        } else {
            //채팅일때
            String msg = new String(inbuf.array()).trim();

            header = new ProtocolHeader()
                    .setProtocolType(ProtocolHeader.PROTOCOL_OPT.REQ_CHAT)
                    .setIDLength(client.getID().length())
                    .setMSGLength(msg.length())
                    .build();

            body.setID(client.getID());
            body.setMsg(msg);
        }

        inbuf.clear();

        int bodyLength = header.getIDLength() + header.getMSGLength();
        inbuf.put(header.packetize());
        inbuf.put(body.packetize(ByteBuffer.allocate(bodyLength)));
        inbuf.flip();

        return inbuf;
    }

    @Override
    public void run() {
        // 키보드 입력받을 채널과 저장할 버퍼 생성
        ReadableByteChannel in = Channels.newChannel(System.in); //ReadableByteChannel?
        ByteBuffer inbuf = ByteBuffer.allocate(1024);

        try {
            while (true) {
                if (!client.isLoggedIn())
                    System.out.print("ID를 입력해주세요(예 : 서진하) :");
                else
                    System.out.print("채팅 : ");

                in.read(inbuf); // 읽어올때까지 블로킹되어 대기상태
                inbuf.flip();

                socket.write(makeByPacket(inbuf));
                inbuf.clear();
            }

        } catch (IOException e) {
            System.out.println("채팅 불가.");
        }
    }


}