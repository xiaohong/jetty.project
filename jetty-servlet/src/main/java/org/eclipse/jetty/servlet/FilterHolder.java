// ========================================================================
// Copyright (c) 1996-2009 Mort Bay Consulting Pty. Ltd.
// ------------------------------------------------------------------------
// All rights reserved. This program and the accompanying materials
// are made available under the terms of the Eclipse Public License v1.0
// and Apache License v2.0 which accompanies this distribution.
// The Eclipse Public License is available at 
// http://www.eclipse.org/legal/epl-v10.html
// The Apache License v2.0 is available at
// http://www.opensource.org/licenses/apache2.0.php
// You may elect to redistribute this code under either of these licenses. 
// ========================================================================

package org.eclipse.jetty.servlet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.FilterRegistration;

import org.eclipse.jetty.util.log.Log;

/* --------------------------------------------------------------------- */
/** 
 * 
 */
public class FilterHolder extends Holder
{    
    /* ------------------------------------------------------------ */
    private transient Filter _filter;
    private transient Config _config;
        
    /* ---------------------------------------------------------------- */
    /** Constructor for Serialization.
     */
    public FilterHolder()
    {
    }   
    
    /* ---------------------------------------------------------------- */
    /** Constructor for Serialization.
     */
    public FilterHolder(Class filter)
    {
        super (filter);
    }

    /* ---------------------------------------------------------------- */
    /** Constructor for existing filter.
     */
    public FilterHolder(Filter filter)
    {
        setFilter(filter);
    }
    
    /* ------------------------------------------------------------ */
    public void doStart()
        throws Exception
    {
        super.doStart();
        
        if (!javax.servlet.Filter.class
            .isAssignableFrom(_class))
        {
            String msg = _class+" is not a javax.servlet.Filter";
            super.stop();
            throw new IllegalStateException(msg);
        }

        if (_filter==null)
            _filter=(Filter)newInstance();
        
        _filter = getServletHandler().customizeFilter(_filter);
        
        _config=new Config();
        _filter.init(_config);
    }

    /* ------------------------------------------------------------ */
    public void doStop()
    {      
        if (_filter!=null)
        {
            try
            {
                destroyInstance(_filter);
            }
            catch (Exception e)
            {
                Log.warn(e);
            }
        }
        if (!_extInstance)
            _filter=null;
        
        _config=null;
        super.doStop();   
    }

    /* ------------------------------------------------------------ */
    public void destroyInstance (Object o)
    throws Exception
    {
        if (o==null)
            return;
        Filter f = (Filter)o;
        f.destroy();
        getServletHandler().customizeFilterDestroy(f);
    }

    /* ------------------------------------------------------------ */
    public synchronized void setFilter(Filter filter)
    {
        _filter=filter;
        _extInstance=true;
        setHeldClass(filter.getClass());
        if (getName()==null)
            setName(filter.getClass().getName());
    }
    
    /* ------------------------------------------------------------ */
    public Filter getFilter()
    {
        return _filter;
    }

    /* ------------------------------------------------------------ */
    public String toString()
    {
        return getName();
    }
    

    public FilterRegistration.Dynamic getRegistration()
    {
        return new Registration();
    }
    
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    protected class Registration extends HolderRegistration implements FilterRegistration.Dynamic
    {
        public void addMappingForServletNames(EnumSet<DispatcherType> dispatcherTypes, boolean isMatchAfter, String... servletNames)
        {
            illegalStateIfContextStarted();
            FilterMapping mapping = new FilterMapping();
            mapping.setFilterHolder(FilterHolder.this);
            mapping.setServletNames(servletNames);
            mapping.setDispatcherTypes(dispatcherTypes);
            if (isMatchAfter)
                _servletHandler.addFilterMapping(mapping);
            else
                _servletHandler.prependFilterMapping(mapping);
        }

        public void addMappingForUrlPatterns(EnumSet<DispatcherType> dispatcherTypes, boolean isMatchAfter, String... urlPatterns)
        {
            illegalStateIfContextStarted();
            FilterMapping mapping = new FilterMapping();
            mapping.setFilterHolder(FilterHolder.this);
            mapping.setPathSpecs(urlPatterns);
            mapping.setDispatcherTypes(dispatcherTypes);
            if (isMatchAfter)
                _servletHandler.addFilterMapping(mapping);
            else
                _servletHandler.prependFilterMapping(mapping);
        }

        public Iterable<String> getServletNameMappings()
        {
            FilterMapping[] mappings =_servletHandler.getFilterMappings();
            List<String> names=new ArrayList<String>();
            for (FilterMapping mapping : mappings)
            {
                if (mapping.getFilterHolder()!=FilterHolder.this)
                    continue;
                String[] servlets=mapping.getServletNames();
                if (servlets!=null && servlets.length>0)
                    names.addAll(Arrays.asList(servlets));
            }
            return names;
        }

        public Iterable<String> getUrlPatternMappings()
        {
            FilterMapping[] mappings =_servletHandler.getFilterMappings();
            List<String> patterns=new ArrayList<String>();
            for (FilterMapping mapping : mappings)
            {
                if (mapping.getFilterHolder()!=FilterHolder.this)
                    continue;
                String[] specs=mapping.getPathSpecs();
                if (specs!=null && specs.length>0)
                    patterns.addAll(Arrays.asList(specs));
            }
            return patterns;
        }
    }

    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    class Config extends HolderConfig implements FilterConfig
    {
        /* ------------------------------------------------------------ */
        public String getFilterName()
        {
            return _name;
        }
    }
}





