/*
 * Copyright (C) 2006-2008 Alfresco Software Limited.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.

 * As a special exception to the terms and conditions of version 2.0 of 
 * the GPL, you may redistribute this Program in connection with Free/Libre 
 * and Open Source Software ("FLOSS") applications as described in Alfresco's 
 * FLOSS exception.  You should have recieved a copy of the text describing 
 * the FLOSS exception, and it is also available here: 
 * http://www.alfresco.com/legal/licensing"
 */

package org.alfresco.jlan.ftp;

import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;

import org.alfresco.jlan.debug.Debug;
import org.alfresco.jlan.server.ServerListener;
import org.alfresco.jlan.server.SrvSession;
import org.alfresco.jlan.server.Version;
import org.alfresco.jlan.server.config.ConfigId;
import org.alfresco.jlan.server.config.ConfigurationListener;
import org.alfresco.jlan.server.config.InvalidConfigurationException;
import org.alfresco.jlan.server.config.ServerConfiguration;
import org.alfresco.jlan.server.core.SharedDeviceList;
import org.alfresco.jlan.server.filesys.NetworkFileServer;
import org.alfresco.jlan.util.UTF8Normalizer;


/**
 * <p>Create an FTP server on the specified port. The default server port is 21.
 *
 * @author gkspencer
 */
public class FTPServer extends NetworkFileServer implements Runnable, ConfigurationListener {

	//	Constants
	//
	//	Server version
	
	private static final String ServerVersion = Version.FTPServerVersion;
	
	//	Listen backlog for the server socket
	
	protected static final int LISTEN_BACKLOG = 10;
	
	//	Default FTP server port
	
	protected static final int SERVER_PORT		= 21;
	
	//  Thread group
  
	protected static final ThreadGroup FTPThreadGroup = new ThreadGroup( "FTPSessions");
  
  	//  FTP server configuration
  
  	private FTPConfigSection m_configSection;
  
	//	Server socket
	
	private ServerSocket m_srvSock;
	
	//	Active session list
	
	private FTPSessionList m_sessions;
	
	//	Active data session list
	
	private FTPDataSessionTable m_dataSessions;
	private int m_dataPortId;
	
	//	List of available shares
	
	private SharedDeviceList m_shares;
	
	//	Next available session id

	private int m_sessId;

	//	Root path for new sessions
	
	private FTPPath m_rootPath;
	
	//	FTP server thread
	
	private Thread m_srvThread;
	
	// SITE command interface
  
	private FTPSiteInterface m_siteInterface;
  
	// UTF-8 string normalizer
  
	private UTF8Normalizer m_normalizer;
  
	/**
	 * Class constructor
	 * 
	 * @param config ServerConfiguration
	 */
	public FTPServer(ServerConfiguration config) {
		super("FTP", config);

		//	Add the FTP server as a configuration change listener of the server configuration
		
		getConfiguration().addListener(this);
		
		//	Set the server version
		
		setVersion(ServerVersion);
		
		//	Allocate the session lists
		
		m_sessions     = new FTPSessionList();
		m_dataSessions = new FTPDataSessionTable();

    //  Find the FTP server configuration
    
    m_configSection = (FTPConfigSection) config.getConfigSection( FTPConfigSection.SectionName);
    if ( m_configSection != null) {
      
  		//	Check if there is a data port range, initialize the data port id
  		
  		m_dataPortId = getFTPConfiguration().getFTPDataPortLow();
  		
  		//	Enable debug
  		
  		if ( getFTPConfiguration().getFTPDebug() != 0)
  			setDebug(true);
  			
  		//	Create the root path, if configured
  		
  		if ( getFTPConfiguration().hasFTPRootPath()) {
  
  			try {			
  
  				//	Create the root path
  				
  				m_rootPath = new FTPPath(getFTPConfiguration().getFTPRootPath());
  			}
  			catch (InvalidPathException ex) {
  				Debug.println(ex);
  			}
  		}
      
      // Set the FTP SITE interface
      
      setSiteInterface( getFTPConfiguration().getFTPSiteInterface());
    }
    else
      setEnabled( false);
    
    // Create the UTF-8 string normalizer, if the normalizer cannot be initialized then swicth off UTF-8 support
    
    try {
      m_normalizer = new UTF8Normalizer();
    }
    catch ( Exception ex) {
    }
	}
	
