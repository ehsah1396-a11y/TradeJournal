package com.example.tradejournal

import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class ImportResult(val trades: List<Trade>, val skipped: Int)

/**
 * بایت‌های فایل گزارش را به متن تبدیل می‌کند، با تشخیص خودکار کدگذاری.
 *
 * چرا این تابع لازم است: قبلاً کدگذاری همیشه ISO-8859-1 فرض می‌شد، اما «ذخیره به‌صورت
 * گزارش» در ترمینال متاتریدر ۴ بسته به نسخه و زبان ویندوز می‌تواند فایل را با UTF-16
 * (با یا بدون BOM)، UTF-8، یا یک صفحه‌کد تک‌بایتی مثل windows-1256/1251 ذخیره کند.
 * وقتی فایل واقعی UTF-16 است ولی با ISO-8859-1 خوانده شود، هر کاراکتر ASCII یک بایت صفرِ
 * چسبیده به خودش می‌گیرد و رجکس‌های <tr>/<td> اصلاً چیزی پیدا نمی‌کنند؛ یعنی همان «اپ
 * نمی‌تواند فایل HTML را بخواند» که این تابع رفعش می‌کند.
 */
fun decodeReportBytes(bytes: ByteArray): String {
    if (bytes.isEmpty()) return ""

    // ۱) BOM صریح
    if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte())
        return String(bytes, 3, bytes.size - 3, Charsets.UTF_8)
    if (bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte())
        return String(bytes, 2, bytes.size - 2, Charsets.UTF_16LE)
    if (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte())
        return String(bytes, 2, bytes.size - 2, Charsets.UTF_16BE)

    // ۲) UTF-16LE بدون BOM: حالت رایج در نسخه‌های قدیمی‌تر متاتریدر روی ویندوز فارسی/عربی.
    // در این حالت، نیمی از بایت‌ها (بایت بالای هر کاراکتر ASCII) صفر هستند.
    val sample = bytes.copyOfRange(0, minOf(bytes.size, 400))
    if (sample.size >= 20) {
        var zerosAtOddPositions = 0
        var i = 1
        while (i < sample.size) { if (sample[i] == 0.toByte()) zerosAtOddPositions++; i += 2 }
        if (zerosAtOddPositions > sample.size / 4) return String(bytes, Charsets.UTF_16LE)
    }

    // ۳) خواندن charset اعلام‌شده در خود HTML (<meta charset=... یا content=...charset=...>).
    // این خواندنِ اولیه با ISO-8859-1 انجام می‌شود چون این کدگذاری هر بایت را بدون تغییر
    // به یک کاراکتر نگاشت می‌کند، پس برچسب charset (که همیشه ASCII است) سالم دیده می‌شود.
    val head = String(bytes, 0, minOf(bytes.size, 4096), Charsets.ISO_8859_1)
    val declared = Regex("charset\\s*=\\s*\"?'?([a-zA-Z0-9_\\-]+)", RegexOption.IGNORE_CASE)
        .find(head)?.groupValues?.get(1)
    if (declared != null) {
        try {
            val cs = charset(declared)
            if (cs != Charsets.ISO_8859_1) return String(bytes, cs)
        } catch (e: Exception) { /* نام ناشناس، ادامه بده */ }
    }

    // ۴) تلاش با UTF-8 (سخت‌گیرانه: اگر بایت نامعتبر دید خطا بده تا رد شویم)
    try {
        val decoder = Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        return decoder.decode(ByteBuffer.wrap(bytes)).toString()
    } catch (e: Exception) { /* UTF-8 معتبر نیست، ادامه بده */ }

    // ۵) صفحه‌کد رایج بروکرهای فارسی/عربی، و در نهایت ISO-8859-1 به‌عنوان آخرین راه‌حل
    return try { String(bytes, charset("windows-1256")) } catch (e: Exception) { String(bytes, Charsets.ISO_8859_1) }
}

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

/**
 * یک فایل Excel واقعی (xlsx) بدون هیچ کتابخانه‌ی جانبی می‌سازد.
 *
 * نسخه‌ی قبلی فقط [Content_Types].xml + workbook.xml + worksheet را می‌ساخت و xl/styles.xml
 * را کم داشت. آفیس دسکتاپ معمولاً با «تعمیر» آن را باز می‌کرد، ولی خیلی از اپ‌های اندرویدی
 * (شیت گوگل، WPS، آفیس موبایل) به همان دلیلِ نبودِ styles.xml فایل را «خراب» گزارش می‌کنند
 * و اصلاً بازش نمی‌کنند. این نسخه styles.xml و docProps را هم اضافه می‌کند تا فایل کاملاً
 * مطابق مشخصات OOXML و در همه‌جا قابل‌باز شدن باشد.
 */
