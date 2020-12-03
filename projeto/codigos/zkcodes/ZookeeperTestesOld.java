package zkcodes;

import org.apache.zookeeper.KeeperException;

import java.nio.ByteBuffer;
import java.util.Random;

public class ZookeeperTestesOld {

    public static void main(String[] args) {
        switch (args[0]) {
            case "qTest":
                queueTest(args);
                break;
            case "barrier":
                barrierTest(args);
                break;
            case "lock":
                lockTest(args);
                break;
            case "leader":
                leaderElection(args);
                break;
            default:
                System.err.println("Unknown option");
                break;
        }
    }

    public static void queueTest(String[] args) {
        ZookeeperQueue q = new ZookeeperQueue(args[1], "/app3");

        System.out.println("Input: " + args[1]);
        int i;
        int max = Integer.parseInt(args[2]);

        if (args[3].equals("p")) {
            System.out.println("Producer");
            for (i = 0; i < max; i++)
                q.produce("element", ByteBuffer.allocate(4).putInt(10 + i).array() );

        } else {
            System.out.println("Consumer");
            for (i = 0; i < max; i++) {
                int r = ByteBuffer.wrap(q.consume("element")).getInt();
                System.out.println("Item: " + r);
            }
        }
    }

    public static void barrierTest(String[] args) {
        ZookeeperBarrier b = new ZookeeperBarrier(args[1], "/b1", Integer.parseInt(args[2]));
        try {
            boolean flag = b.enter();
            System.out.println("Entered barrier: " + args[2]);
            if (!flag) System.out.println("Error when entering the barrier");
        } catch (KeeperException | InterruptedException ignored) {

        }

        // Generate random integer
        Random rand = new Random();
        int r = rand.nextInt(100);
        // Loop for rand iterations
        for (int i = 0; i < r; i++) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {

            }
        }
        try {
            b.leave();
        } catch (KeeperException | InterruptedException ignored) {

        }
        System.out.println("Left barrier");
    }

    public static void lockTest(String[] args) {
        ZookeeperLock lock = new ZookeeperLock(args[1], "/lock", Long.parseLong(args[2]));
        try {
            boolean success = lock.lock();
            if (success) {
                lock.compute();
            } else {
                while (true) {
                    //Waiting for a notification
                }
            }
        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void leaderElection(String[] args) {
        // Generate random integer
        Random rand = new Random();
        int r = rand.nextInt(1000000);
        ZookeeperLeader leader = new ZookeeperLeader(args[0], "/", "/election", "/leader", ""+r);
        boolean success = leader.elect();
        if (success) {
            System.out.println("I will die after 10 seconds!");
            try {
                Thread.sleep(10000);
                System.out.println("Process " + r + " died!");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.exit(0);
        } else {
            leader.setLeaderCallback(new ZookeeperLeader.LeaderCallback() {
                @Override
                public void onNewLeaderFound(String currentLeader) {
                    System.out.println("New leader is "+currentLeader);
                }

                @Override
                public void onNoNewLeaderFound() {
                    System.out.println("Something weird happened");
                }

                @Override
                public void onBecomeLeader() {
                    System.out.println("I am the new leader! Computing...");
                    System.out.println("I will die after 10 seconds!");
                    try {
                        Thread.sleep(10000);
                        System.out.println("Process " + r + " died!");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    System.exit(0);
                }
            });
        }

    }
}