	/**
	 * Add a new session to the server
	 * 
	 * @param sess FTPSrvSession
	 */
	protected final void addSession(FTPSrvSession sess) {

		//	Add the session to the session list
		
		m_sessions.addSession(sess);
		
		//	Propagate the debug settings to the new session
			
    if (hasDebug()) {

      //  Enable session debugging, output to the same stream as the server

      sess.setDebug(getFTPConfiguration().getFTPDebug());
    }
	}
	
	/**
	 * emove a session from the server
	 * 
	 * @param sess FTPSrvSession
	 */
	protected final void removeSession(FTPSrvSession sess) {
		
		//	Remove the session from the active session list
		
		if ( m_sessions.removeSession(sess) != null) {
		
			//	Inform listeners that a session has closed
			
			fireSessionClosedEvent(sess);
		}
	}
	
	/**
	 * Allocate a local port for a data session
	 * 
	 * @param sess FTPSrvSession
	 * @param remAddr InetAddress
	 * @param remPort int
	 * @return FTPDataSession
	 * @exception IOException
	 */
	protected final FTPDataSession allocateDataSession(FTPSrvSession sess, InetAddress remAddr, int remPort)
		throws IOException {
	  
	  //	Check if there is a data port range configured, if not then just create a new FTP data session
	  
	  FTPDataSession dataSess = null;
	  
	  if ( getFTPConfiguration().hasFTPDataPortRange() == false) {
	    
	    //	Create a normal data session
	      
	    dataSess = new FTPDataSession(sess, remAddr, remPort);
	    
	    //	Return the data session
	    
	    return dataSess;
	  }
	  
	  //	Check if all available ports in the valid range are in use

	  int dataPortLow  = getFTPConfiguration().getFTPDataPortLow();
	  int dataPortHigh = getFTPConfiguration().getFTPDataPortHigh();
	  
	  if ( m_dataSessions.numberOfSessions() == ( dataPortHigh - dataPortLow))
	    throw new IOException("No free data session ports");
	  
	  //	Allocate a port for the new data session
	  
	  int dataPort = getNextFreeDataSessionPort();
	  if ( dataPort == -1)
	    throw new IOException("Failed to allocate data port");

	  //	Make sure we can bind to the allocated local port
	  
      dataSess = new FTPDataSession(sess, dataPort, remAddr, remPort);

	  //	Add the data session to the allocated session table
	  
	  m_dataSessions.addSession(dataPort, dataSess);
	  
	  //	DEBUG
	  
	  if ( Debug.EnableInfo && sess.hasDebug(FTPSrvSession.DBG_DATAPORT))
	    Debug.println("[FTP] Allocated data port " + dataPort + " to session " + sess.getSessionId());
	  
	  //	Return the data session
	  
	  return dataSess;
	}
	
