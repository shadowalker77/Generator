package com.alirezabdn.generator

import com.squareup.kotlinpoet.*
import java.io.File
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement

@SupportedSourceVersion(SourceVersion.RELEASE_8)
class Processor : AbstractProcessor() {
    companion object {
        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"
    }

    override fun process(
        annotations: MutableSet<out TypeElement>?,
        roundEnv: RoundEnvironment?
    ): Boolean {

        val kaptKotlinGeneratedDir = processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME]
            ?: return false

        roundEnv?.getElementsAnnotatedWith(AyanAPI::class.java)?.forEach { element ->
            val input = element.enclosedElements.let {
                it.firstOrNull { it.simpleName.endsWith("Input") }?.let {
                    ParameterSpec.builder("input", it.asType().asTypeName())
                }
            }
            val output = element.enclosedElements.let {
                it.firstOrNull { it.simpleName.endsWith("Output") }?.let {
                    ParameterSpec.builder("output", it.asType().asTypeName().copy(nullable = true))
                }
            }
            val funcBuilder = FunSpec.builder("call" + element.simpleName)
                .receiver(Class.forName("ir.ayantech.ayannetworking.api.AyanApi"))
            if (input != null) funcBuilder.addParameter(input.build())
            funcBuilder.addParameter(
                ParameterSpec.builder(
                    "callback", LambdaTypeName.get(
                        parameters = if (output != null) arrayOf(output.build()) else arrayOf(),
                        returnType = Unit::class.asClassName()
                    )
                ).build()
            )
            val endPoint =
                element.getAnnotation(AyanAPI::class.java).endPoint.ifEmpty { element.simpleName }
            funcBuilder.addStatement(
                "this.simpleCall<${
                    output?.build()?.toString()?.replace("output: ", "") ?: "Void"
                }>(\"${endPoint}\","
            )
            funcBuilder.addStatement(if (input != null) "input)" else ")")
            funcBuilder.addStatement("{ callback(${if (output != null) "it" else ""}) }")
            FileSpec.builder("ir.ayantech.networking", "APIs")
                .addFunction(funcBuilder.build()).build()
                .writeTo(File(kaptKotlinGeneratedDir))
        }
        return true
    }

    override fun getSupportedAnnotationTypes(): MutableSet<String> =
        mutableSetOf(AyanAPI::class.java.canonicalName)
}