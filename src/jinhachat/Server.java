package jinhachat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;

public class Server {
    Selector selector;
    Set<SocketChannel> allClient = new HashSet<>();
    Vector<ClientInfo> clientInfos = new Vector<ClientInfo>(); //클라이언트 정보를 따로 들어야할까?
    HashMap<String, Vector> chatRoomMap = new HashMap<String, Vector>(); //채팅방

    private static Server server = new Server();

    public static Server getInstance() {
        return server;
    }

    /* 연결 요청중인 클라이언트를 처리
     */
    void accept(SelectionKey selectionKey) throws IOException {
        ServerSocketChannel server = (ServerSocketChannel) selectionKey.channel(); //해당 요청에 대한 소켓 채널 생성
        SocketChannel clientSocket = server.accept();

        clientSocket.configureBlocking(false); // Selector의 관리를 받기 위해서 논블로킹 채널로 바꿔줌

        allClient.add(clientSocket); // 연결된 클라이언트를 컬렉션에 추가

        Protocol protocol = new Protocol(Protocol.PT_REQ_LOGIN);

        clientSocket.write(ByteBuffer.wrap(protocol.getPacket())); // 아이디를 입력받기 위한 출력을 해당 채널에 해줌

        clientSocket.register(selector, SelectionKey.OP_READ, new ClientInfo()); // 아이디를 입력받을 차례이므로 읽기모드로 셀렉터에 등록해줌
    }

    public static void main(String[] args) {
        Server server = Server.getInstance();

        try (ServerSocketChannel serverSocket = ServerSocketChannel.open()) { // implements AutoCloseable

            serverSocket.bind(new InetSocketAddress(15000));
            serverSocket.configureBlocking(false); // 기본값은 블로킹이므로 바꿔줌

            server.selector = Selector.open();
            serverSocket.register(server.selector, SelectionKey.OP_ACCEPT);

            System.out.println("----------서버 접속 준비 완료----------");

            ByteBuffer inputBuf = ByteBuffer.allocate(1024);
            ByteBuffer outputBuf = ByteBuffer.allocate(1024);

            // 클라이언트 접속 시작
            while (true) {

                server.selector.select(); // 이벤트 발생할 때까지 스레드 블로킹

                Iterator<SelectionKey> iterator = server.selector.selectedKeys().iterator();

                while (iterator.hasNext()) {

                    SelectionKey key = iterator.next();
                    iterator.remove(); // 처리한 키는 제거

                    if (key.isAcceptable()) { // 연결 요청 이벤트
                        server.accept(key);
                    } else if (key.isReadable()) { // 클라이언트 -> 서버 이벤트
                        SocketChannel readSocket = (SocketChannel) key.channel(); // 현재 채널 정보

                        try {
                            readSocket.read(inputBuf);
                        } catch (Exception e) {
                            //TODO : 연결 끊김 처리(퇴장 처리)
                        }

                        Protocol protocol = new Protocol();
                        byte[] bytes = protocol.getPacket();

                        inputBuf.rewind();
                        bytes = inputBuf.array();

                        int packetType = bytes[0];
                        protocol.setPacket(packetType, bytes);

                        switch (packetType) {
                            //클라이언트가 로그인 정보 응답 패킷인 경우 (클라이언트의 로그인 정보 전송일 경우)
                            case Protocol.PT_RES_LOGIN:

                                System.out.println("클라이언트가 로그인 정보를 보냈습니다.");

                                String id = protocol.getId();

                                boolean exist = false;
                                for (ClientInfo client : server.clientInfos) {
                                    if (client.getID().equals(id)) {
                                        exist = true;

                                        // 입장 재요청
                                        protocol.setProtocolType(Protocol.PT_REQ_LOGIN);
                                        break;
                                    }
                                }
                                if (!exist) { // ID생성 및 입장 성공
                                    protocol.setProtocolType(Protocol.PT_LOGIN_RESULT);

                                    System.out.println("["+protocol.getId() + "]님이 입장하셨습니다.");

                                    // TODO : 채팅방 클라이언트에게만 입장 메시지 출력
                                    // 모든 클라이언트에게 입장 메세지 출력
                                    outputBuf.put(ByteBuffer.wrap(protocol.getPacket()));
                                    for (SocketChannel s : server.allClient) {
                                        outputBuf.flip();
                                        s.write(outputBuf);
                                    }
                                }
                                else { // 입장 실패
                                    readSocket.write(ByteBuffer.wrap(protocol.getPacket()));
                                }

                                System.out.println("로그인 처리 결과 전송");
                                break;

                            // TODO : 아이디와 채팅 메시지를 결합한 버퍼 생성 및 작성
                            /*case Protocol.CHAT:
                                break;*/

                        }//end switch

                        inputBuf.clear();
                        outputBuf.clear();
                    }
                }
            }
        } catch (
                IOException e) {
            e.printStackTrace();
        }
    }
}
