package net.sath.data

import android.content.Context
import android.content.SharedPreferences
import net.sath.model.LagConfig

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("cfg", Context.MODE_PRIVATE)

    fun loadConfig(): LagConfig {
        val blockedIpsSet = prefs.getStringSet("fw_ips", null)
        val blockedDomainsSet = prefs.getStringSet("fw_domains", null)

        val defaultIps = listOf(
            "103.249.28.0/24",
            "157.240.241.35",
            "185.199.108.153"
        )
        val defaultDomains = listOf(
            "telemetry.game-analytics.com",
            "adservice.google.com",
            "crashlyticsreports-pa.googleapis.com"
        )

        return LagConfig(
            sizeFreeze = prefs.getInt("sf_z", 150),
            sizeGhost = prefs.getInt("sg_z", 90),
            sizeTele = prefs.getInt("st_z", 90),
            alphaFreeze = prefs.getInt("af_a", 100),
            alphaGhost = prefs.getInt("ag_a", 100),
            alphaTele = prefs.getInt("at_a", 100),
            switchPen = prefs.getBoolean("sw_p", false),
            switchFreeze = prefs.getBoolean("sw_f", true),
            switchGhost = prefs.getBoolean("sw_g", true),
            switchTele = prefs.getBoolean("sw_t", true),
            switchDownloadBoost = prefs.getBoolean("sw_db", false),
            switchReduceFpsDrop = prefs.getBoolean("sw_rf", true),
            freezeDropMin = prefs.getInt("fz_drop_min", 100),
            freezeDropMax = prefs.getInt("fz_drop_max", 500),
            ghostDropMin = prefs.getInt("gh_drop_min", 80),
            ghostDropMax = prefs.getInt("gh_drop_max", 350),
            freezeDurationSeconds = prefs.getInt("fz_dur_sec", 5),
            isFirewallEnabled = prefs.getBoolean("fw_enabled", true),
            blockedIps = blockedIpsSet?.toList() ?: defaultIps,
            blockedDomains = blockedDomainsSet?.toList() ?: defaultDomains,
            language = prefs.getString("ui_lang", "en") ?: "en"
        )
    }

    fun saveConfig(config: LagConfig) {
        prefs.edit()
            .putInt("sf_z", config.sizeFreeze)
            .putInt("sg_z", config.sizeGhost)
            .putInt("st_z", config.sizeTele)
            .putInt("af_a", config.alphaFreeze)
            .putInt("ag_a", config.alphaGhost)
            .putInt("at_a", config.alphaTele)
            .putBoolean("sw_p", config.switchPen)
            .putBoolean("sw_f", config.switchFreeze)
            .putBoolean("sw_g", config.switchGhost)
            .putBoolean("sw_t", config.switchTele)
            .putBoolean("sw_db", config.switchDownloadBoost)
            .putBoolean("sw_rf", config.switchReduceFpsDrop)
            .putInt("fz_drop_min", config.freezeDropMin)
            .putInt("fz_drop_max", config.freezeDropMax)
            .putInt("gh_drop_min", config.ghostDropMin)
            .putInt("gh_drop_max", config.ghostDropMax)
            .putInt("fz_dur_sec", config.freezeDurationSeconds)
            .putBoolean("fw_enabled", config.isFirewallEnabled)
            .putStringSet("fw_ips", config.blockedIps.toSet())
            .putStringSet("fw_domains", config.blockedDomains.toSet())
            .putString("ui_lang", config.language)
            .apply()
    }

    fun setLanguage(lang: String) {
        prefs.edit().putString("ui_lang", lang).apply()
    }
}
