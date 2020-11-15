package com.mongodb.quickpr.ui

import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.ui.layout.Cell
import com.intellij.ui.layout.CellBuilder
import com.intellij.ui.layout.PropertyBinding
import com.intellij.ui.layout.toBinding
import javax.swing.JTextField
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.text.JTextComponent
import kotlin.reflect.KMutableProperty0

fun Cell.eagerBoundTextField(prop: KMutableProperty0<String>, columns: Int? = null): CellBuilder<JBTextField> = eagerBoundTextBasedField(JBTextField::class.java, prop, columns)

fun Cell.eagerBoundTextArea(prop: KMutableProperty0<String>): CellBuilder<JBTextArea> = eagerBoundTextBasedComponent(JBTextArea::class.java, prop.toBinding())

fun Cell.eagerBoundPasswordField(prop: KMutableProperty0<String>, columns: Int? = null): CellBuilder<JBPasswordField> = eagerBoundTextBasedField(JBPasswordField::class.java, prop, columns)

private fun <T : JTextField> Cell.eagerBoundTextBasedField(clazz: Class<T>, prop: KMutableProperty0<String>, columns: Int? = null): CellBuilder<T> = eagerBoundTextBasedField(clazz, prop.toBinding(), columns)

private fun <T : JTextField> Cell.eagerBoundTextBasedField(clazz: Class<T>, binding: PropertyBinding<String>, columns: Int? = null): CellBuilder<T> {
    val builder = eagerBoundTextBasedComponent(clazz, binding)
    builder.component.columns = columns ?: 0

    return builder
}

private fun <T : JTextComponent> Cell.eagerBoundTextBasedComponent(clazz: Class<T>, binding: PropertyBinding<String>): CellBuilder<T> {
    val instance = clazz.getConstructor().newInstance()
    instance.text = binding.get()

    return component(instance)
        .withEagerBoundTextBinding(binding)
}

private fun <T : JTextComponent> CellBuilder<T>.withEagerBoundTextBinding(modelBinding: PropertyBinding<String>): CellBuilder<T> {
    component.document.addDocumentListener(
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
                modelBinding.set(e!!.document.getText(0, e.document.length))
            }
        }
    )

    onReset { component.text = modelBinding.get() }

    return this
}
