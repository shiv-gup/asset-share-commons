/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2020 Adobe
 * %%
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
 * #L%
 */

/* Copied from: https://github.com/Adobe-Consulting-Services/acs-aem-commons/blob/master/bundle/src/main/java/com/adobe/acs/commons/util/impl/RequireAemImpl.java */
package com.adobe.aem.commons.assetshare.util.impl;

import com.adobe.aem.commons.assetshare.util.RequireAem;
import com.adobe.granite.license.ProductInfo;
import com.adobe.granite.license.ProductInfoProvider;
import org.apache.commons.lang3.StringUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.Version;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.Hashtable;

@Component(
        immediate = true,
        service = {},
        property = {
                "service=" + RequireAemImpl.PUBLISH_SERVICE_VALUE
        }
)
@Designate(ocd = RequireAemImpl.Config.class)
public class RequireAemImpl implements RequireAem {
    private static final Logger log = LoggerFactory.getLogger(RequireAemImpl.class);

    static final String PN_DISTRIBUTION = "distribution";
    static final String PN_VERSION = "version";

    protected static final String PUBLISH_SERVICE_VALUE = "publish";

    // This is the first Major/Minor GA Version of AEM as a Cloud Service
    private static final Version originalCloudServiceVersion = new Version(2019, 12,   0);

    @Reference
    private ProductInfoProvider productInfoProvider;

    private ProductInfo productInfo;
    private ServiceRegistration<?> serviceRegistration;

    private RequireAemImpl.Config config;

    @ObjectClassDefinition(
            name = "Asset Share Commons - AEM Service",
            description = "Describes the AEM Service being operated on."
    )
    @interface Config {
        @AttributeDefinition(
                name = "Service name",
                description = "Defines the which AEN service the application is running under. Allowed values are: author or publish. Defaults to: publish."
        )
        String service() default PUBLISH_SERVICE_VALUE;
    }

    @Override
    public Distribution getDistribution() {
        if (productInfo.getVersion().compareTo(originalCloudServiceVersion) > 0) {
            return Distribution.CLOUD_READY;
        } else {
            return Distribution.CLASSIC;
        }
    }

    @Override
    public ServiceType getServiceType() {
        if (StringUtils.equalsIgnoreCase(PUBLISH_SERVICE_VALUE, config.service())) {
            return ServiceType.PUBLISH;
        } else {
            return ServiceType.AUTHOR;
        }
    }

    @Activate
    protected void activate(final RequireAemImpl.Config config, final BundleContext bundleContext) {
        this.config = config;

        productInfo = productInfoProvider.getProductInfo();

        @SuppressWarnings("squid:java:S1149")
        final Dictionary<String, Object> properties = new Hashtable<>();

        String distribution;
        String version = productInfo.getShortVersion();

        if (Distribution.CLOUD_READY.equals(getDistribution())) {
            distribution = Distribution.CLOUD_READY.getValue();
        } else {
            distribution =  Distribution.CLASSIC.getValue();
        }

        properties.put(PN_DISTRIBUTION, distribution);
        properties.put(PN_VERSION, version);

        serviceRegistration = bundleContext.registerService(RequireAem.class.getName(), this, properties);

        log.info("Registering [ RequireAem.class ] as an OSGi Service with OSGi properties [ distribution = {}, version = {} ] so it can be used to enable/disable other OSGi Components",
                properties.get(PN_DISTRIBUTION), properties.get(PN_VERSION));
    }

    @Deactivate
    protected void deactivate() {
        productInfo = null;

        if (serviceRegistration != null) {
            serviceRegistration.unregister();
            serviceRegistration = null;
        }
    }
}