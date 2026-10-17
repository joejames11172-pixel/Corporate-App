package com.peostectica.hereshift

/** Original HereShift business state for eventual Android integration.
 * This module has no location/network/microphone permission and no Android SDK dependency.
 * Audio playback is intentionally not in this file: native UI must obey OS silent/DND and audio focus.
 */
data class ColorRoles(val background: String, val surface: String, val onBackground: String, val onSurface: String, val primary: String, val onPrimary: String)
data class ThemePair(val id: String, val name: String, val light: ColorRoles, val dark: ColorRoles)
data class AudioScheme(val id: String, val name: String, val cues: List<String>)
enum class Appearance { FOLLOW_DEVICE, LIGHT, DARK }

interface PreferenceStore {
    fun get(key: String): String?
    fun put(key: String, value: String)
}
class InMemoryPreferenceStore : PreferenceStore {
    private val data = mutableMapOf<String,String>()
    override fun get(key: String): String? = data[key]
    override fun put(key: String, value: String) { data[key] = value }
}

object Catalog {
    val pairs: List<ThemePair> = listOf(
        ThemePair("01", "Monochrome", ColorRoles("#FFFFFF", "#F4F5F7", "#111827", "#111827", "#30343B", "#FFFFFF"), ColorRoles("#121418", "#20252D", "#F9FAFB", "#F9FAFB", "#E5E7EB", "#121418")),
        ThemePair("02", "Slate Blue", ColorRoles("#F8FAFF", "#E2E8F0", "#0F172A", "#0F172A", "#305F9C", "#FFFFFF"), ColorRoles("#0F172A", "#1E293B", "#F8FAFC", "#F8FAFC", "#87BAFF", "#0F172A")),
        ThemePair("03", "Indigo", ColorRoles("#EEF2FF", "#E0E7FF", "#0F172A", "#0F172A", "#4338CA", "#FFFFFF"), ColorRoles("#111827", "#242442", "#F5F3FF", "#F5F3FF", "#A5B4FC", "#111827")),
        ThemePair("04", "Violet", ColorRoles("#FAF5FF", "#EDE9FE", "#1E1B4B", "#1E1B4B", "#6D28D9", "#FFFFFF"), ColorRoles("#1D142B", "#322143", "#FAF5FF", "#FAF5FF", "#C4B5FD", "#1D142B")),
        ThemePair("05", "Teal", ColorRoles("#F0FDFA", "#CCFBF1", "#064E3B", "#064E3B", "#0F766E", "#FFFFFF"), ColorRoles("#062C2B", "#154240", "#F0FDFA", "#F0FDFA", "#5EEAD4", "#062C2B")),
        ThemePair("06", "Mint", ColorRoles("#F0FDF4", "#DCFCE7", "#14532D", "#14532D", "#047857", "#FFFFFF"), ColorRoles("#072B21", "#164333", "#ECFDF5", "#ECFDF5", "#6EE7B7", "#072B21")),
        ThemePair("07", "Emerald", ColorRoles("#ECFDF5", "#D1FAE5", "#064E3B", "#064E3B", "#047857", "#FFFFFF"), ColorRoles("#04291E", "#0B3E2A", "#ECFDF5", "#ECFDF5", "#6EE7B7", "#04291E")),
        ThemePair("08", "Cyan", ColorRoles("#ECFEFF", "#CFFAFE", "#0C4A6E", "#0C4A6E", "#0E7490", "#FFFFFF"), ColorRoles("#052D44", "#0E4560", "#ECFEFF", "#ECFEFF", "#67E8F9", "#052D44")),
        ThemePair("09", "Sky Blue", ColorRoles("#F0F9FF", "#E0F2FE", "#0F172A", "#0F172A", "#0369A1", "#FFFFFF"), ColorRoles("#0B2538", "#12374F", "#EFF6FF", "#EFF6FF", "#7DD3FC", "#0B2538")),
        ThemePair("10", "Cobalt", ColorRoles("#EFF6FF", "#DBEAFE", "#0F172A", "#0F172A", "#1D4ED8", "#FFFFFF"), ColorRoles("#0F172A", "#203459", "#EFF6FF", "#EFF6FF", "#93C5FD", "#0F172A")),
        ThemePair("11", "Royal Purple", ColorRoles("#F5F3FF", "#E9D5FF", "#1E1B4B", "#1E1B4B", "#6D28D9", "#FFFFFF"), ColorRoles("#21123C", "#362151", "#FAF5FF", "#FAF5FF", "#C4B5FD", "#21123C")),
        ThemePair("12", "Rose", ColorRoles("#FFF1F2", "#FFE4E6", "#881337", "#881337", "#BE123C", "#FFFFFF"), ColorRoles("#33111D", "#4B1B2B", "#FFF1F2", "#FFF1F2", "#FDA4AF", "#33111D")),
        ThemePair("13", "Coral", ColorRoles("#FFF7ED", "#FFEDD5", "#7F1D1D", "#7F1D1D", "#C2410C", "#FFFFFF"), ColorRoles("#361B14", "#50271D", "#FFF7ED", "#FFF7ED", "#FDBA74", "#361B14")),
        ThemePair("14", "Amber", ColorRoles("#FFFBEB", "#FEF3C7", "#78350F", "#78350F", "#92400E", "#FFFFFF"), ColorRoles("#291E10", "#463019", "#FFFBEB", "#FFFBEB", "#FCD34D", "#291E10")),
        ThemePair("15", "Terracotta", ColorRoles("#FDF4F2", "#FDE8E0", "#7C2D12", "#7C2D12", "#9A3412", "#FFFFFF"), ColorRoles("#321D18", "#52271D", "#FFF7ED", "#FFF7ED", "#FCA38B", "#321D18")),
        ThemePair("16", "Olive", ColorRoles("#F7FBEE", "#ECFBD3", "#365314", "#365314", "#4D7C0F", "#FFFFFF"), ColorRoles("#1A2814", "#2C3C1C", "#F7FEE7", "#F7FEE7", "#BEF264", "#1A2814")),
        ThemePair("17", "Sand", ColorRoles("#FAF9F6", "#F3E8D8", "#3F3A2E", "#3F3A2E", "#85632C", "#FFFFFF"), ColorRoles("#27231E", "#3E352B", "#FAF7F0", "#FAF7F0", "#E2BC88", "#27231E")),
        ThemePair("18", "Graphite", ColorRoles("#F8FAFC", "#E5E7EB", "#111827", "#111827", "#334155", "#FFFFFF"), ColorRoles("#111827", "#252E3C", "#F8FAFC", "#F8FAFC", "#CBD5E1", "#111827")),
        ThemePair("19", "Plum", ColorRoles("#FAF5FF", "#EDE9FE", "#3B0764", "#3B0764", "#86198F", "#FFFFFF"), ColorRoles("#2B1439", "#452352", "#FAF5FF", "#FAF5FF", "#F0ABFC", "#2B1439")),
        ThemePair("20", "Sunrise", ColorRoles("#FFF7ED", "#FFE4CC", "#7C2D12", "#7C2D12", "#C2410C", "#FFFFFF"), ColorRoles("#361C14", "#562919", "#FFF7ED", "#FFF7ED", "#FDBA74", "#361C14")),
        ThemePair("21", "Sapphire", ColorRoles("#EFF6FF", "#DBEAFE", "#0F172A", "#0F172A", "#0F62FE", "#FFFFFF"), ColorRoles("#101E36", "#213C60", "#F9FAFB", "#F9FAFB", "#93C5FD", "#101E36")),
        ThemePair("22", "Turquoise", ColorRoles("#ECFDF5", "#CCFBF1", "#064E3B", "#064E3B", "#0F766E", "#FFFFFF"), ColorRoles("#062F2D", "#114A48", "#F9FAFB", "#F9FAFB", "#5EEAD4", "#062F2D")),
        ThemePair("23", "Lime", ColorRoles("#F7FEE7", "#ECFCCB", "#365314", "#365314", "#4D7C0F", "#FFFFFF"), ColorRoles("#1D2B12", "#35461D", "#F9FAFB", "#F9FAFB", "#BEF264", "#1D2B12")),
        ThemePair("24", "Aqua", ColorRoles("#ECFEFF", "#CFFAFE", "#0C4A6E", "#0C4A6E", "#0E7490", "#FFFFFF"), ColorRoles("#0A3245", "#17465C", "#F9FAFB", "#F9FAFB", "#67E8F9", "#0A3245")),
        ThemePair("25", "Fuchsia", ColorRoles("#FAE8FF", "#F5D0FE", "#701A75", "#701A75", "#A21CAF", "#FFFFFF"), ColorRoles("#351338", "#532355", "#F9FAFB", "#F9FAFB", "#F0ABFC", "#351338")),
        ThemePair("26", "Magenta", ColorRoles("#FDF2F8", "#FBCFE8", "#831843", "#831843", "#BE185D", "#FFFFFF"), ColorRoles("#381227", "#54203A", "#F9FAFB", "#F9FAFB", "#F9A8D4", "#381227")),
        ThemePair("27", "Purple Haze", ColorRoles("#F5F3FF", "#E9D5FF", "#3B0764", "#3B0764", "#6D28D9", "#FFFFFF"), ColorRoles("#241339", "#412153", "#F9FAFB", "#F9FAFB", "#C4B5FD", "#241339")),
        ThemePair("28", "Blue Gray", ColorRoles("#F1F5F9", "#E2E8F0", "#1E293B", "#1E293B", "#475569", "#FFFFFF"), ColorRoles("#121E2C", "#253447", "#F9FAFB", "#F9FAFB", "#CBD5E1", "#121E2C")),
        ThemePair("29", "Teal Blue", ColorRoles("#E0F2FE", "#BAE6FD", "#0C4A6E", "#0C4A6E", "#0369A1", "#FFFFFF"), ColorRoles("#082A3B", "#104358", "#F9FAFB", "#F9FAFB", "#7DD3FC", "#082A3B")),
        ThemePair("30", "Seafoam", ColorRoles("#ECFDF5", "#D1FAE5", "#065F46", "#065F46", "#047857", "#FFFFFF"), ColorRoles("#072D22", "#154B37", "#F9FAFB", "#F9FAFB", "#6EE7B7", "#072D22")),
        ThemePair("31", "Forest", ColorRoles("#DCFCE7", "#BBF7D0", "#14532D", "#14532D", "#166534", "#FFFFFF"), ColorRoles("#092A15", "#17412B", "#F9FAFB", "#F9FAFB", "#86EFAC", "#092A15")),
        ThemePair("32", "Sage", ColorRoles("#F0FCE7", "#D9F2C7", "#365314", "#365314", "#4D7C0F", "#FFFFFF"), ColorRoles("#1D2A19", "#34452A", "#F9FAFB", "#F9FAFB", "#B7E1A1", "#1D2A19")),
        ThemePair("33", "Sunset", ColorRoles("#FFF7ED", "#FFEDD5", "#7C2D12", "#7C2D12", "#C2410C", "#FFFFFF"), ColorRoles("#381C14", "#5B2B1E", "#F9FAFB", "#F9FAFB", "#FDBA74", "#381C14")),
        ThemePair("34", "Apricot", ColorRoles("#FFFBEB", "#FEF3C7", "#78350F", "#78350F", "#92400E", "#FFFFFF"), ColorRoles("#32220F", "#543618", "#F9FAFB", "#F9FAFB", "#FCD34D", "#32220F")),
        ThemePair("35", "Peach", ColorRoles("#FFF1F2", "#FFE4E6", "#881337", "#881337", "#BE123C", "#FFFFFF"), ColorRoles("#35151C", "#572934", "#F9FAFB", "#F9FAFB", "#FDA4AF", "#35151C")),
        ThemePair("36", "Salmon", ColorRoles("#FEF2F2", "#FECACA", "#7F1D1D", "#7F1D1D", "#B91C1C", "#FFFFFF"), ColorRoles("#341818", "#572525", "#F9FAFB", "#F9FAFB", "#FCA5A5", "#341818")),
        ThemePair("37", "Chocolate", ColorRoles("#FEF3C7", "#FDE68A", "#78350F", "#78350F", "#92400E", "#FFFFFF"), ColorRoles("#302015", "#50331F", "#F9FAFB", "#F9FAFB", "#FBBF24", "#302015")),
        ThemePair("38", "Rosewood", ColorRoles("#FDF2F8", "#FBCFE8", "#881337", "#881337", "#9F1239", "#FFFFFF"), ColorRoles("#32121D", "#522036", "#F9FAFB", "#F9FAFB", "#F9A8D4", "#32121D")),
        ThemePair("39", "Slate", ColorRoles("#F1F5F9", "#E2E8F0", "#1E293B", "#1E293B", "#475569", "#FFFFFF"), ColorRoles("#111E2D", "#263547", "#F9FAFB", "#F9FAFB", "#CBD5E1", "#111E2D")),
        ThemePair("40", "Midnight", ColorRoles("#F9FAFB", "#E5E7EB", "#111827", "#111827", "#1F2937", "#FFFFFF"), ColorRoles("#0C111B", "#1D2937", "#F9FAFB", "#F9FAFB", "#D1D5DB", "#0C111B")),
    )
    val audio: List<AudioScheme> = listOf(
        AudioScheme("01", "Casino", listOf("sounds/01_casino/01_tap.wav", "sounds/01_casino/02_select.wav", "sounds/01_casino/03_confirm.wav", "sounds/01_casino/04_back.wav", "sounds/01_casino/05_swipe.wav", "sounds/01_casino/06_toggle_on.wav", "sounds/01_casino/07_toggle_off.wav", "sounds/01_casino/08_open.wav", "sounds/01_casino/09_close.wav", "sounds/01_casino/10_success.wav", "sounds/01_casino/11_notice.wav", "sounds/01_casino/12_navigate.wav")),
        AudioScheme("02", "Arcade", listOf("sounds/02_arcade/01_tap.wav", "sounds/02_arcade/02_select.wav", "sounds/02_arcade/03_confirm.wav", "sounds/02_arcade/04_back.wav", "sounds/02_arcade/05_swipe.wav", "sounds/02_arcade/06_toggle_on.wav", "sounds/02_arcade/07_toggle_off.wav", "sounds/02_arcade/08_open.wav", "sounds/02_arcade/09_close.wav", "sounds/02_arcade/10_success.wav", "sounds/02_arcade/11_notice.wav", "sounds/02_arcade/12_navigate.wav")),
        AudioScheme("03", "Concert Band", listOf("sounds/03_band/01_tap.wav", "sounds/03_band/02_select.wav", "sounds/03_band/03_confirm.wav", "sounds/03_band/04_back.wav", "sounds/03_band/05_swipe.wav", "sounds/03_band/06_toggle_on.wav", "sounds/03_band/07_toggle_off.wav", "sounds/03_band/08_open.wav", "sounds/03_band/09_close.wav", "sounds/03_band/10_success.wav", "sounds/03_band/11_notice.wav", "sounds/03_band/12_navigate.wav")),
        AudioScheme("04", "Sci-Fi", listOf("sounds/04_sci-fi/01_tap.wav", "sounds/04_sci-fi/02_select.wav", "sounds/04_sci-fi/03_confirm.wav", "sounds/04_sci-fi/04_back.wav", "sounds/04_sci-fi/05_swipe.wav", "sounds/04_sci-fi/06_toggle_on.wav", "sounds/04_sci-fi/07_toggle_off.wav", "sounds/04_sci-fi/08_open.wav", "sounds/04_sci-fi/09_close.wav", "sounds/04_sci-fi/10_success.wav", "sounds/04_sci-fi/11_notice.wav", "sounds/04_sci-fi/12_navigate.wav")),
        AudioScheme("05", "Computer", listOf("sounds/05_computer/01_tap.wav", "sounds/05_computer/02_select.wav", "sounds/05_computer/03_confirm.wav", "sounds/05_computer/04_back.wav", "sounds/05_computer/05_swipe.wav", "sounds/05_computer/06_toggle_on.wav", "sounds/05_computer/07_toggle_off.wav", "sounds/05_computer/08_open.wav", "sounds/05_computer/09_close.wav", "sounds/05_computer/10_success.wav", "sounds/05_computer/11_notice.wav", "sounds/05_computer/12_navigate.wav")),
        AudioScheme("06", "Cathedral Bells", listOf("sounds/06_cathedral/01_tap.wav", "sounds/06_cathedral/02_select.wav", "sounds/06_cathedral/03_confirm.wav", "sounds/06_cathedral/04_back.wav", "sounds/06_cathedral/05_swipe.wav", "sounds/06_cathedral/06_toggle_on.wav", "sounds/06_cathedral/07_toggle_off.wav", "sounds/06_cathedral/08_open.wav", "sounds/06_cathedral/09_close.wav", "sounds/06_cathedral/10_success.wav", "sounds/06_cathedral/11_notice.wav", "sounds/06_cathedral/12_navigate.wav")),
        AudioScheme("07", "Retro 8-Bit", listOf("sounds/07_8-bit/01_tap.wav", "sounds/07_8-bit/02_select.wav", "sounds/07_8-bit/03_confirm.wav", "sounds/07_8-bit/04_back.wav", "sounds/07_8-bit/05_swipe.wav", "sounds/07_8-bit/06_toggle_on.wav", "sounds/07_8-bit/07_toggle_off.wav", "sounds/07_8-bit/08_open.wav", "sounds/07_8-bit/09_close.wav", "sounds/07_8-bit/10_success.wav", "sounds/07_8-bit/11_notice.wav", "sounds/07_8-bit/12_navigate.wav")),
        AudioScheme("08", "Forest", listOf("sounds/08_forest/01_tap.wav", "sounds/08_forest/02_select.wav", "sounds/08_forest/03_confirm.wav", "sounds/08_forest/04_back.wav", "sounds/08_forest/05_swipe.wav", "sounds/08_forest/06_toggle_on.wav", "sounds/08_forest/07_toggle_off.wav", "sounds/08_forest/08_open.wav", "sounds/08_forest/09_close.wav", "sounds/08_forest/10_success.wav", "sounds/08_forest/11_notice.wav", "sounds/08_forest/12_navigate.wav")),
        AudioScheme("09", "Ocean", listOf("sounds/09_ocean/01_tap.wav", "sounds/09_ocean/02_select.wav", "sounds/09_ocean/03_confirm.wav", "sounds/09_ocean/04_back.wav", "sounds/09_ocean/05_swipe.wav", "sounds/09_ocean/06_toggle_on.wav", "sounds/09_ocean/07_toggle_off.wav", "sounds/09_ocean/08_open.wav", "sounds/09_ocean/09_close.wav", "sounds/09_ocean/10_success.wav", "sounds/09_ocean/11_notice.wav", "sounds/09_ocean/12_navigate.wav")),
        AudioScheme("10", "Spacecraft", listOf("sounds/10_spacecraft/01_tap.wav", "sounds/10_spacecraft/02_select.wav", "sounds/10_spacecraft/03_confirm.wav", "sounds/10_spacecraft/04_back.wav", "sounds/10_spacecraft/05_swipe.wav", "sounds/10_spacecraft/06_toggle_on.wav", "sounds/10_spacecraft/07_toggle_off.wav", "sounds/10_spacecraft/08_open.wav", "sounds/10_spacecraft/09_close.wav", "sounds/10_spacecraft/10_success.wav", "sounds/10_spacecraft/11_notice.wav", "sounds/10_spacecraft/12_navigate.wav")),
        AudioScheme("11", "Workshop", listOf("sounds/11_workshop/01_tap.wav", "sounds/11_workshop/02_select.wav", "sounds/11_workshop/03_confirm.wav", "sounds/11_workshop/04_back.wav", "sounds/11_workshop/05_swipe.wav", "sounds/11_workshop/06_toggle_on.wav", "sounds/11_workshop/07_toggle_off.wav", "sounds/11_workshop/08_open.wav", "sounds/11_workshop/09_close.wav", "sounds/11_workshop/10_success.wav", "sounds/11_workshop/11_notice.wav", "sounds/11_workshop/12_navigate.wav")),
        AudioScheme("12", "Orchestra", listOf("sounds/12_orchestra/01_tap.wav", "sounds/12_orchestra/02_select.wav", "sounds/12_orchestra/03_confirm.wav", "sounds/12_orchestra/04_back.wav", "sounds/12_orchestra/05_swipe.wav", "sounds/12_orchestra/06_toggle_on.wav", "sounds/12_orchestra/07_toggle_off.wav", "sounds/12_orchestra/08_open.wav", "sounds/12_orchestra/09_close.wav", "sounds/12_orchestra/10_success.wav", "sounds/12_orchestra/11_notice.wav", "sounds/12_orchestra/12_navigate.wav")),
        AudioScheme("13", "Jazz Club", listOf("sounds/13_jazz/01_tap.wav", "sounds/13_jazz/02_select.wav", "sounds/13_jazz/03_confirm.wav", "sounds/13_jazz/04_back.wav", "sounds/13_jazz/05_swipe.wav", "sounds/13_jazz/06_toggle_on.wav", "sounds/13_jazz/07_toggle_off.wav", "sounds/13_jazz/08_open.wav", "sounds/13_jazz/09_close.wav", "sounds/13_jazz/10_success.wav", "sounds/13_jazz/11_notice.wav", "sounds/13_jazz/12_navigate.wav")),
        AudioScheme("14", "Carnival", listOf("sounds/14_carnival/01_tap.wav", "sounds/14_carnival/02_select.wav", "sounds/14_carnival/03_confirm.wav", "sounds/14_carnival/04_back.wav", "sounds/14_carnival/05_swipe.wav", "sounds/14_carnival/06_toggle_on.wav", "sounds/14_carnival/07_toggle_off.wav", "sounds/14_carnival/08_open.wav", "sounds/14_carnival/09_close.wav", "sounds/14_carnival/10_success.wav", "sounds/14_carnival/11_notice.wav", "sounds/14_carnival/12_navigate.wav")),
        AudioScheme("15", "Railway Station", listOf("sounds/15_railway/01_tap.wav", "sounds/15_railway/02_select.wav", "sounds/15_railway/03_confirm.wav", "sounds/15_railway/04_back.wav", "sounds/15_railway/05_swipe.wav", "sounds/15_railway/06_toggle_on.wav", "sounds/15_railway/07_toggle_off.wav", "sounds/15_railway/08_open.wav", "sounds/15_railway/09_close.wav", "sounds/15_railway/10_success.wav", "sounds/15_railway/11_notice.wav", "sounds/15_railway/12_navigate.wav")),
        AudioScheme("16", "Cyberpunk", listOf("sounds/16_cyberpunk/01_tap.wav", "sounds/16_cyberpunk/02_select.wav", "sounds/16_cyberpunk/03_confirm.wav", "sounds/16_cyberpunk/04_back.wav", "sounds/16_cyberpunk/05_swipe.wav", "sounds/16_cyberpunk/06_toggle_on.wav", "sounds/16_cyberpunk/07_toggle_off.wav", "sounds/16_cyberpunk/08_open.wav", "sounds/16_cyberpunk/09_close.wav", "sounds/16_cyberpunk/10_success.wav", "sounds/16_cyberpunk/11_notice.wav", "sounds/16_cyberpunk/12_navigate.wav")),
        AudioScheme("17", "Sports Arena", listOf("sounds/17_stadium/01_tap.wav", "sounds/17_stadium/02_select.wav", "sounds/17_stadium/03_confirm.wav", "sounds/17_stadium/04_back.wav", "sounds/17_stadium/05_swipe.wav", "sounds/17_stadium/06_toggle_on.wav", "sounds/17_stadium/07_toggle_off.wav", "sounds/17_stadium/08_open.wav", "sounds/17_stadium/09_close.wav", "sounds/17_stadium/10_success.wav", "sounds/17_stadium/11_notice.wav", "sounds/17_stadium/12_navigate.wav")),
        AudioScheme("18", "Percussion Studio", listOf("sounds/18_percussion/01_tap.wav", "sounds/18_percussion/02_select.wav", "sounds/18_percussion/03_confirm.wav", "sounds/18_percussion/04_back.wav", "sounds/18_percussion/05_swipe.wav", "sounds/18_percussion/06_toggle_on.wav", "sounds/18_percussion/07_toggle_off.wav", "sounds/18_percussion/08_open.wav", "sounds/18_percussion/09_close.wav", "sounds/18_percussion/10_success.wav", "sounds/18_percussion/11_notice.wav", "sounds/18_percussion/12_navigate.wav")),
        AudioScheme("19", "Fantasy Magic", listOf("sounds/19_fantasy/01_tap.wav", "sounds/19_fantasy/02_select.wav", "sounds/19_fantasy/03_confirm.wav", "sounds/19_fantasy/04_back.wav", "sounds/19_fantasy/05_swipe.wav", "sounds/19_fantasy/06_toggle_on.wav", "sounds/19_fantasy/07_toggle_off.wav", "sounds/19_fantasy/08_open.wav", "sounds/19_fantasy/09_close.wav", "sounds/19_fantasy/10_success.wav", "sounds/19_fantasy/11_notice.wav", "sounds/19_fantasy/12_navigate.wav")),
        AudioScheme("20", "Vintage Radio", listOf("sounds/20_vintage-radio/01_tap.wav", "sounds/20_vintage-radio/02_select.wav", "sounds/20_vintage-radio/03_confirm.wav", "sounds/20_vintage-radio/04_back.wav", "sounds/20_vintage-radio/05_swipe.wav", "sounds/20_vintage-radio/06_toggle_on.wav", "sounds/20_vintage-radio/07_toggle_off.wav", "sounds/20_vintage-radio/08_open.wav", "sounds/20_vintage-radio/09_close.wav", "sounds/20_vintage-radio/10_success.wav", "sounds/20_vintage-radio/11_notice.wav", "sounds/20_vintage-radio/12_navigate.wav")),
    )
}

