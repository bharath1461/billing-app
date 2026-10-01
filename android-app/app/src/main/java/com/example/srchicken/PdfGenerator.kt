package com.example.srchicken

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import com.example.srchicken.data.BillEntity
import com.example.srchicken.data.BillItemEntity
import com.example.srchicken.data.SettingsEntity
import java.io.File
import android.content.ActivityNotFoundException

object PdfGenerator {

    fun generateAndShare(
        context: Context,
        bill: BillEntity,
        items: List<BillItemEntity>,
        settings: SettingsEntity,
        shareOnWhatsApp: Boolean = false
    ) {
        val pdf = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
        val page = pdf.startPage(pageInfo)
        val canvas = page.canvas

        val margin = 40f
        var y = 60f
        val pageWidth = 595f

        // Paint styles — Professional Indigo palette
        val titlePaint = Paint().apply {
            color = Color.rgb(79, 70, 229)  // Indigo-600
            textSize = 26f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val subtitlePaint = Paint().apply {
            color = Color.rgb(107, 114, 128)
            textSize = 10f
            isAntiAlias = true
        }
        val labelPaint = Paint().apply {
            color = Color.rgb(107, 114, 128)
            textSize = 9f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val bodyPaint = Paint().apply {
            color = Color.rgb(26, 26, 26)
            textSize = 11f
            isAntiAlias = true
        }
        val boldBodyPaint = Paint().apply {
            color = Color.rgb(26, 26, 26)
            textSize = 11f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val accentPaint = Paint().apply {
            color = Color.rgb(79, 70, 229)  // Indigo-600
            textSize = 13f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val linePaint = Paint().apply {
            color = Color.rgb(229, 231, 235)
            strokeWidth = 1f
        }
        val headerBgPaint = Paint().apply {
            color = Color.rgb(99, 102, 241)  // Indigo-500
            style = Paint.Style.FILL
        }
        val headerTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = 9f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val dangerPaint = Paint().apply {
            color = Color.rgb(239, 68, 68)
            textSize = 11f
            isAntiAlias = true
        }

        // Shop name (centered)
        val shopName = settings.shopName.ifBlank { "SR Billing" }
        canvas.drawText(shopName, pageWidth / 2 - titlePaint.measureText(shopName) / 2, y, titlePaint)
        y += 20f

        // Shop details
        val shopDetails = listOfNotNull(
            settings.shopPhone.ifBlank { null },
            settings.shopAddress.ifBlank { null },
            if (settings.shopGstin.isNotBlank()) "GSTIN: ${settings.shopGstin}" else null
        ).joinToString("  |  ")
        if (shopDetails.isNotBlank()) {
            canvas.drawText(shopDetails, pageWidth / 2 - subtitlePaint.measureText(shopDetails) / 2, y, subtitlePaint)
            y += 16f
        }

        // Divider
        y += 8f
        canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
        y += 18f

        // Invoice label
        val invoiceLabel = "TAX INVOICE"
        val invoiceLabelPaint = Paint().apply {
            color = Color.rgb(79, 70, 229)
            textSize = 10f
            isFakeBoldText = true
            isAntiAlias = true
            letterSpacing = 0.15f
        }
        canvas.drawText(invoiceLabel, pageWidth / 2 - invoiceLabelPaint.measureText(invoiceLabel) / 2, y, invoiceLabelPaint)
        y += 18f

        // Bill info grid (2x2)
        val col2 = pageWidth / 2
        canvas.drawText("BILL NUMBER", margin, y, labelPaint)
        canvas.drawText("DATE", col2, y, labelPaint)
        y += 14f
        canvas.drawText(bill.billNumber, margin, y, boldBodyPaint)
        canvas.drawText(Formatters.dateTime(bill.createdAt), col2, y, bodyPaint)
        y += 20f

        canvas.drawText("CUSTOMER", margin, y, labelPaint)
        canvas.drawText("PHONE", col2, y, labelPaint)
        y += 14f
        canvas.drawText(bill.customerName, margin, y, boldBodyPaint)
        canvas.drawText(if (bill.customerPhone.isNotBlank()) bill.customerPhone else "—", col2, y, bodyPaint)
        y += 24f

        if (bill.customerCode.isNotBlank()) {
            canvas.drawText("CUSTOMER ID", margin, y, labelPaint)
            y += 14f
            canvas.drawText(bill.customerCode, margin, y, bodyPaint)
            y += 20f
        }

        // Items table header
        val col1W = 200f; val col2W = 80f; val col3W = 90f; val col4W = 100f
        val tableRight = pageWidth - margin

        canvas.drawRect(margin, y - 14f, tableRight, y + 4f, headerBgPaint)
        canvas.drawText("#", margin + 4f, y, headerTextPaint)
        canvas.drawText("ITEM", margin + 20f, y, headerTextPaint)
        canvas.drawText("QTY", margin + col1W + 20f, y, headerTextPaint)
        canvas.drawText("RATE", margin + col1W + col2W + 10f, y, headerTextPaint)
        canvas.drawText("AMOUNT", margin + col1W + col2W + col3W + 5f, y, headerTextPaint)
        y += 14f

        // Items rows
        items.forEachIndexed { idx, item ->
            canvas.drawText("${idx + 1}", margin + 4f, y, bodyPaint)
            canvas.drawText(item.productName, margin + 20f, y, bodyPaint)
            canvas.drawText("${item.qty}", margin + col1W + 20f, y, bodyPaint)
            canvas.drawText(Formatters.currency(item.rate), margin + col1W + col2W + 10f, y, bodyPaint)
            canvas.drawText(Formatters.currency(item.amount), margin + col1W + col2W + col3W + 5f, y, bodyPaint)
            y += 4f
            canvas.drawLine(margin, y, tableRight, y, linePaint)
            y += 14f
        }

        y += 10f

        // Totals (right-aligned)
        val totalsLeft = tableRight - 200f
        canvas.drawText("Subtotal", totalsLeft, y, bodyPaint)
        canvas.drawText(Formatters.currency(bill.subtotal), tableRight - boldBodyPaint.measureText(Formatters.currency(bill.subtotal)), y, boldBodyPaint)
        y += 18f

        if (bill.discountAmount > 0) {
            val discLabel = if (bill.discountType == "percent") "Discount (${bill.discountValue.toInt()}%)" else "Discount"
            canvas.drawText(discLabel, totalsLeft, y, bodyPaint)
            val discText = "-${Formatters.currency(bill.discountAmount)}"
            canvas.drawText(discText, tableRight - dangerPaint.measureText(discText), y, dangerPaint)
            y += 18f
        }

        // Tax line (if GSTIN is set, show tax note)
        if (settings.shopGstin.isNotBlank()) {
            canvas.drawText("Tax (included)", totalsLeft, y, bodyPaint)
            canvas.drawText("Inclusive", tableRight - bodyPaint.measureText("Inclusive"), y, bodyPaint)
            y += 18f
        }

        if (bill.openingBalance != 0.0) {
            canvas.drawText("Previous balance", totalsLeft, y, bodyPaint)
            val balanceText = Formatters.currency(bill.openingBalance)
            canvas.drawText(balanceText, tableRight - boldBodyPaint.measureText(balanceText), y, boldBodyPaint)
            y += 18f
        }

        canvas.drawLine(totalsLeft, y, tableRight, y, linePaint)
        y += 14f

        canvas.drawText("AMOUNT DUE", totalsLeft, y, labelPaint)
        val totalText = Formatters.currency(bill.totalDue)
        canvas.drawText(totalText, tableRight - accentPaint.measureText(totalText), y, accentPaint)
        y += 40f

        // Footer
        canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
        y += 16f
        val footer = "Thank you for your business!"
        canvas.drawText(footer, pageWidth / 2 - subtitlePaint.measureText(footer) / 2, y, subtitlePaint)
        y += 14f
        val powered = "Generated with SR Billing"
        val poweredPaint = Paint().apply { color = Color.rgb(156, 163, 175); textSize = 8f; isAntiAlias = true }
        canvas.drawText(powered, pageWidth / 2 - poweredPaint.measureText(powered) / 2, y, poweredPaint)

        pdf.finishPage(page)

        // Save to cache
        val cacheDir = File(context.cacheDir, "bills").apply { mkdirs() }
        val file = File(cacheDir, "${bill.billNumber}.pdf")
        file.outputStream().use { pdf.writeTo(it) }
        pdf.close()

        // Share
        val uri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } else {
            Uri.fromFile(file)
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Invoice ${bill.billNumber} — ${bill.customerName}")
            if (shareOnWhatsApp) {
                setPackage("com.whatsapp")
                putExtra(Intent.EXTRA_TEXT, "Hello ${bill.customerName}, your bill for today from ${settings.shopName.ifBlank { "SR Billing" }} is ${Formatters.currency(bill.totalDue)}. Please find the invoice attached.")
            }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(if (shareOnWhatsApp) intent else Intent.createChooser(intent, "Share Invoice"))
        } catch (_: ActivityNotFoundException) {
            intent.setPackage(null)
            context.startActivity(Intent.createChooser(intent, "Share Invoice"))
        }
    }

    /** Sends the PDF and a ready-to-edit message straight to WhatsApp when installed. */
    fun generateAndShareWhatsApp(
        context: Context,
        bill: BillEntity,
        items: List<BillItemEntity>,
        settings: SettingsEntity
    ) {
        generateAndShare(context, bill, items, settings, shareOnWhatsApp = true)
    }
}
