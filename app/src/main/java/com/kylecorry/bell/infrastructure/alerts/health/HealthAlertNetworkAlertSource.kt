package com.kylecorry.bell.infrastructure.alerts.health

import android.content.Context
import com.kylecorry.bell.domain.Alert
import com.kylecorry.bell.domain.Category
import com.kylecorry.bell.domain.Certainty
import com.kylecorry.bell.domain.Constants
import com.kylecorry.bell.domain.Severity
import com.kylecorry.bell.domain.Urgency
import com.kylecorry.bell.infrastructure.alerts.AlertLoader
import com.kylecorry.bell.infrastructure.alerts.AlertSource
import com.kylecorry.bell.infrastructure.alerts.FileType
import com.kylecorry.bell.infrastructure.parsers.DateTimeParser
import com.kylecorry.bell.infrastructure.parsers.selectors.Selector.Companion.text
import com.kylecorry.bell.infrastructure.utils.HtmlTextFormatter
import java.time.Duration

// https://tools.cdc.gov/api/v2/resources/media/126194/syndicate.json
class HealthAlertNetworkAlertSource(context: Context) : AlertSource {

    private val loader = AlertLoader(context)

    override suspend fun load(): List<Alert> {
        val rawAlerts = loader.load(
            FileType.JSON,
            "https://wcmssearch.cdc.gov/srch/internet_wcms/wcms_widget?q=*:*&fq=(type_txt:%22DFE%20Page%22%20AND%20cdc_dfe_template_str:(%22cdc_health_alert%22))%20OR%20type_txt:(%22Page%22)&fq=(topical_site_context_s:1984-2%20AND%20(permalink:*/han/php/notices/*))&fq=-id:1984_478&fq=-status:%22cdc_archive%22&fq=-is_hidden_b:true&wt=json&start=0&rows=10&fl=title_txt,permalink,cdc_article_date_dt,cdc_last_reviewed_date_dt&sort=cdc_last_reviewed_date_dt%20desc,cdc_article_date_dt%20desc&facet=on&facet.mincount=1&facet.field=cdc_topics_taxonomy_str&facet.field=cdc_topics_taxonomy_parents_str&facet.field=cdc_last_reviewed_date_dt&facet.limit=2000&echoParams=none&indent=false",
            "response.docs",
            mapOf(
                "headline" to text("title_txt"),
                "link" to text("permalink"),
                "publishedDate" to text("cdc_article_date_dt")
            )
        )

        return rawAlerts.mapNotNull {
            val headline = it["headline"] ?: return@mapNotNull null
            val link = it["link"]?.replace("emergency.", "www.") ?: return@mapNotNull null
            val sent =
                DateTimeParser.parseInstant(it["publishedDate"] ?: "") ?: return@mapNotNull null

            val event = if (headline.contains("Health Alert Network (HAN)")) {
                headline.substringAfter("– ")
            } else {
                headline
            }


            Alert(
                id = 0,
                identifier = link,
                sender = "CDC",
                sent = sent,
                source = getUUID(),
                category = Category.Health,
                event = event,
                urgency = Urgency.Unknown,
                severity = Severity.Unknown,
                certainty = Certainty.Observed,
                link = link,
                headline = headline,
                expires = sent.plus(Duration.ofDays(Constants.DEFAULT_EXPIRATION_DAYS)),
                isDownloadRequired = true
            )
        }
    }

    override fun getUUID(): String {
        return "805b0e35-8e77-4425-8089-6c9814682139"
    }

    override fun updateFromFullText(alert: Alert, fullText: String): Alert {
        val severity = when {
            fullText.contains("HAN_badge_HEALTH_ADVISORY") -> Severity.Minor
            fullText.contains("HAN_badge_HEALTH_UPDATE") -> Severity.Minor
            else -> Severity.Severe
        }

        val eventPrefix = when {
            fullText.contains("HAN_badge_HEALTH_ADVISORY") -> "Health Advisory:"
            fullText.contains("HAN_badge_HEALTH_UPDATE") -> "Health Update:"
            else -> "Health Alert:"
        }

        val summary = HtmlTextFormatter.getText(
            fullText.substringAfter(">Summary</h2>")
                .substringBefore("<div")
        )

        return alert.copy(
            event = "$eventPrefix ${alert.event}",
            severity = severity,
            description = summary,
            fullText = HtmlTextFormatter.getText(fullText)
        )
    }

}