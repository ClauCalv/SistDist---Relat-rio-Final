import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.server.ByteBufferOutputStream;
import zkcodes.ZookeeperBarrierLock;
import zkcodes.ZookeeperLeader;
import zkcodes.ZookeeperQueue;
import zkcodes.ZookeeperSimple;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class Client {

    public static final int USER_NOT_FOUND = 0, LOGIN_VALID = 1, BAD_PASSWORD = 2, MESSAGE_SENT = 3, WEIRD_ERROR = -1;

    private static String address = "localhost"; // default
    private static final String usersRoot = "/users", onlineUsersRoot = "/online", chatsRoot = "/chats",
            reunioesRoot = "/reunioes";

    private static final int MAX_MESSAGE_SIZE = Integer.MAX_VALUE; // por enquanto o máximo é infinito
    private static final String MESSAGE_PREFIX = "msg";

    public final String username;

    private Client(String username) {
        this.username = username;
    }

    public static void setup(String address) {
        if (address != null && !address.isEmpty())
            Client.address = address;
    }

    public static int queryLogin(String login, String senha) {
        ZookeeperSimple users = new ZookeeperSimple(address, usersRoot);
        if (users.exists(login)) {
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

        ZookeeperSimple chats = new ZookeeperSimple(address, chatsRoot);
        chats.createEmpty(login, false);
        return doLogin(login);
    }

    public void quit() {
        ZookeeperSimple online = new ZookeeperSimple(address, onlineUsersRoot);
        online.delete(username);
    }

    // por enquanto canEnter e canCreate são só opostas, basta existir/não existir.
    // depois vamos colocar uma regra de se ela já está em andamento não dá mais pra entrar
    public boolean canEnterReuniao(String r) {
        ZookeeperSimple reuniao = new ZookeeperSimple(address, reunioesRoot);
        return reuniao.exists(r);
    }

    public Reuniao enterReuniao(String r) {
        return new Reuniao(r);
    }

    public String[] getAllReunioes() {
        ZookeeperSimple reuniao = new ZookeeperSimple(address, reunioesRoot);
        return reuniao.listAllChildren();
    }

    public boolean canCreateReuniao(String r) {
        ZookeeperSimple reuniao = new ZookeeperSimple(address, reunioesRoot);
        return !reuniao.exists(r);
    }

    public Reuniao createReuniao(String r) {
        ZookeeperSimple reuniao = new ZookeeperSimple(address, reunioesRoot);
        reuniao.createEmpty(r, false);
        return enterReuniao(r);
    }

    public int sendMessage(String target, String msg) {

        ZookeeperSimple chats = new ZookeeperSimple(address, chatsRoot);
        if(!chats.exists(target))
            return USER_NOT_FOUND;

        ZookeeperQueue messageQueue = new ZookeeperQueue(address, chatsRoot + "/" + target);
        byte [] targetbs = target.getBytes();
        byte[] msgbs = msg.getBytes();
        ByteArrayOutputStream baos = new ByteArrayOutputStream(Integer.BYTES + targetbs.length + msgbs.length);
        baos.write(ByteBuffer.allocate(Integer.BYTES).putInt(targetbs.length).array(), 0, Integer.BYTES);
        //Não acredito que seja tão foda levar um int a um byte[4].
        baos.write(targetbs, 0, targetbs.length);
        baos.write(msgbs, 0, Math.max(msgbs.length, MAX_MESSAGE_SIZE));
        byte[] message = baos.toByteArray();

        return messageQueue.produce(MESSAGE_PREFIX, message) ? MESSAGE_SENT : WEIRD_ERROR;
    }

    public String[] retrieveMessage() {
        ZookeeperQueue messageQueue = new ZookeeperQueue(address, chatsRoot + "/" + username);
        byte[] message = messageQueue.consume(MESSAGE_PREFIX);
        if(message == null) //thread interrompida
            return null;

        ByteBuffer buf = ByteBuffer.wrap(message);
        int sourceSize = buf.getInt();
        byte[] sourceby = new byte[sourceSize], msgby = new byte[message.length - sourceSize - Integer.BYTES];
        buf.get(sourceby, Integer.BYTES, sourceSize);
        buf.get(msgby, Integer.BYTES + sourceSize, msgby.length);

        return new String[]{new String(sourceby), new String(msgby)};
    }

    public class Reuniao{

        private static final String ELECTION_NODE = "election", LEADER_NODE = "leader", LOCK_NODE = "lock";

        private final String reuniaoRoot;
        private final ZookeeperLeader leader;
        private final ZookeeperBarrierLock barrier;
        private boolean hasElected = false;
        private boolean hasLeft = false;

        private Reuniao(String reuniaoID){
            reuniaoRoot = reunioesRoot + "/" + reuniaoID;

            leader = new ZookeeperLeader(address, reuniaoRoot, ELECTION_NODE, LEADER_NODE, username);
            barrier = new ZookeeperBarrierLock(address, reuniaoRoot, LOCK_NODE);
        }

        public void setLeaderCallback(ZookeeperLeader.LeaderCallback cb){
            leader.setLeaderCallback(cb);
        }

        public boolean checkElection(){
            if (!hasElected){
                hasElected = true;
                return leader.elect();
            } else return leader.isLeader();
        }

        public String getCurrentLeader() {
            return leader.getCurrentLeader();
        }

        public void startPresentation(){
            barrier.lock();
        }

        public void stopPresentation(){
            barrier.unlock();
        }

        public boolean waitPresentation(){
            return barrier.enter();
        }

        public boolean watchPresentation(){
            return barrier.leave();
        }

        public void leave() {
            leader.exit();
            hasLeft = true;
        }

        public boolean hasLeft(){
            return hasLeft;
        }
    }
}