	/**
	 * Allocate a local port for a passive mode data session
	 * 
	 * @param sess FTPSrvSession
	 * @param localAddr InetAddress
	 * @return FTPDataSession
	 * @exception IOException
	 */
	protected final FTPDataSession allocatePassiveDataSession(FTPSrvSession sess, InetAddress localAddr)
		throws IOException {
	  
	  //	Check if there is a data port range configured, if not then just create a new FTP data session
	  
	  FTPDataSession dataSess = null;
	  
	  if ( getFTPConfiguration().hasFTPDataPortRange() == false) {
	    
	    //	Create a new passive FTP data session
	    
		dataSess = new FTPDataSession(sess, localAddr);
	    
	    //	Return the data session
	    
	    return dataSess;
	  }
	  
	  //	Check if all available ports in the valid range are in use

	  int dataPortLow  = getFTPConfiguration().getFTPDataPortLow();
	  int dataPortHigh = getFTPConfiguration().getFTPDataPortHigh();
	  
	  if ( m_dataSessions.numberOfSessions() == ( dataPortHigh - dataPortLow))
	    throw new IOException("No free data session ports");
	  
	  //	Allocate a port for the new data session
	  
	  int dataPort = getNextFreeDataSessionPort();
	  if ( dataPort == -1)
	    throw new IOException("Failed to allocate data port");

	  //	Make sure we can bind to the allocated local port
	  
	  dataSess = new FTPDataSession(sess, dataPort, localAddr);

	  //	Add the data session to the allocated session table
	  
	  m_dataSessions.addSession(dataPort, dataSess);
	  
	  //	DEBUG
	  
	  if ( Debug.EnableInfo && sess.hasDebug(FTPSrvSession.DBG_DATAPORT))
	    Debug.println("[FTP] Allocated passive data port " + dataPort + " to session " + sess.getSessionId());
	  
	  //	Return the data session
	  
	  return dataSess;
	}
	
	/**
	 * Release a data session
	 * 
	 * @param dataSess FTPDataSession
	 */
	protected final void releaseDataSession(FTPDataSession dataSess) {

	  //	Close the data session

	  dataSess.closeSession();
	  
	  //	Check if there is a data port range configured, if not then do nothing
	  
	  if ( getFTPConfiguration().hasFTPDataPortRange() == false)
	    return;
	  
	  //	Remove the data session from the allocated session table
	  
	  m_dataSessions.removeSession(dataSess);

	  //	DEBUG
	  
	  if ( Debug.EnableInfo && dataSess.getCommandSession().hasDebug(FTPSrvSession.DBG_DATAPORT))
	    Debug.println("[FTP] Released data port " + dataSess.getAllocatedPort() + " for session " + dataSess.getCommandSession().getSessionId());
	}
	
	/**
	 * Allocate the next free data session port
	 * 
	 * @return int
	 */
	private final int getNextFreeDataSessionPort() {
	 
	  int initPort = m_dataPortId;
	  int dataPort = -1;
	  int dataHigh = getFTPConfiguration().getFTPDataPortHigh();
	  
	  synchronized ( m_dataSessions) {

	    //	Check if the upper range has been reached
	    
	    if ( m_dataPortId > dataHigh) {
	      m_dataPortId = getFTPConfiguration().getFTPDataPortLow();
	      initPort = dataHigh;
	    }
	      
	    //	Get the next data session id
	    
	    dataPort = m_dataPortId++;
	    
	    while ( m_dataSessions.findSession(dataPort) != null) {
	      
	      //	Check if we have checked the full range of ports
	      
	      if ( m_dataPortId == initPort)
	        return -1;
	      
	      //	Check if the port number has reached the upper limit, if so then wrap around
	      
	      if ( m_dataPortId > dataHigh)
	        m_dataPortId = getFTPConfiguration().getFTPDataPortLow();
	      dataPort = m_dataPortId++;
	    }
	  }
	  
	  //	Return the allocated data port
	  
	  return dataPort <= dataHigh ? dataPort : -1;
	}
	
  /**
   * Return the server name
   * 
   * @return String
   */
  public final String getServerName() {
    return getConfiguration().getServerName();
  }
  
	/**
	 * Get the shared device list
	 * 
	 * @return SharedDeviceList
	 */
	public final SharedDeviceList getShareList() {
		
		//	Check if the share list has been populated
		
		if ( m_shares == null)
			m_shares = getShareMapper().getShareList(getConfiguration().getServerName(), null, false);
			
		//	Return the share list
		
		return m_shares;
	}
		
