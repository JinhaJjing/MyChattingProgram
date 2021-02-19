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
    Vector<Client> clients = new Vector<Client>(); //클라이언트 정보를 따로 들어야할까?
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

        ProtocolHeader header = new ProtocolHeader();
        header.setProtocolType(ProtocolHeader.PROTOCOL_OPT.ENTER_ROOM.getValue());
        header.setBodyLength(0);
        ByteBuffer outbuf = ByteBuffer.allocate(1024);
        clientSocket.write(header.packetize(outbuf)); // 아이디를 입력받기 위한 출력을 해당 채널에 해줌

        clientSocket.register(selector, SelectionKey.OP_READ); // 아이디를 입력받을 차례이므로 읽기모드로 셀렉터에 등록해줌
    }

    public static void main(String[] args) {
        Server server = Server.getInstance();

        try (ServerSocketChannel serverSocket = ServerSocketChannel.open()) { // implements AutoCloseable

            serverSocket.bind(new InetSocketAddress(15000));
            serverSocket.configureBlocking(false); // 기본값은 블로킹이므로 바꿔줌

            server.selector = Selector.open();
            serverSocket.register(server.selector, SelectionKey.OP_ACCEPT); // selector에 수락 모드 channel 등록

            System.out.println("----------서버 접속 준비 완료----------");

            ByteBuffer inputBuf = ByteBuffer.allocate(1024);
            ByteBuffer outputBuf = ByteBuffer.allocate(1024);

            // 클라이언트 접속 시작
            while (true) {

                server.selector.select(); // 이벤트 발생할 때까지 스레드 블로킹

                Iterator<SelectionKey> iterator = server.selector.selectedKeys().iterator(); // 발생한 이벤트를 가진 채널이 담김

                // 발생한 이벤트들을 담은 Iterator의 이벤트를 하나씩 순서대로 처리
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

                        ProtocolHeader header = new ProtocolHeader();
                        header.parse(inputBuf); //header를 먼저 해석

                        switch (header.getProtocolType()) {
                            // TODO : 클라이언트가 로그인 정보 응답 패킷인 경우 (클라이언트의 로그인 정보 전송일 경우)
                            case ENTER_ROOM:

/*                                System.out.println("클라이언트가 로그인 정보를 보냈습니다.");


                                //protocol.body.setID(bytes.toString().trim()); //ex) 홍길동

                                String id = protocol.body.getID();

                                boolean exist = false;
                                for (Client client : server.clients) {
                                    if (client.getID().equals(id)) {
                                        exist = true;

                                        // 입장 재요청
                                        protocol.header.setProtocolType(ProtocolHeader.PROTOCOL_OPT.ENTER_ROOM.getValue());
                                        protocol.body.setMsg("이미 존재하는 아이디입니다"); //에러 메시지를 메시지 란에 넣기?
                                        break;
                                    }
                                }
                                if (!exist) { // ID생성 및 입장 성공
                                    protocol.header.setProtocolType(ProtocolHeader.PROTOCOL_OPT.ENTER_ROOM.getValue());

                                    server.clients.add(new Client(id));

                                    System.out.println("[" + protocol.body.getID() + "]님이 입장하셨습니다.");

                                    // TODO : 채팅방 클라이언트에게만 입장 메시지 출력
                                    // 모든 클라이언트에게 입장 메세지 출력
                                    outputBuf.put(ByteBuffer.wrap(protocol.getPacket()));
                                    for (SocketChannel s : server.allClient) {
                                        outputBuf.flip();
                                        s.write(outputBuf);
                                    }
                                } else { // 입장 실패
                                    readSocket.write(ByteBuffer.wrap(protocol.getPacket()));
                                }

                                System.out.println("로그인 처리 결과 전송");*/
                                break;

                            // TODO : 아이디와 채팅 메시지를 결합한 버퍼 생성 및 작성
                            case SEND_MESSAGE:
                                break;

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
