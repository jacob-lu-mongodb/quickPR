package com.mongodb.quickpr.ui

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.layout.panel
import com.mongodb.quickpr.actions.SettingsAction
import com.mongodb.quickpr.models.PRModel
import java.awt.Dimension
import java.awt.event.ActionEvent
import javax.swing.AbstractAction
import javax.swing.Action
import javax.swing.JComponent

const val TEXT_AREA_WIDTH = 800
const val TEXT_AREA_HEIGHT = 400

class MainDialogWrapper(
    private val model: PRModel,
    private val doAction: () -> Boolean
) :
    DialogWrapper(true) {
    override fun createCenterPanel(): JComponent {
        return panel {
            row {
                label("Title")
                eagerBoundTextField(model::title)
            }
            row {
                label("Description")
                val textArea = eagerBoundTextArea(model::description).component

                textArea.size = Dimension(TEXT_AREA_WIDTH, TEXT_AREA_HEIGHT)
                textArea.lineWrap = true
                textArea.wrapStyleWord = true
            }
        }
    }

    override fun createLeftSideActions(): Array<Action> {
        return super.createLeftSideActions() + createSettingsAction()
    }

    private fun createSettingsAction(): Action {
        return object : AbstractAction("Settings") {
            override fun actionPerformed(e: ActionEvent?) {
                SettingsAction.invokeAction()
            }
        }
    }

    override fun doOKAction() {
        if (doAction()) {
            super.doOKAction()
        }
    }

    init {
        init()
        title = "Create PR"
    }
}
