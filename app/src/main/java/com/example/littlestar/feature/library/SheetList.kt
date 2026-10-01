package com.example.littlestar.feature.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.littlestar.core.design.theme.Dimens
import com.example.littlestar.core.util.formatMinutes
import com.example.littlestar.domain.model.SheetWithPractice

/**
 * 악보 목록. 한 곡을 탭하면 상세 화면으로 이동한다.
 * 40개 정도의 항목이므로 스크롤 성능을 고려하지 않고 단순 Column으로 렌더링한다.
 */
@Composable
fun SheetList(
    sheets: List<SheetWithPractice>,
    onSheetClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        sheets.forEachIndexed { index, sheet ->
            SheetRow(sheet = sheet, onClick = { onSheetClick(sheet.sheet.id) })
            if (index != sheets.lastIndex) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
private fun SheetRow(
    sheet: SheetWithPractice,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = Dimens.ItemSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = sheet.sheet.title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            sheet.sheet.composer?.let { composer ->
                Spacer(Modifier.height(2.dp))
                Text(
                    text = composer,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (sheet.totalPracticeMinutes > 0) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "누적 연습 ${formatMinutes(sheet.totalPracticeMinutes)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        Text(
            text = if (sheet.pageCount > 0) "${sheet.pageCount}쪽" else "악보 등록 필요",
            style = MaterialTheme.typography.labelMedium,
            color = if (sheet.pageCount > 0) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.secondary
            },
        )
    }
}
