package mobi.sevenwinds.app.budget

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mobi.sevenwinds.app.author.AuthorTable
import mobi.sevenwinds.app.common.toJavaLocalDateTime
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

object BudgetService {
    suspend fun addRecord(body: BudgetRecord): BudgetRecord = withContext(Dispatchers.IO) {
        transaction {
            val entity = BudgetEntity.new {
                this.year = body.year
                this.month = body.month
                this.amount = body.amount
                this.type = body.type
                this.author = body.author?.let { EntityID(it, AuthorTable) }
            }

            return@transaction entity.toResponse()
        }
    }

    suspend fun getYearStats(param: BudgetYearParam): BudgetYearStatsResponse = withContext(Dispatchers.IO) {
        transaction {
            val queryTotalByYear = (BudgetTable leftJoin AuthorTable)
                .slice(
                    BudgetTable.year,
                    BudgetTable.month,
                    BudgetTable.amount,
                    BudgetTable.type,
                    AuthorTable.fullName,
                    AuthorTable.createdAt,
                )
                .select { BudgetTable.year eq param.year }
            val total = queryTotalByYear.count()

            val sumByType = queryTotalByYear
                .map {
                    it[BudgetTable.type].name to it[BudgetTable.amount]
                }
                .groupBy { it.first }
                .mapValues { it.value.sumOf { v -> v.second } }

            val queryPaginated = queryTotalByYear
                .orderBy(BudgetTable.month to SortOrder.ASC, BudgetTable.amount to SortOrder.DESC)
                .limit(param.limit, param.offset)

            val data = queryPaginated.map {
                BudgetStatsView(
                    year = it[BudgetTable.year],
                    month = it[BudgetTable.month],
                    amount = it[BudgetTable.amount],
                    type = it[BudgetTable.type],
                    authorFullName = it[AuthorTable.fullName],
                    authorCreatedAt = it[AuthorTable.createdAt]?.toJavaLocalDateTime(),
                )
            }

            return@transaction BudgetYearStatsResponse(
                total = total,
                totalByType = sumByType,
                items = data
            )
        }
    }
}