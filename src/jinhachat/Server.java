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

    private ByteBuffer packetize(ProtocolBody nProtocolBody) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
        objectOutputStream.writeObject(nProtocolBody);
        objectOutputStream.flush();

        return ByteBuffer.wrap(byteArrayOutputStream.toByteArray());
    }

    private void sendExitNotice(SocketChannel readSocket, ByteBuffer outputBuf) throws IOException {
        String clientID = "", clientChatRoom = "";
        for (String ID : allClient.keySet()) {
            if (allClient.get(ID).getSocketChannel().equals(readSocket)) {
                clientID = allClient.get(ID).getID(); //�����ϴ� ���� ���̵�
                clientChatRoom = allClient.get(ID).getChatRoom(); //�����ϴ� ������ ���� ä�ù�
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
        //ä�ù� Ŭ���̾�Ʈ�鿡�� ������ �˸�
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

    private void sendLoginRes(SocketChannel readSocket, ProtocolBody protocolBody, ByteBuffer outputBuf) throws IOException {
        System.out.println("Client send login message.");
        boolean IDexist = false;
        if (allClient.containsKey(protocolBody.getID())) IDexist = true;

        ProtocolHeader nheader = new ProtocolHeader();

        if (!IDexist) { // ID���� �� ���� ����
            ClientInfo nClientInfo = new ClientInfo();
            nClientInfo.setSocketChannel(readSocket);
            nClientInfo.setID(protocolBody.getID());
            nClientInfo.setChatRoom(protocolBody.getChatRoom());
            nClientInfo.setLoggedIn(true);

            allClient.put(protocolBody.getID(), nClientInfo); //����� Ŭ���̾�Ʈ�� �߰�

            //ä�ù濡 Ŭ���̾�Ʈ �߰�(���� �� ����)
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

            //ä�ù� Ŭ���̾�Ʈ�鿡�� �α��� �˸�
            for (ClientInfo clientinfo : list) {
                if(clientinfo.getID().equals(protocolBody.getID())) continue;
                outputBuf.flip();
                clientinfo.getSocketChannel().write(outputBuf);
            }
            outputBuf.clear();

            //�α��� ��û Ŭ���̾�Ʈ���� ���� �˸�
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
    }

    private void sendChatRes(ProtocolBody protocolBody, ByteBuffer outputBuf) throws IOException {
        System.out.println("Client send messsage.");

        ProtocolHeader nheader = new ProtocolHeader();

        ByteBuffer body = packetize(protocolBody);
        nheader.setProtocolType(ProtocolHeader.PROTOCOL_OPT.NOTICE_CHAT);
        nheader.setBodyLength(body.limit());

        outputBuf.put(nheader.packetize());
        outputBuf.put(body);

        for (String ID : allClient.keySet()) {
            if (allClient.get(ID).getID().equals(protocolBody.getID())) {
                String curChatRoom = allClient.get(ID).getChatRoom();

                //ä�ù� Ŭ���̾�Ʈ�鿡�� ä�� �˸�
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
    }

    private void sendWhisperRes(ProtocolBody protocolBody, ByteBuffer outputBuf) throws IOException{
        System.out.println("Client send whisper.");

        ProtocolHeader nheader = new ProtocolHeader();

        ByteBuffer body = packetize(protocolBody);
        nheader.setProtocolType(ProtocolHeader.PROTOCOL_OPT.NOTICE_WHISPER);
        nheader.setBodyLength(body.limit());

        outputBuf.put(nheader.packetize());
        outputBuf.put(body);

        for (String ID : allClient.keySet()) {
            if (allClient.get(ID).getID().equals(protocolBody.getID())) {
                String curChatRoom = allClient.get(ID).getChatRoom();

                //ä�ù� Ŭ���̾�Ʈ�鿡�� ä�� �˸�
                for (String chatRoom : allChatRoom.keySet()) {
                    if (chatRoom.equals(curChatRoom)) {
                        for (ClientInfo clientInfo : allChatRoom.get(chatRoom)) {
                            if(clientInfo.getID().equals(protocolBody.getTargetID())){
                                outputBuf.flip();
                                clientInfo.getSocketChannel().write(outputBuf);
                            }
                        }
                    }
                }
                break;
            }
        }
    }

    public void serverStart() {
        try (ServerSocketChannel serverSocket = ServerSocketChannel.open()) { // implements AutoCloseable

            serverSocket.bind(new InetSocketAddress(13000));
            serverSocket.configureBlocking(false); // �⺻���� ���ŷ�̹Ƿ� ����ŷ���� �ٲ���

            Selector selector = Selector.open();
            serverSocket.register(selector, SelectionKey.OP_ACCEPT); // selector�� ���� ��� channel ���

            System.out.println("----------Server booting completed----------");

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
                            sendExitNotice(readSocket, outputBuf);
                            continue;
                        }

                        ProtocolHeader header = new ProtocolHeader();
                        header.parse(inputBuf); //header�� ���� �ؼ�

                        byte[] temp = new byte[header.getBodyLength()];
                        inputBuf.get(temp);
                        ProtocolBody protocolBody = (ProtocolBody) new ObjectInputStream(new ByteArrayInputStream(temp)).readObject();

                        switch (header.getProtocolType()) {
                            case REQ_LOGIN:
                                sendLoginRes(readSocket, protocolBody, outputBuf);
                                break;
                            case REQ_CHAT:
                                sendChatRes(protocolBody, outputBuf);
                                break;
                            // TODO : �ӼӸ� ��û ó��
                            case REQ_WHISPER:
                                sendWhisperRes(protocolBody, outputBuf);
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