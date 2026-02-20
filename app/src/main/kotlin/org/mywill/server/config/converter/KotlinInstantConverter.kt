package org.mywill.server.config.converter

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant

@Converter(autoApply = true)
class KotlinInstantConverter : AttributeConverter<kotlin.time.Instant, java.time.Instant> {

    override fun convertToDatabaseColumn(attribute: kotlin.time.Instant?): java.time.Instant? {
        return attribute?.toJavaInstant()
    }

    override fun convertToEntityAttribute(dbData: java.time.Instant?): kotlin.time.Instant? {
        return dbData?.toKotlinInstant()
    }
}