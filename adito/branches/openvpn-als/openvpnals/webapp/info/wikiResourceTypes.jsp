<%@ page contentType="text/plain;charset=UTF-8" language="java" %><jsp:directive.page import="net.openvpn.als.policyframework.PolicyDatabaseFactory"/><jsp:directive.page import="net.openvpn.als.policyframework.ResourceType"/><jsp:directive.page import="java.util.Iterator"/><jsp:directive.page import="java.util.Collections"/><jsp:directive.page import="java.util.List"/><jsp:directive.page import="org.apache.struts.util.MessageResources"/><jsp:directive.page import="net.openvpn.als.core.CoreUtil"/>
<jsp:directive.page import="net.openvpn.als.boot.SystemProperties"/><%
if(!"true".equals(SystemProperties.get("openvpnals.enableInfoPages", "false"))) {
		throw new Exception("Access denied");
}
	List l = PolicyDatabaseFactory.getInstance().getResourceTypes(null);
	Collections.sort(l);
	String permClass = null;
	for(Iterator i = l.iterator(); i.hasNext(); ) {
		ResourceType resourceType = (ResourceType)i.next();
		MessageResources mr = CoreUtil.getMessageResources(session, resourceType.getBundle());
		if(!resourceType.getPermissionClass().equals(permClass)) {
%>
---+ <%= resourceType.getPermissionClass() %> 

|  *Id*  |  *Name*  |  *Bundle*  |
<%
			permClass = resourceType.getPermissionClass();
		}
%>| <%= resourceType.getResourceTypeId() %> | <%= mr.getMessage("resourceType." + resourceType.getResourceTypeId() + ".title") %> | <%= resourceType.getBundle() %> |
<% 	} %>