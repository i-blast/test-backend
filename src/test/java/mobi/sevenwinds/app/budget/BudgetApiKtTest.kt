package mobi.sevenwinds.app.budget

import io.restassured.RestAssured
import mobi.sevenwinds.app.author.AuthorEntity
import mobi.sevenwinds.app.author.AuthorTable
import mobi.sevenwinds.app.common.toJodaDateTime
import mobi.sevenwinds.common.ServerTest
import mobi.sevenwinds.common.jsonBody
import mobi.sevenwinds.common.toResponse
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import org.junit.Assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class BudgetApiKtTest : ServerTest() {

    @BeforeEach
    internal fun setUp() {
        transaction {
            AuthorTable.deleteAll()
            BudgetTable.deleteAll()
        }
    }

    @Test
    fun testBudgetPagination() {
        addRecord(BudgetRecord(2020, 5, 10, BudgetType.Приход))
        addRecord(BudgetRecord(2020, 5, 5, BudgetType.Приход))
        addRecord(BudgetRecord(2020, 5, 20, BudgetType.Приход))
        addRecord(BudgetRecord(2020, 5, 30, BudgetType.Приход))
        addRecord(BudgetRecord(2020, 5, 40, BudgetType.Приход))
        addRecord(BudgetRecord(2030, 1, 1, BudgetType.Расход))

        RestAssured.given()
            .queryParam("limit", 3)
            .queryParam("offset", 1)
            .get("/budget/year/2020/stats")
            .toResponse<BudgetYearStatsResponse>().let { response ->
                println("${response.total} / ${response.items} / ${response.totalByType}")

                Assert.assertEquals(5, response.total)
                Assert.assertEquals(3, response.items.size)
                Assert.assertEquals(105, response.totalByType[BudgetType.Приход.name])
            }
    }

    @Test
    fun testStatsSortOrder() {
        addRecord(BudgetRecord(2020, 5, 100, BudgetType.Приход))
        addRecord(BudgetRecord(2020, 1, 5, BudgetType.Приход))
        addRecord(BudgetRecord(2020, 5, 50, BudgetType.Приход))
        addRecord(BudgetRecord(2020, 1, 30, BudgetType.Приход))
        addRecord(BudgetRecord(2020, 5, 400, BudgetType.Приход))

        // expected sort order - month ascending, amount descending

        RestAssured.given()
            .get("/budget/year/2020/stats?limit=100&offset=0")
            .toResponse<BudgetYearStatsResponse>().let { response ->
                println(response.items)

                Assert.assertEquals(30, response.items[0].amount)
                Assert.assertEquals(5, response.items[1].amount)
                Assert.assertEquals(400, response.items[2].amount)
                Assert.assertEquals(100, response.items[3].amount)
                Assert.assertEquals(50, response.items[4].amount)
            }
    }

    @Test
    fun testGetStatsWithAuthorFields() {
        val authorEntity = createAuthor()
        addRecord(BudgetRecord(2023, 5, 100, BudgetType.Приход, authorEntity.id.value))

        RestAssured.given()
            .get("/budget/year/2023/stats?limit=100&offset=0")
            .toResponse<BudgetYearStatsResponse>().let { response ->
                assertThat(response.items).isNotEmpty
                assertThat(response.items[0].authorFullName).isEqualTo(authorEntity.fullName)
                assertThat(response.items[0].authorCreatedAt?.toJodaDateTime()).isEqualTo(authorEntity.createdAt)
            }
    }

    @Test
    fun testStatsFilterByAuthor() {
        val author1 = createAuthor("author1")
        val author2 = createAuthor("author2")
        addRecord(BudgetRecord(2023, 5, 100, BudgetType.Приход, author1.id.value))
        addRecord(BudgetRecord(2023, 5, 100, BudgetType.Приход, author2.id.value))
        addRecord(BudgetRecord(2023, 6, 100, BudgetType.Приход, author1.id.value))

        RestAssured.given()
            .get("/budget/year/2023/stats?limit=100&offset=0&authorName=author1")
            .toResponse<BudgetYearStatsResponse>().let { response ->
                assertThat(response.items).isNotEmpty
                assertThat(response.items).size().isEqualTo(2)
            }
    }

    @Test
    fun testStatsFilterByEmptyAuthor() {
        val author1 = createAuthor("author1")
        val author2 = createAuthor("author2")
        addRecord(BudgetRecord(2023, 5, 100, BudgetType.Приход, author1.id.value))
        addRecord(BudgetRecord(2023, 5, 100, BudgetType.Приход, author2.id.value))
        addRecord(BudgetRecord(2023, 6, 100, BudgetType.Приход, author1.id.value))

        RestAssured.given()
            .get("/budget/year/2023/stats?limit=100&offset=0")
            .toResponse<BudgetYearStatsResponse>().let { response ->
                assertThat(response.items).isNotEmpty
                assertThat(response.items).size().isEqualTo(3)
            }
    }

    @Test
    fun testInvalidMonthValues() {
        RestAssured.given()
            .jsonBody(BudgetRecord(2020, -5, 5, BudgetType.Приход))
            .post("/budget/add")
            .then().statusCode(400)

        RestAssured.given()
            .jsonBody(BudgetRecord(2020, 15, 5, BudgetType.Приход))
            .post("/budget/add")
            .then().statusCode(400)
    }

    @Test
    fun testCreateBudget() {
        RestAssured.given()
            .jsonBody(BudgetRecord(2025, 1, 5, BudgetType.Приход))
            .`when`()
            .post("/budget/add")
            .then()
            .assertThat()
            .statusCode(200)

        val authorEntity = createAuthor()
        RestAssured.given()
            .jsonBody(BudgetRecord(2025, 1, 5, BudgetType.Приход, authorEntity.id.value))
            .`when`()
            .post("/budget/add")
            .then()
            .assertThat()
            .statusCode(200)
            .extract().body().toResponse<BudgetRecord>().let { budget ->
                println(budget)

                Assert.assertNotNull(budget.author)
            }
    }

    private fun createAuthor(fullName: String = "ilYa"): AuthorEntity = transaction {
        val entity = AuthorEntity.new {
            this.fullName = fullName
            this.createdAt = DateTime.now()
        }
        return@transaction entity
    }

    private fun addRecord(record: BudgetRecord) {
        RestAssured.given()
            .jsonBody(record)
            .post("/budget/add")
            .toResponse<BudgetRecord>().let { response ->
                Assert.assertEquals(record, response)
            }
    }
}