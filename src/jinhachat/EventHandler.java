package jinhachat;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

/*
 * Server에 데이터를 보내고 받아서 처리하는 클래스
 */
public class EventHandler extends Thread {
    private ClientInfo clientInfo;
    private Pipe keyboardAndHandlerPipe = null;
    private Pipe.SourceChannel keyboardToHandlerSourceChannel = null;

    public EventHandler(ClientInfo clientInfo) {
        try {
            this.clientInfo = clientInfo;
            this.keyboardAndHandlerPipe = Pipe.open();
            this.keyboardToHandlerSourceChannel = keyboardAndHandlerPipe.source();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Pipe.SinkChannel getKeyboardToHandlerSinkChannel() {
        return keyboardAndHandlerPipe.sink();
    }

    private ByteBuffer packetize(ProtocolHeader header, ClientInfo nClientInfo, ByteBuffer outputBuf) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
        objectOutputStream.writeObject(nClientInfo);
        objectOutputStream.flush();

        ByteBuffer body = ByteBuffer.wrap(byteArrayOutputStream.toByteArray());
        header.setBodyLength(body.limit()); //맞나?
        outputBuf.put(header.packetize());
        outputBuf.put(body);
        outputBuf.flip();

        return outputBuf;
    }

    @Override
    public void run() {
        try (SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress("localhost", 14000))) {

            Selector selector = Selector.open();

            socketChannel.configureBlocking(false);
            socketChannel.register(selector, SelectionKey.OP_READ);
            this.keyboardToHandlerSourceChannel.configureBlocking(false);
            this.keyboardToHandlerSourceChannel.register(selector, SelectionKey.OP_READ);

            //이벤트 감지
            while (true) {
                selector.select(); // 이벤트 발생할 때까지 블로킹

                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator(); // 발생한 이벤트를 가진 채널이 담김

                // 발생한 이벤트들을 담은 Iterator의 이벤트를 하나씩 순서대로 처리
                while (iterator.hasNext()) {

                    SelectionKey key = iterator.next();
                    iterator.remove(); // 처리한 키는 제거

                    if (key.isReadable()) { // {서버 or 키보드} -> 클라이언트 이벤트

                        //서버에서 수신
                        if (key.channel() == socketChannel) {
                            SocketChannel readSocket = (SocketChannel) key.channel(); // 현재 채널 정보

                            ByteBuffer serverBytebuffer = ByteBuffer.allocate(1024);

                            readSocket.read(serverBytebuffer);
                            serverBytebuffer.flip();

                            ProtocolHeader header = new ProtocolHeader();
                            header.parse(serverBytebuffer); //HEADER_LENGTH 만큼 읽고 파싱

                            ClientInfo clientInfo = (ClientInfo) new ObjectInputStream(new ByteArrayInputStream(serverBytebuffer.array())).readObject();
                            this.clientInfo = clientInfo;//맞는지 검토. 정신이 없음

                            switch (header.getProtocolType()) {
                                case RES_LOGIN_SUCCESS:
                                    System.out.println(clientInfo.getMSG() + "님, 로그인에 성공하여" + clientInfo.getChatRoom() + "에 입장하였습니다!");
                                    break;
                                case RES_LOGIN_FAIL:
                                    System.out.println("이미 있는 아이디입니다. 다시 입력해주세요.");
                                    break;
                                case RES_WHISPER_FAIL:
                                    System.out.println("귓속말을 하지 못하였습니다.");
                                    break;
                                case NOTICE_LOGIN:
                                    System.out.println(clientInfo.getMSG() + "님이 입장하였습니다.");
                                    break;
                                case NOTICE_CHAT:
                                    System.out.println(clientInfo.getID() + " : ");
                                    break;
                                case NOTICE_EXIT:
                                    System.out.println(clientInfo.getMSG() + "님이 퇴장하였습니다.");
                                    break;
                                case NOTICE_WHISPER:
                                    System.out.println("[귓속말]" + clientInfo.getID() + " : ");
                                    break;
                            }

                            serverBytebuffer.clear();

                        }
                        // 키보드에서 수신
                        else if (key.channel() == keyboardToHandlerSourceChannel) {
                            Pipe.SourceChannel readSocket = (Pipe.SourceChannel) key.channel(); // 현재 채널 정보
                            ByteBuffer keyboardByteBuffer = ByteBuffer.allocate(1024);
                            ByteBuffer outputBuf = ByteBuffer.allocate(1024);

                            readSocket.read(keyboardByteBuffer);

                            ProtocolHeader header = new ProtocolHeader();
                            ClientInfo nclientInfo = new ClientInfo();

                            byte[] bytes = new byte[keyboardByteBuffer.position()];
                            keyboardByteBuffer.flip();
                            keyboardByteBuffer.get(bytes);
                            String[] strings = new String(bytes).trim().split("/");

                            // ID/채팅방
                            if (clientInfo.getID() == "") {
                                String id = strings[0];
                                String chatRoom = strings[1];

                                header.setProtocolType(ProtocolHeader.PROTOCOL_OPT.REQ_LOGIN);
                                nclientInfo.setReqID(id);
                                nclientInfo.setReqChatRoom(chatRoom);
                            }
                            // 안녕
                            // /귓 홍길동 안녕
                            // /퇴장
                            // /채팅방리스트
                            else {
                                String message = strings[0];

                                header.setProtocolType(ProtocolHeader.PROTOCOL_OPT.REQ_LOGIN);
                                clientInfo.setMSG(message);
                            }

                            socketChannel.write(packetize(header, nclientInfo, outputBuf));
                            outputBuf.clear();
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
