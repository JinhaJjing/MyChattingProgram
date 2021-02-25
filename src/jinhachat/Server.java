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

    /* ���� ��û���� Ŭ���̾�Ʈ�� ó��
     */
    void accept(SelectionKey selectionKey) throws IOException {
        ServerSocketChannel server = (ServerSocketChannel) selectionKey.channel(); //�ش� ��û�� ���� ���� ä�� ����
        SocketChannel clientSocket = server.accept();

        clientSocket.configureBlocking(false); // Selector�� ������ �ޱ� ���ؼ� ����ŷ ä�η� �ٲ���
        clientSocket.register(selector, SelectionKey.OP_READ); // ���̵� �Է¹��� �����̹Ƿ� �б���� �����Ϳ� �������
    }

    public void run() {
        try (ServerSocketChannel serverSocket = ServerSocketChannel.open()) { // implements AutoCloseable

            serverSocket.bind(new InetSocketAddress(15000));
            serverSocket.configureBlocking(false); // �⺻���� ���ŷ�̹Ƿ� �ٲ���

            selector = Selector.open();
            serverSocket.register(selector, SelectionKey.OP_ACCEPT); // selector�� ���� ��� channel ���

            System.out.println("----------���� ���� �غ� �Ϸ�----------");

            ByteBuffer inputBuf = ByteBuffer.allocate(1024);
            ByteBuffer outputBuf = ByteBuffer.allocate(1024);

            // Ŭ���̾�Ʈ ���� ����
            while (true) {

                selector.select(); // �̺�Ʈ �߻��� ������ ������ ���ŷ

                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator(); // �߻��� �̺�Ʈ�� ���� ä���� ���

                // �߻��� �̺�Ʈ���� ���� Iterator�� �̺�Ʈ�� �ϳ��� ������� ó��
                while (iterator.hasNext()) {

                    SelectionKey key = iterator.next();
                    iterator.remove(); // ó���� Ű�� ����

                    if (key.isAcceptable()) { // ���� ��û �̺�Ʈ
                        accept(key);

                    } else if (key.isReadable()) { // Ŭ���̾�Ʈ -> ���� �̺�Ʈ
                        SocketChannel readSocket = (SocketChannel) key.channel(); // ���� ä�� ����

                        try {
                            readSocket.read(inputBuf);
                            inputBuf.flip();
                        } catch (Exception e) {
                            //TODO : ���� ���� ó��(���� ó��)
                            allClient.remove(readSocket);
                        }

                        ProtocolHeader header = new ProtocolHeader();
                        header.parse(inputBuf); //header�� ���� �ؼ�

                        switch (header.getProtocolType()) {
                            // TODO : Ŭ���̾�Ʈ�� �α��� ���� ������ ���
                            case REQ_LOGIN:
                                System.out.println("Ŭ���̾�Ʈ�� �α��� ������ ���½��ϴ�.");

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

                                if (!exist) { // ID���� �� ���� ����
                                    allClient.put(readSocket, id); // ����� Ŭ���̾�Ʈ�� �÷��ǿ� �߰�

                                    nheader.setProtocolType(ProtocolHeader.PROTOCOL_OPT.RES_LOGIN_SUCCESS)
                                            .setIDLength(id.length())
                                            .build();
                                    nbody.setID(id);

                                    // TODO : ��� Ŭ���̾�Ʈ���� ���� �޼��� ���
/*                                    ProtocolHeader nnheader = new ProtocolHeader()
                                            .setProtocolType(ProtocolHeader.PROTOCOL_OPT.BROADCAST)
                                            .setIDLength(id.length())
                                            .setMSGLength();
                                    ProtocolBody nnbody = new ProtocolBody();
                                    outputBuf.put(ByteBuffer.wrap((id + "���� �����Ͽ����ϴ�.").getBytes()));
                                    for (SocketChannel client : server.allClient.keySet()) {
                                        outputBuf.flip();
                                        client.write(outputBuf);
                                    }*/

                                } else { // TODO : ���� ���û
                                    nheader.setProtocolType(ProtocolHeader.PROTOCOL_OPT.RES_LOGIN_FAIL)
                                            .setMSGLength(nbody.getMsg().length())
                                            .build();
                                }

                                int bodyLength = nheader.getIDLength() + nheader.getMSGLength();
                                outputBuf.put(nheader.packetize());
                                outputBuf.put(nbody.packetize(ByteBuffer.allocate(bodyLength)));

                                outputBuf.flip();
                                readSocket.write(outputBuf);

                                System.out.println("�α��� ó�� ��� ����");
                                break;

                            // TODO : ä�� �޽����� ���. ���̵�� ä�� �޽����� ������ ���� ���� �� �ۼ�
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