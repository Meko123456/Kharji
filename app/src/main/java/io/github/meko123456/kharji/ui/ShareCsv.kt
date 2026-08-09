package io.github.meko123456.kharji.ui

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import io.github.meko123456.kharji.data.Category
import io.github.meko123456.kharji.data.Entry
import io.github.meko123456.kharji.domain.CsvExporter
import java.io.File

/** Writes entries to a cache CSV and opens the system share sheet. */
fun shareCsv(context: Context, entries: List<Entry>, categories: List<Category>) {
    val dir = File(context.cacheDir, "exports").apply { mkdirs() }
    val file = File(dir, "kharji-export.csv")
    file.writeText(CsvExporter.toCsv(entries, categories))

    val uri = FileProvider.getUriForFile(
        context,
        "io.github.meko123456.kharji.fileprovider",
        file,
    )
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(send, "Export Kharji CSV"))
}
