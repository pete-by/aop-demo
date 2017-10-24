package com.axamit.aop.target.hidden.impl;

import com.axamit.aop.target.exported.ConsumerService;
import com.axamit.aop.target.exported.TargetService;
import org.apache.felix.scr.annotations.*;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@Service
@Component(immediate = true)
public class ConsumerServiceImpl implements ConsumerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsumerService.class);

    @Reference
    private TargetService targetService;

    @Activate
    void activate(ComponentContext componentContext,
                  BundleContext bundleContext,
                  Map<String, Object> config) {
        LOGGER.info("Activated");
        modified(config);
    }

    @Deactivate
    void deactivate(ComponentContext componentContext,
                    BundleContext bundleContext,
                    Map<String, Object> config) {
        LOGGER.info("Deactivated");
    }

    @Modified
    void modified(Map<String,Object> config) {
        LOGGER.info("Modified");
    }

    @Override
    public void updateTitle(String title) {

        PrivateClass privateClass = new PrivateClass();
        privateClass.foo(new PojoArg(title), 42, 3.141569); // java.lang.NoClassDefFoundError: com/axamit/aop/demo/hidden/impl/PrivateClass$AjcClosure1 may be thrown (workaround is a aspectj bundle dep)

    }

}
