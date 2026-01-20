# Storage Migration Guide

This guide explains how to migrate your PerPlayerKit data between different storage backends.

## Supported Storage Types

PerPlayerKit supports the following storage backends:

| Type | Description | Use Case |
|------|-------------|----------|
| `sqlite` | Local SQLite database file | Default, good for single-server setups |
| `mysql` | MySQL/MariaDB database | Multi-server setups, larger deployments |
| `redis` | Redis key-value store | High-performance, distributed setups |
| `yml` | YAML flat file | Development/testing only (not recommended for production) |

## Migration Command

To migrate data between storage types, use the following command:

```
/perplayerkit migrate <source> <destination>
```

**Examples:**
- `/perplayerkit migrate sqlite mysql` - Migrate from SQLite to MySQL
- `/perplayerkit migrate mysql redis` - Migrate from MySQL to Redis
- `/perplayerkit migrate redis sqlite` - Migrate from Redis to SQLite

**Permission:** `perplayerkit.admin`

## Before You Migrate

1. **Backup your data** - Always create a backup before migrating. For SQLite, copy the `database.db` file. For YAML, copy the `please-use-a-real-database.yml` file.

2. **Configure the destination** - Make sure the destination storage is properly configured in your `config.yml` before migrating. For example, if migrating to MySQL, ensure your MySQL credentials are set:

   ```yaml
   mysql:
     host: "localhost"
     port: 3306
     dbname: "perplayerkit"
     username: "your_username"
     password: "your_password"
     useSSL: false
     maximumPoolSize: 10
   ```

   For Redis:
   ```yaml
   redis:
     host: "localhost"
     port: 6379
     password: ""
   ```

3. **Ensure connectivity** - Test that you can connect to the destination database/service.

## Migration Process

1. Run the migration command with the source and destination types.

2. The migration runs asynchronously to avoid server lag. Progress updates will be shown in chat and logged to the console.

3. Wait for the "Migration completed successfully!" message.

4. **Update your config.yml** - Change the `storage.type` setting to your new storage type:
   ```yaml
   storage:
     type: "mysql"  # Change this to your new storage type
   ```

5. **Restart the server** - The plugin needs to restart to use the new storage backend.

## Post-Migration Verification

After restarting:

1. Check the console for any storage connection errors.
2. Test that player kits load correctly.
3. Test that saving kits works.
4. Verify public kits are accessible.

## Troubleshooting

### Migration Failed: Connection Error
- Verify your database credentials in `config.yml`
- Ensure the database server is running and accessible
- Check firewall rules if connecting to a remote database

### Missing Data After Migration
- Check the console logs for any errors during migration
- Verify the migration completed successfully (check for the completion message)
- Ensure you updated `config.yml` to use the new storage type before restarting

### Partial Migration
- If some entries failed to migrate, check the console for specific error messages
- You can run the migration again - existing entries will be overwritten

## Rolling Back

If you need to roll back to your previous storage:

1. Change `storage.type` in `config.yml` back to your original storage type
2. Restart the server

Your original data should still be intact (the migration copies data, it doesn't delete from the source).

## Notes

- The migration process copies data from source to destination. It does not delete data from the source.
- Large datasets may take some time to migrate. Progress updates are provided every 100 entries.
- The server remains functional during migration, but avoid making changes to kits while migrating.
- For very large datasets, consider performing the migration during low-traffic periods.
