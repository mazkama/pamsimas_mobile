package ahmat.dafa.pamsimas.utils


object CurrencyHelper {

    fun formatCurrency(value: Int?): String {
        return String.format("%,d", value ?: 0).replace(',', '.')
    }

    fun cleanCurrencyString(value: String): Int {
        return value.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0
    }
}