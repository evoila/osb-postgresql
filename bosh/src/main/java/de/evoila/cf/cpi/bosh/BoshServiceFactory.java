package de.evoila.cf.cpi.bosh;

import de.evoila.cf.broker.bean.BoshBean;
import de.evoila.cf.broker.cpi.endpoint.EndpointAvailabilityService;
import de.evoila.cf.broker.model.Catalog;
import de.evoila.cf.broker.model.Plan;
import de.evoila.cf.broker.model.ServiceInstance;
import de.evoila.cf.broker.model.cpi.AvailabilityState;
import de.evoila.cf.broker.model.cpi.EndpointServiceState;
import de.evoila.cf.broker.service.CatalogService;
import de.evoila.cf.broker.service.PlatformService;
import de.evoila.cf.cpi.bosh.connection.BoshConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;

/**
 * Created by reneschollmeyer, evoila on 09.10.17.
 */
public abstract class BoshServiceFactory implements PlatformService {

    private final static String BOSH_SERVICE_KEY = ""; //TODO: service key einfügen

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    protected CatalogService catalogService;

    @Autowired
    private BoshBean boshBean;

    @Autowired
    private EndpointAvailabilityService endpointAvailabilityService;
    protected BoshConnection connection;



    @PostConstruct
    private void initialize() {
        log.debug("Initializing Bosh Connection Factory");
        try {
            if (endpointAvailabilityService.isAvailable(BOSH_SERVICE_KEY)) {
                connection = new BoshConnection(boshBean.getUsername(), boshBean.getPassword(), boshBean.getHost()).authenticate();

                endpointAvailabilityService.add(BOSH_SERVICE_KEY,
                        new EndpointServiceState(BOSH_SERVICE_KEY, AvailabilityState.AVAILABLE));
            }
        } catch (Exception ex) {
            endpointAvailabilityService.add(BOSH_SERVICE_KEY,
                    new EndpointServiceState(BOSH_SERVICE_KEY, AvailabilityState.ERROR, ex.toString()));
        }
    }

    @Override
    public boolean isSyncPossibleOnCreate (Plan plan) {
        return false;
    }

    @Override
    public boolean isSyncPossibleOnDelete (ServiceInstance instance) {
        return false;
    }

    @Override
    public boolean isSyncPossibleOnUpdate (ServiceInstance instance, Plan plan) {
        return false;
    }
}