/** Converts approved selection into a deterministic state with no privileged OS access. */
class SettingsEngine(private val store: PreferenceStore) {
    private val pairs = Catalog.pairs.associateBy { it.id }
    private val sounds = Catalog.audio.associateBy { it.id }
    var themeId: String = store.get("theme")?.takeIf { pairs.containsKey(it) } ?: "01"
        private set
    var appearance: Appearance = runCatching { Appearance.valueOf(store.get("appearance") ?: "FOLLOW_DEVICE") }.getOrDefault(Appearance.FOLLOW_DEVICE)
        private set
    var audioId: String = store.get("audio")?.takeIf { sounds.containsKey(it) } ?: "none"
        private set
    var volumePercent: Int = (store.get("volume")?.toIntOrNull() ?: 35).coerceIn(0,100)
        private set
    private val positions = mutableMapOf<String,Int>()

    fun chooseTheme(id: String): Boolean {
        if (!pairs.containsKey(id)) return false
        themeId=id; store.put("theme", id); return true
    }
    fun chooseAppearance(mode: Appearance) { appearance=mode; store.put("appearance",mode.name) }
    fun currentPalette(systemDark: Boolean): ColorRoles {
        val dark = when (appearance) {
            Appearance.FOLLOW_DEVICE -> systemDark
            Appearance.DARK -> true
            Appearance.LIGHT -> false
        }
        return pairs.getValue(themeId).let { if(dark) it.dark else it.light }
    }
    fun chooseAudio(id: String): Boolean {
        if (id != "none" && !sounds.containsKey(id)) return false
        audioId=id; store.put("audio",id); return true
    }
    fun setVolume(percent: Int) {
        volumePercent=percent.coerceIn(0,100); store.put("volume",volumePercent.toString())
    }
    /** A tap, swipe, or eligible click advances exactly once, wraps after the twelfth cue.
     * `None` returns null and does not access a sound file.
     */
    fun nextInteractionSound(): String? {
        if (audioId == "none" || volumePercent==0) return null
        val scheme = sounds[audioId] ?: return null
        if (scheme.cues.isEmpty()) return null
        val pos = positions[audioId] ?: 0
        positions[audioId] = (pos + 1) % scheme.cues.size
        return scheme.cues[pos]
    }
    fun previewSequence(id: String): List<String> = sounds[id]?.cues.orEmpty()
    fun stopPreview() { /* Playback adapter owns audio focus and must stop audio on Back/background. */ }
}
