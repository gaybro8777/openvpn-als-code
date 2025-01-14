/*
 *  OpenVPNALS-PAM
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2 of
 *  the License, or (at your option) any later version.
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public
 *  License along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package net.openvpn.als.pam;

import java.io.File;

import org.jdom.Element;

import net.openvpn.als.core.UserDatabaseManager;
import net.openvpn.als.extensions.ExtensionDescriptor;
import net.openvpn.als.extensions.ExtensionException;
import net.openvpn.als.extensions.types.DefaultPlugin;
import net.openvpn.als.extensions.types.PluginDefinition;
import net.openvpn.als.security.UserDatabaseDefinition;

/**
 * This is the entry for PAM Extension. It allows the use of <a href="http://www.kernel.org/pub/linux/libs/pam/">Linux PAM</a> to authenticate users.
 * The startPlugin method (called by OpenVPNALS extension manager) checks PAM configurations location is readable.
 * Then the method registers PAMUserDatabase to OpenVPNALS UserDatabaseManager or throws an exception if PAM configuration could not be reached. 
 * 
 */
public class PAMCommunityPlugin extends DefaultPlugin {
	
	/**
	 * Default Constructor
	 */
	public PAMCommunityPlugin() {
		super(null, false);

	}
	
	/* (non-Javadoc)
	 * @see net.openvpn.als.extensions.types.Plugin#startPlugin(net.openvpn.als.extensions.types.PluginDefinition, net.openvpn.als.extensions.ExtensionDescriptor, org.jdom.Element)
	 */
	public void startPlugin(PluginDefinition definition, ExtensionDescriptor descriptor, Element element) throws ExtensionException {
		super.startPlugin(definition, descriptor, element);
		
		if(new File("/etc/pam.d").canRead()){
			/* If last parameter of UserDatabaseDefinition specifie where the plugin configuration screens should be displayed :
			 * -1 : displayed in system configuration.
			 * 2010 : displayed during install. This is the container Id set in extension.xml
			 * 
			 */
	        UserDatabaseDefinition databaseDefinition = new UserDatabaseDefinition(PAMUserDatabase.class, "pam", "pam", 2010);
	        UserDatabaseManager.getInstance().registerDatabase(databaseDefinition);
		
		} else {
			throw new ExtensionException(ExtensionException.FAILED_TO_LAUNCH,"PAM not currently supported on this platform or PAM not installed");
		}
		
    }

}