	/**
	 * Check if the FTP server is to be bound to a specific network adapter
	 * 
	 * @return boolean
	 */
	public final boolean hasBindAddress() {
		return getFTPConfiguration().getFTPBindAddress() != null ? true : false;
	}
	
	/**
	 * Return the address that the FTP server should bind to
	 * 
	 * @return InetAddress
	 */
	public final InetAddress getBindAddress() {
		return getFTPConfiguration().getFTPBindAddress();
	}
	
	/**
	 * Check if the root path is set
	 * 
	 * @return boolean
	 */
	public final boolean hasRootPath() {
		return m_rootPath != null ? true : false;
	}
	
	/**
	 * Check if anonymous logins are allowed
	 * 
	 * @return boolean
	 */
	public final boolean allowAnonymous() {
		return getFTPConfiguration().allowAnonymousFTP();
	}
	
	/**
	 * Return the anonymous login user name
	 * 
	 * @return String
	 */
	public final String getAnonymousAccount() {
		return getFTPConfiguration().getAnonymousFTPAccount();
	}

	/**
	 * Return the next available session id
	 * 
	 * @return int
	 */
	protected final synchronized int getNextSessionId() {
		return m_sessId++;
	}
	
  /**
   * Return the FTP server configuration
   * 
   * @return FTPConfigSection
   */
  protected final FTPConfigSection getFTPConfiguration() {
    return m_configSection;
  }
  
	/**
	 * Return the FTP server port
	 * 
	 * @return int
	 */
	public final int getPort() {
		return getFTPConfiguration().getFTPPort();
	}
	
	/**
	 * Return the server socket
	 * 
	 * @return ServerSocket
	 */
	protected final ServerSocket getSocket() {
		return m_srvSock;
	}

	/**
	 * Return the root path for new sessions
	 *
	 * @return FTPPath
	 */	
	public final FTPPath getRootPath() {
		return m_rootPath;
	}
	
	/**
	 * Notify the server that a user has logged on.
	 *
	 * @param sess SrvSession
	 */
	protected final void sessionLoggedOn(SrvSession sess) {

		//	Notify session listeners that a user has logged on.

		fireSessionLoggedOnEvent(sess);
	}

