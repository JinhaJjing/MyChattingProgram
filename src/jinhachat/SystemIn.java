package jinhachat;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;

class SystemIn extends Thread {
    private SocketChannel keyboardSocketChannel;
    private ClientInfo clientInfo;

    // 연결된 소켓 채널을 생성자로 받음
    SystemIn(EventHandler eventHandler, ClientInfo clientInfo) {
        this.keyboardSocketChannel = eventHandler.getKeyboardSocketChannel();
        this.clientInfo = clientInfo;
    }

    @Override
    public void run() {
        // 키보드 입력받을 채널과 저장할 버퍼 생성
        ReadableByteChannel in = Channels.newChannel(System.in); //ReadableByteChannel?
        ByteBuffer inbuf = ByteBuffer.allocate(1024);


        while (true) {
/*                if (!client.isLoggedIn())
                    System.out.print("ID를 입력해주세요(예 : 서진하) :");
                else
                    System.out.print("채팅 : ");*/

            try {
                in.read(inbuf); // 읽어올때까지 블로킹되어 대기상태
                inbuf.flip();

                keyboardSocketChannel.write(inbuf);
                //socket.write(makeByPacket(inbuf));
                inbuf.clear();

            } catch (IOException e) {
                System.out.println("채팅 불가.");
            }
        }
    }


}