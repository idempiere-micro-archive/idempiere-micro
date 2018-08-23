import org.junit.Ignore
import org.junit.Test
import software.hsharp.core.util.EMail
import javax.mail.internet.InternetAddress

class TestMail {
    @Test
    fun sendTestEmail() {
        val email = EMail(
            SmtpHost = host,
            SmtpPort = port,
            From = InternetAddress(userName),
            ReplyTo = InternetAddress(userName),
            Subject = "Test email",
            UserName = userName,
            Password = password,
            MessageHTML = "<p>iDempiere Micro Testing Email</p>"
        )

        email.addTo(to)
        email.send()
    }
}
