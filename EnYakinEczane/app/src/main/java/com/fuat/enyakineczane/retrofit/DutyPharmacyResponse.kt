package com.fuat.enyakineczane.retrofit

data class DutyPharmacyResponse(
    val success: Boolean,
    val result: List<DutyPharmacy>
)

data class DutyPharmacy(
    val name: String,
    val dist: String,
    val address: String,
    val phone: String,
    val loc: String
)
