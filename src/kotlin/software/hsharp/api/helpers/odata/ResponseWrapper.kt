package software.hsharp.api.helpers.odata

import javax.ws.rs.core.Response
import javax.ws.rs.core.MultivaluedMap
import javax.ws.rs.core.EntityTag
import javax.ws.rs.core.NewCookie
import java.util.Locale
import javax.ws.rs.core.GenericType
import java.net.URI
import java.util.*
import javax.ws.rs.core.*
import javax.ws.rs.core.Link.Builder

abstract class ResponseWrapper(val delegate: Response) : Response() {

    protected fun headers(headers: Map<String, List<Any>>?, responseBuilder: Response.ResponseBuilder): Response.ResponseBuilder {
        if (headers != null) {
            for ((key, value1) in headers) {
                for (value in value1) {
                    responseBuilder.header(key, value)
                }
            }
        }

        return responseBuilder
    }

    override fun hashCode(): Int {
        return delegate.hashCode()
    }

    override fun getStatus(): Int {
        return delegate.status
    }

    override fun getStatusInfo(): Response.StatusType {
        return delegate.statusInfo
    }

    override fun equals(other: Any?): Boolean {
        return delegate.equals(other)
    }

    override fun getEntity(): Any {
        return delegate.entity
    }

    override fun <T> readEntity(entityType: Class<T>): T {
        return delegate.readEntity(entityType)
    }

    override fun <T> readEntity(entityType: GenericType<T>): T {
        return delegate.readEntity(entityType)
    }

    override fun <T> readEntity(entityType: Class<T>, annotations: Array<Annotation>): T {
        return delegate.readEntity(entityType, annotations)
    }

    override fun toString(): String {
        return delegate.toString()
    }

    override fun <T> readEntity(entityType: GenericType<T>, annotations: Array<Annotation>): T {
        return delegate.readEntity(entityType, annotations)
    }

    override fun hasEntity(): Boolean {
        return delegate.hasEntity()
    }

    override fun bufferEntity(): Boolean {
        return delegate.bufferEntity()
    }

    override fun close() {
        delegate.close()
    }

    override fun getMediaType(): MediaType {
        return delegate.mediaType
    }

    override fun getLanguage(): Locale {
        return delegate.language
    }

    override fun getLength(): Int {
        return delegate.length
    }

    override fun getAllowedMethods(): Set<String> {
        return delegate.allowedMethods
    }

    override fun getCookies(): Map<String, NewCookie> {
        return delegate.cookies
    }

    override fun getEntityTag(): EntityTag {
        return delegate.entityTag
    }

    override fun getDate(): Date {
        return delegate.date
    }

    override fun getLastModified(): Date {
        return delegate.lastModified
    }

    override fun getLocation(): URI {
        return delegate.location
    }

    override fun getLinks(): Set<Link> {
        return delegate.links
    }

    override fun hasLink(relation: String): Boolean {
        return delegate.hasLink(relation)
    }

    override fun getLink(relation: String): Link {
        return delegate.getLink(relation)
    }

    override fun getLinkBuilder(relation: String): Builder {
        return delegate.getLinkBuilder(relation)
    }

    override fun getMetadata(): MultivaluedMap<String, Any> {
        return delegate.metadata
    }

    override fun getHeaders(): MultivaluedMap<String, Any> {
        return delegate.headers
    }

    override fun getStringHeaders(): MultivaluedMap<String, String> {
        return delegate.stringHeaders
    }

    override fun getHeaderString(name: String): String {
        return delegate.getHeaderString(name)
    }
}