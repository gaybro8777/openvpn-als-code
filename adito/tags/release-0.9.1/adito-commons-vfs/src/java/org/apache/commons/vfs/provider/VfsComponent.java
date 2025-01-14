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
package org.apache.commons.vfs.provider;

import org.apache.commons.logging.Log;
import org.apache.commons.vfs.FileSystemException;

/**
 * This interface is used to manage the lifecycle of all VFS components.
 * This includes all implementations of the following interfaces:
 * <ul>
 * <li>{@link FileProvider}
 * <li>{@link org.apache.commons.vfs.FileSystem}
 * <li>{@link FileReplicator}
 * <li>{@link TemporaryFileStore}
 * </ul>
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 */
public interface VfsComponent
{
    /**
     * Sets the Logger to use for the component.
     *
     * @param logger
     */
    void setLogger(Log logger);

    /**
     * Sets the context for the component.
     *
     * @param context The context.
     */
    void setContext(VfsComponentContext context);

    /**
     * Initialises the component.
     */
    void init() throws FileSystemException;

    /**
     * Closes the component.
     */
    void close();
}
