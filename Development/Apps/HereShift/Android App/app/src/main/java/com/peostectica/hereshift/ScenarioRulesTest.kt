package com.peostectica.hereshift

fun scenarioRulesChecks() {
    check(ScenarioRules.parse("41.8781", "-87.6298") == TestPoint(41.8781, -87.6298))
    check(ScenarioRules.parse("90", "-180") == TestPoint(90.0, -180.0))
    check(ScenarioRules.parse("-90", "180") == TestPoint(-90.0, 180.0))
    for ((lat, lon) in listOf("91" to "0", "0" to "181", "-91" to "0", "0" to "-181", "NaN" to "0", "Infinity" to "0", "hello" to "1", "" to "", "12" to "five")) {
        check(ScenarioRules.parse(lat,lon) == null) { "Invalid coordinate accepted: $lat, $lon" }
    }
}