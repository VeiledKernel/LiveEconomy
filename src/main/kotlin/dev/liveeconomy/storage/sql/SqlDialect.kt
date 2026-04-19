package dev.liveeconomy.storage.sql

/**
 * SQL dialect abstraction for provider-specific syntax differences.
 *
 * Upsert syntax is the primary divergence between SQLite and MySQL:
 *  - SQLite: `INSERT ... ON CONFLICT(col) DO UPDATE SET col=excluded.col`
 *  - MySQL:  `INSERT ... ON DUPLICATE KEY UPDATE col=VALUES(col)`
 *
 * Each [SqlStorageProvider] subclass provides its dialect.
 * SQL store implementations call these methods instead of hardcoding syntax.
 */
interface SqlDialect {

    /** UPSERT for a single-column primary key table. */
    fun upsert(table: String, pkCol: String, vararg dataCols: String): String

    /** UPSERT for a two-column composite primary key table. */
    fun upsert2pk(table: String, pk1: String, pk2: String, vararg dataCols: String): String
}

/**
 * SQLite dialect — uses `ON CONFLICT ... DO UPDATE SET ... = excluded.*`
 */
object SqliteDialect : SqlDialect {

    override fun upsert(table: String, pkCol: String, vararg dataCols: String): String {
        val cols    = (listOf(pkCol) + dataCols).joinToString(",")
        val params  = List(1 + dataCols.size) { "?" }.joinToString(",")
        val updates = dataCols.joinToString(",") { "$it=excluded.$it" }
        return "INSERT INTO $table($cols) VALUES($params) ON CONFLICT($pkCol) DO UPDATE SET $updates"
    }

    override fun upsert2pk(table: String, pk1: String, pk2: String, vararg dataCols: String): String {
        val cols    = (listOf(pk1, pk2) + dataCols).joinToString(",")
        val params  = List(2 + dataCols.size) { "?" }.joinToString(",")
        val updates = dataCols.joinToString(",") { "$it=excluded.$it" }
        return "INSERT INTO $table($cols) VALUES($params) ON CONFLICT($pk1,$pk2) DO UPDATE SET $updates"
    }
}

/**
 * MySQL dialect — uses `INSERT ... ON DUPLICATE KEY UPDATE col=VALUES(col)`
 */
object MysqlDialect : SqlDialect {

    override fun upsert(table: String, pkCol: String, vararg dataCols: String): String {
        val cols    = (listOf(pkCol) + dataCols).joinToString(",")
        val params  = List(1 + dataCols.size) { "?" }.joinToString(",")
        val updates = dataCols.joinToString(",") { "$it=VALUES($it)" }
        return "INSERT INTO $table($cols) VALUES($params) ON DUPLICATE KEY UPDATE $updates"
    }

    override fun upsert2pk(table: String, pk1: String, pk2: String, vararg dataCols: String): String {
        val cols    = (listOf(pk1, pk2) + dataCols).joinToString(",")
        val params  = List(2 + dataCols.size) { "?" }.joinToString(",")
        val updates = dataCols.joinToString(",") { "$it=VALUES($it)" }
        return "INSERT INTO $table($cols) VALUES($params) ON DUPLICATE KEY UPDATE $updates"
    }
}
