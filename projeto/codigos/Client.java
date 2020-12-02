import org.apache.zookeeper.KeeperException;
import zkcodes.ZookeeperSimple;

import java.util.Arrays;

public class Client {

    public static final int USER_NOT_FOUND = 0, LOGIN_VALID = 1, BAD_PASSWORD = 2;
    private static String address = "localhost"; // default
    private static final String usersRoot = "users", onlineUsersRoot = "online";

    private final String username;

    private Client(String username) {
        this.username = username;
    }

    public static void setup(String address) {
        if (address != null && !address.isEmpty())
            Client.address = address;
    }

    public static int queryLogin(String login, String senha) {
        ZookeeperSimple users = new ZookeeperSimple(address, usersRoot);
        if(users.exists(login)){
            byte[] senhaGuardada = users.getData(login);
            byte[] senhaNova = senha.getBytes(); // Se usássemos alguma criptografia faríamos aqui
            return Arrays.equals(senhaGuardada, senhaNova) ? LOGIN_VALID : BAD_PASSWORD;
        } else return USER_NOT_FOUND;
    }

    public static Client doLogin(String login) {
        //Não sei se precisa uma segunda validação aqui
        ZookeeperSimple online = new ZookeeperSimple(address, onlineUsersRoot);
        online.createEmpty(login, true);
        return new Client(login);
    }

    public static Client createUser(String login, String senha) {
        ZookeeperSimple users = new ZookeeperSimple(address, usersRoot);
        byte[] senhaNova = senha.getBytes(); // Se usássemos alguma criptografia faríamos aqui

        users.createWithData(login, senhaNova);
        return doLogin(login);
    }



    public void quit() {
        ZookeeperSimple online = new ZookeeperSimple(address, onlineUsersRoot);
        online.delete(username);
    }
}
