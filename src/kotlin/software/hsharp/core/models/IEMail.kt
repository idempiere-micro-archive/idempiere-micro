package software.hsharp.core.models

import javax.mail.internet.InternetAddress

interface IEMail {
    var SmtpHost: String
    var SmtpPort: Int
    var From: InternetAddress
    fun addTo(newTo: String): Boolean
    fun addCc(newCc: String): Boolean
    fun addBcc(newBcc: String): Boolean
    var ReplyTo: InternetAddress
    var Subject: String
    var MessageHTML: String
    fun isValid(): Boolean
    fun send(): String

    var UserName: String
    var Password: String
}