  /**
   * Start the SMB server.
   */
  public void run() {

    //  Debug

    if (Debug.EnableInfo && hasDebug()) {
      Debug.println("[FTP] FTP Server starting on port " + getPort());
      Debug.println("[FTP] Version " + isVersion());
    }

    //  Create a server socket to listen for incoming FTP session requests

    try {

			//	Create the server socket to listen for incoming FTP session requests
			
			if ( hasBindAddress())
				m_srvSock = new ServerSocket(getPort(), LISTEN_BACKLOG, getBindAddress());
			else {
				
	    		// If IPv6 is enabled we need to bind to the global IPv6 address in order to get an IPv6 socket
	    	
				if ( getFTPConfiguration().isIPv6Enabled())
					m_srvSock = new ServerSocket(getPort(), LISTEN_BACKLOG, InetAddress.getByName("::"));
				else
					m_srvSock = new ServerSocket(getPort(), LISTEN_BACKLOG);
			}
				
			//	DEBUG
			
			if ( Debug.EnableInfo && hasDebug()) {
				Debug.print("[FTP] FTP Binding to local address ");
				if ( hasBindAddress())
					Debug.println(getBindAddress().getHostAddress());
				else
					Debug.println("ALL");
				
				if ( m_srvSock.getInetAddress() instanceof Inet6Address)
					Debug.println("[FTP] Listening on IPv6");
			}

			//	Check if the FTP server is using a limited data port range
			
			if ( Debug.EnableInfo && hasDebug() && getFTPConfiguration().hasFTPDataPortRange())
			  Debug.println("[FTP] Data ports restricted to range " + getFTPConfiguration().getFTPDataPortLow() + " - " + getFTPConfiguration().getFTPDataPortHigh());
			
			//	Indicate that the server is active
			
			setActive(true);
			fireServerEvent(ServerListener.ServerActive);
						
      //  Wait for incoming connection requests

      while ( hasShutdown() == false) {

		    //  Wait for a connection
		
		    Socket sessSock = getSocket().accept();
		    
			//	Set socket options
				
			sessSock.setTcpNoDelay(true);
						
		    //  Debug
		
		    if (Debug.EnableInfo && hasDebug())
		      Debug.println("[FTP] FTP session request received from " + sessSock.getInetAddress().getHostAddress());
		      
		    //  Create a server session for the new request, and set the session id.
		
		    FTPSrvSession srvSess = new FTPSrvSession(sessSock, this);
		    srvSess.setSessionId(getNextSessionId());
		    srvSess.setUniqueId("FTP" + srvSess.getSessionId());
		    srvSess.setDebugPrefix("[FTP" + srvSess.getSessionId() + "] ");

			//	Initialize the root path for the new session, if configured
				
			if ( hasRootPath())
				srvSess.setRootPath(getRootPath());
							
			//	Add the session to the active session list
	
			addSession(srvSess);

			//	Inform listeners that a new session has been created
				
			fireSessionOpenEvent(srvSess);
									
		    //  Start the new session in a seperate thread
		
		    Thread srvThread = new Thread(FTPThreadGroup, srvSess);
		    srvThread.setDaemon(true);
		    srvThread.setName("Sess_FTP" + srvSess.getSessionId() + "_" + sessSock.getInetAddress().getHostAddress());
		    srvThread.start();

			//	Sleep for a while
				
			try {
				Thread.sleep(1000L);
			}
			catch (InterruptedException ex) {
			}
      }
    }
    catch (SocketException ex) {

      //	Do not report an error if the server has shutdown, closing the server socket
      //	causes an exception to be thrown.

      if (hasShutdown() == false) {
        Debug.println("[FTP] FTP Socket error : " + ex.toString());
      	Debug.println(ex);
      	
      	//	Inform listeners of the error, store the exception
      	
      	setException(ex);
      	fireServerEvent(ServerListener.ServerError);
      }
    }
    catch (Exception ex) {

      //	Do not report an error if the server has shutdown, closing the server socket
      //	causes an exception to be thrown.

			if ( hasShutdown() == false) {
        Debug.println("[FTP] FTP Server error : " + ex.toString());
        Debug.println(ex);
			}
      	
			//	Inform listeners of the error, store the exception
      	
			setException(ex);
			fireServerEvent(ServerListener.ServerError);
    }

		//	Close the active sessions
    
		Enumeration<Integer> enm = m_sessions.enumerate();
    
		while(enm.hasMoreElements()) {
      
			//	Get the session id and associated session
      
			Integer sessId = enm.nextElement();
			FTPSrvSession sess = m_sessions.findSession(sessId);

			//	DEBUG
				
			if (Debug.EnableInfo && hasDebug())
				Debug.println("[FTP] FTP Close session, id = " + sess.getSessionId());
					
			//	Close the session

			sess.closeSession();			
		}

    //  Debug

    if (Debug.EnableInfo && hasDebug())
      Debug.println("[FTP] FTP Server shutting down ...");
      
    //	Indicate that the server has shutdown, inform listeners
    
    setActive(false);
    fireServerEvent(ServerListener.ServerShutdown);
  }

	/**
	 * Shutdown the FTP server
	 * 
	 * @param immediate boolean
	 */
	public void shutdownServer(boolean immediate) {
		
		//	Set the shutdown flag
		
		setShutdown(true);
		
		//	Close the FTP server listening socket to wakeup the main FTP server thread
		
		try {
			if ( getSocket() != null)
				getSocket().close();
		}
		catch (IOException ex) {
		}

		//	Wait for the main server thread to close
    
		if ( m_srvThread != null) {
    	
			try {
				m_srvThread.join(3000);
			}
			catch (Exception ex) {
			}
		}
    
		// Close the authenticator
		
		m_configSection.closeConfig();
		
		//	Fire a shutdown notification event
    
		fireServerEvent(ServerListener.ServerShutdown);
	}

