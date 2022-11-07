package com.alirezabdn.generator

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import java.io.File
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
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

        val functions = ArrayList<FunSpec.Builder>()
        roundEnv?.getElementsAnnotatedWith(AyanAPI::class.java)?.forEach { element ->
            val input = element.enclosedElements.let {
                it.firstOrNull { it.simpleName.endsWith("Input") }?.let {
                    ParameterSpec.builder("input", it.asType().asTypeName())
                }
            }
            var outputClass: Element? = null
            val nullableOutput = element.enclosedElements.let {
                it.firstOrNull { it.simpleName.endsWith("Output") }?.let {
                    outputClass = it
                    ParameterSpec.builder("output", it.asType().asTypeName().copy(nullable = true))
                }
            }
            val nonNullableOutput = element.enclosedElements.let {
                it.firstOrNull { it.simpleName.endsWith("Output") }?.let {
                    ParameterSpec.builder("output", it.asType().asTypeName())
                }
            }

            val endPoint =
                element.getAnnotation(AyanAPI::class.java).endPoint.ifEmpty { element.simpleName }
            val endPointParameterSpec = ParameterSpec.builder("endPoint", (String::class)).defaultValue("%S", "$endPoint")

            //------------------------------create simple call -------------------------------------
            val simpleCallFuncBuilder = FunSpec.builder("simpleCall" + element.simpleName)
                .receiver(Class.forName("ir.ayantech.ayannetworking.api.AyanApi"))
            if (input != null) simpleCallFuncBuilder.addParameter(input.build())
            simpleCallFuncBuilder.addParameter(endPointParameterSpec.build())
            simpleCallFuncBuilder.addParameter(
                ParameterSpec.builder(
                    "callback", LambdaTypeName.get(
                        parameters = if (nullableOutput != null) arrayOf(nullableOutput.build()) else arrayOf(),
                        returnType = Unit::class.asClassName()
                    )
                ).build()
            )
            simpleCallFuncBuilder.addStatement(
                "this.simpleCall<${
                    nullableOutput?.build()?.toString()?.replace("output: ", "") ?: "Void"
                }>(endPoint,"
            )
            simpleCallFuncBuilder.addStatement(if (input != null) "input)" else ")")
            simpleCallFuncBuilder.addStatement("{ callback(${if (nullableOutput != null) "it" else ""}) }")
            //--------------------------------------------------------------------------------------

            //-----------------------------------create call ---------------------------------------
            val callFuncBuilder = FunSpec.builder("call" + element.simpleName)
                .receiver(Class.forName("ir.ayantech.ayannetworking.api.AyanApi"))
            if (input != null) callFuncBuilder.addParameter(input.build())
            callFuncBuilder.addParameter(endPointParameterSpec.build())
            callFuncBuilder.addParameter(
                ParameterSpec.builder(
                    "callback", LambdaTypeName.get(
                        parameters = arrayOf<ParameterSpec>(),
                        returnType = Unit::class.asClassName(),
                        receiver = Class.forName("ir.ayantech.ayannetworking.api.AyanApiCallback")
                            .asClassName().parameterizedBy(
                                outputClass?.asType()?.asTypeName() ?: Void::class.java.asTypeName()
                            )
                    )
                ).build()
            )
            callFuncBuilder.addStatement(
                "this.call<${
                    nonNullableOutput?.build()?.toString()?.replace("output: ", "") ?: "Void"
                }>(endPoint,"
            )
            callFuncBuilder.addStatement(if (input != null) "input" else "null")
            callFuncBuilder.addStatement(",callback)")
            //--------------------------------------------------------------------------------------

            functions.add(simpleCallFuncBuilder)
            functions.add(callFuncBuilder)
        }
        if (functions.isNotEmpty())
            FileSpec.builder("ir.ayantech.networking", "APIs").also { fileBuilder ->
                functions.forEach { fileBuilder.addFunction(it.build()) }
            }.build().writeTo(File(kaptKotlinGeneratedDir))
        return true
    }

    override fun getSupportedAnnotationTypes(): MutableSet<String> =
        mutableSetOf(AyanAPI::class.java.canonicalName)
}