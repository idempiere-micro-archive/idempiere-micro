package software.hsharp.core.util

import software.hsharp.core.models.IEMail
import javax.mail.internet.InternetAddress
import org.apache.commons.mail.DefaultAuthenticator
import org.apache.commons.mail.HtmlEmail

open class EMail(
    override var SmtpHost: String,
    override var SmtpPort: Int,
    override var From: InternetAddress,
    override var ReplyTo: InternetAddress,
    override var Subject: String,
    override var UserName: String,
    override var Password: String,
    override var MessageHTML: String
) : IEMail {

    companion object {
        fun convert(s: String, email: EMail): InternetAddress? {
            try {
                return InternetAddress(s, true)
            } catch (e: Exception) {
                email.m_valid = false
                return null
            }
        }
    }

    protected fun convert(s: String): InternetAddress? {
        return EMail.convert(s, this)
    }

    protected var m_valid = false

    protected var m_to: MutableList<InternetAddress> = mutableListOf()
    /** CC Addresses				 */
    protected var m_cc: MutableList<InternetAddress> = mutableListOf()
    /** BCC Addresses				 */
    protected var m_bcc: MutableList<InternetAddress> = mutableListOf()

    override fun isValid(): Boolean {
        return m_valid
    }

    override fun addTo(newTo: String): Boolean {
        val ia = convert(newTo)
        if (ia == null) {
            return false
        } else {
            this.m_to.add(ia)
            return true
        }
    }

    override fun addCc(newCc: String): Boolean {
        val ia = convert(newCc)
        if (ia == null) {
            return false
        } else {
            this.m_cc.add(ia)
            return true
        }
    }

    override fun addBcc(newBcc: String): Boolean {
        val ia = convert(newBcc)
        if (ia == null) {
            return false
        } else {
            this.m_bcc.add(ia)
            return true
        }
    }

    override fun send(): String {
        val email = HtmlEmail()
        email.hostName = SmtpHost
        email.setSmtpPort(SmtpPort)
        email.setAuthenticator(DefaultAuthenticator(UserName, Password))
        email.isSSLOnConnect = true
        email.setFrom(From.address)
        m_to.forEach { email.addTo(it.address) }
        email.subject = Subject
        // val kotlinLogoURL = URL("https://kotlinlang.org/assets/images/twitter-card/kotlin_800x320.png")
        // val cid = email.embed(kotlinLogoURL, "Kotlin logo")
        email.setHtmlMsg(MessageHTML)
        email.send()

        return "Success"
    }
}