import zkcodes.ZookeeperSimple;

public class Chat {

    private int idChat = 0;
    private ZookeeperSimple chat;

    public Chat(ZookeeperSimple a) {
        // TODO
        this.chat = a;
        this.idChat++;// id do chat
    }

    public int sendMessage(String mensagem, byte[] user) {

        // TODO
        this.chat.createWithData(mensagem, user);

        return 1;
    }

    // TODO

}
