import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Interface remota para as operações do servidor
interface ServerInterface extends Remote {
    String join(String ipPeer, int portPeer, List<String> files) throws RemoteException;
    List<String> search(String adress,String fileName) throws RemoteException;
    String update(String peerName, String fileName) throws RemoteException;
}

// Implementação do servidor
class ServerImpl implements ServerInterface {
    private Map<String, List<String>> peers = new HashMap<>(); // Armazena as informações dos peers

    public ServerImpl() {}

    public String join(String ipPeer, int portPeer, List<String> files) throws RemoteException {
        if (peers.containsKey(ipPeer + ':' + portPeer)) return "JOIN_ERROR: Peer já cadastrado"; 
        peers.put(ipPeer + ":" + portPeer, files); // Adiciona as informações do peer no mapa
        System.out.println("Peer " + ipPeer + ":" + portPeer + " adicionado com arquivos " + files.toString());
        return "JOIN_OK"; 
    }

    public List<String> search(String adress,String fileName) throws RemoteException {
        List<String> foundPeers = new ArrayList<>(); // Armazena os peers que possuem o arquivo
        for (Map.Entry<String, List<String>> entry : peers.entrySet()) {
            if (entry.getValue().contains(fileName)) {
                foundPeers.add(entry.getKey()); // Adiciona o peer à lista de peers encontrados
            }
        }
        System.out.println("Peer " + adress + " solicitou arquivo " + fileName);
        return foundPeers;
    }

    public String update(String peerName, String fileName) throws RemoteException {
    	System.out.println(peerName);
        if (peers.containsKey(peerName)) {
            peers.get(peerName).add(fileName); // Adiciona o arquivo ao peer correspondente
        	System.out.println(peers.get(peerName));
            return "UPDATE_OK"; // Retorna resposta de sucesso
        }
        return "UPDATE_FAILED";
    }
}

public class Server {
    public static void main(String[] args) {
        try {
            // Criando instância do servidor
            ServerImpl server = new ServerImpl();

            // Criando registro RMI na porta 1099
            LocateRegistry.createRegistry(1099);

            // Registrando o objeto remoto no registro
            Registry registry = LocateRegistry.getRegistry("127.0.0.1", 1099);
            
            ServerInterface stub = (ServerInterface) UnicastRemoteObject.exportObject(server, 0);
            
            registry.rebind("server", stub);            
               
            System.out.println(registry);
            System.out.println("Servidor inicializado. Aguardando requisições...");
            
        } catch (Exception e) {
            System.err.println("Erro no servidor: " + e.toString());
            e.printStackTrace();
        }
    }
}
