package zkcodes;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.data.Stat;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * Producer-Consumer queue
 */
public class ZookeeperQueue extends ZookeeperSync {

    /**
     * Constructor of producer-consumer queue
     *
     * @param address
     * @param name
     */
    public ZookeeperQueue(String address, String name) {
        super(address);
        this.root = name;
        // Create ZK node name
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

    //Modificado para aceitar qualquer dado e não disparar excessão
    public boolean produce(String elem, byte[] value){
        try {
            zk.create(root + "/" + elem, value, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT_SEQUENTIAL);
            return true;
        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
        }
        return false;
    }


    //Modificado para aceitar qualquer dado e não disparar excessão
    public byte[] consume(String elem) {
        byte[] retvalue;

        // Get the first element available
        while (true) {
            synchronized (mutex) {
                try {
                    List<String> list = zk.getChildren(root, true);

                    if (!list.isEmpty()) {
                        String minString = null;
                        int min = Integer.MAX_VALUE;
                        for (String node : list) {
                            if (node.startsWith(elem)) { // filtra agora só os elems corretos
                                int nodeValue = Integer.parseInt(node.substring(elem.length())); // e calcula seu tamanho
                                if (nodeValue < min) {
                                    min = nodeValue;
                                    minString = node;
                                }
                            }
                        }

                        if (minString != null) { //podemos ter achado algo que não era do tipo elem
                            byte[] b = zk.getData(root + "/" + minString, false, null);
                            zk.delete(root + "/" + minString, 0); // version não deveria ser -1?
                            return b;
                        }
                    }

                    System.out.println("Fila vazia, esperando...");
                    mutex.wait();
                } catch (KeeperException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
