package org.mywill.server.service

import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Service

interface EmailService {
    fun sendVerificationCode(to: String, code: String)
}

@Service
class EmailServiceImpl(private val mailSender: JavaMailSender) : EmailService {
    override fun sendVerificationCode(to: String, code: String) {
        val message = SimpleMailMessage()
        message.from = "alexey9113@gmail.com"
        message.setTo(to)
        message.subject = "Your verification code"
        message.text = "Your verification code is: $code"
        mailSender.send(message)
    }
}
