import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.data.Stat;
import org.apache.zookeeper.ZooKeeper;

public class Client {
    static ZooKeeper zk = null;

    public static final int USER_NOT_FOUND = 0, LOGIN_VALID = 1, BAD_PASSWORD = 2;
    private static String address = "localhost"; // default
    private static String loginUser = "";

    public static int queryLogin(String login, String senha) {
        return 1; // TODO
    }

    public static void setup(String address) {
        if (address != null && !address.isEmpty())
            Client.address = address;
    }

    public static Client createUser(String login, String senha) {
        loginUser = login;
        String root = login;
        // Create ZK node name
        if (zk != null) {
            try {
                Stat s = zk.exists(root, false);
                if (s == null) {
                    zk.create(root, new byte[0], Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                }
            } catch (KeeperException e) {
                System.out.println("Keeper exception when instantiating queue: " + e.toString());
            } catch (InterruptedException e) {
                System.out.println("Interrupted exception");
            }
        }

        return null;
    }

    public static Client doLogin(String login, String senha) {
        // TODO
        return null;
    }

    public void quit() {

        try {
            zk.delete(address + "/" + loginUser, 0);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (KeeperException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        // TODO
    }
}
