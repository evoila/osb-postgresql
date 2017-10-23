package de.evoila.cf.broker.service;

import de.evoila.cf.broker.exception.PlatformException;
import de.evoila.cf.broker.model.Plan;
import de.evoila.cf.broker.model.ServiceInstance;

import java.util.Map;

public abstract class PlatformServiceAdapter implements PlatformService {

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

    @Override
    public ServiceInstance postProvisioning (ServiceInstance serviceInstance, Plan plan) throws PlatformException {
        return serviceInstance;
    }

    @Override
    public void preDeprovisionServiceInstance (ServiceInstance serviceInstance) {
        return;
    }

    @Override
    public ServiceInstance getCreateInstancePromise (ServiceInstance instance, Plan plan) {
        return new ServiceInstance(instance, null, null);
    }

    @Override
    public ServiceInstance updateInstance (ServiceInstance instance, Plan plan) throws PlatformException {
        throw new PlatformException("Could not update Service Instance. Operation is not supported");
    }
}
