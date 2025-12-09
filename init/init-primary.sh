#!/bin/bash
set -e

# This script runs when the primary DB is first created.

# 1. Create the replication user with a password
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE USER replicator WITH REPLICATION ENCRYPTED PASSWORD 'replicator_password';
EOSQL

# 2. Add a rule to pg_hba.conf to allow the replicator user to connect.
#    Note: We specify the user 'replicator' here for better security.
echo "host replication replicator all md5" >> "$PGDATA/pg_hba.conf"