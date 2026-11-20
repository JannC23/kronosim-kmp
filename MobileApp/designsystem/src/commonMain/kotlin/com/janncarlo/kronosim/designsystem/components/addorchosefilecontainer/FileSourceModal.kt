package com.janncarlo.kronosim.designsystem.components.addorchosefilecontainer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.janncarlo.kronosim.designsystem.components.AppButton
import com.janncarlo.kronosim.designsystem.components.ButtonSize
import com.janncarlo.kronosim.designsystem.components.TipsUiComponent
import com.janncarlo.kronosim.designsystem.components.modals.AppModalBottomSheet
import com.janncarlo.kronosim.designsystem.generated.resources.UiRes
import com.janncarlo.kronosim.designsystem.generated.resources.btn_choose_from_gallery
import com.janncarlo.kronosim.designsystem.generated.resources.btn_record_video
import com.janncarlo.kronosim.designsystem.generated.resources.btn_take_photo
import com.janncarlo.kronosim.designsystem.generated.resources.ic_camera
import com.janncarlo.kronosim.designsystem.generated.resources.ic_file_image
import com.janncarlo.kronosim.designsystem.generated.resources.ic_file_video
import com.janncarlo.kronosim.designsystem.generated.resources.ic_record_video
import com.janncarlo.kronosim.designsystem.generated.resources.select_photo_source_modal_title
import com.janncarlo.kronosim.designsystem.generated.resources.select_video_source_modal_title
import com.janncarlo.kronosim.designsystem.generated.resources.tip_text_for_better_result
import com.janncarlo.kronosim.designsystem.theme.AppTheme
import com.janncarlo.kronosim.designsystem.util.UiText
import org.jetbrains.compose.resources.stringResource

@Composable
fun FileSourceModal(
    type: FileUiType,
    onDismiss: () -> Unit,
    onClickCaptureOrRecord: () -> Unit,
    onClickSelectFromGallery: () -> Unit,
    isTipsTextVisible: Boolean = true,
    isCaptureSupported: Boolean = true,
    hintUiText: UiText? = null,
) {
    val modalTitle =
        when (type) {
            FileUiType.IMAGE -> stringResource(UiRes.string.select_photo_source_modal_title)
            FileUiType.VIDEO -> stringResource(UiRes.string.select_video_source_modal_title)
        }

    val btnRecordOrCaptureNowText =
        when (type) {
            FileUiType.IMAGE -> stringResource(UiRes.string.btn_take_photo)
            FileUiType.VIDEO -> stringResource(UiRes.string.btn_record_video)
        }

    val btnRecordOrCaptureNowIcon =
        when (type) {
            FileUiType.IMAGE -> UiRes.drawable.ic_camera
            FileUiType.VIDEO -> UiRes.drawable.ic_record_video
        }

    val btnSelectFromGalleryIcon =
        when (type) {
            FileUiType.IMAGE -> UiRes.drawable.ic_file_image
            FileUiType.VIDEO -> UiRes.drawable.ic_file_video
        }

    AppModalBottomSheet(
        hideButtons = true,
        title = modalTitle,
        isDismissable = true,
        onDismiss = onDismiss,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.groupedVerticalElementSpacing),
        ) {
            if (isCaptureSupported) {
                AppButton(
                    size = ButtonSize.SMALL,
                    modifier = Modifier.fillMaxWidth(),
                    text = btnRecordOrCaptureNowText,
                    startIcon = btnRecordOrCaptureNowIcon,
                    onClick = onClickCaptureOrRecord,
                )
            }

            AppButton(
                size = ButtonSize.SMALL,
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(UiRes.string.btn_choose_from_gallery),
                startIcon = btnSelectFromGalleryIcon,
                onClick = onClickSelectFromGallery,
            )

            if (isTipsTextVisible) {
                TipsUiComponent(
                    text =
                    hintUiText?.value
                        ?: stringResource(UiRes.string.tip_text_for_better_result),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
