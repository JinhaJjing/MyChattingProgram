package jinhachat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

/*
 * Server에 데이터를 보내고 받아서 처리하는 클래스
 */
public class EventHandler extends Thread {
    private ClientInfo clientInfo;
    private SocketChannel keyboardSocketChannel;

    public EventHandler (ClientInfo clientInfo) {
        this.clientInfo = clientInfo;
    }

    public SocketChannel getKeyboardSocketChannel() {
        return keyboardSocketChannel;
    }

    public ByteBuffer makeByPacket(ByteBuffer inbuf) {
        ProtocolHeader header = new ProtocolHeader();
        ClientInfo body = new ClientInfo();

        if (!clientInfo.isLoggedIn()) {
            //ID일때
            String id = new String(inbuf.array()).trim();

            header.setProtocolType(ProtocolHeader.PROTOCOL_OPT.REQ_LOGIN);
            header.setBodyLength();
            body.setID(id);
        } else {
            //채팅일때
            String msg = new String(inbuf.array()).trim();

            header.setProtocolType(ProtocolHeader.PROTOCOL_OPT.REQ_CHAT);
            header.setBodyLength();

            body.setID(clientInfo.getID());
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
        try (SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress("localhost", 15000))){
             //SocketChannel keyboardSocketChannel = SocketChannel.open(new InetSocketAddress("localhost", 16000))) {

            Selector selector = Selector.open();

            socketChannel.configureBlocking(false);
            socketChannel.register(selector, SelectionKey.OP_READ);

            SocketChannel keyboardSocketChannel = SocketChannel.open();
            keyboardSocketChannel.configureBlocking(false);
            keyboardSocketChannel.register(selector, SelectionKey.OP_READ);

            //이벤트 감지
            while (true) {
                selector.select(); // 이벤트 발생할 때까지 블로킹

                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator(); // 발생한 이벤트를 가진 채널이 담김

                // 발생한 이벤트들을 담은 Iterator의 이벤트를 하나씩 순서대로 처리
                while (iterator.hasNext()) {

                    SelectionKey key = iterator.next();
                    iterator.remove(); // 처리한 키는 제거

                    if (key.isReadable()) { // 클라이언트 -> 서버 이벤트
                        SocketChannel readSocket = (SocketChannel) key.channel(); // 현재 채널 정보

                        //서버에서 수신
                        if (key.channel() == socketChannel) {
                            ByteBuffer serverBytebuffer = ByteBuffer.allocate(1024);

                            readSocket.read(serverBytebuffer);
                            serverBytebuffer.flip();

                            ProtocolHeader header = new ProtocolHeader();
                            header.parse(serverBytebuffer); //HEADER_LENGTH 만큼 읽고 파싱

                            ClientInfo clientInfo = (ClientInfo) new ObjectInputStream(new ByteArrayInputStream(serverBytebuffer.array())).readObject();

                            switch (header.getProtocolType()) {
                                case RES_LOGIN_SUCCESS:
                                    System.out.print(clientInfo.getMSG() + "님, 로그인에 성공하였습니다!");
                                    clientInfo.setID(clientInfo.getMSG());
                                    clientInfo.setLoggedIn(true);
                                    break;

                                case RES_LOGIN_FAIL:
                                    System.out.print("이미 있는 아이디입니다.");
                                    break;
                            }

                            serverBytebuffer.clear();

                        }
                        // 키보드에서 수신
                        else if (key.channel() == keyboardSocketChannel) {
                            ByteBuffer keyboardByteBuffer = ByteBuffer.allocate(1024);

                            readSocket.read(keyboardByteBuffer);
                            keyboardByteBuffer.flip();

                            // 홍길동/채팅방1
                            // 안녕
                            // /귓 홍길동 안녕
                            // /퇴장
                            // /채팅방리스트

                            //TODO : 입력받은 값 가공
                        }
                    }
                }
            }

        } catch (IOException | ClassNotFoundException e) {
            System.out.println("\n서버와의 연결을 실패했습니다.");
            e.printStackTrace();
        }

    }
}
