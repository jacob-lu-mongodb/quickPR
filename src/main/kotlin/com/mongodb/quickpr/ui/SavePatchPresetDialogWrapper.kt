package com.mongodb.quickpr.ui

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.layout.CCFlags
import com.intellij.ui.layout.panel
import com.mongodb.quickpr.models.PatchPreset
import javax.swing.JComponent

class SavePatchPresetDialogWrapper(
    private val model: PatchPreset
) :
    DialogWrapper(true) {

    override fun createCenterPanel(): JComponent {
        return panel {

            row {
                label("Save the preset as:")
                eagerBoundTextField(model::name).constraints(CCFlags.growX, CCFlags.pushX)

                setSize(300, 50)
            }
        }
    }

    override fun doValidate(): ValidationInfo? {
        var validationResult: String? =
            if (model.name.isNullOrEmpty() || model.name.contains("*")) "Invalid name" else null

        return validationResult?.let { ValidationInfo(it) }
    }

    init {
        init()
        startTrackingValidation()
        title = "Save Patch Preset"
    }
}
