package com.axamit.aop.osgi.bundle;

import com.axamit.aop.osgi.weaver.Weaver;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.hooks.weaving.WeavingHook;
import org.osgi.framework.hooks.weaving.WovenClassListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

/**
 * The main purpose of this bundle activator is to register a WeavingHook
 */
public class Activator implements BundleActivator {

    private static final Logger LOGGER = LoggerFactory.getLogger(Activator.class);

    private List<ServiceRegistration> serviceRegistrations = new ArrayList<>();

    private List<ServiceRegistration> getServiceRegistrations() {
        return serviceRegistrations;
    }

    private void registerWeavingHook(BundleContext context, WeavingHook weavingHook) {
        String bundleName = context.getBundle().getSymbolicName();
        LOGGER.info("Registering WeavingHook in bundle {} {}", bundleName);
        getServiceRegistrations().add(
            context.registerService(WeavingHook.class, weavingHook, new Hashtable<>())
        );

        getServiceRegistrations().add(
            context.registerService(WovenClassListener.class, (WovenClassListener)weavingHook, new Hashtable<>())
        );

        LOGGER.info("WeavingHook is registered in bundle {} {}", bundleName);
    }

    @Override
    public void start(BundleContext context) throws Exception {
        String bundleName = context.getBundle().getSymbolicName();
        LOGGER.info("Starting bundle {}", bundleName);
        registerWeavingHook(context, new Weaver(context));
        LOGGER.info("Bundle {} is started", bundleName);
    }

    @Override
    public void stop(BundleContext context) throws Exception {

        String bundleName = context.getBundle().getSymbolicName();
        LOGGER.info("Stopping bundle {}", bundleName);

        LOGGER.info("Unregistering WeavingHook in bundle {}", bundleName);
        for(ServiceRegistration registration : getServiceRegistrations()) {
            registration.unregister();
        }
        getServiceRegistrations().clear();
        LOGGER.info("WeavingHook in bundle {} is unregistered", bundleName);

        LOGGER.info("Bundle {} is stopped", bundleName);
    }

}
