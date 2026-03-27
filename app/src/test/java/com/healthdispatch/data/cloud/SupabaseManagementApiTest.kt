package com.healthdispatch.data.cloud

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SupabaseManagementApiTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private fun createClient(handler: suspend (io.ktor.client.engine.mock.MockRequestData) -> io.ktor.client.engine.mock.MockRequestHandleScope.() -> io.ktor.http.HttpResponseData): Pair<HttpClient, MockEngine> {
        val engine = MockEngine { request ->
            handler(request).invoke(this)
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) { json(json) }
        }
        return client to engine
    }

    private fun jsonHeaders() = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    @Test
    fun `listProjects returns list of projects on success`() = runTest {
        val (client, _) = createClient { _ ->
            {
                respond(
                    content = """[{"id":"proj-1","name":"My Project","organization_id":"org-1","region":"us-east-1","created_at":"2026-01-01T00:00:00Z","status":"ACTIVE_HEALTHY"}]""",
                    status = HttpStatusCode.OK,
                    headers = jsonHeaders()
                )
            }
        }

        val api = SupabaseManagementApiImpl(client, json)
        val result = api.listProjects("fake-access-token")

        assertTrue(result.isSuccess)
        val projects = result.getOrThrow()
        assertEquals(1, projects.size)
        assertEquals("proj-1", projects[0].id)
        assertEquals("My Project", projects[0].name)
    }

    @Test
    fun `listProjects returns failure on HTTP error`() = runTest {
        val (client, _) = createClient { _ ->
            {
                respond(
                    content = """{"message":"Unauthorized"}""",
                    status = HttpStatusCode.Unauthorized,
                    headers = jsonHeaders()
                )
            }
        }

        val api = SupabaseManagementApiImpl(client, json)
        val result = api.listProjects("bad-token")

        assertTrue(result.isFailure)
    }

    @Test
    fun `createProject sends correct payload and returns project`() = runTest {
        var capturedBody = ""
        val (client, engine) = createClient { request ->
            capturedBody = request.body.toByteArray().decodeToString()
            {
                respond(
                    content = """{"id":"new-proj","name":"HealthDispatch-user1","organization_id":"org-1","region":"us-east-1","created_at":"2026-01-01T00:00:00Z","status":"COMING_UP"}""",
                    status = HttpStatusCode.Created,
                    headers = jsonHeaders()
                )
            }
        }

        val api = SupabaseManagementApiImpl(client, json)
        val result = api.createProject(
            accessToken = "fake-token",
            name = "HealthDispatch-user1",
            organizationId = "org-1",
            dbPassword = "secure-pass-123",
            region = "us-east-1"
        )

        assertTrue(result.isSuccess)
        val project = result.getOrThrow()
        assertEquals("new-proj", project.id)
        assertEquals("HealthDispatch-user1", project.name)
        assertTrue(capturedBody.contains("HealthDispatch-user1"))
        assertTrue(capturedBody.contains("secure-pass-123"))
    }

    @Test
    fun `createProject returns failure on error`() = runTest {
        val (client, _) = createClient { _ ->
            {
                respond(
                    content = """{"message":"Project limit reached"}""",
                    status = HttpStatusCode.Forbidden,
                    headers = jsonHeaders()
                )
            }
        }

        val api = SupabaseManagementApiImpl(client, json)
        val result = api.createProject("token", "name", "org", "pass", "us-east-1")

        assertTrue(result.isFailure)
    }

    @Test
    fun `getApiKeys returns keys on success`() = runTest {
        val (client, _) = createClient { _ ->
            {
                respond(
                    content = """[{"name":"anon","api_key":"anon-key-123"},{"name":"service_role","api_key":"service-key-456"}]""",
                    status = HttpStatusCode.OK,
                    headers = jsonHeaders()
                )
            }
        }

        val api = SupabaseManagementApiImpl(client, json)
        val result = api.getApiKeys("fake-token", "proj-1")

        assertTrue(result.isSuccess)
        val keys = result.getOrThrow()
        assertEquals(2, keys.size)
        assertEquals("anon", keys[0].name)
        assertEquals("anon-key-123", keys[0].apiKey)
    }

    @Test
    fun `getApiKeys returns failure on error`() = runTest {
        val (client, _) = createClient { _ ->
            {
                respond(
                    content = """{"message":"Not found"}""",
                    status = HttpStatusCode.NotFound,
                    headers = jsonHeaders()
                )
            }
        }

        val api = SupabaseManagementApiImpl(client, json)
        val result = api.getApiKeys("token", "bad-ref")

        assertTrue(result.isFailure)
    }

    @Test
    fun `runSql executes SQL on success`() = runTest {
        val (client, _) = createClient { _ ->
            {
                respond(
                    content = """[{"command":"CREATE TABLE"}]""",
                    status = HttpStatusCode.Created,
                    headers = jsonHeaders()
                )
            }
        }

        val api = SupabaseManagementApiImpl(client, json)
        val result = api.runSql("fake-token", "proj-1", "CREATE TABLE test (id int);")

        assertTrue(result.isSuccess)
    }

    @Test
    fun `runSql returns failure on error`() = runTest {
        val (client, _) = createClient { _ ->
            {
                respond(
                    content = """{"message":"SQL error"}""",
                    status = HttpStatusCode.BadRequest,
                    headers = jsonHeaders()
                )
            }
        }

        val api = SupabaseManagementApiImpl(client, json)
        val result = api.runSql("token", "proj-1", "INVALID SQL")

        assertTrue(result.isFailure)
    }

    @Test
    fun `getOrganizations returns organizations on success`() = runTest {
        val (client, _) = createClient { _ ->
            {
                respond(
                    content = """[{"id":"org-1","name":"My Org"}]""",
                    status = HttpStatusCode.OK,
                    headers = jsonHeaders()
                )
            }
        }

        val api = SupabaseManagementApiImpl(client, json)
        val result = api.getOrganizations("fake-token")

        assertTrue(result.isSuccess)
        val orgs = result.getOrThrow()
        assertEquals(1, orgs.size)
        assertEquals("org-1", orgs[0].id)
    }

    @Test
    fun `listProjects sends authorization header correctly`() = runTest {
        var capturedAuthHeader = ""
        val (client, _) = createClient { request ->
            capturedAuthHeader = request.headers["Authorization"] ?: ""
            {
                respond(
                    content = "[]",
                    status = HttpStatusCode.OK,
                    headers = jsonHeaders()
                )
            }
        }

        val api = SupabaseManagementApiImpl(client, json)
        api.listProjects("my-access-token")

        assertEquals("Bearer my-access-token", capturedAuthHeader)
    }

    @Test
    fun `createProject sends authorization header correctly`() = runTest {
        var capturedAuthHeader = ""
        val (client, _) = createClient { request ->
            capturedAuthHeader = request.headers["Authorization"] ?: ""
            {
                respond(
                    content = """{"id":"p","name":"n","organization_id":"o","region":"r","created_at":"2026-01-01T00:00:00Z","status":"COMING_UP"}""",
                    status = HttpStatusCode.Created,
                    headers = jsonHeaders()
                )
            }
        }

        val api = SupabaseManagementApiImpl(client, json)
        api.createProject("my-token", "n", "o", "p", "r")

        assertEquals("Bearer my-token", capturedAuthHeader)
    }
}