	/**
	 * Start the FTP server in a seperate thread
	 */
	public void startServer() {
		
		//	Create a seperate thread to run the FTP server
		
		m_srvThread = new Thread(this);
		m_srvThread.setName("FTP Server");
		m_srvThread.start();
		
		//	Fire a server startup event
		
		fireServerEvent(ServerListener.ServerStartup);
	}
	
	/**
	 *	Validate configuration changes that are relevant to the FTP server
	 *
	 * @param id int
	 * @param config ServerConfiguration
	 * @param newVal Object
	 * @return int
	 * @throws InvalidConfigurationException 
	 */
	public int configurationChanged(int id, ServerConfiguration config, Object newVal)
		throws InvalidConfigurationException {

			int sts = StsIgnored;
		
			try {

				//	Check if the configuration change affects the FTP server
			
				switch ( id) {
				
					//	Server enable/disable
				
					case ConfigId.ServerFTPEnable:
				
						//	Check if the server is active

						Boolean enaFTP = (Boolean) newVal;
										
						if ( isActive() && enaFTP.booleanValue() == false) {
						
							//	Shutdown the server
						
							shutdownServer(false);
						}
						else if ( isActive() == false && enaFTP.booleanValue() == true) {
						
							//	Start the server
						
							startServer();
						}
					
						//	Indicate that the setting was accepted
					
						sts = StsAccepted;
						break;
					
					//	Debug enable/disable
					
					case ConfigId.FTPDebugEnable:
						Boolean dbg = (Boolean) newVal;
						setDebug(dbg.booleanValue());
						sts = StsAccepted;
						break;
						
					//	Changes that can be accepted without restart
				
					case ConfigId.FTPAllowAnon:
					case ConfigId.ServerTimezone:
					case ConfigId.ServerTZOffset:
					case ConfigId.ShareList:
					case ConfigId.ShareMapper:
					case ConfigId.SecurityAuthenticator:
					case ConfigId.UsersList:
					case ConfigId.DebugDevice:
						sts = StsAccepted;
						break;

					//	Changes that affect new sessions only
				
					case ConfigId.FTPDebugFlags:
						sts = StsNewSessionsOnly;
						break;
									
					//	Changes that require a restart
				 		
					case ConfigId.FTPBindAddress:
					case ConfigId.FTPPort:
						sts = StsRestartRequired;
						break;
				}
			}
			catch (Exception ex) {
				throw new InvalidConfigurationException("FTP Server configuration error", ex);
			}

			//	Return the status
		
			return sts;
	}
  
  /**
   * Check if the site interface is valid
   * 
   * @return boolean
   */
  public final boolean hasSiteInterface() {
    return m_siteInterface != null ? true : false;
  }
  
  /**
   * Return the site interface
   * 
   * @return FTPSiteInterface
   */
  public final FTPSiteInterface getSiteInterface() {
    return m_siteInterface;
  }
  
  /**
   * Set the site specific commands interface
   * 
   * @param siteInterface FTPSiteInterface
   */
  public final void setSiteInterface( FTPSiteInterface siteInterface) {
    m_siteInterface = siteInterface;
  }
  
  /**
   * Check if the UTF-8 string normalizer is valid
   * 
   * @return boolean
   */
  public final boolean hasUTF8Normalizer() {
    return m_normalizer != null ? true : false;
  }
  
  /**
   * Return the UTF-8 normalizer
   * 
   * @return UTF8Normalizer
   */
  public final UTF8Normalizer getUTF8Normalizer() {
    return m_normalizer;
  }
}
