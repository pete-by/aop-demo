package com.axamit.aop.osgi.weaver;

import org.aspectj.weaver.bcel.BcelWeakClassLoaderReference;
import org.aspectj.weaver.loadtime.ClassLoaderWeavingAdaptor;
import org.aspectj.weaver.loadtime.IWeavingContext;
import org.aspectj.weaver.tools.GeneratedClassHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OsgiClassLoaderWeavingAdaptor extends ClassLoaderWeavingAdaptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(OsgiClassLoaderWeavingAdaptor.class);

    private ClassLoaderHelper classLoaderHelper;

    public OsgiClassLoaderWeavingAdaptor() {
        super();
        classLoaderHelper = new ClassLoaderHelper();
    }

    @Override
    public void initialize(ClassLoader classLoader /* target bundle's classloader, not used */, IWeavingContext context) {
        super.initialize(context.getClassLoader(), context);

        // We need to add Closure ($AjcClosure1) classes to bundle where woven class resides, but we do not do this immediately
        // we rather use our classloader and define class later
        this.generatedClassHandler = new SimpleGeneratedClassHandler(context.getClassLoader());
    }

    class SimpleGeneratedClassHandler implements GeneratedClassHandler {

        private BcelWeakClassLoaderReference loaderRef;

        SimpleGeneratedClassHandler(ClassLoader loader) {
            loaderRef = new BcelWeakClassLoaderReference(loader);
        }

        /**
         * Callback when we need to define a Closure in the JVM
         */
        public void acceptClass (String name, byte[] originalBytes, byte[] wovenBytes) {

           /*
             We could define the closure class directly in target bundle classloader, but unfortunately dynamic imports
             are not necessarily ready and this may cause ClassNotFoundException for AspectJ classes.
             So we do it later after woven class is DEFINED and imports are added
             */
            /*
            ClassLoader classLoader = loaderRef.getClassLoader();
            classLoaderHelper.defineClass(classLoader, name, wovenBytes, activeProtectionDomain);
            */
            OsgiWeavingClassLoader classLoader = (OsgiWeavingClassLoader)loaderRef.getClassLoader();
            classLoader.acceptClass(name, originalBytes, wovenBytes); // just save for future use

        }

    }


}
