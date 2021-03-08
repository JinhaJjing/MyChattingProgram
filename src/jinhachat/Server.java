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

    private ByteBuffer packetize(ProtocolBody nProtocolBody) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
        objectOutputStream.writeObject(nProtocolBody);
        objectOutputStream.flush();

        return ByteBuffer.wrap(byteArrayOutputStream.toByteArray());
    }

    private void noticeExit(SocketChannel readSocket, ByteBuffer outputBuf) throws IOException {
        String clientID = "", clientChatRoom = "";
        for (String ID : allClient.keySet()) {
            if (allClient.get(ID).getSocketChannel().equals(readSocket)) {
                clientID = allClient.get(ID).getID(); //퇴장하는 유저 아이디
                clientChatRoom = allClient.get(ID).getChatRoom(); //퇴장하는 유저가 속한 채팅방
                allClient.remove(ID);
                break;
            }
        }

        ProtocolBody nProtocolBody = new ProtocolBody();
        nProtocolBody.setID(clientID);
        ByteBuffer body = packetize(nProtocolBody);

        ProtocolHeader header = new ProtocolHeader();
        header.setProtocolType(ProtocolHeader.PROTOCOL_OPT.NOTICE_EXIT);
        header.setBodyLength(body.limit());

        outputBuf.put(header.packetize());
        outputBuf.put(body);

        List<ClientInfo> list = new ArrayList<>();
        ClientInfo curClientInfo = null;
        //채팅방 클라이언트들에게 퇴장을 알림
        for (String chatRoom : allChatRoom.keySet()) {
            if (chatRoom.equals(clientChatRoom)) {
                list = allChatRoom.get(chatRoom);
                for (ClientInfo clientInfo : allChatRoom.get(chatRoom)) {
                    if (clientInfo.getID().equals(clientID)) {
                        curClientInfo = clientInfo;
                        continue;
                    }
                    outputBuf.flip();
                    clientInfo.getSocketChannel().write(outputBuf);
                }
            }
        }
        list.remove(curClientInfo);
        allChatRoom.put(clientChatRoom, list);

        outputBuf.clear();
    }

    public void serverStart() {
        try (ServerSocketChannel serverSocket = ServerSocketChannel.open()) { // implements AutoCloseable

            serverSocket.bind(new InetSocketAddress(13000));
            serverSocket.configureBlocking(false); // 기본값은 블로킹이므로 논블로킹으로 바꿔줌

            Selector selector = Selector.open();
            serverSocket.register(selector, SelectionKey.OP_ACCEPT); // selector에 수락 모드 channel 등록

            System.out.println("----------Server booting completed----------");

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
                            continue;
                        }

                        ProtocolHeader header = new ProtocolHeader();
                        header.parse(inputBuf); //header를 먼저 해석

                        byte[] temp = new byte[header.getBodyLength()];
                        inputBuf.get(temp);
                        ProtocolBody protocolBody = (ProtocolBody) new ObjectInputStream(new ByteArrayInputStream(temp)).readObject();

                        switch (header.getProtocolType()) {
                            case REQ_LOGIN:
                                System.out.println("Client send login message.");
                                boolean IDexist = false;
                                if (allClient.containsKey(protocolBody.getID())) IDexist = true;

                                ProtocolHeader nheader = new ProtocolHeader();

                                if (!IDexist) { // ID생성 및 입장 성공
                                    ClientInfo nClientInfo = new ClientInfo();
                                    nClientInfo.setSocketChannel(readSocket);
                                    nClientInfo.setID(protocolBody.getID());
                                    nClientInfo.setChatRoom(protocolBody.getChatRoom());
                                    nClientInfo.setLoggedIn(true);

                                    allClient.put(protocolBody.getID(), nClientInfo); //연결된 클라이언트에 추가

                                    //채팅방에 클라이언트 추가(개설 및 입장)
                                    List<ClientInfo> list = new ArrayList<>();
                                    if (allChatRoom.containsKey(protocolBody.getChatRoom())) {
                                        for (String chatRoom : allChatRoom.keySet()) {
                                            if (chatRoom.equals(protocolBody.getChatRoom())) {
                                                list = allChatRoom.get(chatRoom);
                                                break;
                                            }
                                        }
                                    }
                                    list.add(nClientInfo);
                                    allChatRoom.put(protocolBody.getChatRoom(), list);

                                    ByteBuffer body = packetize(protocolBody);
                                    nheader.setProtocolType(ProtocolHeader.PROTOCOL_OPT.NOTICE_LOGIN);
                                    nheader.setBodyLength(body.limit());

                                    outputBuf.put(nheader.packetize());
                                    outputBuf.put(body);

                                    //채팅방 클라이언트들에게 로그인 알림
                                    for (ClientInfo clientinfo : list) {
                                        outputBuf.flip();
                                        clientinfo.getSocketChannel().write(outputBuf);
                                    }
                                    outputBuf.clear();

                                    //로그인 요청 클라이언트에게 성공 알림
                                    nheader.setProtocolType(ProtocolHeader.PROTOCOL_OPT.RES_LOGIN_SUCCESS);
                                    outputBuf.put(nheader.packetize());
                                    body.flip();
                                    outputBuf.put(body);

                                    System.out.println(protocolBody.getID() + " is logined and entered " + protocolBody.getChatRoom());

                                } else {
                                    nheader.setProtocolType(ProtocolHeader.PROTOCOL_OPT.RES_LOGIN_FAIL);
                                    outputBuf.put(nheader.packetize());

                                    System.out.println("Send login request");
                                }

                                outputBuf.flip();
                                readSocket.write(outputBuf);
                                outputBuf.clear();
                                break;

                            case REQ_CHAT:
                                System.out.println("Client send messsage.");

                                nheader = new ProtocolHeader();

                                ByteBuffer body = packetize(protocolBody);
                                nheader.setProtocolType(ProtocolHeader.PROTOCOL_OPT.NOTICE_CHAT);
                                nheader.setBodyLength(body.limit());

                                outputBuf.put(nheader.packetize());
                                outputBuf.put(body);

                                for (String ID : allClient.keySet()) {
                                    if (allClient.get(ID).getID().equals(protocolBody.getID())) {
                                        String curChatRoom = allClient.get(ID).getChatRoom();

                                        //채팅방 클라이언트들에게 채팅 알림
                                        for (String chatRoom : allChatRoom.keySet()) {
                                            if (chatRoom.equals(curChatRoom)) {
                                                for (ClientInfo clientInfo : allChatRoom.get(chatRoom)) {
                                                    outputBuf.flip();
                                                    clientInfo.getSocketChannel().write(outputBuf);
                                                }
                                            }
                                        }
                                        break;
                                    }
                                }
                                outputBuf.clear();
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