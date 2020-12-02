import java.util.Scanner;

public class ClientCLI {

    public static final String zookeeperAddress = "localhost";
    private final Scanner input;
    private Client client = null;

    private final String COMMAND_AJUDA = "ajuda", COMMAND_LISTAR = "listar", COMMAND_CHAT = "chat",
            COMMAND_CHATS = "chats", COMMAND_REUNIAO = "reuniao", COMMAND_REUNIOES = "reunioes",
            COMMAND_ENTRAR = "entrar", COMMAND_SAIR = "sair";

    public ClientCLI(Scanner input) {
        this.input = input;
    }

    public static void main(String[] args) {
        Scanner input = new Scanner(System.in);

        if (args.length > 0)
            Client.setup(args[0]);
        new ClientCLI(input).inputLoop();
    }

    public void inputLoop() {
        while (true) {

            if (client == null)
                buildClient();

            String[] command = input.nextLine().split("[ ]+");
            switch (command[0]) {

                case COMMAND_AJUDA:
                    ajuda(command);
                    break;

                case COMMAND_LISTAR:
                    listar(command);
                    break;

                case COMMAND_ENTRAR:
                    entrar(command);
                    break;

                case COMMAND_SAIR:
                    sair(command);
                    break;

                default:
                    msg("Comando não entendido. Tente \"ajuda\"");
            }
        }
    }

    private void sair(String[] command) {
        if (yesNoQuestion("Deseja mesmo sair?"))
            client.quit();
        client = null;
    }

    private void entrar(String[] command) {
        if (command.length < 3)
            msg("Especifique onde entrar. Tente \"ajuda entrar\"");

        switch (command[1]) {

            case COMMAND_CHAT:
                /*
                 * ideia:
                 * 
                 * entrarChat(user, idChat);
                 * 
                 */
                break;
            case COMMAND_CHATS:
                /*
                 * ideia:
                 * 
                 * listaOpenChats()
                 */
                break;

            case COMMAND_REUNIAO:

                // TODO
                break;
            case COMMAND_REUNIOES:
                // TODO
                break;

            default:
                msg("Comando não entendido. Tente \"ajuda entrar\"");
        }
    }

    private void listar(String[] command) {
        if (command.length < 2)
            msg("Especifique o que listar. Tente \"ajuda listar\"");

        switch (command[1]) {

            case COMMAND_CHAT:

                // TODO
                break;
            case COMMAND_CHATS:
                // TODO
                break;

            case COMMAND_REUNIAO:
                // TODO
                break;
            case COMMAND_REUNIOES:
                // TODO
                break;

            default:
                msg("Comando não entendido. Tente \"ajuda listar\"");
        }
    }

    private void ajuda(String[] command) {
        if (command.length > 1) {
            switch (command[1]) {

                case COMMAND_ENTRAR:
                    msg("Para entrar num chat ou numa reunião, informe o id correspondente.");
                    break;

                case COMMAND_LISTAR:
                    msg("Para listar um chat ou uma reunião, selecione a opção listar");
                    break;

                case COMMAND_SAIR:
                    msg("Para sair, selecione a opção sair.");
                    break;

                default:
                    msg("Comando não entendido. Tente somente \"ajuda\" para ver os comandos disponíveis.");
            }
        } else {
            msg("Para entrar num chat ou numa reunião, selecione a opção com o id correspondente. Para listar um chat ou uma reunião, selecione a opção listar. Para sair, selecione a opção sair.");
            // TODO printar todas as ajudas.
        }
    }

    private void buildClient() {
        while (client == null) {
            msg("Insira o login e a senha");
            String login = input.next();
            String senha = input.next();
            resetInput();
            int status = Client.queryLogin(login, senha);
            if (status == Client.USER_NOT_FOUND) {
                if (yesNoQuestion("Usuário não encontrado. Criar usuário novo com essas credenciais?")) {
                    client = Client.createUser(login, senha);
                }
            } else if (status == Client.LOGIN_VALID) {
                client = Client.doLogin(login);
            } else if (status == Client.BAD_PASSWORD) {
                msg("Senha incorreta! Tente novamente");
            }
        }
    }

    private boolean yesNoQuestion(String question) {
        msg(question + " (S/N)");
        while (true) {
            char ans = input.nextLine().charAt(0);
            if (ans == 's' || ans == 'S') {
                return true;
            } else if (ans == 'n' || ans == 'N') {
                return false;
            }
            msg("(S/N)");
        }
    }

    private void resetInput() {
        if (input.hasNextLine())
            input.nextLine(); // não sei se vai precisar mas dá uma dor de cabeça se esquecer
    }

    private static void msg(String s) {
        System.out.println(s);
    }
}
