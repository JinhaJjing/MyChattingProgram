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
    private Map<String, ClientInfo> allClient = new HashMap<>(); //<����ID, ��������>
    private Map<String, List<ClientInfo>> allChatRoom = new HashMap<>(); //<ä�ù�, ����ID����Ʈ>

    /* ���� ��û���� Ŭ���̾�Ʈ�� ó��
     */
    void accept(Selector selector, SelectionKey selectionKey) throws IOException {
        ServerSocketChannel server = (ServerSocketChannel) selectionKey.channel(); //�ش� ��û�� ���� ���� ä�� ����
        SocketChannel clientSocket = server.accept();

        clientSocket.configureBlocking(false); // Selector�� ������ �ޱ� ���ؼ� ����ŷ ä�η� �ٲ���
        clientSocket.register(selector, SelectionKey.OP_READ); // ���̵� �Է¹��� �����̹Ƿ� �б���� �����Ϳ� �������
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
                clientID = allClient.get(ID).getID(); //�����ϴ� ���� ���̵�
                clientChatRoom = allClient.get(ID).getChatRoom(); //�����ϴ� ������ ���� ä�ù�
                break;
            }
        }

        ClientInfo nClientInfo = new ClientInfo();
        nClientInfo.setMSG(clientID);
        ByteBuffer body = packetize(nClientInfo);

        ProtocolHeader header = new ProtocolHeader();
        header.setProtocolType(ProtocolHeader.PROTOCOL_OPT.NOTICE_EXIT);
        header.setBodyLength(body.limit()); //�³�?

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
            serverSocket.configureBlocking(false); // �⺻���� ���ŷ�̹Ƿ� ����ŷ���� �ٲ���

            Selector selector = Selector.open();
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
                        accept(selector, key);

                    } else if (key.isReadable()) { // Ŭ���̾�Ʈ -> ���� �̺�Ʈ
                        SocketChannel readSocket = (SocketChannel) key.channel(); // ���� ä�� ����

                        try {
                            readSocket.read(inputBuf);
                            inputBuf.flip();
                        } catch (Exception e) {
                            //ä�ù� Ŭ���̾�Ʈ�鿡�� ���� �˸�
                            noticeExit(readSocket, outputBuf);
                            continue; //���� while�� ���� �ٶ��
                        }

                        ProtocolHeader header = new ProtocolHeader();
                        header.parse(inputBuf); //header�� ���� �ؼ�

                        switch (header.getProtocolType()) {
                            case REQ_LOGIN:
                                System.out.println("Ŭ���̾�Ʈ�� �α��� ������ ���½��ϴ�.");

                                byte[] temp = new byte[header.getBodyLength()];
                                inputBuf.get(temp);
                                ClientInfo newClientInfo = (ClientInfo) new ObjectInputStream(new ByteArrayInputStream(temp)).readObject();

                                boolean IDexist = false;
                                if (allClient.containsKey(newClientInfo.getReqID())) IDexist = true;
                                // TODO : ä�ù� Ȯ�� ����

                                ProtocolHeader nheader = new ProtocolHeader();
                                ClientInfo nClientInfo = new ClientInfo();

                                if (!IDexist) { // ID���� �� ���� ����
                                    nClientInfo.setSocketChannel(readSocket);
                                    nClientInfo.setID(newClientInfo.getReqID());
                                    nClientInfo.setChatRoom(newClientInfo.getReqChatRoom());
                                    nClientInfo.setLoggedIn(true);

                                    allClient.put(newClientInfo.getMSG(), nClientInfo); // ����� Ŭ���̾�Ʈ�� �÷��ǿ� �߰�
                                    //TODO : ä�ù� ����
                                    //allChatRoom.put(nClientInfo.getReqChatRoom(),List<...>);

                                    ByteBuffer body = packetize(nClientInfo);

                                    nheader.setProtocolType(ProtocolHeader.PROTOCOL_OPT.RES_LOGIN_SUCCESS);
                                    nheader.setBodyLength(body.limit()); //�³�?

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
                                    nheader.setBodyLength(body.limit()); //�³�?

                                    System.out.println(nClientInfo.getID() + "���� �α����Ͽ� " + nClientInfo.getChatRoom() + "�� �����Ͽ����ϴ�");

                                } else {
                                    nheader.setProtocolType(ProtocolHeader.PROTOCOL_OPT.RES_LOGIN_FAIL);
                                    System.out.println("���� ���û ����");
                                }

                                outputBuf.put(nheader.packetize());
                                outputBuf.put(packetize(nClientInfo));

                                outputBuf.flip();
                                readSocket.write(outputBuf);

                                break;

                            // TODO : ä�� �޽����� ���. ���̵�� ä�� �޽����� ������ ���� ���� �� �ۼ�
                            case REQ_CHAT:

                                break;

                            // TODO : �ӼӸ� ��û ó��
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