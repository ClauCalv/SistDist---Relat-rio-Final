import java.util.Scanner;

public class ClientCLI {

    public static final String zookeeperAddress = "localhost";
    private final Scanner input;
    private Client client = null;
    Client.Reuniao reuniao = client.new Reuniao();

    private final String COMMAND_AJUDA = "ajuda", COMMAND_CHAT = "chat", COMMAND_REUNIAO = "reuniao",
            COMMAND_SAIR = "sair", COMMAND_ENTRAR = "entrar", COMMAND_CRIAR = "criar", COMMAND_LISTAR = "listar",
            COMMAND_ENVIAR = "enviar", COMMAND_RECEBER = "receber", COMMAND_PARAR = "parar";

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

                case COMMAND_CHAT:
                    chat(command);
                    break;

                case COMMAND_REUNIAO:
                    reuniao(command);

                case COMMAND_SAIR:
                    sair();
                    break;

                default:
                    msg("Comando não entendido. Tente \"ajuda\"");
            }
        }
    }

    private void sair() {
        if (yesNoQuestion("Deseja mesmo sair?"))
            client.quit();
        client = null;
    }

    private void reuniao(String[] command) {
        if (command.length < 2)
            msg("Comando não entendido. Tente \"ajuda reuniao\"");

        switch (command[1]) {

            case COMMAND_LISTAR:
                String[] reunioes = (client.getAllReunioes());
                msg("Lista de reuniões ativas no momento:");
                for (String s : reunioes)
                    msg(" - " + s);
                break;

            case COMMAND_ENTRAR:
                if (client.canEnterReuniao(command[2]))
                    reuniaoLoop(client.enterReuniao(command[2]));
                else
                    msg("Reunião não encontrada");
                break;

            case COMMAND_CRIAR:
                if (client.canCreateReuniao(command[2])) {

                    reuniaoLoop(reuniao.createReuniao(command[2]));

                } else
                    msg("Reunião não encontrada");
                break;

            case COMMAND_AJUDA:
                ajuda(new String[] { COMMAND_AJUDA, COMMAND_REUNIAO });

            default:
                msg("Comando não entendido. Tente \"ajuda reuniao\"");
        }
    }

    private void reuniaoLoop(Client.Reuniao enterReuniao) {
        // TODO
    }

    private void chat(String[] command) {
        if (command.length < 2)
            msg("Comando não entendido. Tente \"ajuda chat\"");

        switch (command[1]) {

            case COMMAND_ENVIAR:
                if (command.length < 4)
                    msg("Comando necessita de destinatário e mensagem. Tente \"ajuda chat\".");
                else {
                    String user = command[2];
                    StringBuilder message = new StringBuilder(command[3]);
                    if (command.length > 4)
                        for (int i = 4; i < command.length; i++)
                            message.append(" ").append(command[i]); // desfazer o split

                    int result = client.sendMessage(user, message.toString());
                    if (result == Client.MESSAGE_SENT)
                        msg("Mensagem enviada");
                    else if (result == Client.USER_NOT_FOUND)
                        msg("Usuário não existente");
                }
                break;

            case COMMAND_RECEBER:
                if (command.length < 3)
                    msg("Necessário informar quantas mensagens receber. Tente \"ajuda chat\".");
                else {
                    int amount = 0;
                    try {
                        if (command[2].equals("all"))
                            amount = Integer.MAX_VALUE;
                        else
                            amount = Integer.parseInt(command[2]);
                    } catch (NumberFormatException ignored) {
                    }

                    if (amount <= 0)
                        msg("Quantidade deve ser \"all\" ou um inteiro positivo");
                    else
                        receberLoop(amount);
                }
                break;

            case COMMAND_AJUDA:
                ajuda(new String[] { COMMAND_AJUDA, COMMAND_CHAT });

            default:
                msg("Comando não entendido. Tente \"ajuda chat\".");
        }
    }

    private void receberLoop(final int amount) { // gambiarra com busy-wait para parar quando terminar OU o usuario
                                                 // digitar quit
        Thread retrieveMessages = new Thread(() -> {
            for (int i = amount; i > 0; i--) {
                String[] message = client.retrieveMessage();
                msg("Mensagem de " + message[0] + ":" + message[1]);
            }
        });

        Thread stopCommand = new Thread(() -> {
            while (true) {
                String[] command = input.nextLine().split("[ ]+");
                if (command[0] == COMMAND_PARAR) {
                    msg("Interrompendo recebimento de mensagens");
                    break;
                }
            }
        });

        msg("Procurando mensagens");
        retrieveMessages.start();
        stopCommand.start();
        while (true) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {
            }

            if (!retrieveMessages.isAlive() || !stopCommand.isAlive()) {
                if (stopCommand.isAlive())
                    stopCommand.stop();
                if (retrieveMessages.isAlive())
                    retrieveMessages.stop();

                break;
            }
        }
        msg("Fim do recebimento de mensagens");
    }

    private void ajuda(String[] command) {
        if (command.length > 1) {
            switch (command[1]) {

                case COMMAND_CHAT:
                    msg("\"chat enviar USER MSG\": Envia uma mensagem MSG ao usuário USER.");
                    msg("\"chat receber N|all\": Recupera todas ou as primeiras N mensagens não lidas");
                    msg("\"chat ajuda\": Igual a \"ajuda chat\".");
                    break;

                case COMMAND_REUNIAO:
                    msg("\"reuniao listar\": Lista todas as reuniões em andamento.");
                    msg("\"reuniao entrar|criar ID\": Entra ou cria a reunião ID");
                    msg("\"reuniao ajuda\": Igual a \"ajuda reuniao\".");
                    break;

                case COMMAND_SAIR:
                    msg("\"sair\": Sai da sua conta (logoff)");
                    break;

                default:
                    msg("Comando não entendido. Tente somente \"ajuda\" para ver os comandos disponíveis.");
            }
        } else {
            msg("Digite \"ajuda chat\" para saber mais sobre o comando chat");
            msg("Digite \"ajuda reuniao\" para saber mais sobre o comando reuniao");
            msg("Digite \"ajuda sair\" para saber mais sobre o comando sair");
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
