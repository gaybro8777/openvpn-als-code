
				/*
 *  OpenVPNALS
 *
 *  Copyright (C) 2003-2006 3SP LTD. All Rights Reserved
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
			
package net.openvpn.als.security.itemactions;

import net.openvpn.als.boot.Util;
import net.openvpn.als.core.UserDatabaseManager;
import net.openvpn.als.policyframework.Permission;
import net.openvpn.als.policyframework.PolicyConstants;
import net.openvpn.als.policyframework.PolicyDatabaseFactory;
import net.openvpn.als.policyframework.ResourceType;
import net.openvpn.als.policyframework.itemactions.AbstractPathAction;
import net.openvpn.als.security.SessionInfo;
import net.openvpn.als.security.User;
import net.openvpn.als.security.UserDatabase;
import net.openvpn.als.security.UserItem;
import net.openvpn.als.table.AvailableTableItemAction;

/**
 * Class which provides the functionality for editing a user.
 */
public class EditAccountTableAction extends AbstractPathAction {
    private final boolean requiresPasswordSupport;
    private final boolean requiresAccountCreationSupport;

    /**
     * @param actionName 
     * @param weight 
     * @param important 
     * @param requiredPath
     */
    public EditAccountTableAction(String actionName, int weight, boolean important, String requiredPath) {
        this(actionName, weight, important, requiredPath, false, false);
    }

    /**
     * @param actionName 
     * @param messageResourcesKey 
     * @param weight 
     * @param important 
     * @param requiredPath
     */
    public EditAccountTableAction(String actionName, String messageResourcesKey, int weight, boolean important, String requiredPath) {
        this(actionName, messageResourcesKey, weight, important, requiredPath, false, false);
    }
    
    /**
     * @param actionName
     * @param weight
     * @param important
     * @param requiredPath
     * @param requiresPasswordSupport
     * @param requiresAccountCreationSupport
     */
    public EditAccountTableAction(String actionName, int weight, boolean important, String requiredPath, boolean requiresPasswordSupport, boolean requiresAccountCreationSupport) {
        this(actionName, "security", weight, important, requiredPath, requiresPasswordSupport, requiresAccountCreationSupport);
    }
    
    /**
     * @param actionName
     * @param messageResourcesKey 
     * @param weight
     * @param important
     * @param requiredPath
     * @param requiresPasswordSupport
     * @param requiresAccountCreationSupport
     */
    public EditAccountTableAction(String actionName, String messageResourcesKey, int weight, boolean important, String requiredPath, boolean requiresPasswordSupport, boolean requiresAccountCreationSupport) {
        super(actionName, messageResourcesKey, weight, important, SessionInfo.MANAGEMENT_CONSOLE_CONTEXT, requiredPath);
        this.requiresPasswordSupport = requiresPasswordSupport;
        this.requiresAccountCreationSupport = requiresAccountCreationSupport;
    }

    /*
     * (non-Javadoc)
     * @see net.openvpn.als.table.TableItemAction#getPath(net.openvpn.als.table.AvailableTableItemAction)
     */
    @Override
    public String getPath(AvailableTableItemAction availableItem) {
        UserItem item = (UserItem) availableItem.getRowItem();
        String principalName = item.getUser().getPrincipalName();
        String encodedPrincipalName = Util.urlEncode(principalName);
        return getPath(encodedPrincipalName, availableItem);
    }

    /*
     * (non-Javadoc)
     * @see net.openvpn.als.table.TableItemAction#isEnabled(net.openvpn.als.table.AvailableTableItemAction)
     */
    public boolean isEnabled(AvailableTableItemAction availableItem) {
        try {
            User user = availableItem.getSessionInfo().getUser();
            UserItem item = (UserItem) availableItem.getRowItem();
            return isPermitted(user) && isEnabled(item);
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean isPermitted(User user) throws Exception {
        ResourceType resourceType = PolicyDatabaseFactory.getInstance().getResourceType(PolicyConstants.ACCOUNTS_AND_GROUPS_RESOURCE_TYPE_ID);
        Permission[] permissions = getPermissions(resourceType);
        boolean isPermitted = permissions.length == 0 ? true : PolicyDatabaseFactory.getInstance().isPermitted(resourceType, permissions, user, true);
        
        UserDatabase userDatabase = UserDatabaseManager.getInstance().getUserDatabase(user.getRealm().getResourceId());
        if(requiresPasswordSupport) {
            isPermitted = isPermitted && userDatabase.supportsPasswordChange();
        }
        if(requiresAccountCreationSupport) {
            isPermitted = isPermitted && userDatabase.supportsAccountCreation();
        }
        return isPermitted;
    }
    
    /**
     * @param resourceType
     * @return Permission[]
     */
    public Permission[] getPermissions(ResourceType resourceType) {
        return new Permission[] {resourceType.getPermission(PolicyConstants.PERM_CREATE_AND_ASSIGN_ID)};
    }
    
    /**
     * @param userItem
     * @return boolean
     * @throws Exception 
     */
    public boolean isEnabled(UserItem userItem) throws Exception {
        return true;
    }
}