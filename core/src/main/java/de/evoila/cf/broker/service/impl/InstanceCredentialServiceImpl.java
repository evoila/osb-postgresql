package de.evoila.cf.broker.service.impl;

import de.evoila.cf.broker.exception.ServiceInstanceDoesNotExistException;
import de.evoila.cf.broker.model.ServiceInstance;
import de.evoila.cf.broker.repository.ServiceInstanceRepository;
import de.evoila.cf.broker.service.BackupTypeService;
import de.evoila.cf.broker.service.InstanceCredentialService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;

@Service
public class InstanceCredentialServiceImpl implements InstanceCredentialService {
    @Autowired
    BackupTypeService backupTypeService;
    @Autowired
    ServiceInstanceRepository repository;

    @Override
    public HashMap getCredentialsForInstanceId (String serviceInstanceId) throws ServiceInstanceDoesNotExistException{
        ServiceInstance instance = repository.getServiceInstance(serviceInstanceId);
        if(instance == null || instance.getHosts().size() <= 0){
            throw new ServiceInstanceDoesNotExistException(serviceInstanceId);
        }
        HashMap<String, Object> credentials = new HashMap<>();
        credentials.put("username", serviceInstanceId);
        credentials.put("password", serviceInstanceId);
        credentials.put("hostname", instance.getHosts().get(0).getIp());
        credentials.put("port", instance.getHosts().get(0).getPort());
        credentials.put("context", serviceInstanceId);
        credentials.put("type", backupTypeService.getType());
        return credentials;
    }
}
