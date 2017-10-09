package de.evoila.cf.cpi.bosh;

import de.evoila.cf.broker.bean.BoshBean;
import de.evoila.cf.broker.cpi.endpoint.EndpointAvailabilityService;
import de.evoila.cf.broker.model.cpi.AvailabilityState;
import de.evoila.cf.broker.model.cpi.EndpointServiceState;
import de.evoila.cf.broker.service.PlatformService;
import de.evoila.cf.cpi.bosh.connection.BoshConnectionFactory;
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

    private String host;

    private String username;

    private String password;

    @Autowired
    private BoshBean boshBean;

    @Autowired
    private EndpointAvailabilityService endpointAvailabilityService;

    @PostConstruct
    private void initialize() {
        host = boshBean.getHost();
        username = boshBean.getUsername();
        password = boshBean.getPassword();

        log.debug("Initializing Bosh Connection Factory");

        try {
            if (endpointAvailabilityService.isAvailable(BOSH_SERVICE_KEY)) {
                BoshConnectionFactory.getInstance().setCredentials(username, password).authenticate(host);

                endpointAvailabilityService.add(BOSH_SERVICE_KEY,
                        new EndpointServiceState(BOSH_SERVICE_KEY, AvailabilityState.AVAILABLE));
            }
        } catch (Exception ex) {
            endpointAvailabilityService.add(BOSH_SERVICE_KEY,
                    new EndpointServiceState(BOSH_SERVICE_KEY, AvailabilityState.ERROR, ex.toString()));
        }
    }
}
