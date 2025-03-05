package mobi.sevenwinds.app.author

import io.restassured.RestAssured
import io.restassured.http.ContentType
import mobi.sevenwinds.common.ServerTest
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
        val requestBody = "{\"fullName\": \"ilYa\"}"

        /*        val objectMapper = ObjectMapper().apply {
                    registerModule(JodaModule())
                    findAndRegisterModules()
                }*/

        RestAssured.given()
            .contentType(ContentType.JSON)
            .body(requestBody)
            .`when`()
            .post("/author/add")
            .then()
            .assertThat()
            .statusCode(200)
            .extract().body().`as`(AuthorRecord::class.java).let { author ->
                println(author)

                Assert.assertEquals("ilYa", author.fullName)
                Assert.assertNotNull(author.createdAt)
            }
    }

}