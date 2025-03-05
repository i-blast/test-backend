package mobi.sevenwinds.app.common

import org.joda.time.DateTime
import java.time.LocalDateTime
import java.time.ZoneId

const val DATE_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS"

fun DateTime.toJavaLocalDateTime(): LocalDateTime = toDate()
    .toInstant()
    .atZone(ZoneId.systemDefault())
    .toLocalDateTime()

fun LocalDateTime.toJodaDateTime(): DateTime = DateTime(
    atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
)