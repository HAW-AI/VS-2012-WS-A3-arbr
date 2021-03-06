package name_service;

import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import mware_lib.Communication; 

/**
 * @author Benjamin Trapp
 * 		   Christoph Gr�bke
 */
public class NameServiceDaemon implements Runnable 
{
    /**
     * Variable needed to set up the communication between the distributed 
     * software
     */
	private Communication nameServiceCom;
	/**
	 * Thread safe HashMap containing the elements of the Name Service
	 */
	private ConcurrentHashMap<String, String> nameServiceElements;
	
	/**
	 * Constructor to create a new NameServiceDaemon to handle the client calls
	 * @param socket Socket for the communication
	 * @param map Threadsafe HashMap that contains the elements of the Name Service
	 */
	public NameServiceDaemon(Socket socket, ConcurrentHashMap<String, String> map) 
	{
		this.nameServiceElements = map;
		this.nameServiceCom = new Communication(socket);
	}
	
	/**
	 * Declares the remote object under the specified name
	 * to the name service. 
	 * 
	 * If the name already exists, the method will
	 * throw a RemoteException 
	 * 
	 * @param name - name of the declared object
	 * @param infos - info about the remote object
	 */
	public void rebind(String name, String infos) 
	{
		String infosTmp = nameServiceCom.getHostAddr() + "|" + infos;
		logInfo("[Rebind] Name: "+name+" Infos: "+infosTmp);
		if (!nameServiceElements.containsKey(name)) 
		{
			nameServiceElements.put(name, infosTmp);
			
			if (!nameServiceElements.containsKey(name)){ 
				logError("[Rebind] failed. Name: "+name);
				throw new RuntimeException("Call of [" + name + "] failed...");
			}
		} 
	}			
	
	/**
	 * Get's the object back under it's saved info. If the name
	 * is not mentioned in the object list, a RuntimeException will 
	 * be thrown
	 * 
	 * @param name - name of the known declared object
	 * @return String- info for the object
	 */
	public String resolve(String name) 
	{
		if (nameServiceElements.get(name) == null){
			logError("[Resolve] Passed name was null. Name: "+name);
			throw new RuntimeException("ERROR: Passed name was null");
		}
		
		return nameServiceElements.get(name);
	}
	
	/**
	 * Unmarshales a given marshaled string and calls the
	 * corresponding Method with its parameters if it has some.
	 * 
	 * If no exception occurre the "OK"-Code will be send back
	 * otherwise an "ERROR" Code
	 * 
	 * @param marshalled String that shall be marshaled
	 */
	private void unmarshal(String marshalled) 
	{
		String[] unmarshalled = marshalled.split("\\|\\|");
		String method = unmarshalled[0];
				
		if (method.equals("rebind"))
		{
			try {
				rebind(unmarshalled[2], unmarshalled[1] + "|" + unmarshalled[2]);
				nameServiceCom.send("OK|");
			} catch (RuntimeException e) {
				nameServiceCom.send("ERROR|" + e.getMessage());
			}
		} else if (method.equals("resolve")) 
		{
			try {
				nameServiceCom.send("OK|" + resolve(unmarshalled[1]));
			} catch(RuntimeException e) {
				nameServiceCom.send("ERROR|" + e.getMessage());
			}
		}else
		{
			System.err.println("err what the hecK? How did this happend? Looks like i took the wrong direction...");
		}
	}
	
	/**
	 * Waits on requests and calls after the request was received the unmarshal()-Method with the 
	 * marshaled String.
	 */
	@Override
	public void run() 
	{
		logInfo("NameServiceDaemon started");
		while (true)
		{
			unmarshal(nameServiceCom.receive());
		}
	}
	
	private void logInfo(String log){
		LoggerImpl.info(this.getClass().getName(), log);
	}
	
	private void logError(String log){
		LoggerImpl.error(this.getClass().getName(), log);
	}
}
