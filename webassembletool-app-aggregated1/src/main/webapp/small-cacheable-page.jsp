<%@page contentType="text/plain; charset=UTF-8" import="java.net.URLDecoder"%><%
	if(request.getHeader("If-modified-since")!=null)
		response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
	else {
		// Cacheable page that should not be parsed as it is not html
		Thread.sleep(100);
		response.addDateHeader("Last-modified", System.currentTimeMillis());
		// Set lots of headers to check for concurrent access on headers collection
		for(int i= 0; i<100;i++)
			response.addHeader("dummy" + i, "dummy");
		%>This page is cacheable.<%
	}
%>
