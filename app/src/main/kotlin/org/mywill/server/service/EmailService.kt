package org.mywill.server.service

import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Service

/**
 * Сервис для отправки электронных писем.
 */
interface EmailService {
    /**
     * Отправляет код верификации на указанный email.
     * @param to Email получателя.
     * @param code Код верификации.
     */
    fun sendVerificationCode(to: String, code: String)

    /**
     * Отправляет текстовое сообщение на указанный email.
     * @param to Email получателя.
     * @param subject Тема письма.
     * @param text Содержимое письма.
     */
    fun sendSimpleMessage(to: String, subject: String, text: String)
}

/**
 * Реализация сервиса отправки писем с использованием JavaMailSender.
 */
@Service
class EmailServiceImpl(private val mailSender: JavaMailSender) : EmailService {
    /**
     * Отправляет стандартное сообщение с кодом верификации.
     */
    override fun sendVerificationCode(to: String, code: String) {
        sendSimpleMessage(to, "Your verification code", "Your verification code is: $code")
    }

    /**
     * Формирует и отправляет письмо.
     */
    override fun sendSimpleMessage(to: String, subject: String, text: String) {
        val message = SimpleMailMessage()
        message.from = "alexey9113@gmail.com"
        message.setTo(to)
        message.subject = subject
        message.text = text
        mailSender.send(message)
    }
}
