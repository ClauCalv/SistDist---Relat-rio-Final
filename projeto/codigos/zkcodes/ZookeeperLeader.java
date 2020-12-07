package zkcodes;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.data.Stat;

import java.util.List;

public class ZookeeperLeader extends ZookeeperSync {

    private static final String ELECTION_PREFIX = "n-";
    String electionNode;
    String leaderNode;
    String id; //Id of the leader
    String myElection;

    private String currentLeader = null;
    private LeaderCallback leaderCallback;
    private boolean isLeader = false;

    // Reconstruído para aceitar callbacks
    public ZookeeperLeader(String address, String root, String leaderNode, String electionNode, String user) {
        super(address);
        this.root = root;
        this.leaderNode = root + "/" + leaderNode;
        this.electionNode = root + "/" + electionNode;
        this.id = user;

        // Create ZK node name
        if (zk != null) {
            try {

                //Create election znode
                Stat s1 = zk.exists(this.electionNode, false);
                if (s1 == null)
                    zk.create(this.electionNode, new byte[0], Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);

                //Checking for a leader
                Stat s2 = zk.exists(this.leaderNode, this); // coloquei uma watch aqui pra saber quando trocou de líder
                if (s2 != null) {
                    byte[] idLeader = zk.getData(this.leaderNode, false, s2);
                    currentLeader = new String(idLeader);
                }

            } catch (InterruptedException | KeeperException e) {
                e.printStackTrace();
            }
        }
    }

    public String getCurrentLeader() {
        return currentLeader;
    }

    public void setLeaderCallback(LeaderCallback leaderCallback) {
        this.leaderCallback = leaderCallback;
    }

    public boolean elect() {
        try {
            String fullElectionPath = zk.create(electionNode + "/" + ELECTION_PREFIX, new byte[0], Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
            this.myElection = fullElectionPath.substring(fullElectionPath.lastIndexOf('/') + 1); // Só o prefixo+sufixo
            return check();
        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void exit() {
        try {
            zk.delete(electionNode + "/" + myElection, 0);
            if(isLeader)
                zk.delete(leaderNode, 0);
        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public boolean isLeader(){
        return isLeader;
    }

    private boolean check()  {

        Integer suffix = Integer.valueOf(myElection.substring(ELECTION_PREFIX.length()));
        while (true) {
            try{

                List<String> list = zk.getChildren(electionNode, false);

                int min = Integer.parseInt(list.get(0).substring(ELECTION_PREFIX.length()));
                String minString = list.get(0);
                for (String s : list) {
                    int tempValue = Integer.parseInt(s.substring(ELECTION_PREFIX.length()));
                    if (tempValue < min) {
                        min = tempValue;
                        minString = s;
                    }
                }

                if (suffix.equals(min)) {
                    this.leader();
                    return true;
                }

                int max = min;
                String maxString = minString;
                for (String s : list) {
                    int tempValue = Integer.parseInt(s.substring(ELECTION_PREFIX.length()));
                    if (tempValue > max && tempValue < suffix) {
                        max = tempValue;
                        maxString = s;
                    }
                }

                Stat s = zk.exists(electionNode + "/" + maxString, this); //Exists with watch
                if (s != null)
                    break;

            } catch (KeeperException | InterruptedException e) {
                e.printStackTrace();
            }
        }

        return false;
    }

    synchronized public void process(WatchedEvent event) {
        synchronized (mutex) {
            try{

                if (event.getPath().equals(leaderNode)){ // Se alguém alterou a leaderNode
                    Stat s2 = zk.exists(leaderNode, this); // coloquei uma watch aqui pra saber quando trocou de líder
                    if (s2 != null) {
                        byte[] idLeader = zk.getData(leaderNode, false, s2);
                        currentLeader = new String(idLeader);

                        if (leaderCallback != null)
                            leaderCallback.onNewLeaderFound(currentLeader);
                    } else if (leaderCallback != null)
                        leaderCallback.onNoNewLeaderFound();

                } else if (event.getType() == Event.EventType.NodeDeleted) { // Se deletaram algo e não foi a leaderNode
                    if (check())
                        leaderCallback.onBecomeLeader();

                }
            } catch (KeeperException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void leader()  {
        try { //Create leader znode

            isLeader = true;

            Stat s2 = zk.exists(leaderNode, false);
            if (s2 == null)
                zk.create(leaderNode, id.getBytes(), Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
            else zk.setData(leaderNode, id.getBytes(), 0);

        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public interface LeaderCallback {

        void onNewLeaderFound(String currentLeader);

        void onNoNewLeaderFound();

        void onBecomeLeader();
    }
}
