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
    Vector<Client> clients = new Vector<Client>(); //Ŭ���̾�Ʈ ������ ���� �����ұ�?
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

        ProtocolHeader header = new ProtocolHeader();
        header.setProtocolType(ProtocolHeader.PROTOCOL_OPT.ENTER_ROOM.getValue());
        header.setBodyLength(0);
        ByteBuffer outbuf = ByteBuffer.allocate(1024);
        clientSocket.write(header.packetize(outbuf)); // ���̵� �Է¹ޱ� ���� ����� �ش� ä�ο� ����

        clientSocket.register(selector, SelectionKey.OP_READ); // ���̵� �Է¹��� �����̹Ƿ� �б���� �����Ϳ� �������
    }

    public static void main(String[] args) {
        Server server = Server.getInstance();

        try (ServerSocketChannel serverSocket = ServerSocketChannel.open()) { // implements AutoCloseable

            serverSocket.bind(new InetSocketAddress(15000));
            serverSocket.configureBlocking(false); // �⺻���� ���ŷ�̹Ƿ� �ٲ���

            server.selector = Selector.open();
            serverSocket.register(server.selector, SelectionKey.OP_ACCEPT); // selector�� ���� ��� channel ���

            System.out.println("----------���� ���� �غ� �Ϸ�----------");

            ByteBuffer inputBuf = ByteBuffer.allocate(1024);
            ByteBuffer outputBuf = ByteBuffer.allocate(1024);

            // Ŭ���̾�Ʈ ���� ����
            while (true) {

                server.selector.select(); // �̺�Ʈ �߻��� ������ ������ ���ŷ

                Iterator<SelectionKey> iterator = server.selector.selectedKeys().iterator(); // �߻��� �̺�Ʈ�� ���� ä���� ���

                // �߻��� �̺�Ʈ���� ���� Iterator�� �̺�Ʈ�� �ϳ��� ������� ó��
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

                        ProtocolHeader header = new ProtocolHeader();
                        header.parse(inputBuf); //header�� ���� �ؼ�

                        switch (header.getProtocolType()) {
                            // TODO : Ŭ���̾�Ʈ�� �α��� ���� ���� ��Ŷ�� ��� (Ŭ���̾�Ʈ�� �α��� ���� ������ ���)
                            case ENTER_ROOM:

/*                                System.out.println("Ŭ���̾�Ʈ�� �α��� ������ ���½��ϴ�.");


                                //protocol.body.setID(bytes.toString().trim()); //ex) ȫ�浿

                                String id = protocol.body.getID();

                                boolean exist = false;
                                for (Client client : server.clients) {
                                    if (client.getID().equals(id)) {
                                        exist = true;

                                        // ���� ���û
                                        protocol.header.setProtocolType(ProtocolHeader.PROTOCOL_OPT.ENTER_ROOM.getValue());
                                        protocol.body.setMsg("�̹� �����ϴ� ���̵��Դϴ�"); //���� �޽����� �޽��� ���� �ֱ�?
                                        break;
                                    }
                                }
                                if (!exist) { // ID���� �� ���� ����
                                    protocol.header.setProtocolType(ProtocolHeader.PROTOCOL_OPT.ENTER_ROOM.getValue());

                                    server.clients.add(new Client(id));

                                    System.out.println("[" + protocol.body.getID() + "]���� �����ϼ̽��ϴ�.");

                                    // TODO : ä�ù� Ŭ���̾�Ʈ���Ը� ���� �޽��� ���
                                    // ��� Ŭ���̾�Ʈ���� ���� �޼��� ���
                                    outputBuf.put(ByteBuffer.wrap(protocol.getPacket()));
                                    for (SocketChannel s : server.allClient) {
                                        outputBuf.flip();
                                        s.write(outputBuf);
                                    }
                                } else { // ���� ����
                                    readSocket.write(ByteBuffer.wrap(protocol.getPacket()));
                                }

                                System.out.println("�α��� ó�� ��� ����");*/
                                break;

                            // TODO : ���̵�� ä�� �޽����� ������ ���� ���� �� �ۼ�
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
