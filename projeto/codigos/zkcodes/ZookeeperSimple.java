package zkcodes;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.Stat;

public class ZookeeperSimple extends ZookeeperSync {

    public ZookeeperSimple(String address, String root) {
        super(address);
        this.root = root;

        if (zk != null) {
            try {
                Stat s = zk.exists(root, false);
                if (s == null) {
                    zk.create(root, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                }
            } catch (KeeperException e) {
                System.out.println("Keeper exception when instantiating queue: " + e.toString());
            } catch (InterruptedException e) {
                System.out.println("Interrupted exception");
            }
        }
    }

    public void createEmpty(String elem, boolean ephemeral){
        if (zk != null) {
            try {
                Stat s = zk.exists(root + "/" + elem, false);
                if (s == null) {
                    zk.create(root + "/" + elem, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE,
                            ephemeral ? CreateMode.EPHEMERAL : CreateMode.PERSISTENT);
                }
            } catch (KeeperException e) {
                System.out.println("Keeper exception when instantiating queue: " + e.toString());
            } catch (InterruptedException e) {
                System.out.println("Interrupted exception");
            }
        }
    }

    public void createWithData(String elem, byte[] data){
        if (zk != null) {
            try {
                Stat s = zk.exists(root + "/" + elem, false);
                if (s == null) {
                    zk.create(root + "/" + elem, data, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                }
            } catch (KeeperException e) {
                System.out.println("Keeper exception when instantiating queue: " + e.toString());
            } catch (InterruptedException e) {
                System.out.println("Interrupted exception");
            }
        }
    }

    public byte[] getData(String elem){
        if (zk != null) {
            try {
                Stat stat = new Stat();
                return zk.getData(root + "/" + elem, false, stat);
            } catch (KeeperException e) {
                System.out.println("Keeper exception when instantiating queue: " + e.toString());
            } catch (InterruptedException e) {
                System.out.println("Interrupted exception");
            }
        }
        return null;
    }

    public boolean exists(String elem) {
        if (zk != null) {
            try {
                Stat stat = zk.exists(root + "/" + elem, false);
                return stat == null;
            } catch (KeeperException e) {
                System.out.println("Keeper exception when instantiating queue: " + e.toString());
            } catch (InterruptedException e) {
                System.out.println("Interrupted exception");
            }
        }
        return false;
    }

    public boolean delete(String elem) {
        if (zk != null) {
            try {
                zk.delete(root + "/" + elem, -1);
            } catch (KeeperException e) {
                System.out.println("Keeper exception when instantiating queue: " + e.toString());
            } catch (InterruptedException e) {
                System.out.println("Interrupted exception");
            }
        }
        return false;
    }
}
