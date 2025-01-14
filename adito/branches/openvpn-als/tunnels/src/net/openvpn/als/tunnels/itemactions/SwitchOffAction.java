
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
			
package net.openvpn.als.tunnels.itemactions;

import net.openvpn.als.core.CoreUtil;
import net.openvpn.als.security.SessionInfo;
import net.openvpn.als.table.AvailableTableItemAction;
import net.openvpn.als.table.TableItemAction;
import net.openvpn.als.tunnels.forms.TunnelItem;

/**
 * <i>Table Item Action</i> that stops a tunnel
 */
public final class SwitchOffAction extends TableItemAction {

    public static final String TABLE_ITEM_ACTION_ID = "switchOff";

    /**
     * Constructor.
     * 
     */
    public SwitchOffAction() {
        super(TABLE_ITEM_ACTION_ID, "tunnels", 400, false, SessionInfo.ALL_CONTEXTS);
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.openvpn.als.table.TableItemAction#isEnabled(net.openvpn.als.table.AvailableTableItemAction)
     */
    public boolean isEnabled(AvailableTableItemAction availableItem) {
        TunnelItem item = (TunnelItem) availableItem.getRowItem();
        return item.getOpen().equals("true");
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.openvpn.als.table.TableItemAction#getPath(net.openvpn.als.table.AvailableTableItemAction)
     */
    public String getPath(AvailableTableItemAction availableItem) {
        TunnelItem item = (TunnelItem) availableItem.getRowItem();
        return item.getCloseLink(CoreUtil.getReferer(availableItem.getRequest()), availableItem.getRequest());
    }
}