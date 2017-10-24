package com.axamit.aop.osgi.weaver;

import com.axamit.aop.osgi.weaver.utils.ClassUtils;
import org.aspectj.weaver.bcel.BcelWeakClassLoaderReference;
import org.aspectj.weaver.loadtime.IWeavingContext;
import org.aspectj.weaver.loadtime.definition.Definition;
import org.aspectj.weaver.loadtime.definition.DocumentParser;
import org.aspectj.weaver.tools.WeavingAdaptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class OsgiWeavingContext implements IWeavingContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(OsgiWeavingContext.class);

    /** We cannot use META-INF/aop.xml in OSGi */
    private final static String AOP_XML = "org/aspectj/aop.xml";

    protected BcelWeakClassLoaderReference loaderRef;

    private String shortName;

    public OsgiWeavingContext(OsgiWeavingClassLoader osgiWeavingClassLoader) {
        super();
        loaderRef = new BcelWeakClassLoaderReference( osgiWeavingClassLoader );
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        return getClassLoader().getResources(name);
    }

    @Override
    public String getBundleIdFromURL(URL url) {
        // TODO: rewrite if CompositeClassLoader works
        return String.valueOf( getClassLoader().getBundle().getBundleId() );
    }

    @Override
    public String getClassLoaderName() {
        ClassLoader loader = getClassLoader();
        return ((loader != null) ? loader.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(loader))
                : "null");
    }

    @Override
    public OsgiWeavingClassLoader getClassLoader() {
        return (OsgiWeavingClassLoader)loaderRef.getClassLoader();
    }

    @Override
    public String getFile(URL url) {
        return url.getFile();
    }

    @Override
    public String getId() {
        if (shortName == null) {
            shortName = getClassLoaderName().replace('$', '.');
            int index = shortName.lastIndexOf(".");
            if (index != -1) {
                shortName = shortName.substring(index + 1);
            }
        }
        return shortName;
    }

    @Override
    public boolean isLocallyDefined(String classname) {
        String asResource = ClassUtils.classNameToPath(classname);
        ClassLoader loader = getClassLoader();
        URL localURL = loader.getResource(asResource);
        if (localURL == null) {
            return false;
        }

        boolean isLocallyDefined = true;

        // Should this part present?
        ClassLoader parent = loader.getParent();
        if (parent != null) {
            URL parentURL = parent.getResource(asResource);
            if (localURL.equals(parentURL)) {
                isLocallyDefined = false;
            }
        }
        return isLocallyDefined;
    }

    @Override
    public List getDefinitions(ClassLoader loader, WeavingAdaptor adaptor) {

        List<Definition> definitions = new ArrayList<>();
        try {

            String resourcePath = System.getProperty("org.aspectj.weaver.loadtime.configuration", AOP_XML);
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("parseDefinitions {}", resourcePath);
            }

            StringTokenizer st = new StringTokenizer(resourcePath, ";");

            while (st.hasMoreTokens()) {
                String nextDefinition = st.nextToken();
                if (nextDefinition.startsWith("file:")) {
                    try {
                        String fpath = new URL(nextDefinition).getFile();
                        File configFile = new File(fpath);
                        if (!configFile.exists()) {
                            LOGGER.warn("configuration does not exist: {} ", nextDefinition);
                        } else {
                            definitions.add(DocumentParser.parse(configFile.toURI().toURL()));
                        }
                    } catch (MalformedURLException mue) {
                        LOGGER.error("malformed definition url: {}", nextDefinition);
                    }
                } else {
                    Enumeration<URL> xmls = getResources(nextDefinition);

                    Set<URL> seenBefore = new HashSet<>();
                    while (xmls.hasMoreElements()) {
                        URL xml = xmls.nextElement();
                        if (LOGGER.isTraceEnabled()) {
                            LOGGER.trace("parseDefinitions {}", xml);
                        }
                        if (!seenBefore.contains(xml)) {
                            LOGGER.info("using configuration ", getFile(xml));
                            definitions.add(DocumentParser.parse(xml));
                            seenBefore.add(xml);
                        } else {
                            LOGGER.debug("ignoring duplicate definition: ", xml);
                        }
                    }
                }
            }
            if (definitions.isEmpty()) {
                LOGGER.info("no configuration found. Check aspect bundles symbolic naming. Disabling weaver for class loader {}", getClassLoaderName());
            }
        } catch (Exception e) {
            definitions.clear();
            LOGGER.warn("parse definitions failed", e);
        }

        return definitions;
    }

}
