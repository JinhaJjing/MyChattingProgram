package jinhachat;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.Pipe;
import java.nio.channels.ReadableByteChannel;

/*
 * EventHandler에 키보드 입력값을 보내는 클래스
 */
class SystemIn extends Thread {
    private Pipe.SinkChannel keyboardToHandlerSinkChannel;

    // 연결된 소켓 채널을 생성자로 받음
    SystemIn(EventHandler eventHandler) {
        this.keyboardToHandlerSinkChannel = eventHandler.getKeyboardToHandlerSinkChannel();
    }

    @Override
    public void run() {
        // 키보드 입력받을 채널과 저장할 버퍼 생성
        ReadableByteChannel in = Channels.newChannel(System.in);
        ByteBuffer inbuf = ByteBuffer.allocate(1024);
        System.out.println("아이디와 채팅방을 입력해주세요.");

        while (true) {
            try {
                in.read(inbuf); // 읽어올때까지 블로킹되어 대기상태
                inbuf.flip();
                keyboardToHandlerSinkChannel.write(inbuf);
                inbuf.clear();

            } catch (IOException e) {
                System.out.println("채팅 불가.");
            }
        }
    }
}