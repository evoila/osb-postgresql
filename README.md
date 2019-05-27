# Service Broker
This repository is part of our service broker project. For documentation see [evoila/cf-service-broker](https://github.com/evoila/cf-service-broker)

# cf-service-broker-Postgres
Cloud Foundry Service Broker providing Postgres Service Instances. Supports deployment to OpenStack and Existing Postgres servers. Configuration files and deployment scripts must be added. 

Restore Tests:
pg_restore -U beuidgjdxi -c -d DD69H8D3-7876-4056-A0EE-DE8D3498259T -v ~/Downloads/190509_prod_1_0_dump.tar -W -h 10.245.0.3