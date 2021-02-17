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
    Vector<ClientInfo> clientInfos = new Vector<ClientInfo>(); //Ŭ���̾�Ʈ ������ ���� �����ұ�?
    HashMap<String, Vector> chatRoomMap = new HashMap<String, Vector>(); //ä�ù�

    private static Server server = new Server();

    public static Server getInstance() {
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

        try (ServerSocketChannel serverSocket = ServerSocketChannel.open()) { // implements AutoCloseable

            serverSocket.bind(new InetSocketAddress(15000));
            serverSocket.configureBlocking(false); // �⺻���� ���ŷ�̹Ƿ� �ٲ���

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

                        try {
                            readSocket.read(inputBuf);
                        } catch (Exception e) {
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

                                String id = protocol.getId();

                                boolean exist = false;
                                for (ClientInfo client : server.clientInfos) {
                                    if (client.getID().equals(id)) {
                                        exist = true;

                                        // ���� ���û
                                        protocol.setProtocolType(Protocol.PT_REQ_LOGIN);
                                        break;
                                    }
                                }
                                if (!exist) { // ID���� �� ���� ����
                                    protocol.setProtocolType(Protocol.PT_LOGIN_RESULT);

                                    System.out.println("["+protocol.getId() + "]���� �����ϼ̽��ϴ�.");

                                    // TODO : ä�ù� Ŭ���̾�Ʈ���Ը� ���� �޽��� ���
                                    // ��� Ŭ���̾�Ʈ���� ���� �޼��� ���
                                    outputBuf.put(ByteBuffer.wrap(protocol.getPacket()));
                                    for (SocketChannel s : server.allClient) {
                                        outputBuf.flip();
                                        s.write(outputBuf);
                                    }
                                }
                                else { // ���� ����
                                    readSocket.write(ByteBuffer.wrap(protocol.getPacket()));
                                }

                                System.out.println("�α��� ó�� ��� ����");
                                break;

                            // TODO : ���̵�� ä�� �޽����� ������ ���� ���� �� �ۼ�
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
