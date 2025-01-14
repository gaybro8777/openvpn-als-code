
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
			
package net.openvpn.als.webforwards.webforwardwizard.actions;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import net.openvpn.als.policyframework.PolicyUtil;
import net.openvpn.als.webforwards.WebForward;
import net.openvpn.als.webforwards.webforwardwizard.forms.WebForwardPersonalPolicyForm;
import net.openvpn.als.webforwards.webforwardwizard.forms.WebForwardTypeSelectionForm;
import net.openvpn.als.wizard.actions.AbstractWizardPolicySelectionAction;
import net.openvpn.als.wizard.forms.AbstractWizardForm;

/**
 * This provides a personal policy.
 * Implementation of {@link net.openvpn.als.wizard.actions.AbstractWizardAction}.
 */
public class WebForwardPersonalPolicyAction extends AbstractWizardPolicySelectionAction {

    /**
     * Constructor
     */
    public WebForwardPersonalPolicyAction() {
        super();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.struts.actions.DispatchAction#unspecified(org.apache.struts.action.ActionMapping,
     *      org.apache.struts.action.ActionForm,
     *      javax.servlet.http.HttpServletRequest,
     *      javax.servlet.http.HttpServletResponse)
     */
    public ActionForward unspecified(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                    HttpServletResponse response) throws Exception {
        WebForwardPersonalPolicyForm webForwardPersonalPolicyForm = (WebForwardPersonalPolicyForm)form;
        webForwardPersonalPolicyForm.setPersonalPolicyName(PolicyUtil.getPersonalPolicyName(getSessionInfo(request).getUser().getPrincipalName()));
        
        return super.unspecified(mapping, form, request, response);
    }
    
    /* (non-Javadoc)
     * @see net.openvpn.als.wizard.actions.AbstractWizardAction#next(org.apache.struts.action.ActionMapping, org.apache.struts.action.ActionForm, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    public ActionForward previous(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
        int type = ((Integer) getWizardSequence(request).getAttribute(WebForwardTypeSelectionForm.ATTR_TYPE, new Integer(0))).intValue();
        if(type == WebForward.TYPE_TUNNELED_SITE) {
            applyToSequence(mapping, (AbstractWizardForm) form, request, response);
            return mapping.findForward("previousSkipAuthentication");
        }
        return super.previous(mapping, form, request, response);
    }
}
