package com.example.tradejournal

import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class ImportResult(val trades: List<Trade>, val skipped: Int)

/**
 * گزارش تاریخچه‌ی متاتریدر ۴ (فایل HTML که از ترمینال با «ذخیره به‌صورت گزارش» گرفته می‌شود) را می‌خواند.
 * چون قالب گزارش بین بروکرها کمی فرق دارد، این یک تشخیص حدسی است: ردیف‌هایی که ستون «buy» یا «sell»
 * دارند به‌عنوان معامله در نظر گرفته می‌شوند و آخرین عدد ردیف به‌عنوان سود یا زیان خوانده می‌شود.
 */
fun parseMt4Report(html: String): ImportResult {
    val rowRe = Regex("<tr[^>]*>(.*?)</tr>", RegexOption.DOT_MATCHES_ALL)
    val cellRe = Regex("<t[dh][^>]*>(.*?)</t[dh]>", RegexOption.DOT_MATCHES_ALL)
    val tagRe = Regex("<[^>]+>")
    val dateFmt = SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.US)
    val out = ArrayList<Trade>()
    var skipped = 0
    var n = 0

    for (rowMatch in rowRe.findAll(html)) {
        val cells = cellRe.findAll(rowMatch.groupValues[1]).map {
            tagRe.replace(it.groupValues[1], "")
                .replace("&nbsp;", " ").replace("&amp;", "&")
                .trim()
        }.toList()
        if (cells.size < 5) continue

        val typeIdx = cells.indexOfFirst { it.equals("buy", true) || it.equals("sell", true) }
        if (typeIdx < 1 || typeIdx + 2 >= cells.size) continue

        try {
            val isBuy = cells[typeIdx].equals("buy", true)
            val symbol = cells[typeIdx + 1].uppercase()
            if (symbol.isBlank() || symbol.length > 15 || symbol.toDoubleOrNull() != null) { skipped++; continue }

            val entry = cells.getOrNull(typeIdx + 2)?.toNum()

            var profit: Double? = null
            for (i in cells.size - 1 downTo typeIdx + 3) {
                val cand = cells[i].replace(" ", "").toNum()
                if (cand != null) { profit = cand; break }
            }
            if (profit == null) { skipped++; continue }

            var openTime: Long? = null
            try { openTime = dateFmt.parse(cells[typeIdx - 1])?.time } catch (e: Exception) { }
            var closeTime: Long? = null
            for (i in cells.size - 1 downTo typeIdx + 1) {
                try {
                    val d = dateFmt.parse(cells[i])?.time
                    if (d != null) { closeTime = d; break }
                } catch (e: Exception) { }
            }

            n++
            out.add(
                Trade(
                    symbol = symbol, isBuy = isBuy, entry = entry, exit = null, pl = profit,
                    note = "وارد شده از متاتریدر", mood = 1,
                    createdAt = closeTime ?: openTime ?: (System.currentTimeMillis() - n * 1000L)
                )
            )
        } catch (e: Exception) {
            skipped++
        }
    }
    return ImportResult(out, skipped)
}

/** یک فایل Excel واقعی (xlsx) بدون هیچ کتابخانه‌ی جانبی می‌سازد */
fun writeTradesXlsx(out: OutputStream, trades: List<Trade>) {
    val headers = listOf(
        tr("نماد"), tr("نوع"), tr("ورود"), tr("خروج"), tr("سود/زیان"), tr("احساس"), tr("استراتژی"), tr("یادداشت"), tr("تاریخ")
    )
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

    fun esc(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    fun rowXml(rIdx: Int, cells: List<Pair<String, Boolean>>): String {
        val b = StringBuilder("<row r=\"$rIdx\">")
        cells.forEachIndexed { i, (v, isNum) ->
            val col = ('A' + i)
            if (isNum && v.isNotBlank()) b.append("<c r=\"$col$rIdx\"><v>${esc(v)}</v></c>")
            else b.append("<c r=\"$col$rIdx\" t=\"inlineStr\"><is><t xml:space=\"preserve\">${esc(v)}</t></is></c>")
        }
        b.append("</row>")
        return b.toString()
    }

    val sb = StringBuilder()
    sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
    sb.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>")
    sb.append(rowXml(1, headers.map { it to false }))
    trades.reversed().forEachIndexed { idx, t ->
        val cells = listOf(
            t.symbol to false,
            (if (t.isBuy) tr("خرید") else tr("فروش")) to false,
            (t.entry?.toString() ?: "") to true,
            (t.exit?.toString() ?: "") to true,
            t.pl.toString() to true,
            MOOD_NAMES.getOrElse(t.mood) { "" } to false,
            t.strategy to false,
            t.note to false,
            sdf.format(Date(t.createdAt)) to false
        )
        sb.append(rowXml(idx + 2, cells))
    }
    sb.append("</sheetData></worksheet>")

    val contentTypes = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
<Default Extension="xml" ContentType="application/xml"/>
<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
<Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
</Types>"""
    val rootRels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""
    val workbook = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
<sheets><sheet name="Trades" sheetId="1" r:id="rId1"/></sheets>
</workbook>"""
    val workbookRels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
</Relationships>"""

    val zip = ZipOutputStream(out)
    fun put(name: String, content: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(content.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }
    put("[Content_Types].xml", contentTypes)
    put("_rels/.rels", rootRels)
    put("xl/workbook.xml", workbook)
    put("xl/_rels/workbook.xml.rels", workbookRels)
    put("xl/worksheets/sheet1.xml", sb.toString())
    zip.finish()
}
