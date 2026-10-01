package com.example.srchicken

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Formatters {
    private val inrFormat = NumberFormat.getCurrencyInstance(Locale.Builder().setLanguage("en").setRegion("IN").build())
    private val dateFormat = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat("d MMM yyyy, hh:mm a", Locale.getDefault())
    private val shortDateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())

    fun currency(amount: Double): String = inrFormat.format(amount)

    fun date(timestamp: Long): String = dateFormat.format(Date(timestamp))

    fun time(timestamp: Long): String = timeFormat.format(Date(timestamp))

    fun dateTime(timestamp: Long): String = dateTimeFormat.format(Date(timestamp))

    fun shortDate(timestamp: Long): String = shortDateFormat.format(Date(timestamp))

    fun initials(name: String): String =
        name.trim().split(Regex("\\s+")).take(2).map { it.firstOrNull()?.uppercaseChar() ?: ' ' }
            .joinToString("")
}
