package com.mongodb.quickpr.ui

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.layout.panel
import com.mongodb.quickpr.models.SettingsModel
import javax.swing.JComponent
import javax.swing.JPasswordField
import javax.swing.JTextField
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class SettingsDialogWrapper(private val model: SettingsModel, private val validateSettings: (SettingsModel) -> String?) :
    DialogWrapper(true) {
    override fun createCenterPanel(): JComponent {
        return panel {
            row {
                label("Please follow the steps")
                link("here") {
                    BrowserUtil.browse("https://wiki.corp.mongodb.com/display/MMS/Setup+the+MMS+Python+Environment#SetuptheMMSPythonEnvironment-GetOAuthAccessforJIRAScripts")
                }
                label("to set up your JIRA config file")
            }
            row {
                label("JIRA Config File")

                // this does not work, why?
                // textField(model::jiraConfigPath)

                val jiraField = JTextField(model.jiraConfigPath)
                jiraField()

                jiraField.document.addDocumentListener(
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
                            model.jiraConfigPath = e!!.document.getText(0, e.document.length)
                        }
                    }
                )
            }
            row {
                label("GitHub Token")
                val passwordField = JPasswordField(model.githubToken)
                passwordField()

                passwordField.document.addDocumentListener(
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
                            model.githubToken = e!!.document.getText(0, e.document.length)
                        }
                    }
                )
            }
        }
    }

    override fun doValidate(): ValidationInfo? {
        return validateSettings(model)?.let { ValidationInfo(it) }
    }

    init {
        init()
        startTrackingValidation()
        title = "QuickPR Settings"
    }
}
