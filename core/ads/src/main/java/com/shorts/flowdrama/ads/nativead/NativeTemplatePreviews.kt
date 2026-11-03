package com.shorts.flowdrama.ads.nativead

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.shorts.flowdrama.ads.config.NativeTemplate

class NativeTemplatePreviewProvider : PreviewParameterProvider<NativeTemplate> {
    override val values = NativeTemplate.entries.asSequence()
}

@Preview(name = "Native templates", widthDp = 360, showBackground = true, backgroundColor = 0xFF160F0A)
@Composable
private fun TemplatePreview(@PreviewParameter(NativeTemplatePreviewProvider::class) template: NativeTemplate) {
    NativeAdPreview(template, attribution = "Ad · Preview", modifier = Modifier.padding(12.dp))
}

/** Developer gallery: no SDK requests. Suitable for a debug screen or a screenshot test. */
@Composable
fun NativeTemplateGallery(modifier: Modifier = Modifier) {
    LazyColumn(modifier.background(Color(0xFF160F0A)), contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(NativeTemplate.entries, key = { it.key }) { template ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(template.key, color = Color.White)
                NativeAdPreview(template, attribution = "Ad · Preview")
            }
        }
    }
}
