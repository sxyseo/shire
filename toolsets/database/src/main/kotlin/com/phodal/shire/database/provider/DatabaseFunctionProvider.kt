package com.phodal.shire.database.provider

import com.intellij.database.model.DasTable
import com.intellij.database.model.RawDataSource
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.phodal.shire.database.DatabaseSchemaAssistant
import com.phodal.shirecore.provider.function.ToolchainFunctionProvider

enum class DatabaseFunction(val funName: String) {
    Database("database"),
    Table("table"),
    Column("column");

    companion object {
        fun fromString(value: String): DatabaseFunction? {
            return values().firstOrNull { it.funName == value }
        }
    }
}

class DatabaseFunctionProvider : ToolchainFunctionProvider {
    override fun isApplicable(project: Project, funcName: String): Boolean {
        return DatabaseFunction.values().any { it.funName == funcName }
    }

    override fun execute(
        project: Project,
        funcName: String,
        args: List<Any>,
        allVariables: Map<String, Any?>,
    ): Any {
        val databaseFunction =
            DatabaseFunction.fromString(funcName) ?: throw IllegalArgumentException("Invalid Database function name")
        when (databaseFunction) {
            DatabaseFunction.Database -> {
                if (args.isEmpty()) {
                    val dataSource = DatabaseSchemaAssistant.getAllRawDatasource(project).firstOrNull()
                        ?: return "ShireError: No database found"
                    return DatabaseSchemaAssistant.getTableByDataSource(dataSource)
                }

                val database = DatabaseSchemaAssistant.getDatabase(project, args[0] as String)
                    ?: return "ShireError: Database not found"
                return DatabaseSchemaAssistant.getTableByDataSource(database)
            }

            DatabaseFunction.Table -> {
                if (args.isEmpty()) {
                    val allTables = DatabaseSchemaAssistant.getAllTables(project)
                    return allTables.map {
                        DatabaseSchemaAssistant.getTableColumn(it)
                    }
                }


                when (args[0]) {
                    is RawDataSource -> {
                        return if (args.size == 1) {
                            DatabaseSchemaAssistant.getTableByDataSource(args[0] as RawDataSource)
                        } else {
                            DatabaseSchemaAssistant.getTable(args[0] as RawDataSource, args[1] as String)
                        }
                    }

                    is DasTable -> {
                        return DatabaseSchemaAssistant.getTableColumn(args[0] as DasTable)
                    }

                    is ArrayList<*> -> {
                        return when (val first = args[0]) {
                            is RawDataSource -> {
                                DatabaseSchemaAssistant.getTableByDataSource(first)
                            }

                            else -> {
                                "ShireError: Table function requires a data source or a list of table names"
                            }
                        }
                    }

                    else -> {
                        /// logger type
                        logger<DatabaseFunctionProvider>().error("args types: ${args.map { it::class }}")
                        return "ShireError: Table function requires a data source or a list of table names"
                    }
                }
            }

            DatabaseFunction.Column -> {
                if (args.size < 2) {
                    logger<DatabaseFunctionProvider>().error("Column function requires a table name")
                    return "ShireError: Column function requires a table name"
                }

                val table = args[0] as? DasTable
                if (table == null) {
                    logger<DatabaseFunctionProvider>().error("Column function requires a table")
                    return "ShireError: Column function requires a table"
                }

                return DatabaseSchemaAssistant.getTableColumn(table)
            }
        }
    }
}