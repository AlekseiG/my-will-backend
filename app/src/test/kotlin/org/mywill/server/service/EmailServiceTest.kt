package org.mywill.server.service

import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender

class EmailServiceTest {

    private val mailSender = mock(JavaMailSender::class.java)
    private val emailService = EmailServiceImpl(mailSender)

    @Test
    fun `sendSimpleMessage should send mail via mailSender`() {
        val to = "test@example.com"
        val subject = "Test Subject"
        val text = "Test Body"

        emailService.sendSimpleMessage(to, subject, text)

        verify(mailSender, times(1)).send(any(SimpleMailMessage::class.java))
    }

    @Test
    fun `sendVerificationCode should send mail with code`() {
        val to = "test@example.com"
        val code = "123456"

        emailService.sendVerificationCode(to, code)

        verify(mailSender, times(1)).send(argThat<SimpleMailMessage> { message ->
            message.to?.contains(to) == true && 
            message.subject == "Your verification code" && 
            message.text?.contains(code) == true
        })
    }
}
