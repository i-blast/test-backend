package mobi.sevenwinds.app.author

import io.restassured.RestAssured
import mobi.sevenwinds.common.ServerTest
import mobi.sevenwinds.common.jsonBody
import mobi.sevenwinds.common.toResponse
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.Assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AuthorApiKtTest : ServerTest() {
    @BeforeEach
    internal fun setUp() {
        transaction { AuthorTable.deleteAll() }
    }

    @Test
    fun testCreateAuthor() {
        RestAssured.given()
            .jsonBody(CreateAuthorRequest("ilYa"))
            .`when`()
            .post("/author/add")
            .then()
            .assertThat()
            .statusCode(200)
            .extract().body().toResponse<AuthorRecord>().let { author ->
                println(author)

                Assert.assertEquals("ilYa", author.fullName)
                Assert.assertNotNull(author.createdAt)
            }
    }

}