package com.elevadorcom.ioritv

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import java.util.Date

data class Cadastro(

    @DocumentId
    var id: String? = null,
    @PropertyName("NOME")
    var nome: String? = null,
    @PropertyName("USUARIO")
    var usuario: String? = null,
    @PropertyName("SENHA")
    var senha: String? = null,
    @PropertyName("WHATSAPP")
    var whatsapp: String? = null,
    @PropertyName("MODELO")
    var modelo: String? = null,
    @PropertyName("INICIO")
    var inicio: Date? = null,
    @PropertyName("TERMINO")
    var termino: Date? = null,
    @PropertyName("CREDITOS")
    var creditos: Long? = null,
    @PropertyName("VENCIMENTO")
    var vencimento: String? = null,
    @PropertyName("MAC")
    var mac: String? = null,
    @PropertyName("OTP")
    var otp: String? = null,
    @PropertyName("DEVICE")
    var device: String? = null,
    @PropertyName("VALOR")
    var valor: Double? = null,
    @PropertyName("CUSTO")
    var custo: Double? = null,
    @PropertyName("DESCONTO")
    var desconto: Double? = null,
    @PropertyName("SERVIDOR")
    var servidor: String? = null,
    @PropertyName("SITUACAO")
    var situacao: String? = null
)


