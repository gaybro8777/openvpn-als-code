/*
 * Copyright 2002-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.vfs.provider.http;

import org.apache.commons.vfs.FileSystemConfigBuilder;
import org.apache.commons.vfs.FileSystemOptions;

/**
 * Configuration options for HTTP
 * 
 * @author <a href="mailto:imario@apache.org">Mario Ivankovits</a>
 */
public class HttpFileSystemConfigBuilder extends FileSystemConfigBuilder
{
    private final static HttpFileSystemConfigBuilder builder = new HttpFileSystemConfigBuilder();

    public static HttpFileSystemConfigBuilder getInstance()
    {
        return builder;
    }

    private HttpFileSystemConfigBuilder()
    {
    }

    /**
     * Set the charset used for url encoding<br>
     *
     * @param chaset the chaset
     */
    public void setUrlCharset(FileSystemOptions opts, String chaset)
    {
        setParam(opts, "urlCharset", chaset);
    }

    /**
     * Set the charset used for url encoding<br>
     *
     * @return the chaset
     */
    public String getUrlCharset(FileSystemOptions opts)
    {
        return (String) getParam(opts, "urlCharset");
    }

    /**
     * Set the proxy to use for http connection.<br>
     * You have to set the ProxyPort too if you would like to have the proxy relly used.
     *
     * @param proxyHost the host
     * @see #setProxyPort
     */
    public void setProxyHost(FileSystemOptions opts, String proxyHost)
    {
        setParam(opts, "proxyHost", proxyHost);
    }

    /**
     * Set the proxy-port to use for http connection
     * You have to set the ProxyHost too if you would like to have the proxy relly used.
     *
     * @param proxyPort the port
     * @see #setProxyHost
     */
    public void setProxyPort(FileSystemOptions opts, int proxyPort)
    {
        setParam(opts, "proxyPort", new Integer(proxyPort));
    }

    /**
     * Get the proxy to use for http connection
     * You have to set the ProxyPort too if you would like to have the proxy relly used.
     *
     * @return proxyHost
     * @see #setProxyPort
     */
    public String getProxyHost(FileSystemOptions opts)
    {
        return (String) getParam(opts, "proxyHost");
    }

    /**
     * Get the proxy-port to use for http the connection
     * You have to set the ProxyHost too if you would like to have the proxy relly used.
     *
     * @return proxyPort: the port number or 0 if it is not set
     * @see #setProxyHost
     */
    public int getProxyPort(FileSystemOptions opts)
    {
        if (!hasParam(opts, "proxyPort"))
        {
            return 0;
        }

        return ((Number) getParam(opts, "proxyPort")).intValue();
    }

    protected Class getConfigClass()
    {
        return HttpFileSystem.class;
    }
}
