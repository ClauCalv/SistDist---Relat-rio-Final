import zkcodes.ZookeeperLeader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;

public class ClientCLI {

    public static final String zookeeperAddress = "localhost";
    private final BufferedReader input;
    private Client client = null;

    private final String COMMAND_AJUDA = "ajuda", COMMAND_CHAT = "chat", COMMAND_REUNIAO = "reuniao",
            COMMAND_SAIR = "sair", COMMAND_ENTRAR = "entrar", COMMAND_CRIAR = "criar", COMMAND_LISTAR = "listar",
            COMMAND_ENVIAR = "enviar", COMMAND_RECEBER = "receber", COMMAND_PARAR = "parar",
            COMMAND_TRANSMITIR = "transmitir";

    public ClientCLI(BufferedReader input) {
        this.input = input;
    }

    public static void main(String[] args) {
        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));

        if (args.length > 0)
            Client.setup(args[0]);
        try {
            new ClientCLI(input).inputLoop();
        } catch (IOException e) {
            msg("Erro grave encontrado");
            e.printStackTrace();
        }
    }

    public void inputLoop() throws IOException {
        while (true) {

            if (client == null)
                buildClient();

            String[] command = input.readLine().split("[ ]+");
            switch (command[0]) {

                case COMMAND_AJUDA:
                    ajuda(command);
                    break;

                case COMMAND_CHAT:
                    chat(command);
                    break;

                case COMMAND_REUNIAO:
                    reuniao(command);
                    break;

                case COMMAND_SAIR:
                    sair();
                    break;

                default:
                    msg("Comando não entendido. Tente \"ajuda\"");
            }
        }
    }

    private void sair() throws IOException {
        if (yesNoQuestion("Deseja mesmo sair?")) {
            client.quit();
            System.exit(0);
        }
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

                    reuniaoLoop(client.createReuniao(command[2]));

                } else
                    msg("Reunião não encontrada");
                break;

            case COMMAND_AJUDA:
                ajuda(new String[] { COMMAND_AJUDA, COMMAND_REUNIAO });

            default:
                msg("Comando não entendido. Tente \"ajuda reuniao\"");
        }
    }

    private void reuniaoLoop(Client.Reuniao reuniao) {

        //Preciso que uma thread referencie a outra, mas os lambdas exigem vaiáveis finais!
        FlexiRunnable pollInput = new FlexiRunnable(), waitBarrier = new FlexiRunnable();
        Thread t1 = new Thread(pollInput), t2 = new Thread(waitBarrier);

        boolean hadNoLeader = reuniao.getCurrentLeader() == null; // não lembro mais pra que eu ia usar isso
        boolean isLeader = reuniao.checkElection();// não lembro mais pra que eu ia usar isso, mas tem efeito colateral!

        reuniao.setLeaderCallback(new ZookeeperLeader.LeaderCallback() { // callback == "terceira thread"
            @Override
            public void onNewLeaderFound(String currentLeader) {
                msg("Usuário "+currentLeader+" é o novo apresentador da reunião.");
                t2.interrupt();
            }

            @Override
            public void onNoNewLeaderFound() {
                msg("Algo deu errado. Estamos sem apresentadores?");
            }

            @Override
            public void onBecomeLeader() {
                msg("Você agora é o apresentador!");
                t1.interrupt();
            }
        });


        pollInput.setDelegate(() -> {
            msg("Digite \"sair\" para sair da reunião. Digite \"transmitir\" para iniciar a apresentação");
            while (true) {
                try {
                    boolean isCurrentlyLeader = reuniao.checkElection();

                    if (input.ready()) {
                        String[] command = input.readLine().split("[ ]+");
                        switch (command[0]){
                            case COMMAND_SAIR:
                                if(yesNoQuestion("Gostaria mesmo de sair?"))
                                    if(isCurrentlyLeader){
                                        reuniao.stopPresentation();
                                        reuniao.leave();
                                    } else {
                                        reuniao.leave();
                                        t2.interrupt();
                                    }
                                return;

                            case COMMAND_TRANSMITIR:
                                if(isCurrentlyLeader){
                                    reuniao.startPresentation();
                                    msg("Começando a apresentação ...");
                                    msg("( Nada acontecerá, é só uma simulação )");
                                    msg("Digite \"parar\" para parar de transmitir.");
                                } else {
                                    msg("Somente o apresentador pode iniciar ou encerrar uma transmissão.");
                                }
                                break;

                            case COMMAND_PARAR:
                                if(isCurrentlyLeader){
                                    reuniao.stopPresentation();
                                    msg("Encerrando a apresentação. Você ainda pode apresentar novamente.");
                                } else {
                                    msg("Somente o apresentador pode iniciar ou encerrar uma transmissão.");
                                }
                                break;

                            default:
                                msg("Comando não entendido. Digite \"parar\" para interromper a busca por mensagens.");
                        }

                    }

                    Thread.sleep(500);
                } catch (InterruptedException ignored) { // thread interrompida
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        waitBarrier.setDelegate(() -> {
            while (true) {
                if(reuniao.hasLeft())
                    return;

                boolean isCurrentlyLeader = reuniao.checkElection();
                if(isCurrentlyLeader)
                    return;

                //msg("Usuário "+currentLeader+" é o novo apresentador da reunião."); //Dito pelo callback
                msg("Esperando a apresentação começar. Digite \"sair\" para sair da reunião");
                if(reuniao.waitPresentation()){ // entrou na barreira
                    msg("A apresentação começou");
                    msg("( Nada acontecerá, é só uma simulação )");
                    msg("Digite \"sair\" para sair da reunião\"");
                    if(reuniao.watchPresentation()){ // saiu da barreira
                        msg("A apresentação terminou. Aguardando outra apresentação ...");
                    } else { //interrompido enquanto tentava sair da barreira, ou seja, outro líder foi eleito enquanto
                            // transmitia (ele caiu, pq "sair" encerra a transmissão primeiro. Como a trava é efêmera,
                            // nem sei se isso vai chegar a ser disparado ou se vai dar como encerrado antes.
                        msg("A apresentação caiu! Esperando o próximo apresentador ...");
                    }
                } else { //interrompido enquanto tentava entrar na barreira. O líder pode ter saído ou caído nesse caso.
                        // a trava nunca chegou a ser levantada, então tenho certeza que a interrupção disparou.
                    msg("O apresentador saiu sem apresentar! Esperando o próximo apresentador ...");
                }
            }
        });

        t1.start();
        t2.start();

        try {
            t1.join();
        } catch (InterruptedException e) {
            e.printStackTrace(); // Não deve acontecer.
        }
        msg("Você saiu da reunião.");

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

    // gambiarra com busy-wait para parar quando terminar OU o usuario digitar quit
    private void receberLoop(final int amount) {
        //Preciso que uma thread referencie a outra, mas os lambdas exigem vaiáveis finais!
        FlexiRunnable retrieveMessages = new FlexiRunnable(), stopCommand = new FlexiRunnable();
        Thread t1 = new Thread(retrieveMessages), t2 = new Thread(stopCommand);

        retrieveMessages.setDelegate(() -> {
            for (int i = amount; i > 0; i--) {
                String[] message = client.retrieveMessage();
                if(message == null)// thread interrompida
                    break;

                msg("Mensagem de " + message[0] + ":" + message[1]);
            }
            if(!t2.isInterrupted()) t2.interrupt();
        });

        stopCommand.setDelegate(() -> {
            msg("Digite \"parar\" para interromper a busca por mensagens.");
            while (true) {
                try {
                    if (input.ready()) {
                        String[] command = input.readLine().split("[ ]+");
                        if (command[0].equals(COMMAND_PARAR)) {
                            msg("Interrompendo recebimento de mensagens");
                            break;
                        } else msg("Comando não entendido. Digite \"parar\" para interromper a busca por mensagens.");
                    } else Thread.sleep(500);
                } catch (InterruptedException e) { // thread interrompida
                    if(!t1.isInterrupted()) t1.interrupt();
                    break;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        msg("Procurando mensagens.");
        t2.start();
        t1.start();

        try {
            t1.join();
        } catch (InterruptedException e) {
            e.printStackTrace(); // Não deve acontecer.
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

    private void buildClient() throws IOException {
        while (client == null) {
            msg("Insira o login e a senha");
            String[] entry = input.readLine().split("[ ]+");
            String login = entry[0];
            String senha = entry.length > 1 ? entry [1] : input.readLine().split("[ ]+")[0];
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

    private boolean yesNoQuestion(String question) throws IOException {
        msg(question + " (S/N)");
        while (true) {
            char ans = input.readLine().charAt(0);
            if (ans == 's' || ans == 'S') {
                return true;
            } else if (ans == 'n' || ans == 'N') {
                return false;
            }
            msg("(S/N)");
        }
    }

    private static void msg(String s) {
        System.out.println(s);
    }

    public static class FlexiRunnable implements Runnable { // Precisava que as runnables das threads se referenciassem
        private Runnable delegate;
        private volatile boolean running = false;
        public void run() {
            running = true;
            if (delegate != null) {
                delegate.run();
            }
        }
        public void setDelegate(Runnable delegate) {
            if (running) {
                throw new IllegalStateException("The thread is already running...");
            }
            this.delegate = delegate;
        }
    }
}
