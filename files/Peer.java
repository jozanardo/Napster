import java.io.*;
import java.net.*;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.Scanner;
import java.util.ArrayList;

public class Peer {
    private String ip;
    private int port;
    private String fileDirectory;
    private List<String> files;
    private String requestedFile;
    private String adress;
    
    public void setRequestedFile(String requestedFile) {
        this.requestedFile = requestedFile;
    }
    
    public Peer(String IP, int Port, String fileDirectory) {
        this.ip = IP;
        this.port = Port;
        this.fileDirectory = fileDirectory;
    }

    public void start() {
        try {
            // Inicialização do peer
            System.out.println("Peer inicializado. Aguardando comandos...");

            Scanner scanner = new Scanner(System.in);
            int command = 1;

            Registry registry = LocateRegistry.getRegistry("127.0.0.1", 1099);
            ServerInterface server = (ServerInterface) registry.lookup("server");
            
            this.files = addFilesNamesInPeer(this.fileDirectory);                
            
            while (command != 0) {
            	System.out.println("-------MENU-------");
            	System.out.println("[0] Sair");
            	System.out.println("[1] Join");
                System.out.println("[2] Search");
                System.out.println("[3] Download");
                System.out.print("Digite um numero: ");                

                command = scanner.nextInt();
                
                switch(command) {   
                	case 1:
                		// Envio de requisição JOIN ao servidor
                		join(this,server);
                								
						int p = this.port;
						ThreadForDownload thread = this.new ThreadForDownload(p);
						thread.start();
                		break;
                	case 2:
                		// Envio de requisição SEARCH ao servidor
                		search(this,server);
                		
                        break;
                    case 3:
                    	// Envio de requisição DOWNLOAD a outro peer                
                    	download(this,server);
                        break;
                    default:
                    	command = 0;
                    	break;                   	
                	
                }                
            }
        } catch (Exception e) {
            System.err.println("Erro: " + e.getMessage());
            e.printStackTrace();
        }
    }    

    public static void main(String[] args) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

            System.out.print("Insira o IP: ");
            String ip = reader.readLine();

            System.out.print("Insira a Porta: ");
            int port = Integer.parseInt(reader.readLine());

            System.out.print("Diretório de arquivos: ");
            String fileDirectory = reader.readLine();

            Peer peer = new Peer(ip, port, fileDirectory);
            peer.start();
        } catch (IOException e) {
            System.err.println("Erro durante a inicialização do peer: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static void join(Peer peer,ServerInterface server) throws RemoteException{
	    String response = server.join(peer.ip, peer.port, peer.files);
	
	    if (response.equals("JOIN_OK")) {
	    	System.out.println("Sou peer " + peer.ip + ":" + peer.port + " com arquivos: " + peer.files.toString());
	    } else {
	        System.out.println("Falha ao realizar o JOIN.");
	    }
    }
    
    public static void search(Peer peer,ServerInterface server) throws RemoteException, IOException{
    	BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        System.out.print("Nome do arquivo: ");
        String fileName = reader.readLine();

        List<String> peersWithFile = server.search(peer.ip + ":" + peer.port,fileName);

        System.out.println("peers com arquivo solicitado: " + peersWithFile.toString());
        peer.setRequestedFile(fileName);

    }
    
    public static void download(Peer peer, ServerInterface server) {
    	try {
    		Scanner scanner = new Scanner(System.in);

            System.out.print("Digite o IP do peer de destino: ");
            String ip = scanner.nextLine().trim();

            System.out.print("Digite a porta do peer de destino: ");
            int port = scanner.nextInt();

            try (Socket socket = new Socket(ip, port)) {
                DataOutputStream dataOutput = new DataOutputStream(socket.getOutputStream());
                DataInputStream dataInput = new DataInputStream(socket.getInputStream());

                // Preparando para salvar o arquivo recebido
                String filePath = peer.fileDirectory + File.separator + peer.requestedFile;
                FileOutputStream fileOutputStream = new FileOutputStream(filePath);

                // Envio do nome do arquivo para o Peer
                dataOutput.writeUTF(peer.requestedFile);

                // Recebimento do arquivo em pacotes de 4096 bytes
                byte[] buffer = new byte[4096];
                int bytesRead;               
                while ((bytesRead = dataInput.read(buffer)) != -1) {
                    fileOutputStream.write(buffer, 0, bytesRead);
                }

                fileOutputStream.flush();

                // Fechando conexões e recursos
                dataOutput.close();
                dataInput.close();
                fileOutputStream.close();
                
                server.update(peer.ip + ":" + peer.port, peer.requestedFile);
                System.out.println(peer.requestedFile);
                System.out.println("Arquivo "+ peer.requestedFile +" baixado com sucesso na pasta " + peer.fileDirectory);
            } catch (IOException e) {
                e.printStackTrace();
            }
           
    	}catch (Exception e) {
    		e.printStackTrace();
		}		
	}
    
    
    public static List<String> addFilesNamesInPeer(String directory) {
    	List <String> filesList = new ArrayList<String>();
		//Criando um arquivo que lista os arquivos de um diretorio
		File[] files = new File(directory).listFiles();
		System.out.println("putamerdaaaaaaaaa?"+directory);
		for (File file : files) {
			System.out.println("putamerda?"+file);
			if (file.isFile()) {
				System.out.println("aqui?"+file);
				filesList.add(file.getName());
				System.out.println("aqui2?"+filesList);
			}
		}	

        
		return filesList;     
    }
        
    public class ThreadForDownload extends Thread{
    	
		private int port;
		
		public ThreadForDownload(int Port) {
			this.port = Port;
		}
		
		public void run() {
				try {
					while (true) {
					    // Inicialização do Socket do servidor
					    ServerSocket serverSocket = new ServerSocket(port);
					    Socket client = serverSocket.accept();

					    // Canais de fluxo de entrada e saída
					    DataInputStream dataInput = new DataInputStream(client.getInputStream());
					    DataOutputStream dataOutput = new DataOutputStream(client.getOutputStream());

					    // Criação de uma variável do tipo File para receber o arquivo
					    String fileName = dataInput.readUTF();
					    File file = new File(fileDirectory + "\\" + fileName);
					   
					    // Canal de transmissão para enviar o arquivo
					    FileInputStream fileInputStream = new FileInputStream(file);

					    // Enviando o conteúdo do arquivo para o cliente em pacotes de 4096 bytes
					    byte[] buffer = new byte[4096];
					    int bytesRead;
					 
					    while ((bytesRead = fileInputStream.read(buffer)) != -1) {
					        dataOutput.write(buffer, 0, bytesRead);
					    }
					    dataOutput.flush();

					    // Fechando conexões e recursos
					    fileInputStream.close();
					    dataInput.close();
					    dataOutput.close();
					    client.close();
					    serverSocket.close();
					}
							
				} catch (Exception e) {
					e.printStackTrace();
				}		
		}
	}    
 
}
