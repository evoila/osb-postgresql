# Table of Contents
1. [OSB-PostgreSQL](../README.md)
2. [Plan properties](#plan-properties)
---

# Plan properties

Several properties can be set within the field "properties" inside the field "metadata" of a service plan.

## Example

The following JSON shows, how properties can be set. For a full catalog example, see X.
```json
{
    "postgres": {
        "ssl": {
            "enabled": true,
            "max_protocol_version": "TLSv1.2"
        },
        "database": {
            "extensions": [
                "postgis",
                "postgis_topology",
                "fuzzystrmatch",
                "address_standardizer",
                "postgis_tiger_geocoder",
                "pg_trgm"
            ]
        },
        "config": {
            "authentication_timeout": 15,
            "max_locks_per_transaction": 15,
            "max_pred_locks_per_page": 15,
            "max_pred_locks_per_relation": 15,
            "max_pred_locks_per_transaction": 15
        },
        "databases": [
            {
                "name": "TestDB1",
                "users": [
                    "TestUser1",
                    "TestUser2"
                ],
                "extensions": [
                    "postgis",
                    "postgis_topology",
                    "fuzzystrmatch",
                    "address_standardizer",
                    "postgis_tiger_geocoder",
                    "pg _trgm"
                ]
            },
            {
                "name": "TestDB2",
                "users": [
                    "TestUser2",
                    "TestUser3"
                ]
            }
        ],
        "users": [
            {
                "username": "TestUser1",
                "password": "Test123"
            },
            {
                "username": "TestUser2",
                "password": "Test123",
                "admin": true
            },
            {
                "username": "TestUser3",
                "password": "Test123"
            }
        ]
    }
}
```