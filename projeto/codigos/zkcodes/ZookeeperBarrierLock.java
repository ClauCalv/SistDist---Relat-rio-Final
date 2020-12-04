package zkcodes;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.data.Stat;

import java.util.List;

// Adaptado para esperar o líder disparar a barreira em vez de esperar SIZE pessoas chegarem.
// Não sei se ainda deveria se chamar barrier. É um híbrido de barreira e trava.
// Um processo aciona a trava sem participar da barreira em si.
public class ZookeeperBarrierLock extends ZookeeperSync {
    String lock;

    public ZookeeperBarrierLock(String address, String root, String lock) {
        super(address);
        this.root = root;
        this.lock = lock;

        // Create barrier node
        if (zk != null) {
            try {
                Stat s = zk.exists(root, false);
                if (s == null) {
                    zk.create(root, new byte[0], Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                }
            } catch (KeeperException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void lock(){
        try {
            zk.create(root+"/"+lock, new byte[0], Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void unlock(){
        try {
            zk.delete(root + "/" + lock, 0);
        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private boolean isLocked() {
        try {
            Stat s = zk.exists(root + "/" + lock, this);
            return s == null;
        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Joins barrier. Proceeds when detect its now allowed.
    public boolean enter(){
        while (true) {
            synchronized (mutex) {
                try {
                    if (isLocked()) {
                        mutex.wait();
                    } else {
                        return true;
                    }
                } catch (InterruptedException e) {
                    return false;
                }
            }
        }
    }

    // Leaves barrier. Proceeds when detect its now allowed.
    public boolean leave() {
        while (true) {
            synchronized (mutex) {
                try {
                    if (!isLocked()) {
                        mutex.wait();
                    } else {
                        return true;
                    }
                } catch (InterruptedException e) {
                    return false;
                }
            }
        }
    }
}
