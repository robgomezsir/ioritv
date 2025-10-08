package com.elevadorcom.ioritv

import android.content.Context
import android.content.Intent
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import com.google.firebase.firestore.FirebaseFirestore
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utilitário para exportar dados de clientes para arquivo Excel
 */
object ExcelExportUtil {

    private const val TAG = "ExcelExportUtil"

    /**
     * Exporta a lista de clientes do Firebase para um arquivo Excel
     * Baixa dinamicamente todas as colunas que existem nos documentos
     */
    fun exportClientesToExcel(context: Context, onComplete: (success: Boolean, message: String, file: File?) -> Unit) {
        val db = FirebaseFirestore.getInstance()

        db.collection("clientes")
            .get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    onComplete(false, "Nenhum cliente encontrado para exportar", null)
                    return@addOnSuccessListener
                }

                try {
                    // Criar workbook e sheet
                    val workbook = XSSFWorkbook()
                    val sheet = workbook.createSheet("Clientes")

                    // Criar estilos
                    val headerStyle = createHeaderStyle(workbook)
                    val currencyStyle = createCurrencyStyle(workbook)

                    // Coletar todas as chaves (colunas) de todos os documentos
                    val allKeys = mutableSetOf<String>()
                    for (document in result) {
                        allKeys.addAll(document.data.keys)
                    }
                    
                    // Ordenar as chaves alfabeticamente para consistência
                    val sortedKeys = allKeys.sorted()
                    
                    // Criar cabeçalho dinamicamente
                    val headerRow = sheet.createRow(0)
                    sortedKeys.forEachIndexed { index, key ->
                        val cell = headerRow.createCell(index)
                        cell.setCellValue(key)
                        cell.setCellStyle(headerStyle)
                    }

                    // Preencher dados dinamicamente
                    var rowNum = 1
                    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
                    
                    for (document in result) {
                        val row = sheet.createRow(rowNum++)
                        
                        sortedKeys.forEachIndexed { colIndex, key ->
                            val cell = row.createCell(colIndex)
                            val value = document.get(key)
                            
                            // Trata diferentes tipos de dados
                            when (value) {
                                null -> cell.setCellValue("")
                                
                                is String -> cell.setCellValue(value)
                                
                                is Number -> {
                                    val doubleValue = value.toDouble()
                                    cell.setCellValue(doubleValue)
                                    // Aplica estilo de moeda se o campo contém "VALOR" no nome
                                    if (key.contains("VALOR", ignoreCase = true)) {
                                        cell.setCellStyle(currencyStyle)
                                    }
                                }
                                
                                is Boolean -> cell.setCellValue(if (value) "Sim" else "Não")
                                
                                is com.google.firebase.Timestamp -> {
                                    try {
                                        cell.setCellValue(sdf.format(value.toDate()))
                                    } catch (e: Exception) {
                                        cell.setCellValue(value.toString())
                                    }
                                }
                                
                                is Date -> {
                                    try {
                                        cell.setCellValue(sdf.format(value))
                                    } catch (e: Exception) {
                                        cell.setCellValue(value.toString())
                                    }
                                }
                                
                                is List<*> -> {
                                    // Converte listas para string separada por vírgulas
                                    cell.setCellValue(value.joinToString(", "))
                                }
                                
                                is Map<*, *> -> {
                                    // Converte mapas para JSON string simplificado
                                    cell.setCellValue(value.toString())
                                }
                                
                                else -> cell.setCellValue(value.toString())
                            }
                        }
                    }

                    // Definir largura das colunas dinamicamente
                    // Largura padrão baseada no tamanho do nome da coluna
                    sortedKeys.forEachIndexed { index, key ->
                        // Largura base: 3000 + (tamanho do nome * 300)
                        val width = minOf(3000 + (key.length * 300), 8000)
                        sheet.setColumnWidth(index, width)
                    }

                    // Salvar arquivo
                    val file = saveWorkbook(context, workbook)
                    workbook.close()

                    if (file != null) {
                        val totalColumns = sortedKeys.size
                        val totalRows = result.size()
                        onComplete(true, "Excel criado! $totalRows registros com $totalColumns colunas", file)
                    } else {
                        onComplete(false, "Erro ao salvar arquivo", null)
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    onComplete(false, "Erro ao criar Excel: ${e.message}", null)
                }
            }
            .addOnFailureListener { e ->
                onComplete(false, "Erro ao buscar clientes: ${e.message}", null)
            }
    }

    /**
     * Salva o workbook em um arquivo
     */
    private fun saveWorkbook(context: Context, workbook: Workbook): File? {
        return try {
            // Criar nome do arquivo com timestamp
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "Clientes_IoriTV_$timestamp.xlsx"

            // Obter diretório de downloads
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }

            val file = File(downloadsDir, fileName)
            val outputStream = FileOutputStream(file)
            workbook.write(outputStream)
            outputStream.close()

            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Cria estilo para o cabeçalho
     */
    private fun createHeaderStyle(workbook: Workbook): CellStyle {
        val style = workbook.createCellStyle()
        val font = workbook.createFont()
        font.bold = true
        font.fontHeightInPoints = 12
        style.setFont(font)
        style.fillForegroundColor = IndexedColors.GREY_25_PERCENT.index
        style.fillPattern = FillPatternType.SOLID_FOREGROUND
        style.borderBottom = BorderStyle.THIN
        style.borderTop = BorderStyle.THIN
        style.borderLeft = BorderStyle.THIN
        style.borderRight = BorderStyle.THIN
        style.alignment = HorizontalAlignment.CENTER
        return style
    }

    /**
     * Cria estilo para valores monetários
     */
    private fun createCurrencyStyle(workbook: Workbook): CellStyle {
        val style = workbook.createCellStyle()
        val format = workbook.createDataFormat()
        style.dataFormat = format.getFormat("R$ #,##0.00")
        return style
    }

    /**
     * Abre o arquivo Excel gerado
     */
    fun openExcelFile(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(Intent.createChooser(intent, "Abrir com"))
        } catch (e: Exception) {
            Toast.makeText(context, "Nenhum aplicativo disponível para abrir Excel", Toast.LENGTH_LONG).show()
        }
    }
}

