package com.elevadorcom.ioritv.utils

import java.util.*

object SituacaoUtil {
    fun calcularSituacao(termino: Date): String {
        val hoje = clearTime(Date())
        val terminoSemHora = clearTime(termino)
        val diasParaTermino = calculateDaysDifference(hoje, terminoSemHora)

        return when {
            diasParaTermino <= -15 -> SituacaoConstants.STANDBY
            diasParaTermino in -14..-1 -> SituacaoConstants.VENCIDO
            diasParaTermino in 0..2 -> SituacaoConstants.A_VENCER
            else -> SituacaoConstants.ATIVO
        }
    }

    private fun clearTime(date: Date): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.time
    }

    private fun calculateDaysDifference(date1: Date, date2: Date): Int {
        val diffInMillis = date2.time - date1.time
        return (diffInMillis / (1000 * 60 * 60 * 24)).toInt()
    }
}

object SituacaoConstants {
    const val ATIVO = "ATIVO"
    const val A_VENCER = "A VENCER"
    const val VENCIDO = "VENCIDO"
    const val STANDBY = "STANDBY"
}