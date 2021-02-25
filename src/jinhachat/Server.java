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
    private Selector selector;
    private Map<SocketChannel, String> allClient = new HashMap<>();

    /* 연결 요청중인 클라이언트를 처리
     */
    void accept(SelectionKey selectionKey) throws IOException {
        ServerSocketChannel server = (ServerSocketChannel) selectionKey.channel(); //해당 요청에 대한 소켓 채널 생성
        SocketChannel clientSocket = server.accept();

        clientSocket.configureBlocking(false); // Selector의 관리를 받기 위해서 논블로킹 채널로 바꿔줌
        clientSocket.register(selector, SelectionKey.OP_READ); // 아이디를 입력받을 차례이므로 읽기모드로 셀렉터에 등록해줌
    }

    public void run() {
        try (ServerSocketChannel serverSocket = ServerSocketChannel.open()) { // implements AutoCloseable

            serverSocket.bind(new InetSocketAddress(15000));
            serverSocket.configureBlocking(false); // 기본값은 블로킹이므로 바꿔줌

            selector = Selector.open();
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
                        accept(key);

                    } else if (key.isReadable()) { // 클라이언트 -> 서버 이벤트
                        SocketChannel readSocket = (SocketChannel) key.channel(); // 현재 채널 정보

                        try {
                            readSocket.read(inputBuf);
                            inputBuf.flip();
                        } catch (Exception e) {
                            //TODO : 연결 끊김 처리(퇴장 처리)
                            allClient.remove(readSocket);
                        }

                        ProtocolHeader header = new ProtocolHeader();
                        header.parse(inputBuf); //header를 먼저 해석

                        switch (header.getProtocolType()) {
                            // TODO : 클라이언트의 로그인 정보 전송일 경우
                            case REQ_LOGIN:
                                System.out.println("클라이언트가 로그인 정보를 보냈습니다.");

                                byte[] temp = new byte[header.getIDLength()];
                                inputBuf.get(temp);
                                String id = new String(temp);

                                boolean exist = false;
                                for (SocketChannel client : allClient.keySet()) {
                                    if (id.equals(allClient.get(client))) {
                                        exist = true;
                                        break;
                                    }
                                }

                                ProtocolBody nbody = new ProtocolBody();
                                ProtocolHeader nheader = new ProtocolHeader();

                                if (!exist) { // ID생성 및 입장 성공
                                    allClient.put(readSocket, id); // 연결된 클라이언트를 컬렉션에 추가

                                    nheader.setProtocolType(ProtocolHeader.PROTOCOL_OPT.RES_LOGIN_SUCCESS)
                                            .setIDLength(id.length())
                                            .build();
                                    nbody.setID(id);

                                    // TODO : 모든 클라이언트에게 입장 메세지 출력
/*                                    ProtocolHeader nnheader = new ProtocolHeader()
                                            .setProtocolType(ProtocolHeader.PROTOCOL_OPT.BROADCAST)
                                            .setIDLength(id.length())
                                            .setMSGLength();
                                    ProtocolBody nnbody = new ProtocolBody();
                                    outputBuf.put(ByteBuffer.wrap((id + "님이 입장하였습니다.").getBytes()));
                                    for (SocketChannel client : server.allClient.keySet()) {
                                        outputBuf.flip();
                                        client.write(outputBuf);
                                    }*/

                                } else { // TODO : 입장 재요청
                                    nheader.setProtocolType(ProtocolHeader.PROTOCOL_OPT.RES_LOGIN_FAIL)
                                            .setMSGLength(nbody.getMsg().length())
                                            .build();
                                }

                                int bodyLength = nheader.getIDLength() + nheader.getMSGLength();
                                outputBuf.put(nheader.packetize());
                                outputBuf.put(nbody.packetize(ByteBuffer.allocate(bodyLength)));

                                outputBuf.flip();
                                readSocket.write(outputBuf);

                                System.out.println("로그인 처리 결과 전송");
                                break;

                            // TODO : 채팅 메시지일 경우. 아이디와 채팅 메시지를 결합한 버퍼 생성 및 작성
                            case REQ_CHAT:

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

    public static void main(String[] args) {
        Server server = new Server();
        server.serverStart();
    }
}