/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.jcr.jcrinstall.impl;

import java.lang.reflect.Array;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.sling.osgi.installer.InstallableResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Converts configuration nodes to InstallableData */
class ConfigNodeConverter implements JcrInstaller.NodeConverter {

	public static final String CONFIG_NODE_TYPE = "sling:OsgiConfig";
	private final Logger log = LoggerFactory.getLogger(getClass());

	/** Convert n to an InstallableData, or return null
	 * 	if we don't know how to convert it.
	 */
	/**
	 * @see org.apache.sling.jcr.jcrinstall.impl.JcrInstaller.NodeConverter#convertNode(java.lang.String, javax.jcr.Node, int)
	 */
	public InstallableResource convertNode(String urlScheme, Node n, final int priority) throws Exception {
		InstallableResource result = null;

		// We only consider CONFIG_NODE_TYPE nodes
		if(n.isNodeType(CONFIG_NODE_TYPE)) {
			result = new InstallableResource(urlScheme + ":" + n.getPath(), load(n), priority);
			log.debug("Converted node {} to {}", n.getPath(), result);
		} else {
			log.debug("Node is not a {} node, ignored:{}", CONFIG_NODE_TYPE, n.getPath());
		}
		return result;
	}

    /** Load config from node n */
    protected Dictionary<String, Object> load(Node n) throws RepositoryException {
        Dictionary<String, Object> result = new Hashtable<String, Object>();

        log.debug("Loading config from Node {}", n.getPath());

        // load default values from node itself
        log.debug("Loading {} properties", n.getPath());
        loadProperties(result, n);

        return result;
    }

    /** Load properties of n into d */
    protected void loadProperties(Dictionary<String, Object> d, Node n) throws RepositoryException {
        final PropertyIterator pi = n.getProperties();
        while(pi.hasNext()) {
            final Property p = pi.nextProperty();
            final String name = p.getName();

            // ignore jcr: and similar properties
            if(name.contains(":")) {
                continue;
            }
            if(p.getDefinition().isMultiple()) {
                Object [] data = null;
                final Value [] values = p.getValues();
                int i = 0;
                for(Value v : values) {
                    Object o = convertValue(v);
                    if(i == 0) {
                        data = (Object[])Array.newInstance(o.getClass(), values.length);
                    }
                    data[i++] = o;
                }
                d.put(name, data);

            } else {
                final Object o = convertValue(p.getValue());
                if(o != null) {
                    d.put(name, o);
                }
            }
        }
    }

    /** Convert v according to its type */
    protected Object convertValue(Value v) throws RepositoryException {
        switch(v.getType()) {
        case PropertyType.STRING:
            return v.getString();
        case PropertyType.DATE:
            return v.getDate();
        case PropertyType.DOUBLE:
            return v.getDouble();
        case PropertyType.LONG:
            return v.getLong();
        case PropertyType.BOOLEAN:
            return v.getBoolean();
        }
        log.debug("Value of type {} ignored", v.getType());
        return null;
    }
}