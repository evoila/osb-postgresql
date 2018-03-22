package de.evoila.cf.broker.custom.postgres;

import de.evoila.cf.broker.service.BackupTypeService;
import de.evoila.cf.model.enums.DatabaseType;
import org.springframework.stereotype.Service;

@Service
public class PostgreSQLBackupTypeService implements BackupTypeService {

    @Override
    public DatabaseType getType () {
        return DatabaseType.PostgreSQL;
    }
}
