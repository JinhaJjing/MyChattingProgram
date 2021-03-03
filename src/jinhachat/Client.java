package jinhachat;

public class Client {

    public void clientStart() {
        //EventHandler는 사용자가 키보드 입력과 서버로부터 오는 이벤트를 감지하고 처리
        EventHandler eventHandler = new EventHandler();
        eventHandler.start();

        SystemIn systemIn = new SystemIn(eventHandler);
        systemIn.start();
    }

    public static void main(String[] args) {
        Client client = new Client();
        client.clientStart();
    }
}