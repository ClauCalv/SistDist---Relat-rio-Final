public class Client {

    public static final int USER_NOT_FOUND = 0, LOGIN_VALID = 1, BAD_PASSWORD = 2;
    private static String address = "localhost"; //default

    public static int queryLogin(String login, String senha) {
        return 1; //TODO
    }

    public static void setup(String address) {
        if(address != null && !address.isEmpty())
            Client.address = address;
    }

    public static Client createUser(String login, String senha) {
        //TODO
        return null;
    }

    public static Client doLogin(String login, String senha) {
        //TODO
        return null;
    }

    public void quit() {
        //TODO
    }
}