fun writeTradesXlsx(out: OutputStream, trades: List<Trade>) {
    val headers = listOf(
        tr("نماد"), tr("نوع"), tr("ورود"), tr("خروج"), tr("سود/زیان"), tr("احساس"), tr("استراتژی"), tr("یادداشت"), tr("تاریخ")
    )
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

    fun esc(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    // سبک ۰: متن معمولی. سبک ۱: هدر (بولد). به xl/styles.xml زیر نگاه کن.
    fun rowXml(rIdx: Int, cells: List<Pair<String, Boolean>>, headerStyle: Boolean): String {
        val b = StringBuilder("<row r=\"$rIdx\">")
        val s = if (headerStyle) " s=\"1\"" else ""
        cells.forEachIndexed { i, (v, isNum) ->
            val col = ('A' + i)
            if (isNum && v.isNotBlank()) b.append("<c r=\"$col$rIdx\"$s><v>${esc(v)}</v></c>")
            else b.append("<c r=\"$col$rIdx\"$s t=\"inlineStr\"><is><t xml:space=\"preserve\">${esc(v)}</t></is></c>")
        }
        b.append("</row>")
        return b.toString()
    }

    val lastCol = ('A' + (headers.size - 1))
    val lastRow = trades.size + 1

    val sb = StringBuilder()
    sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
    sb.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">")
    sb.append("<dimension ref=\"A1:$lastCol$lastRow\"/>")
    sb.append("<sheetViews><sheetView workbookViewId=\"0\"/></sheetViews>")
    sb.append("<sheetData>")
    sb.append(rowXml(1, headers.map { it to false }, headerStyle = true))
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
        sb.append(rowXml(idx + 2, cells, headerStyle = false))
    }
    sb.append("</sheetData></worksheet>")

    val contentTypes = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
<Default Extension="xml" ContentType="application/xml"/>
<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
<Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
<Override PartName="/docProps/core.xml" ContentType="application/vnd.openxmlformats-package.core-properties+xml"/>
<Override PartName="/docProps/app.xml" ContentType="application/vnd.openxmlformats-officedocument.extended-properties+xml"/>
</Types>"""

    val rootRels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
<Relationship Id="rId2" Type="http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties" Target="docProps/core.xml"/>
<Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties" Target="docProps/app.xml"/>
</Relationships>"""

    val workbook = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
<sheets><sheet name="Trades" sheetId="1" r:id="rId1"/></sheets>
</workbook>"""

    val workbookRels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
<Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""

    // سبک ۰ = پیش‌فرض. سبک ۱ = هدر بولد با پس‌زمینه‌ی خاکستری روشن.
    val styles = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
<fonts count="2"><font><sz val="11"/><name val="Calibri"/></font><font><sz val="11"/><name val="Calibri"/><b/></font></fonts>
<fills count="3"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill><fill><patternFill patternType="solid"><fgColor rgb="FFE9E9E9"/><bgColor indexed="64"/></patternFill></fill></fills>
<borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders>
<cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
<cellXfs count="2">
<xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
<xf numFmtId="0" fontId="1" fillId="2" borderId="0" xfId="0" applyFont="1" applyFill="1"/>
</cellXfs>
<cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles>
</styleSheet>"""

    val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
    val core = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<cp:coreProperties xmlns:cp="http://schemas.openxmlformats.org/package/2006/metadata/core-properties" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:dcterms="http://purl.org/dc/terms/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
<dc:title>Trades</dc:title>
<dcterms:created xsi:type="dcterms:W3CDTF">$nowIso</dcterms:created>
<dcterms:modified xsi:type="dcterms:W3CDTF">$nowIso</dcterms:modified>
</cp:coreProperties>"""
    val app = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Properties xmlns="http://schemas.openxmlformats.org/officeDocument/2006/extended-properties" xmlns:vt="http://schemas.openxmlformats.org/officeDocument/2006/docPropsVTypes">
<Application>TradeJournal</Application>
</Properties>"""

    val zip = ZipOutputStream(out)
    fun put(name: String, content: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(content.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }
    put("[Content_Types].xml", contentTypes)
    put("_rels/.rels", rootRels)
    put("docProps/core.xml", core)
    put("docProps/app.xml", app)
    put("xl/workbook.xml", workbook)
    put("xl/_rels/workbook.xml.rels", workbookRels)
    put("xl/styles.xml", styles)
    put("xl/worksheets/sheet1.xml", sb.toString())
    zip.finish()
}
