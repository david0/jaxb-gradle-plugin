/*
 * Copyright 2019 Intershop Communications AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intershop.gradle.jaxb

import com.intershop.gradle.jaxb.extension.JavaToSchema
import com.intershop.gradle.jaxb.extension.JaxbExtension
import com.intershop.gradle.jaxb.extension.SchemaToJava
import com.intershop.gradle.jaxb.task.JavaToSchemaTask
import com.intershop.gradle.jaxb.task.SchemaToJavaTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.DependencySet
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaPluginConvention

/**
 * Plugin Class implementation.
 */
open class JaxbPlugin: Plugin<Project> {

    /**
     * Applies the extension and calls the
     * task initialization for this plugin.
     *
     * @param project current project
     */
    override fun apply(project: Project) {

        with(project) {
            logger.info("Add extension {} for {}", JaxbExtension.JAXB_EXTENSION_NAME, name)
            val extension = extensions.findByType(
                    JaxbExtension::class.java
            ) ?: extensions.create( JaxbExtension.JAXB_EXTENSION_NAME, JaxbExtension::class.java )

            addJaxbConfiguration(this)

            val jaxbTask = tasks.maybeCreate("jaxb")
            jaxbTask.group = JaxbExtension.JAXB_TASK_GROUP
            jaxbTask.description = "JAXB code generation tasks"

            configureJavaCodeGenTasks(this, extension, jaxbTask)
            configureSchemaCodeGenTasks(this, extension, jaxbTask)
        }
    }

    /*
     * Configures tasks for java code generation.
     *
     * @param project       current project
     * @param configuration configuration for jaxb dependencies
     * @param extension     plugin extension
     * @param jaxbTask      the main jaxb task
     */
    private fun configureJavaCodeGenTasks(project: Project,
                                          extension: JaxbExtension,
                                          jaxbTask: Task) {
        extension.javaGen.all({ schemaToJava: SchemaToJava ->
            project.tasks.create(schemaToJava.taskName,
                                 SchemaToJavaTask::class.java).apply {
                description = "Generate java code for " + schemaToJava.name
                group = JaxbExtension.JAXB_TASK_GROUP

                provideOutputDir(schemaToJava.outputDirProvider)
                provideEncoding(schemaToJava.encodingProvider)
                provideStrictValidation(schemaToJava.strictValidationProvider)
                provideExtension(schemaToJava.extensionProvider)
                provideHeader(schemaToJava.headerProvider)
                providePackageName(schemaToJava.packageNameProvider)
                provideLanguage(schemaToJava.languageProvider)
                provideTargetVersion(schemaToJava.targetVersionProvider)
                provideArgs(schemaToJava.argsProvider)

                provideSchema(schemaToJava.schemaProvider)
                provideBinding(schemaToJava.bindingProvider)
                provideCatalog(schemaToJava.catalogProvider)

                schemas = schemaToJava.schemas
                bindings = schemaToJava.bindings

                provideAntTaskClassName(schemaToJava.antTaskClassNameProvider)

                project.afterEvaluate {
                    project.plugins.withType(JavaBasePlugin::class.java) {
                        val javaPluginConvention = project.convention.getPlugin(JavaPluginConvention::class.java)
                        javaPluginConvention.sourceSets.matching {
                            it.name == schemaToJava.sourceSetName
                        }.forEach {
                            it.java.srcDir(this@apply.outputs)
                        }
                    }
                }

                jaxbTask.dependsOn(this)
            }
        })
    }

    /*
     * Configures tasks for schema generation.
     *
     * @param project       current project
     * @param configuration configuration for jaxb dependencies
     * @param extension     plugin extension
     * @param jaxbTask      the main jaxb task
     */
    private fun configureSchemaCodeGenTasks(project: Project,
                                                 extension: JaxbExtension,
                                                 jaxbTask: Task) {
        extension.schemaGen.all({ javaToSchema: JavaToSchema ->
            project.tasks.create(javaToSchema.taskName,
                                 JavaToSchemaTask::class.java).apply {
                description = "Generate Schema for " + javaToSchema.name
                group = JaxbExtension.JAXB_TASK_GROUP
                provideOutputDir(javaToSchema.outputDirProvider)
                provideInputDir(javaToSchema.inputDirProvider)
                provideExcludes(javaToSchema.excludesProvider)
                provideIncludes(javaToSchema.includesProvider)
                provideNamespaceconfigs(javaToSchema.namespaceconfigsProvider)
                provideEpisode(javaToSchema.episodeProvider)
                provideAntTaskClassName(javaToSchema.antTaskClassNameProvider)

                jaxbTask.dependsOn(this)
            }
        })
    }

    /*
     * Adds the dependencies for the code generation. It is possible to override this.
     *
     * @param project   current project
     */
    private fun addJaxbConfiguration(project: Project) {
        val configuration = project.configurations.maybeCreate(JaxbExtension.JAXB_CONFIGURATION_NAME)
        configuration
                .setVisible(false)
                .setTransitive(false)
                .setDescription("Jaxb configuration is used for code generation")
                .defaultDependencies { dependencies: DependencySet ->
                    // this will be executed if configuration is empty
                    val dependencyHandler = project.dependencies
                    dependencies.add(dependencyHandler.create("org.glassfish.jaxb:jaxb-runtime:2.3.2"))
                    dependencies.add(dependencyHandler.create("com.sun.xml.bind:jaxb-jxc:2.2.11"))
                    dependencies.add(dependencyHandler.create("com.sun.xml.bind:jaxb-xjc:2.2.11"))
                    dependencies.add(dependencyHandler.create("com.sun.xml.bind:jaxb-core:2.2.11"))
                }

        project.configurations.maybeCreate(JaxbExtension.ADD_JAXB_CONFIGURATION_NAME)
    }
}
