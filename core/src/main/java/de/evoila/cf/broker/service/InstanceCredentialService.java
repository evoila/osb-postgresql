package de.evoila.cf.broker.service;

import de.evoila.cf.broker.exception.ServiceInstanceDoesNotExistException;

import java.util.HashMap;

public interface InstanceCredentialService {
    HashMap getCredentialsForInstanceId (String serviceInstanceId) throws ServiceInstanceDoesNotExistException;
}
