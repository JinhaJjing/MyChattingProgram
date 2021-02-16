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
    Set<SocketChannel> allClient = new HashSet<>(); //�� HashSet?
    Vector<ClientInfo> clientInfos = new Vector<ClientInfo>(); //Ŭ���̾�Ʈ ������ ���� �����ұ�?
    HashMap<String, Vector> chatRoomMap = new HashMap<String, Vector>(); //ä�ù�

    private static Server server = new Server();

    public static Server getInstance(){
        return server;
    }

    /* ���� ��û���� Ŭ���̾�Ʈ�� ó��
     */
    void accept(SelectionKey selectionKey) throws IOException {
        ServerSocketChannel server = (ServerSocketChannel) selectionKey.channel(); //�ش� ��û�� ���� ���� ä�� ����
        SocketChannel clientSocket = server.accept();

        clientSocket.configureBlocking(false); // Selector�� ������ �ޱ� ���ؼ� ����ŷ ä�η� �ٲ���

        allClient.add(clientSocket); // ����� Ŭ���̾�Ʈ�� �÷��ǿ� �߰�

        Protocol protocol = new Protocol(Protocol.PT_REQ_LOGIN);

        clientSocket.write(ByteBuffer.wrap(protocol.getPacket())); // ���̵� �Է¹ޱ� ���� ����� �ش� ä�ο� ����

        clientSocket.register(selector, SelectionKey.OP_READ, new ClientInfo()); // ���̵� �Է¹��� �����̹Ƿ� �б���� �����Ϳ� �������
    }

    public static void main(String[] args) {
        Server server = Server.getInstance();

        try (ServerSocketChannel serverSocket = ServerSocketChannel.open()) { // implements AutoCloseable, client channel���� ����?

            serverSocket.bind(new InetSocketAddress(15000));
            serverSocket.configureBlocking(false);

            server.selector = Selector.open();
            serverSocket.register(server.selector, SelectionKey.OP_ACCEPT);

            System.out.println("----------���� ���� �غ� �Ϸ�----------");

            ByteBuffer inputBuf = ByteBuffer.allocate(1024);
            ByteBuffer outputBuf = ByteBuffer.allocate(1024);

            // Ŭ���̾�Ʈ ���� ����
            while (true) {

                server.selector.select(); // �̺�Ʈ �߻��� ������ ������ ���ŷ

                Iterator<SelectionKey> iterator = server.selector.selectedKeys().iterator();

                while (iterator.hasNext()) {

                    SelectionKey key = iterator.next();
                    iterator.remove(); // ó���� Ű�� ����

                    if (key.isAcceptable()) { // ���� ��û �̺�Ʈ
                        server.accept(key);
                    } else if (key.isReadable()) { // Ŭ���̾�Ʈ -> ���� �̺�Ʈ
                        SocketChannel readSocket = (SocketChannel) key.channel(); // ���� ä�� ����
                        ClientInfo clientInfo = (ClientInfo) key.attachment();

                        try { readSocket.read(inputBuf); }
                        catch (Exception e) {
                            //TODO : ���� ���� ó��(���� ó��)
                        }

                        Protocol protocol = new Protocol();
                        byte[] bytes = protocol.getPacket();

                        inputBuf.rewind();
                        bytes = inputBuf.array();

                        int packetType = bytes[0];
                        protocol.setPacket(packetType, bytes);

                        switch (packetType) {
                            //Ŭ���̾�Ʈ�� �α��� ���� ���� ��Ŷ�� ��� (Ŭ���̾�Ʈ�� �α��� ���� ������ ���)
                            case Protocol.PT_RES_LOGIN:

                                System.out.println("Ŭ���̾�Ʈ�� �α��� ������ ���½��ϴ�.");

                                inputBuf.limit(inputBuf.position() - 1).position(0);
                                byte[] b = new byte[inputBuf.limit()];
                                inputBuf.get(b);

                                // TODO : �̹� �ִ� ID�� ��
                                if(exist) {
                                    // �ٽ� �Է� ��û
                                    protocol = new Protocol(Protocol.PT_REQ_LOGIN);
                                }
                                else{ // ID���� �� ���� ����
                                    clientInfo.setID(new String(b));

                                    protocol = new Protocol(Protocol.PT_LOGIN_RESULT);
                                    System.out.println(clientInfo.getID() + "���� �����ϼ̽��ϴ�.");

                                    // ��� Ŭ���̾�Ʈ���� ���� �޼��� ���
                                    outputBuf.put((clientInfo.getID()+ "���� �����ϼ̽��ϴ�.\n").getBytes());
                                    for (SocketChannel s : server.allClient) {
                                        outputBuf.flip();
                                        s.write(outputBuf);
                                    }
                                }

                                System.out.println("�α��� ó�� ��� ����");
                                readSocket.write(ByteBuffer.wrap(protocol.getPacket()));
                                break;

                            // TODO : �о�� �����Ϳ� ���̵� ������ ������ ����� ���� ����
                            case Protocol.CHAT:
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
