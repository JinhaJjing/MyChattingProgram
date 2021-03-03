package jinhachat;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;

public class Server {
    private Map<String, ClientInfo> allClient = new HashMap<>(); //<유저ID, 유저정보>
    private Map<String, List<ClientInfo>> allChatRoom = new HashMap<>(); //<채팅방, 유저ID리스트>

    /* 연결 요청중인 클라이언트를 처리
     */
    void accept(Selector selector, SelectionKey selectionKey) throws IOException {
        ServerSocketChannel server = (ServerSocketChannel) selectionKey.channel(); //해당 요청에 대한 소켓 채널 생성
        SocketChannel clientSocket = server.accept();

        clientSocket.configureBlocking(false); // Selector의 관리를 받기 위해서 논블로킹 채널로 바꿔줌
        clientSocket.register(selector, SelectionKey.OP_READ); // 아이디를 입력받을 차례이므로 읽기모드로 셀렉터에 등록해줌
    }

    private ByteBuffer packetize(ClientInfo nClientInfo) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
        objectOutputStream.writeObject(nClientInfo);
        objectOutputStream.flush();

        return ByteBuffer.wrap(byteArrayOutputStream.toByteArray());
    }

    private void noticeExit(SocketChannel readSocket, ByteBuffer outputBuf) throws IOException {
        String clientID = "", clientChatRoom = "";
        for (String ID : allClient.keySet()) {
            if (allClient.get(ID).getSocketChannel().equals(readSocket)) {
                allClient.remove(ID);
                clientID = allClient.get(ID).getID(); //퇴장하는 유저 아이디
                clientChatRoom = allClient.get(ID).getChatRoom(); //퇴장하는 유저가 속한 채팅방
                break;
            }
        }

        ClientInfo nClientInfo = new ClientInfo();
        nClientInfo.setMSG(clientID);
        ByteBuffer body = packetize(nClientInfo);

        ProtocolHeader header = new ProtocolHeader();
        header.setProtocolType(ProtocolHeader.PROTOCOL_OPT.NOTICE_EXIT);
        header.setBodyLength(body.limit()); //맞나?

        outputBuf.put(header.packetize());
        outputBuf.put(body);

        for (String chatRoom : allChatRoom.keySet()) {
            if (allChatRoom.get(chatRoom).equals(clientChatRoom)) {
                for (ClientInfo clientInfo : allChatRoom.get(chatRoom)) {
                    outputBuf.flip();
                    clientInfo.getSocketChannel().write(outputBuf);
                }
            }
        }

        outputBuf.clear();
    }

    public void serverStart() {
        try (ServerSocketChannel serverSocket = ServerSocketChannel.open()) { // implements AutoCloseable

            serverSocket.bind(new InetSocketAddress(14000));
            serverSocket.configureBlocking(false); // 기본값은 블로킹이므로 논블로킹으로 바꿔줌

            Selector selector = Selector.open();
            serverSocket.register(selector, SelectionKey.OP_ACCEPT); // selector에 수락 모드 channel 등록

            System.out.println("----------서버 접속 준비 완료----------");

            ByteBuffer inputBuf = ByteBuffer.allocate(1024);
            ByteBuffer outputBuf = ByteBuffer.allocate(1024);

            // 클라이언트 접속 시작
            while (true) {

                selector.select(); // 이벤트 발생할 때까지 스레드 블로킹

                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator(); // 발생한 이벤트를 가진 채널이 담김

                // 발생한 이벤트들을 담은 Iterator의 이벤트를 하나씩 순서대로 처리
                while (iterator.hasNext()) {

                    SelectionKey key = iterator.next();
                    iterator.remove(); // 처리한 키는 제거

                    if (key.isAcceptable()) { // 연결 요청 이벤트
                        accept(selector, key);

                    } else if (key.isReadable()) { // 클라이언트 -> 서버 이벤트
                        SocketChannel readSocket = (SocketChannel) key.channel(); // 현재 채널 정보

                        try {
                            readSocket.read(inputBuf);
                            inputBuf.flip();
                        } catch (Exception e) {
                            //채팅방 클라이언트들에게 퇴장 알림
                            noticeExit(readSocket, outputBuf);
                            continue; //다음 while로 가길 바라며
                        }

                        ProtocolHeader header = new ProtocolHeader();
                        header.parse(inputBuf); //header를 먼저 해석

                        switch (header.getProtocolType()) {
                            case REQ_LOGIN:
                                System.out.println("클라이언트가 로그인 정보를 보냈습니다.");

                                byte[] temp = new byte[header.getBodyLength()];
                                inputBuf.get(temp);
                                ClientInfo newClientInfo = (ClientInfo) new ObjectInputStream(new ByteArrayInputStream(temp)).readObject();

                                boolean IDexist = false;
                                if (allClient.containsKey(newClientInfo.getReqID())) IDexist = true;
                                // TODO : 채팅방 확인 로직

                                ProtocolHeader nheader = new ProtocolHeader();
                                ClientInfo nClientInfo = new ClientInfo();

                                if (!IDexist) { // ID생성 및 입장 성공
                                    nClientInfo.setSocketChannel(readSocket);
                                    nClientInfo.setID(newClientInfo.getReqID());
                                    nClientInfo.setChatRoom(newClientInfo.getReqChatRoom());
                                    nClientInfo.setLoggedIn(true);

                                    allClient.put(newClientInfo.getMSG(), nClientInfo); // 연결된 클라이언트를 컬렉션에 추가
                                    //TODO : 채팅방 개설
                                    //allChatRoom.put(nClientInfo.getReqChatRoom(),List<...>);

                                    ByteBuffer body = packetize(nClientInfo);

                                    nheader.setProtocolType(ProtocolHeader.PROTOCOL_OPT.RES_LOGIN_SUCCESS);
                                    nheader.setBodyLength(body.limit()); //맞나?

                                    for (String chatRoom : allChatRoom.keySet()) {
                                        if (allChatRoom.get(chatRoom).equals(newClientInfo.getChatRoom())) {
                                            for (ClientInfo clientInfo : allChatRoom.get(chatRoom)) {
                                                outputBuf.put(nheader.packetize());
                                                outputBuf.put(body);
                                                outputBuf.flip();
                                                clientInfo.getSocketChannel().write(outputBuf);
                                                outputBuf.clear();
                                            }
                                        }
                                    }

                                    nheader.setProtocolType(ProtocolHeader.PROTOCOL_OPT.NOTICE_LOGIN);
                                    nheader.setBodyLength(body.limit()); //맞나?

                                    System.out.println(nClientInfo.getID() + "님이 로그인하여 " + nClientInfo.getChatRoom() + "에 입장하였습니다");

                                } else {
                                    nheader.setProtocolType(ProtocolHeader.PROTOCOL_OPT.RES_LOGIN_FAIL);
                                    System.out.println("입장 재요청 전송");
                                }

                                outputBuf.put(nheader.packetize());
                                outputBuf.put(packetize(nClientInfo));

                                outputBuf.flip();
                                readSocket.write(outputBuf);

                                break;

                            // TODO : 채팅 메시지일 경우. 아이디와 채팅 메시지를 결합한 버퍼 생성 및 작성
                            case REQ_CHAT:

                                break;

                            // TODO : 귓속말 요청 처리
                            case REQ_WHISPER:

                                break;

                        }//end switch

                        inputBuf.clear();
                        outputBuf.clear();
                    }
                }
            }
        } catch (
                IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.serverStart();
    }
}