package com.mongodb.quickpr.ui

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.layout.panel
import com.mongodb.quickpr.actions.SettingsAction
import com.mongodb.quickpr.models.PRModel
import java.awt.Dimension
import java.awt.event.ActionEvent
import javax.swing.AbstractAction
import javax.swing.Action
import javax.swing.JComponent
import javax.swing.JTextArea
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class MainDialogWrapper(
    private val model: PRModel,
    private val doAction: () -> Boolean,
    private val actionEvent: AnActionEvent
) :
    DialogWrapper(true) {
    override fun createCenterPanel(): JComponent {
        return panel {
            row {
                label("Title")
                textField(model::title)
            }
            row {
                label("Description")
                val textArea = JTextArea(model.description)
                textArea.size = Dimension(800, 400)
                textArea.lineWrap = true
                textArea.wrapStyleWord = true
                textArea()

                textArea.document.addDocumentListener(
                    object : DocumentListener {
                        override fun insertUpdate(e: DocumentEvent?) {
                            updateModel(e)
                        }

                        override fun removeUpdate(e: DocumentEvent?) {
                            updateModel(e)
                        }

                        override fun changedUpdate(e: DocumentEvent?) {
                            updateModel(e)
                        }

                        private fun updateModel(e: DocumentEvent?) {
                            model.description = e!!.document.getText(0, e.document.length)
                        }
                    }
                )
            }
        }
    }

    override fun createLeftSideActions(): Array<Action> {
        return super.createLeftSideActions() + createSettingsAction()
    }

    private fun createSettingsAction(): Action {
        return object : AbstractAction("Settings") {
            override fun actionPerformed(e: ActionEvent?) {
                SettingsAction("QuickPR Settings", null, null).actionPerformed(actionEvent)
            }
        }
    }

    override fun doOKAction() {
        if(doAction()) {
            super.doOKAction()
        }
    }

    init {
        init()
        title = "Create PR"
    }
}
