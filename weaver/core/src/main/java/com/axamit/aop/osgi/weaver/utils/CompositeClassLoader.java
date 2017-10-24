package com.axamit.aop.osgi.weaver.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public class CompositeClassLoader extends ClassLoader {

    private final List<ClassLoader> classLoaders = Collections.synchronizedList(new ArrayList<>());

    public CompositeClassLoader() {
        add(Object.class.getClassLoader()); // bootstrap loader.
        add(getClass().getClassLoader()); // whichever classloader loaded this jar.
    }

    /**
     * Add a loader to the list
     * @param classLoader classloader
     */
    public CompositeClassLoader add(ClassLoader classLoader) {
        if (classLoader != null) {
            classLoaders.add(0, classLoader);
        }

        return this;
    }

    public CompositeClassLoader addAll(List<ClassLoader> classLoaders) {
        for(ClassLoader classLoader : classLoaders) {
            add(classLoader);
        }
        return this;
    }

    @Override
    public Class loadClass(String name) throws ClassNotFoundException {

        for (ClassLoader classLoader : classLoaders) {
            try {
                return classLoader.loadClass(name);
            } catch (ClassNotFoundException notFound) {
                // ok.. try another one
            }
        }
        throw new ClassNotFoundException(name);
    }

    @Override
    public URL getResource(String name) {
        for(ClassLoader classLoader : classLoaders) {
            URL url = classLoader.getResource(name);
            if(url != null) {
                return url;
            }
        }
        return super.getResource(name);
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {

        List<Enumeration<URL>> enumerations = new ArrayList<>();
        for(ClassLoader classLoader : classLoaders) {
            Enumeration<URL> urls = classLoader.getResources(name);
            if(urls.hasMoreElements()) {
                enumerations.add(urls);
            }
        }

        Enumeration<URL>[] enumerationsArray = enumerations.toArray( new Enumeration[] {} );
        CompoundEnumeration<URL> compoundEnumeration =
                new CompoundEnumeration<>( enumerationsArray );

        return compoundEnumeration;
    }

    @Override
    protected URL findResource(String name) {

        for(ClassLoader classLoader : classLoaders) {
            if(classLoader instanceof URLClassLoader) {
                URL url = ((URLClassLoader)classLoader).findResource(name);
                if(url != null) {
                    return url;
                }
            }
        }

        return super.findResource(name);
    }

    @Override
    protected Enumeration<URL> findResources(String name) throws IOException {
        return super.findResources(name);
    }

    @Override
    public InputStream getResourceAsStream(String name) {

        for(ClassLoader classLoader : classLoaders) {
            InputStream is = classLoader.getResourceAsStream(name);
            if(is != null) {
                return is;
            }
        }

        return super.getResourceAsStream(name);
    }
}
