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
    private ClientInfo clientInfo = new ClientInfo();
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

    private ByteBuffer packetize(ProtocolHeader header, ProtocolBody nProtocolBody, ByteBuffer outputBuf) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
        objectOutputStream.writeObject(nProtocolBody);
        objectOutputStream.flush();

        ByteBuffer body = ByteBuffer.wrap(byteArrayOutputStream.toByteArray());
        header.setBodyLength(body.limit());
        outputBuf.put(header.packetize());
        outputBuf.put(body);
        outputBuf.flip();

        return outputBuf;
    }

    @Override
    public void run() {
        try (SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress("localhost", 13000))) {
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
                        if (key.channel().equals(socketChannel)) {
                            SocketChannel readSocket = (SocketChannel) key.channel(); // 현재 채널 정보

                            ByteBuffer headerBytebuffer = ByteBuffer.allocate(16);

                            readSocket.read(headerBytebuffer);
                            headerBytebuffer.flip();

                            ProtocolHeader header = new ProtocolHeader();
                            header.parse(headerBytebuffer); //HEADER_LENGTH 만큼 읽고 파싱

                            ByteBuffer bodyBytebuffer = ByteBuffer.allocate(header.getBodyLength());
                            readSocket.read(bodyBytebuffer);
                            bodyBytebuffer.flip();

                            ProtocolBody protocolBody = null;
                            if (header.getBodyLength() > 0) {
                                byte[] temp = new byte[header.getBodyLength()];
                                bodyBytebuffer.get(temp);
                                protocolBody = (ProtocolBody) new ObjectInputStream(new ByteArrayInputStream(temp)).readObject();
                            }

                            switch (header.getProtocolType()) {
                                case RES_LOGIN_SUCCESS:
                                    clientInfo.setLoggedIn(true);
                                    clientInfo.setID(protocolBody.getID());
                                    clientInfo.setChatRoom(protocolBody.getChatRoom());
                                    System.out.println("[알림]'"+clientInfo.getID()+"' 로 로그인 후 '"+clientInfo.getChatRoom()+"' 입장");
                                    break;
                                case RES_LOGIN_FAIL:
                                    System.out.println("[알림]이미 있는 아이디입니다. 다시 입력해주세요.");
                                    break;
                                case RES_WHISPER_FAIL:
                                    System.out.println("[알림]귓속말을 하지 못하였습니다.");
                                    break;
                                case NOTICE_LOGIN:
                                    System.out.println("[알림]" + protocolBody.getID() + "님이 입장하였습니다.");
                                    break;
                                case NOTICE_CHAT:
                                    System.out.println(protocolBody.getID() + " : " + protocolBody.getMSG());
                                    break;
                                case NOTICE_EXIT:
                                    System.out.println("[알림]" + protocolBody.getID() + "님이 퇴장하였습니다.");
                                    break;
                                case NOTICE_WHISPER:
                                    System.out.println("[귓속말]" + protocolBody.getID() + " : ");
                                    break;
                            }

                            bodyBytebuffer.clear();
                            headerBytebuffer.clear();
                        }
                        // 키보드에서 수신
                        else if (key.channel().equals(keyboardToHandlerSourceChannel)) {
                            Pipe.SourceChannel readSocket = (Pipe.SourceChannel) key.channel(); // 현재 채널 정보
                            ByteBuffer keyboardByteBuffer = ByteBuffer.allocate(1024);
                            ByteBuffer outputBuf = ByteBuffer.allocate(1024);

                            readSocket.read(keyboardByteBuffer);

                            ProtocolHeader header = new ProtocolHeader();
                            ProtocolBody body = new ProtocolBody();

                            byte[] bytes = new byte[keyboardByteBuffer.position()];
                            keyboardByteBuffer.flip();
                            keyboardByteBuffer.get(bytes);
                            String[] strings = new String(bytes).trim().split("/");
                            String inputMessage = new String(bytes);

                            // ID/채팅방
                            if (!clientInfo.isLoggedIn()) {
                                String id = strings[0];
                                String chatRoom = strings[1];

                                header.setProtocolType(ProtocolHeader.PROTOCOL_OPT.REQ_LOGIN);
                                body.setID(id);
                                body.setChatRoom(chatRoom);
                            }
                            else {
                                String message = strings[0];

                                // 귓속말, 퇴장, 채팅방리스트
                                if (message.equals("")) {
                                    if(inputMessage.substring(0,1).equals("/")){
                                        if(inputMessage.substring(1,2).equals("귓")){
                                            String[] cmdAndMSG = strings[1].trim().split(" ");
                                            header.setProtocolType(ProtocolHeader.PROTOCOL_OPT.REQ_WHISPER);
                                            body.setID(clientInfo.getID());
                                            body.setTargetID(cmdAndMSG[1]);
                                            body.setMSG(new String(bytes).substring(4+cmdAndMSG[1].length()).trim());
                                        } else if(inputMessage.substring(1,3).equals("퇴장")){
                                            //TODO : terminate client
                                        } else if(inputMessage.substring(1,7).equals("채팅방리스트")){
                                            //TODO : show chatroomlist
                                        }
                                        else{

                                        }
                                    }
                                }
                                // 채팅
                                else {
                                    header.setProtocolType(ProtocolHeader.PROTOCOL_OPT.REQ_CHAT);
                                    body.setID(clientInfo.getID());
                                    body.setMSG(message);
                                }
                            }

                            socketChannel.write(packetize(header, body, outputBuf));
                            outputBuf.clear();
                        }
                    }
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
